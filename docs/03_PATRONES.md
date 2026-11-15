# 3. Patrones de diseño aplicados

Los cinco patrones que pide la consigna (Factory, Repository, Strategy, Observer, Facade) más los patrones de
integración que necesita un sistema distribuido. Para cada uno: qué problema resuelve, dónde está y cómo se prueba.

## 3.1 Patrones de diseño (GoF y de dominio)

### Factory — `ProductoFactory`
- **Problema:** cada proveedor devuelve ofertas con el mismo formato, pero el dominio necesita el objeto concreto (`Vuelo`, `Hotel`, `Excursion`, `AutoAlquiler`) para describirlo y cotizarlo.
- **Solución:** `travelhub-common/.../factory/ProductoFactory.crear(tipo, id, destino, precio)` decide qué subclase de `ProductoTuristico` instanciar. Quien busca no conoce las subclases.
- **Uso:** `catalogo-service` `BusquedaService.mapear`.
- **Prueba:** `ProductoFactoryTest`.

### Repository — Spring Data JPA
- **Problema:** separar la lógica de negocio de cómo se guardan los datos.
- **Solución:** cada agregado tiene su interfaz (`ReservaRepository`, `ViajeroRepository`, `OfertaPublicadaRepository`, `AutoRepository`, `PagoRepository`, ...) que extiende `JpaRepository`. Las consultas se declaran por nombre (`findByViajeroIdOrderByCreadaEnDesc`).
- **Prueba:** `AutoRepositoryTest` contra un Postgres real (Testcontainers).

### Strategy — precios y pagos
- **Problema:** el precio final depende de varios factores que cambian de forma independiente; agregar uno no debería tocar los demás.
- **Solución:** interfaz `EstrategiaPrecio` (`factor`, `motivo`, `aplicaA`) con cuatro implementaciones: `TemporadaStrategy`, `AnticipacionStrategy`, `OcupacionStrategy`, `GrupoStrategy`. `CotizadorService` recibe la lista por inyección de dependencias y aplica cada una; agregar una estrategia es crear una clase nueva.
- **Segundo uso:** `ProcesadorPago` en `pagos-service` (hoy `ProcesadorPagoSimulado`; un procesador real como Mercado Pago sería otra implementación).
- **Prueba:** `EstrategiasPrecioTest`; funcional: el mismo hotel cuesta 364,65 en julio y 252,45 en abril.

### Observer — eventos de dominio por RabbitMQ
- **Problema:** cuando pasa algo importante (se confirma una reserva, cambia un precio), varios componentes tienen que reaccionar sin que el emisor los conozca.
- **Solución:** el emisor publica un evento (`ReservasEventPublisher`, `EventPublisher` del catálogo) y cada interesado se suscribe con su propia cola (`@RabbitListener`). Ejemplo: `precio.cambiado` lo escuchan la IA (anomalías) y reservas (protección de precio); el catálogo no sabe que existen.
- **Detalle:** ver la tabla de eventos en [04_INTEGRACION.md](04_INTEGRACION.md#43-mensajería).
- **Prueba:** `PagoIntegracionTest` (RabbitMQ real con Testcontainers).

### Facade — `ReservaFacade`
- **Problema:** reservar un paquete implica verificar ofertas en el catálogo, cotizar en precios, reservar en cada proveedor, aplicar crédito, armar los participantes y pedir el pago. El controller no debería conocer todo eso.
- **Solución:** `ReservaFacade` expone operaciones de alto nivel (`reservarPaquete`, `cancelar`, `pagoAprobado`, `pagarParte`) y coordina por dentro a `CatalogoClient`, `PreciosClient`, `ReservaSaga`, `CreditoService`, los repositories y el publisher.

### Adapter — proveedores externos
- **Problema:** Duffel y Hotelbeds tienen APIs, formatos y autenticación distintos (Hotelbeds firma cada request con SHA-256).
- **Solución:** interfaces propias `ProveedorVuelos`, `ProveedorHoteles`, `ProveedorExcursiones` y un adapter por proveedor: `DuffelAdapter`, `HotelbedsHotelAdapter`, `HotelbedsActivitiesAdapter`. Todos devuelven `OfertaProveedor`. Con `PROVEEDORES_MODO=mock` se usa `MockProveedores` sin tocar el resto del código.
- **Prueba:** `HotelbedsSignatureTest`.

### Composite — `PaqueteCotizado`
- **Problema:** un paquete se cotiza igual que un producto (tiene un total) pero está hecho de productos.
- **Solución:** interfaz `Cotizable` (`total()`) implementada por `ItemCotizado` (hoja) y por `PaqueteCotizado` (compuesto), que suma sus partes y aplica el descuento por combinar 2 o más tipos.
- **Expuesto por:** `POST /api/precios/cotizar-paquete` y la operación SOAP `CotizarPaquete`.

## 3.2 Patrones de integración y resiliencia

| Patrón | Dónde | Qué resuelve |
|---|---|---|
| **Saga orquestada** | `ReservaSaga` | Reserva en cada proveedor en orden; si uno falla, cancela los anteriores (compensación). Cada paso queda en `SagaLog` y se ve en "Mis viajes" |
| **Transactional Outbox** | `outbox/OutboxEvento`, `OutboxRelay` | El evento se guarda en la misma transacción que el cambio de estado; un relay lo publica cada segundo. Si RabbitMQ está caído, no se pierde |
| **Dead Letter Queue** | `MensajeriaConfig.colaConDlq` | Tras 3 reintentos con backoff, el mensaje va a `travelhub.dlq` en vez de perderse o trabar la cola |
| **Circuit Breaker** | Resilience4j: `catalogo`, `precios`, `tipoCambio`, `duffel`, `hotelbeds` | Si un servicio o proveedor falla seguido, se deja de llamarlo un tiempo y se responde rápido; vuelve a probar solo |
| **Idempotencia** | `Idempotency-Key` en reservas; un pago por participante | Reintentar no duplica reservas ni cobros |
| **API Gateway** | `gateway/nginx.conf` | Un solo punto de entrada; propaga `X-Correlation-Id` |
| **Database per service** | `infra/postgres/init.sql` | Cada servicio es dueño de sus datos; se integran solo por contratos |
| **Correlation ID** | `CorrelationIdFilter` | El mismo id aparece en los logs de todos los servicios que tocó un request |
| **Cache-aside** | `@Cacheable` en los adapters (Redis, 15 min) | No repetir llamadas pagas a proveedores |
| **DTO** | `*Dtos` en cada servicio | La API no expone entidades (ej.: el documento sale enmascarado y el hash de la contraseña nunca sale) |
