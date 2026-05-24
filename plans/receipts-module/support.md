# Support: receipts-module

## Preguntas sin contestar

- Ninguna pendiente. Todas las decisiones fueron resueltas en la fase de research.

## Cosas no comprobadas

- No se ha verificado si `hibernate-community-dialects` es compatible con la versión exacta de Hibernate que usa Spring Boot 4.0.6. Puede requerir ajuste de versión o nombre de clase del dialect.
- No se ha comprobado si SQLite + `GenerationType.UUID` funciona con el dialect — puede necesitar un `AttributeConverter<UUID, String>`.

## Scope fuera de esta iteración

- IVA / impuestos — el usuario lo descartó explícitamente
- Parsers para otros comercios (Carrefour, Lidl)
- Autenticación / autorización
- Paginación en el listado
- Eliminación de receipts
