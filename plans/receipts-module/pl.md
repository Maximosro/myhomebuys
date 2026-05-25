# Plan: receipts-module

## Contexto

Primer servicio de myhomebuys. Se necesita un módulo capaz de recibir PDFs de tickets de compra, parsearlos y almacenar los datos estructurados en BD. El primer parser soporta tickets de Mercadona online. La arquitectura debe ser modular para añadir más comercios en el futuro (Carrefour, Lidl, etc.).

## Decisiones del usuario (confirmadas en research)

- Package: `com.sro.myhomebuys.receipts`
- El usuario indica el store manualmente (`?store=MERCADONA`). Si el parser falla, error genérico de parseo.
- PDF parsing: Apache PDFBox 3.x
- BD: entidad común `Receipt` (id UUID, store enum, date, total) + tabla propia por comercio. Mercadona → `MercadonaItem` (product_name, quantity, price_per_unit). Sin IVA.
- Hibernate dialect para SQLite: `hibernate-community-dialects`

## Dependencias nuevas

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.3</version>
</dependency>
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-community-dialects</artifactId>
</dependency>
```

## Configuración (`application.yaml`)

```yaml
spring:
  application:
    name: myhomebuys
  datasource:
    url: jdbc:sqlite:myhomebuys.db
    driver-class-name: org.sqlite.JDBC
  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false
    properties:
      hibernate:
        dialect: org.hibernate.community.dialect.SQLiteDialect
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
```

## Arquitectura

```
com.sro.myhomebuys.receipts/
  model/
    Store.java                    ← enum { MERCADONA }
    Receipt.java                  ← entidad común (id UUID, store, date, total)
    MercadonaItem.java            ← entidad items Mercadona (FK receipt_id, product_name, quantity, price_per_unit)
  parser/
    ReceiptParser.java            ← interfaz: canHandle(String store), parse(InputStream)
    ParsedReceipt.java            ← DTO interno (store, date, total, List<ParsedItem>)
    MercadonaOnlineParser.java    ← implementación PDFBox + regex
  repository/
    ReceiptRepository.java        ← JpaRepository<Receipt, UUID>
    MercadonaItemRepository.java  ← JpaRepository<MercadonaItem, Long>
  service/
    ReceiptParserRegistry.java    ← descubre parsers (List<ReceiptParser> inyectada)
    ReceiptService.java           ← orquesta: registry → parser → save → response
  controller/
    ReceiptController.java        ← POST /upload, GET /, GET /{id}
    dto/
      ReceiptResponse.java
      ReceiptListResponse.java
      ErrorResponse.java
  exception/
    ReceiptParsingException.java
    UnsupportedStoreException.java
    GlobalExceptionHandler.java
```

## Endpoints

| Método | Path | Respuesta |
|--------|------|-----------|
| POST | `/api/receipts/upload?store=MERCADONA` (multipart: file) | 201 + `ReceiptResponse` con items |
| GET | `/api/receipts` | 200 + `List<ReceiptListResponse>` (sin items) |
| GET | `/api/receipts/{id}` | 200 + `ReceiptResponse` con items |

## Estrategia de parseo (MercadonaOnlineParser)

### Extracción de texto

PDFBox `Loader.loadPDF()` + `PDFTextStripper.getText()` (con `sortByPosition=true`, por defecto). El texto extraído mantiene el orden de lectura — cada campo en su propia línea, con líneas vacías entre ellos.

### Labels exactos del PDF

El parser usará estos marcadores textuales (extraídos del PDF real `Receipt-30267537.pdf`):

| Marcador | Qué hace |
|----------|----------|
| `Cobrado el ` | Inicia la línea con la fecha: `Cobrado el 07/05/26 a las 03:26` |
| `Nombre Producto` | Inicio del bloque de columnas de items |
| `Cantidad entregada` | Segunda cabecera de columna (confirma sección de items) |
| `PVP` | Tercera cabecera de columna (confirma sección de items) |
| `Productos` | Subtotal de productos (fin de la lista de items) |
| `TOTAL` | Total del ticket (valor en la línea siguiente) |
| `Desglose de impuestos` | Inicio del bloque de IVA (fin definitivo de la sección de items) |
| `MERCADONA.S.A` | Inicio del texto legal que separa página 1 y 2 |

### Parseo de fecha

- Buscar línea que empiece por `Cobrado el `
- Ejemplo real: `Cobrado el 07/05/26 a las 03:26`
- Extraer con regex: `Cobrado el (\d{2})/(\d{2})/(\d{2})`
- Formato: `dd/MM/yy` → `LocalDate` (ej: 07/05/26 → 2026-05-07)

### Parseo de items

El PDF de Mercadona NO tiene los productos en formato tabla horizontal. Los datos están en vertical: cada producto ocupa 3 líneas no vacías separadas por líneas vacías:

```
Nombre Producto      ← cabecera, ignorar
                     ← línea vacía
Cantidad entregada   ← cabecera, ignorar
                     ← línea vacía
PVP                  ← cabecera, ignorar
                     ← línea vacía
Bolsas de basura...  ← línea 1: nombre del producto
                     ← línea vacía
1                    ← línea 2: cantidad (entero solo)
                     ← línea vacía
1,40 €               ← línea 3: precio (con coma y €)
                     ← línea vacía
