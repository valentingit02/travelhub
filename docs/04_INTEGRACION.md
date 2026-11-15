# 4. Integración: contratos, mensajería y procesos

## 4.1 Contratos REST (OpenAPI)
Los contratos exportados están en [`docs/contratos/`](contratos/). Con el sistema levantado también se pueden
probar desde Swagger UI.

| Servicio | Contrato | Swagger UI | Operaciones principales |
|---|---|---|---|
| catalogo | [openapi-catalogo.json](contratos/openapi-catalogo.json) | http://localhost:8081/swagger-ui.html | `GET /api/catalogo/buscar`, `GET /ofertas` (verificar), `POST/DELETE /reservas-proveedor`, CRUD de autos, aprobar ofertas bloqueadas |
| precios | [openapi-precios.json](contratos/openapi-precios.json) | http://localhost:8082/swagger-ui.html | `POST /api/precios/cotizar`, `POST /api/precios/cotizar-paquete` |
| reservas | [openapi-reservas.json](contratos/openapi-reservas.json) | http://localhost:8083/swagger-ui.html | `/api/auth/*`, `/api/viajeros/me`, `POST/GET/DELETE /api/reservas`, viaje compartido, créditos, seguimientos |
| pagos | [openapi-pagos.json](contratos/openapi-pagos.json) | http://localhost:8084/swagger-ui.html | `GET /api/pagos` (consulta; los cobros entran por la cola) |
| notificaciones | [openapi-notificaciones.json](contratos/openapi-notificaciones.json) | http://localhost:8085/swagger-ui.html | `GET /api/notificaciones` (últimos envíos), `POST /prueba` |
| ia | [openapi-ia.json](contratos/openapi-ia.json) | http://localhost:8000/docs | anomalías, resumen, recomendaciones, asistente, kit |

**Convenciones**
- JSON, fechas ISO-8601, montos en USD con 2 decimales.
- Errores con un formato único (`ApiError`): `status`, `error`, `mensaje`, `correlationId`, `timestamp`, `detalles`.
  400 datos inválidos · 401 sin sesión · 403 recurso ajeno · 404 no existe · 409 conflicto (ej. oferta vencida) · 502 falla de un proveedor.
- Las rutas de reservas, créditos, seguimientos y perfil piden `Authorization: Bearer <JWT>` (ver [PR_LOGIN.md](PR_LOGIN.md)).
- `POST /api/reservas` acepta `Idempotency-Key`.

## 4.2 Contrato SOAP
- **Servicio:** `precios-service`, endpoint `http://localhost:8082/ws` (también por el gateway en `:8080/ws`).
- **WSDL:** http://localhost:8082/ws/cotizacion.wsdl · copia en [contratos/cotizacion.wsdl](contratos/cotizacion.wsdl).
- **Esquema (contrato primero):** [contratos/cotizacion.xsd](contratos/cotizacion.xsd). El WSDL se genera a partir del XSD.
- **Namespace:** `http://travelhub.com/precios/soap`

| Operación | Request | Response |
|---|---|---|
| `CotizarProducto` | `item`: tipoProducto, precioBase, moneda, fechaInicio, fechaFin, pasajeros, ocupacion | precio final y desglose de factores (`estrategia`, `factor`, `motivo`) |
| `CotizarPaquete` | lista de `item` | total por ítem, subtotal, descuento y total del paquete |

Errores: un dato inválido devuelve un `SOAP-ENV:Fault` con `faultcode` `Client` y el motivo (ej. "precioBase debe ser positivo").
Ejemplos listos para SoapUI o Postman: [`tests/soap/`](../tests/soap/).

