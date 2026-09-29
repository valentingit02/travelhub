# TravelHub

![CI](https://github.com/valentingit02/travelhub/actions/workflows/ci.yml/badge.svg)

Plataforma distribuida de reservas de viajes con cotizacion dinamica e IA.
Trabajo Practico Obligatorio - Desarrollo de Aplicaciones II (UADE).

**Empezar aca:** [docs/GUIA_INSTALACION.md](docs/GUIA_INSTALACION.md) · Demo: [docs/DEMO.md](docs/DEMO.md)

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
| reservas-service | Viajeros, reservas (Facade), **Saga** con compensacion, alertas de precio |
| pagos-service | Cobro asincronico (Strategy de procesador de pago) |
| notificaciones-service | Mails de confirmacion, cancelacion y alertas |
| ia-service | Anomalias (Isolation Forest + z-score), resumen con LLM local, recomendaciones |
| gateway | nginx: punto unico de entrada, sirve el frontend, propaga X-Correlation-Id |

## Eventos
| Routing key | Productor | Consumidores |
|---|---|---|
| reserva.creada | reservas | pagos |
| pago.aprobado / pago.rechazado | pagos | reservas |
| reserva.confirmada | reservas | notificaciones |
| reserva.cancelada | reservas | catalogo, notificaciones |
| precio.cambiado | catalogo | ia, reservas |
| anomalia.detectada | ia | catalogo |
| alerta.precio | reservas | notificaciones |

Todas las colas tienen reintentos (3, con backoff) y dead letter queue `travelhub.dlq`.

## Patrones
Factory (`ProductoFactory`), Strategy (`EstrategiaPrecio`, `ProcesadorPago`), Repository (todos),
Facade (`ReservaFacade`), Observer (eventos RabbitMQ), Adapter (`DuffelAdapter`, `Hotelbeds*Adapter`),
Composite (`PaqueteCotizado`), Saga orquestada (`ReservaSaga`).

## Pruebas
| Tipo | Donde |
|---|---|
| Unitarias | `EstrategiasPrecioTest`, `ProductoFactoryTest`, `ReservaSagaTest` |
| Integracion | `PagoIntegracionTest` (Testcontainers: RabbitMQ + Postgres), `AutoRepositoryTest` |
| Local vs remota | `InvocacionLocalVsRemotaTest` (llamada directa vs REST vs SOAP, con tiempos) |
| IA | `ia-service/tests`, `evaluar_modelo.py` (matriz de confusion, recall, latencia) |
| Funcionales | `tests/http/demo.http`, `tests/soap/*.xml` |
| Carga | `tests/k6/busqueda.js` |

## Como contribuir
Todo cambio entra por Pull Request. Ver [CONTRIBUTING.md](CONTRIBUTING.md).
