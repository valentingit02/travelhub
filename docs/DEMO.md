# Guion de la demo (7 minutos)

1. `docker compose ps`: todo sano. Mostrar la consola de RabbitMQ con el exchange `travelhub.events` y las colas.
2. En http://localhost:8080 buscar AEP -> Bariloche en julio. Mostrar vuelos, hoteles, excursiones y autos.
3. Armar el paquete: se ve el desglose de cada Strategy (temporada, anticipacion, ocupacion, grupo) y el descuento Composite.
   Cambiar las fechas a abril y mostrar que baja. Repetir la cotizacion por SOAP (`tests/soap/cotizar-paquete.xml`).
4. Reservar: la reserva queda PENDIENTE, en RabbitMQ pasan `reserva.creada` -> `pago.aprobado` -> `reserva.confirmada`
   y a los 2 segundos queda CONFIRMADA sola.
5. Abrir Mailpit y mostrar el mail con el resumen de la IA (y quien lo genero: LLM o plantilla).
6. Anomalia: `PUT` del auto 2 a 1 USD (`tests/http/demo.http`, paso 7). La IA publica `anomalia.detectada` y el auto queda en revision.
7. Saga: reservar con "Hotel Falla (demo de Saga)". Mostrar la bitacora: vuelo OK -> hotel FALLO -> COMPENSAR_VUELO OK, y el mail de reserva fallida.
8. Resiliencia: `docker compose stop notificaciones`, reservar, mostrar el mensaje esperando en la cola,
   `docker compose start notificaciones` y ver que llega el mail.
