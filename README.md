# Event Ticketing Platform (eventos)

> **En desarrollo.** Esta documentación es temporal. La API, el modelo de datos y la configuración pueden cambiar sin previo aviso.

API REST para gestionar eventos, sus tipos de entrada (GENERAL / VIP) con control de aforo, usuarios y compras de entradas.

## Stack

| Área            | Tecnología                                   |
|-----------------|----------------------------------------------|
| Lenguaje        | Java 25                                      |
| Framework       | Spring Boot 4.1.1 (Web MVC, Data JPA, Security, Validation, Actuator) |
| Build           | Gradle (wrapper incluido)                    |
| Documentación API | springdoc-openapi 3.1.0 (Swagger UI)       |
| Base de datos   | H2 en memoria (local) · PostgreSQL 17 (Docker) |
| Mensajería      | Apache Kafka 4.0.1 (Docker)                  |
| Otros           | Lombok                                       |

## Estado actual

| Funcionalidad | Estado | Notas |
|---|---|---|
| CRUD de eventos con tipos de entrada | ✅ | Filtros opcionales por `ciudad` (sin distinguir mayúsculas) y `fecha` |
| Reglas de negocio al editar eventos | ✅ | No se puede reducir el aforo por debajo de lo vendido, ni eliminar un tipo con ventas, ni repetir tipos |
| Compra de entradas | ✅ | `POST /compras` con control de aforo y bloqueo pesimista frente a compras concurrentes |
| Usuarios | 🚧 | Entidad y repositorio listos; aún sin endpoint de registro |
| Estados de compra | 🚧 | Las compras se crean `CONFIRMADA`; `PENDIENTE` (pago) y `CANCELADA` (devolución de aforo) pendientes |
| Kafka | 🚧 | Dependencia y broker configurados; aún sin productores/consumidores |
| Seguridad | 🚧 | HTTP Basic (`admin`/`admin` por defecto). `/eventos/**`, Swagger y `/actuator/health`/`info` son públicos; `/compras` requiere autenticación |

## Arranque rápido

### Local (H2 en memoria)

```bash
./gradlew bootRun        # Linux / macOS
gradlew.bat bootRun      # Windows
```

La aplicación arranca en `http://localhost:8080`. Por defecto Kafka apunta a `localhost:9092` (configurable con `SPRING_KAFKA_BOOTSTRAP_SERVERS`).

### Docker (app + PostgreSQL + Kafka)

```bash
cp .env.example .env
docker compose up --build
```

Variables disponibles en `.env`:

