# PR: Precio verificado por el servidor, Outbox, resiliencia, observabilidad, IA v2 y rediseño

## Por qué
Cerrar tres problemas de consistencia que aparecerían en la defensa y sumar las mejoras
de arquitectura del plan (secciones 11 a 13).

## Cambios

### Consistencia (críticos)
- **Precio verificado por el servidor.** `POST /api/reservas` recibe solo `{tipo, productoId}`.
  `reservas-service` consulta `GET /api/catalogo/ofertas` y usa ese precio. Nuevo registro
  `oferta_publicada` con vencimiento (60 min) y verificación de destino y tipo.
- **Tipo de cambio.** Todo se normaliza a USD (`TipoCambioService`, API Frankfurter/BCE con valores
  de respaldo). El precio original se muestra en la tarjeta. Ya se pueden combinar Duffel (USD) y Hotelbeds (EUR).
- **Transactional Outbox.** Los eventos de reservas se guardan en `outbox_evento` en la misma
  transacción que el cambio de estado; `OutboxRelay` los publica cada 1 s. Si RabbitMQ se cae, no se pierde nada.
- **Reservas vencidas.** `ReservasVencidasJob` compensa y marca FALLIDA toda reserva PENDIENTE
  sin pago después de 15 min (configurable con `RESERVAS_VENCIMIENTO`).
- **Idempotencia.** Header `Idempotency-Key`: repetir el request devuelve la misma reserva.

### Resiliencia y observabilidad
- **Circuit breaker (Resilience4j)** en Duffel, Hotelbeds, tipo de cambio, catálogo y precios.
  Estado en `/actuator/circuitbreakers`.
- **Flyway**: migraciones versionadas en catálogo, reservas y pagos (`ddl-auto: none`).
- **Trazas distribuidas con Zipkin** (HTTP + RabbitMQ). UI en http://localhost:9411.
- **Cobertura JaCoCo** en el CI, con resumen por módulo en la pestaña del job.

### IA v2
- Tres capas: variación respecto del precio anterior, modelo por **tipo + destino entrenado con
  historial real** (tabla `precio_observado`), y referencia conservadora si no hay historial.
- Se reentrena cada 25 precios normales y al arrancar. Nuevos endpoints `/api/ia/anomalias/modelo`
  y `/reentrenar`. Ahora evalúa vuelos, hoteles y excursiones, no solo autos: las ofertas anómalas quedan bloqueadas.
- `evaluar_modelo.py` mide 3 escenarios (todos con recall 1.0 y precisión ≥ 0.89).

### Interfaz
- Rediseño editorial: esquinas rectas, Fraunces + Inter, bloques de color por destino y tipo de producto.
- Manejo de ofertas vencidas (botón "Volver a buscar") y aviso si el precio cambió al verificarlo.

## Cómo probar
```bash
docker compose down -v          # IMPORTANTE: Flyway necesita bases limpias
docker compose up -d --build
```
1. Reservar un paquete desde la web: queda CONFIRMADA y llega el mail.
2. Precio forzado: mandar un `POST /api/reservas` con un `precioBase` falso → se ignora (el campo ya no existe).
3. Outbox: `docker compose stop rabbitmq`, reservar, ver la fila pendiente en `outbox_evento`,
   `docker compose start rabbitmq` y ver que se publica sola.
4. Circuit breaker: `docker compose stop precios`, intentar reservar 5 veces, ver `/actuator/circuitbreakers` en 8083.
5. Trazas: abrir http://localhost:9411 y buscar `reservas-service`.
6. IA: `docker compose exec ia python evaluar_modelo.py`.

## Breaking changes
- Hay que borrar los volúmenes una vez (`docker compose down -v`): el esquema ahora lo crea Flyway.
- El body de `POST /api/reservas` cambió (solo `tipo` y `productoId` por item).