## 4.3 Mensajería
**Broker:** RabbitMQ 3.13 · **exchange:** `travelhub.events` (tipo *topic*, durable) · **dead letter:** exchange `travelhub.dlx` → cola `travelhub.dlq`.
La tabla completa de eventos, colas y consumidores está en el [README](../README.md#eventos).

### Configuración de colas
| Parámetro | Valor | Dónde |
|---|---|---|
| Colas | Durables, una por consumidor (`<servicio>.<evento>`) | `*RabbitConfig` de cada servicio |
| Formato | JSON (`Jackson2JsonMessageConverter`) | `MensajeriaConfig` |
| Reintentos | 3 intentos, backoff de 1 s multiplicado por 2 | `spring.rabbitmq.listener.simple.retry` |
| Mensaje que sigue fallando | No se reencola: va a `travelhub.dlx` → `travelhub.dlq` | `x-dead-letter-exchange` en cada cola |
| Publicación desde reservas | Outbox: tabla `outbox_evento`, relay cada 1 s, en orden | `OutboxRelay` |
| Consumidor Python (ia) | `pika`, `basic_nack(requeue=False)` ante error → DLQ | `ia-service/app/mensajeria.py` |
| Trazas | El `traceId` viaja en los headers del mensaje (Micrometer) | `observation-enabled: true` |

### Flujo de mensajes
```mermaid
flowchart LR
    RES[reservas] -- pago.solicitado --> PAG[pagos]
    PAG -- pago.aprobado / pago.rechazado --> RES
    RES -- reembolso.solicitado --> PAG
    RES -- reserva.confirmada --> NOT[notificaciones]
    RES -- reserva.cancelada --> NOT
    RES -- reserva.cancelada --> CAT[catalogo]
    RES -- invitacion.viaje --> NOT
    RES -- credito.otorgado --> NOT
    RES -- alerta.precio --> NOT
    CAT -- precio.cambiado --> IA[ia]
    CAT -- precio.cambiado --> RES
    IA -- anomalia.detectada --> CAT
    PAG & RES & NOT & CAT & IA -. falla 3 veces .-> DLQ[(travelhub.dlq)]
```

## 4.4 Proceso de negocio: reservar un paquete (BPMN simplificado)
```mermaid
flowchart TD
    A([Viajero arma el paquete]) --> B{¿Tiene sesión?}
    B -- No --> B1[Ingresar o crear cuenta] --> C
    B -- Sí --> C[Verificar cada oferta en el catálogo<br/>precio, vigencia, disponibilidad]
    C --> D{¿Todas vigentes?}
    D -- No --> D1([409: volver a buscar])
    D -- Sí --> E[Cotizar el paquete en precios<br/>Strategy + Composite]
    E --> F[Reservar en cada proveedor<br/>Saga paso a paso]
    F --> G{¿Algún proveedor falló?}
    G -- Sí --> G1[Compensar: cancelar lo ya reservado] --> G2([Reserva FALLIDA + mail])
    G -- No --> H[Reserva PENDIENTE<br/>publicar pago.solicitado]
    H --> I{¿Es compartida?}
    I -- Sí --> I1[Mandar invitación a cada amigo<br/>con su link de pago]
    I -- No --> J
    I1 --> J[Cobrar cada parte<br/>pagos-service, asincrónico]
    J --> K{¿Pagaron todos<br/>antes del vencimiento?}
    K -- Sí --> L([Reserva CONFIRMADA<br/>mail con resumen de la IA])
    K -- Rechazo --> K1[Avisar y permitir reintentar] --> J
    K -- Venció --> M[Compensar y reembolsar lo pagado] --> N([Reserva FALLIDA + mail])
```

## 4.5 Diagramas de secuencia

### Reserva exitosa (sincrónico + asincrónico)
```mermaid
sequenceDiagram
    autonumber
    actor V as Viajero
    participant GW as gateway
    participant R as reservas
    participant C as catalogo
    participant P as precios
    participant DB as reservas_db
    participant MQ as RabbitMQ
    participant PG as pagos
    participant N as notificaciones
    participant IA as ia

    V->>GW: POST /api/reservas (JWT, ids de productos)
    GW->>R: proxy + X-Correlation-Id
    R->>R: validar JWT y dueño
    loop por cada producto
        R->>C: GET /api/catalogo/ofertas?id=... (verificar)
        C-->>R: precio en USD y vigencia
    end
    R->>P: POST /api/precios/cotizar-paquete
    P-->>R: total con estrategias y descuento
    loop Saga: por cada producto
        R->>C: POST /api/catalogo/reservas-proveedor
        C-->>R: referencia del proveedor
    end
    R->>DB: guardar reserva PENDIENTE + evento en outbox (misma transacción)
    R-->>V: 201 PENDIENTE
    R->>MQ: OutboxRelay publica pago.solicitado
    MQ->>PG: pagos.pago-solicitado
    PG->>PG: ProcesadorPago (Strategy)
    PG->>MQ: pago.aprobado
    MQ->>R: reservas.pago-aprobado
    R->>DB: CONFIRMADA + reserva.confirmada en outbox
    R->>MQ: reserva.confirmada
    MQ->>N: notificaciones.reserva-confirmada
    N->>IA: POST /api/ia/resumen
    IA-->>N: resumen (LLM o plantilla si tarda más de 5 s)
    N->>N: enviar mail (SMTP)
    V->>GW: GET /api/reservas (cada 2 s)
    GW-->>V: CONFIRMADA
```

### Falla de un proveedor: compensación (Saga)
```mermaid
sequenceDiagram
    autonumber
    participant R as reservas (ReservaSaga)
    participant C as catalogo
    participant MQ as RabbitMQ
    participant N as notificaciones
    R->>C: reservar VUELO
    C-->>R: OK (ref VUELO-123)
    R->>C: reservar HOTEL
    C-->>R: 502 sin disponibilidad
    Note over R: SagaLog: RESERVAR_HOTEL = FALLO
    R->>C: DELETE reservas-proveedor (VUELO-123)
    C-->>R: cancelado
    Note over R: SagaLog: COMPENSAR_VUELO = OK
    R->>R: reserva FALLIDA (no se cobra nada)
    R->>MQ: reserva.cancelada
    MQ->>N: mail "Reserva fallida"
    MQ->>C: liberar bloqueos de autos
```

### Detección de anomalías (IA asincrónica)
```mermaid
sequenceDiagram
    autonumber
    actor O as Operador
    participant C as catalogo
    participant MQ as RabbitMQ
    participant IA as ia
    participant DB as ia_db
    O->>C: PUT /api/catalogo/autos/2 (precio 1 USD)
    C->>MQ: precio.cambiado (precio anterior y nuevo)
    MQ->>IA: ia.precio-cambiado
    IA->>IA: 1) variación vs precio anterior<br/>2) Isolation Forest + z-score por destino<br/>3) distribución de referencia
    IA->>DB: guardar evaluación (motivo, fuente)
    IA->>MQ: anomalia.detectada
    MQ->>C: catalogo.anomalia-detectada
    C->>C: auto EN REVISION (no aparece en búsquedas)
    O->>C: POST /api/catalogo/autos/2/aprobar
    C->>C: vuelve a publicarse
```

### Viaje compartido
```mermaid
sequenceDiagram
    autonumber
    actor V as Organizador
    actor A as Amigo (sin cuenta)
    participant R as reservas
    participant MQ as RabbitMQ
    participant N as notificaciones
    participant PG as pagos
    V->>R: POST /api/reservas (compartirCon: [amigo])
    R->>R: dividir el total en partes (Reparto)
    R->>MQ: pago.solicitado (parte del organizador)
    R->>MQ: invitacion.viaje (token del amigo)
    MQ->>N: mail con link /?pagar=token
    A->>R: GET /api/reservas/compartidas/{token}
    A->>R: POST /api/reservas/compartidas/{token}/pagar
    R->>MQ: pago.solicitado (parte del amigo)
    MQ->>PG: cobrar cada parte
    PG->>MQ: pago.aprobado x2
    MQ->>R: cuando pagaron todos
    R->>R: CONFIRMADA
    R->>MQ: reserva.confirmada (mail a todos)
```

## 4.6 Integración con terceros
| Tercero | Uso | Tipo | Si falla |
|---|---|---|---|
| Duffel | Vuelos reales (modo `real`) | REST + token | Circuit breaker y vuelve a datos simulados |
| Hotelbeds Hotels / Activities | Hoteles y excursiones reales | REST con firma SHA-256 | Circuit breaker y vuelve a datos simulados |
| Frankfurter | Tipo de cambio a USD | REST, sin clave | Circuit breaker y cache |
| Open-Meteo | Clima del destino para el kit y las recomendaciones | REST, sin clave | El kit sale sin clima |
| Open Library | Libros sobre el destino | REST, sin clave | Lista de respaldo |
| OpenStreetMap | Mapa base del kit de viaje | Teselas desde el navegador | Aviso; el itinerario se ve igual |
| Ollama (local) | LLM para el resumen y el asistente | REST local | Plantilla / reglas |
| SMTP (Mailpit o Gmail) | Envío de mails | SMTP | El mensaje va a la DLQ tras 3 intentos |