Patatas bravas...    ← siguiente producto...
2
5,20 €
...
Productos            ← marca de fin de items
138,23 €
Coste de preparación
8,20 €
TOTAL
146,43 €
```

**Algoritmo:**
1. Localizar la línea `Nombre Producto` y confirmar que las 2 siguientes cabeceras son `Cantidad entregada` y `PVP` (con líneas vacías entre ellas).
2. Tras confirmar las cabeceras, avanzar hasta la primera línea no vacía → es el nombre del primer producto.
3. Bucle de lectura de 3 líneas:
   - **Línea N (nombre)**: no vacía, es el nombre del producto. Si es `Productos`, `TOTAL`, o `Desglose de impuestos` → fin de items.
   - **Línea N+1 (cantidad)**: número entero (tras saltar línea vacía). Validar con `\d+`.
   - **Línea N+2 (precio)**: patrón `\d{1,3}[.,]\d{2}\s*€?` (tras saltar línea vacía). Reemplazar `,` por `.` para `BigDecimal`.
4. Si `Productos` aparece **después** de un nombre de producto (en vez de después del salto de línea), volver atrás: esa línea no era un producto, era el marcador de fin.

**Segunda página:** El PDF tiene 2 páginas. Entre página 1 y 2 aparece el texto legal (`MERCADONA.S.A | A-46103834 | ...`) y se repiten las cabeceras (`Nombre Producto`, `Cantidad entregada`, `PVP`). El algoritmo debe ignorar el bloque legal y volver a saltar las cabeceras repetidas para seguir leyendo items.

### Parseo del total

- Localizar la línea cuyo contenido sea exactamente `TOTAL` (trimmed).
- El valor está en la línea siguiente (no vacía): `146,43 €`
- Extraer con regex: `(\d{1,3}[.,]\d{2})`, reemplazar `,` → `.`, parsear `BigDecimal`.

## Errores

| Excepción | HTTP | Código |
|-----------|------|--------|
| `ReceiptParsingException` | 400 | PARSE_ERROR |
| `UnsupportedStoreException` | 400 | UNSUPPORTED_STORE |
| `MissingServletRequestPartException` | 400 | MISSING_FILE |
| Genérica | 500 | INTERNAL_ERROR |

Mapeadas en `@RestControllerAdvice` → `GlobalExceptionHandler`.

## Issues

- [ ] 1. Añadir dependencias (pdfbox, hibernate-community-dialects) y configurar application.yaml
- [ ] 2. Crear modelo: Store enum, Receipt entity, MercadonaItem entity
- [ ] 3. Crear interfaz ReceiptParser y DTO ParsedReceipt
- [ ] 4. Implementar MercadonaOnlineParser (PDFBox + regex)
- [ ] 5. Crear repositorios JPA (ReceiptRepository, MercadonaItemRepository)
- [ ] 6. Crear ReceiptParserRegistry + ReceiptService (orquestación + @Transactional)
- [ ] 7. Crear DTOs de respuesta (ReceiptResponse, ReceiptListResponse, ErrorResponse)
- [ ] 8. Crear excepciones y GlobalExceptionHandler
- [ ] 9. Crear ReceiptController con los 3 endpoints
- [ ] 10. Tests unitarios del parser (MercadonaOnlineParserTest)
- [ ] 11. Tests de integración (controller + service)

## Rama

`feature/receipts-module`

## Archivos

| Acción | Archivo |
|--------|---------|
| MODIFICAR | `pom.xml` |
| MODIFICAR | `src/main/resources/application.yaml` |
| CREAR | `src/main/java/.../receipts/model/Store.java` |
| CREAR | `src/main/java/.../receipts/model/Receipt.java` |
| CREAR | `src/main/java/.../receipts/model/MercadonaItem.java` |
| CREAR | `src/main/java/.../receipts/parser/ReceiptParser.java` |
| CREAR | `src/main/java/.../receipts/parser/ParsedReceipt.java` |
| CREAR | `src/main/java/.../receipts/parser/MercadonaOnlineParser.java` |
| CREAR | `src/main/java/.../receipts/repository/ReceiptRepository.java` |
| CREAR | `src/main/java/.../receipts/repository/MercadonaItemRepository.java` |
| CREAR | `src/main/java/.../receipts/service/ReceiptParserRegistry.java` |
| CREAR | `src/main/java/.../receipts/service/ReceiptService.java` |
| CREAR | `src/main/java/.../receipts/controller/ReceiptController.java` |
| CREAR | `src/main/java/.../receipts/controller/dto/ReceiptResponse.java` |
| CREAR | `src/main/java/.../receipts/controller/dto/ReceiptListResponse.java` |
| CREAR | `src/main/java/.../receipts/controller/dto/ErrorResponse.java` |
| CREAR | `src/main/java/.../receipts/exception/ReceiptParsingException.java` |
| CREAR | `src/main/java/.../receipts/exception/UnsupportedStoreException.java` |
| CREAR | `src/main/java/.../receipts/exception/GlobalExceptionHandler.java` |
| CREAR | `src/test/java/.../receipts/parser/MercadonaOnlineParserTest.java` |
| CREAR | `src/test/java/.../receipts/controller/ReceiptControllerTest.java` |

## Verificación

1. Arrancar la app con `./mvnw spring-boot:run` — debe crear las tablas en `myhomebuys.db`
2. `curl -X POST "http://localhost:8080/api/receipts/upload?store=MERCADONA" -F "file=@Receipt-30267537.pdf"` → 201 con el receipt parseado y sus 26 items
3. `curl "http://localhost:8080/api/receipts"` → 200 con el listado
4. `curl "http://localhost:8080/api/receipts/{id}"` → 200 con el detalle
5. `curl -X POST ".../upload?store=LIDL" -F "file=@..."` → 400 UNSUPPORTED_STORE
6. `curl -X POST ".../upload?store=MERCADONA" -F "file=@foto.jpg"` → 400 PARSE_ERROR