| Variable | Por defecto | Descripción |
|---|---|---|
| `APP_PORT` | `8080` | Puerto expuesto de la aplicación |
| `POSTGRES_PORT` | `5432` | Puerto expuesto de PostgreSQL |
| `POSTGRES_DB` | `eventos` | Nombre de la base de datos |
| `POSTGRES_USER` | `eventos` | Usuario de PostgreSQL |
| `POSTGRES_PASSWORD` | `eventos` | Contraseña de PostgreSQL |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` | Estrategia de esquema de Hibernate |
| `KAFKA_PORT` | `29092` | Puerto externo de Kafka |

## API

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI: http://localhost:8080/v3/api-docs
- Health: http://localhost:8080/actuator/health

### Eventos

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| `GET` | `/eventos` | Lista eventos. Filtros opcionales: `ciudad`, `fecha` (`YYYY-MM-DD`) | `200` |
| `GET` | `/eventos/{id}` | Obtiene un evento con sus tipos de entrada | `200` / `404` |
| `POST` | `/eventos` | Crea un evento | `201` / `400` |
| `PUT` | `/eventos/{id}` | Reemplaza los datos de un evento | `200` / `400` / `404` / `409` |
| `DELETE` | `/eventos/{id}` | Elimina un evento | `204` / `404` |

Ejemplo de petición (`POST /eventos`):

```json
{
  "nombre": "Concierto de verano",
  "descripcion": "Festival al aire libre",
  "fecha": "2026-12-01",
  "ciudad": "Madrid",
  "lugar": "Estadio Central",
  "tipoEntrada": [
    { "nombre": "GENERAL", "precio": 35.00, "aforoTotal": 500 },
    { "nombre": "VIP", "precio": 90.00, "aforoTotal": 50 }
  ]
}
```

Ejemplo de filtro:

```
GET /eventos?ciudad=Madrid&fecha=2026-12-01
```

### Compras

Requiere autenticación HTTP Basic (credenciales configurables con `SPRING_SECURITY_USER_NAME` / `SPRING_SECURITY_USER_PASSWORD`).

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| `POST` | `/compras` | Compra entradas de un tipo y descuenta el aforo | `201` / `400` / `401` / `404` / `409` |

Ejemplo de petición:

```json
{ "usuarioId": 1, "tipoEntradaId": 1, "cantidad": 3 }
```

Ejemplo de respuesta (`201`):

```json
{
  "id": 1,
  "usuarioId": 1,
  "eventoId": 1,
  "tipoEntradaId": 1,
  "tipoEntrada": "GENERAL",
  "cantidad": 3,
  "precioUnitario": 35.00,
  "total": 105.00,
  "estado": "CONFIRMADA",
  "fecha": "2026-10-13T12:00:00"
}
```

El precio unitario se copia del tipo de entrada en el momento de la compra, así que un cambio de precio posterior no altera las compras existentes.

### Errores

| Código | Cuándo |
|---|---|
| `400` | Validación fallida o tipo de entrada repetido |
| `404` | Evento, usuario o tipo de entrada no encontrado |
| `409` | Aforo insuficiente o edición que entra en conflicto con entradas ya vendidas |

## Modelo de dominio

```mermaid
erDiagram
    EVENTO ||--o{ TIPO_ENTRADA : tiene
    TIPO_ENTRADA ||--o{ COMPRA : "se compra en"
    USUARIO ||--o{ COMPRA : realiza

    EVENTO {
        Long id
        String nombre
        String descripcion
        LocalDate fecha
        String ciudad
        String lugar
    }
    TIPO_ENTRADA {
        Long id
        TiposEntrada nombre "GENERAL | VIP"
        BigDecimal precio
        Integer aforoTotal
        Integer aforoDisponible
    }
    USUARIO {
        Long id
        String nombre
        String email "único"
    }
    COMPRA {
        Long id
        Integer cantidad
        EstadoCompra estado "PENDIENTE | CONFIRMADA | CANCELADA"
        LocalDateTime fecha
        BigDecimal precioUnitario
    }
```

## Concurrencia en las compras

**Problema.** La versión inicial de `CompraService.comprar` leía el aforo disponible, comprobaba que había suficiente y lo descontaba. Con varias compras simultáneas del mismo tipo de entrada, todas leían el mismo aforo antes de que ninguna confirmara, todas pasaban la comprobación y cada una sobrescribía el valor de la anterior (*lost update*). Resultado: se vendían más entradas que el aforo y el aforo disponible no cuadraba con las compras guardadas.

**Cómo se demuestra.** `CompraServiceIntegrationTests.comprasConcurrentesNoVendenMasQueElAforo` lanza 20 compras de 1 entrada a la vez (sincronizadas con un `CountDownLatch`) sobre un tipo con aforo 10, y comprueba que:

- exactamente 10 compras tienen éxito y 10 se rechazan con `AforoInsuficienteException` (`409`);
- el aforo disponible queda en 0 y hay 10 compras guardadas;
- se cumple el invariante `aforo disponible + entradas compradas = aforo total`.

Con la versión inicial el test falla; con el bloqueo pasa.

**Solución.** El tipo de entrada se lee con bloqueo pesimista (`@Lock(LockModeType.PESSIMISTIC_WRITE)` en `TipoEntradaRepository.findByIdForUpdate`, que genera un `SELECT ... FOR UPDATE`). Las compras del mismo tipo de entrada esperan su turno en la base de datos y cada una ve el aforo ya descontado por la anterior. Se eligió el bloqueo pesimista en lugar del optimista (`@Version`) porque la venta de entradas tiene mucha contención sobre la misma fila: con el optimista, la mayoría de compras fallarían y habría que reintentarlas.

## Estructura del proyecto

```
src/main/java/com/jnrptt/eventos/
├── config/       # Configuración de seguridad
├── controller/   # Controladores REST
├── dto/          # Objetos de petición y respuesta
├── exception/    # Excepciones propias y manejador global
├── model/        # Entidades JPA y enums
├── repository/   # Repositorios Spring Data y Specifications
└── service/      # Lógica de negocio
```

## Tests

```bash
./gradlew test
```

Tests de integración con `@SpringBootTest` sobre H2 en memoria.

## Próximos pasos (tentativo)

- [x] Endpoint de compras con control de aforo y concurrencia
- [ ] Autenticación con JWT, registro/login y roles (usuario / organizador); el `usuarioId` de la compra saldrá del token
- [ ] Publicación de eventos de compra en Kafka
- [ ] Estados de compra `PENDIENTE` (pago simulado) y `CANCELADA` (devuelve aforo)
