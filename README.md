# TravelHub

![CI](https://github.com/valentingit02/travelhub/actions/workflows/ci.yml/badge.svg)

Plataforma distribuida de reservas de viajes con cotizacion dinamica e IA.
Trabajo Practico Obligatorio - Desarrollo de Aplicaciones II (UADE).

**Empezar aca:** [docs/GUIA_INSTALACION.md](docs/GUIA_INSTALACION.md) · Demo: [docs/DEMO.md](docs/DEMO.md)

## Documentacion
| # | Documento | Contenido |
|---|---|---|
| 1 | [Requerimientos](docs/01_REQUERIMIENTOS.md) | Funcionales, no funcionales y de IA (latencia, exactitud, privacidad) con lo medido |
| 2 | [Arquitectura](docs/02_ARQUITECTURA.md) | Informe: componentes, capas, clases, despliegue, local vs remoto, decisiones |
| 3 | [Patrones](docs/03_PATRONES.md) | Factory, Repository, Strategy, Observer, Facade, Adapter, Composite, Saga, Outbox, ... |
| 4 | [Integracion](docs/04_INTEGRACION.md) | Contratos REST y SOAP, configuracion de colas, flujo de mensajes, BPMN y secuencias |
| 5 | [IA](docs/05_IA.md) | Decisiones de diseno de la IA y su validacion |
| 6 | [Pruebas](docs/06_PRUEBAS.md) | Tipos de prueba, resultados y [evidencias](docs/evidencias/) |
| — | [Contratos](docs/contratos/) | OpenAPI de cada servicio, WSDL y XSD |
| — | [Instalacion](docs/GUIA_INSTALACION.md) · [Demo](docs/DEMO.md) | Manual de despliegue y guion de la defensa |

```bash
cp .env.example .env
docker compose up -d --build
# abrir http://localhost:8080
```

## Arquitectura
```
 Frontend React ─┐
                 ▼
          gateway (nginx :8080)
     ┌──────┬──────┼────────┬────────┐
     ▼      ▼      ▼        ▼        ▼
 catalogo precios reservas pagos    ia (Python)
  :8081  :8082    :8083    :8084    :8000
  REST   REST+SOAP REST     REST     REST
  │ Duffel / Hotelbeds / Redis
  └──────────── RabbitMQ (travelhub.events + DLQ) ──── notificaciones :8085 -> Mailpit
```

| Modulo | Responsabilidad |
|---|---|
| travelhub-common | Dominio, Factory, eventos, utilidades (correlationId, errores, firma Hotelbeds), config de RabbitMQ |
| catalogo-service | Busqueda (Duffel, Hotelbeds Hotel y Activities, autos propios), cache Redis, reserva en proveedores |
| precios-service | Cotizacion con Strategies, paquete con Composite; REST y **SOAP** (WSDL) |
| reservas-service | Viajeros y **login** (BCrypt + JWT), reservas (Facade), **Saga** con compensacion, alertas de precio |
| pagos-service | Cobro asincronico (Strategy de procesador de pago) |
| notificaciones-service | Mails de confirmacion, cancelacion y alertas |
| ia-service | Anomalias (Isolation Forest + z-score), resumen con LLM local, recomendaciones, kit de viaje con mapa de excursiones |
| gateway | nginx: punto unico de entrada, sirve el frontend, propaga X-Correlation-Id |

## Eventos
Exchange `travelhub.events` (topic). Cada consumidor tiene su propia cola durable.

| Routing key | Productor | Cola -> Consumidor | Para que |
|---|---|---|---|
| pago.solicitado | reservas | `pagos.pago-solicitado` -> pagos | Cobrar la reserva (una vez por participante si es compartida) |
| pago.aprobado | pagos | `reservas.pago-aprobado` -> reservas | Avanzar la Saga: confirmar cuando pagaron todos |
| pago.rechazado | pagos | `reservas.pago-rechazado` -> reservas | Avisar el rechazo y permitir reintentar |
| reembolso.solicitado | reservas | `pagos.reembolso-solicitado` -> pagos | Devolver lo pagado al cancelar o fallar |
| reserva.confirmada | reservas | `notificaciones.reserva-confirmada` -> notificaciones | Mail de confirmacion con resumen de la IA |
| reserva.cancelada | reservas | `catalogo.reserva-cancelada` -> catalogo | Liberar el auto reservado |
| | | `notificaciones.reserva-cancelada` -> notificaciones | Mail de cancelacion o reserva fallida |
| invitacion.viaje | reservas | `notificaciones.invitacion-viaje` -> notificaciones | Mail con el link de pago para cada amigo |
| credito.otorgado | reservas | `notificaciones.credito-otorgado` -> notificaciones | Mail de proteccion de precio |
| alerta.precio | reservas | `notificaciones.alerta-precio` -> notificaciones | Mail cuando un destino seguido baja de precio |
| precio.cambiado | catalogo | `ia.precio-cambiado` -> ia | Evaluar si el nuevo precio es una anomalia |
| | | `reservas.precio-cambiado` -> reservas | Proteccion de precio y alertas |
| anomalia.detectada | ia | `catalogo.anomalia-detectada` -> catalogo | Poner el producto en revision |

**Confiabilidad**
- Los eventos de reservas salen por un **Outbox** (se guardan en la misma transaccion que el cambio y un relay los publica): no se pierden si RabbitMQ esta caido.
- Los consumidores Java reintentan 3 veces con backoff (1 s, 2 s). Si sigue fallando, el mensaje va al exchange `travelhub.dlx` y queda en la cola **`travelhub.dlq`** para revisarlo.
- El consumidor de la IA (Python) hace `nack` sin reencolar ante un error, asi que tambien termina en la DLQ.
- Los pagos son idempotentes (un cobro por participante).

## Seguridad
- **Login** con email y contrasena (`POST /api/auth/registro`, `/login`, `/activar`). Las contrasenas se guardan con BCrypt.
- **Sesion** con JWT (HS256) en el header `Authorization: Bearer`. Clave en `JWT_SECRET` (minimo 32 caracteres).
- **Privacidad**: cada viajero solo ve y opera sus reservas, creditos, alertas y perfil (401 sin sesion, 403 si pide datos ajenos).
- Buscar, cotizar, el asistente y el link de viaje compartido siguen siendo publicos. Detalle: [docs/PR_LOGIN.md](docs/PR_LOGIN.md).

## Patrones
Factory (`ProductoFactory`), Strategy (`EstrategiaPrecio`, `ProcesadorPago`), Repository (todos),
Facade (`ReservaFacade`), Observer (eventos RabbitMQ), Adapter (`DuffelAdapter`, `Hotelbeds*Adapter`),
Composite (`PaqueteCotizado`), Saga orquestada (`ReservaSaga`).

## Pruebas
Detalle y resultados: [docs/06_PRUEBAS.md](docs/06_PRUEBAS.md).

| Tipo | Donde |
|---|---|
| Unitarias | `EstrategiasPrecioTest`, `ProductoFactoryTest`, `ReservaSagaTest`, `OfertaServiceTest`, `TokenServiceTest`, `AuthServiceTest`, ... |
| Seguridad (HTTP) | `SeguridadWebTest` (401 sin sesion, 403 con datos ajenos, rutas publicas) |
| Integracion | `PagoIntegracionTest` (Testcontainers: RabbitMQ + Postgres), `AutoRepositoryTest` |
| Local vs remota | `InvocacionLocalVsRemotaTest` (llamada directa vs REST vs SOAP, con tiempos) |
| IA | `ia-service/tests` (25 tests), `evaluar_modelo.py` (matriz de confusion, recall, latencia) |
| De punta a punta | `python tests/e2e/prueba_funcional.py` (45 chequeos contra el sistema levantado) |
| Manuales | `tests/http/demo.http`, `tests/soap/*.xml` |
| Carga | `tests/k6/busqueda.js` (50 usuarios, p95 < 500 ms) |

## Como contribuir
Todo cambio entra por Pull Request. Ver [CONTRIBUTING.md](CONTRIBUTING.md).
