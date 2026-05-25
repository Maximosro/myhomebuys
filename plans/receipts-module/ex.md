# Research: receipts-module

## Problema

Crear un servicio que reciba por el controller un PDF de ticket de compra de Mercadona, lo lea, lo formatee y lo almacene en BD como datos estructurados (no blob). Luego exponer una API para consultarlo. Es el primer servicio de varios comercios — la arquitectura debe ser modular desde el día 1.

## Código / assets relevantes

- `pom.xml` — Spring Boot 4.0.6, Java 21, JPA, SQLite, Lombok. Proyecto esqueleto.
- `MyhomebuysApplication.java` — entry point estándar, sin beans extra.
- `application.yaml` — solo `spring.application.name: myhomebuys`. Sin config de datasource ni dialect.
- `Receipt-30267537.pdf` — PDF de Mercadona online generado con WeasyPrint. 2 páginas, texto extraíble con pdftotext. 26 productos.

## Flujo actual

No hay flujo — proyecto vacío. Se implementa desde cero:

```
POST /api/receipts/upload?store=MERCADONA (PDF multipart)
  → ReceiptController
    → ReceiptService
      → ReceiptParserRegistry.findParser(store)
      → parser.parse(pdfStream) → ParsedReceipt DTO
      → save: Receipt entity + MercadonaItem entities (transaccional)
      → ReceiptResponse (201)
```

## Estructura del PDF de Mercadona

- **Cabecera**: Pedido Nº, Factura simplificada, fecha entrega, dirección, cliente (nombre, teléfono, email)
- **Pago**: MasterCard ****, AUT, fecha cobro
- **Tabla de productos**: nombre producto | cantidad entregada | PVP
- **Totales**: Productos X€ + Coste de preparación Y€ = TOTAL Z€
- **Desglose IVA**: 21%, 10%, 4% (no se almacena en esta iteración)

## Patrones existentes

Ninguno — es un proyecto nuevo. Se usan patrones estándar de Spring Boot:
- `@RestController` + `@RequestMapping`
- `@Service` + `@Transactional`
- `JpaRepository` con derived query methods
- `@RestControllerAdvice` para manejo global de errores

## Preguntas / Decisiones

- **Nombre del módulo** → `receipts`
- **Detección del parser** → el usuario pasa el store manualmente como query param. Si falla el parseo, error sin más.
- **Librería PDF** → Apache PDFBox 3.x
- **Modelo de datos** → entidad común `Receipt` (id UUID, store enum, date, total). Tabla propia por comercio: `MercadonaItem` (receipt_id FK, product_name, quantity, price_per_unit). Sin IVA.
