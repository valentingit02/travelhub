# Guion de la demo (7 minutos)

1. `docker compose ps`: todo sano. Mostrar la consola de RabbitMQ con el exchange `travelhub.events` y las colas.
2. En http://localhost:8080 buscar AEP -> Bariloche en julio. Mostrar vuelos, hoteles, excursiones y autos.
3. Armar el paquete: se ve el desglose de cada Strategy (temporada, anticipacion, ocupacion, grupo) y el descuento Composite.
   Cambiar las fechas a abril y mostrar que baja. Repetir la cotizacion por SOAP (`tests/soap/cotizar-paquete.xml`).
4. Reservar: pide **ingresar** (email y contrasena). Mostrar "Mi perfil" y, en `tests/http/demo.http`, el 401 sin token y el 403 con otro viajero.
   La reserva queda PENDIENTE, en RabbitMQ pasan `reserva.creada` -> `pago.aprobado` -> `reserva.confirmada`
   y a los 2 segundos queda CONFIRMADA sola.
5. En Mis viajes, "Ver kit de viaje": mapa de excursiones (OpenStreetMap). Tocar un dia del itinerario y ver como el mapa se acerca.
6. Abrir Mailpit y mostrar el mail con el resumen de la IA (y quien lo genero: LLM o plantilla).
7. Anomalia: `PUT` del auto 2 a 1 USD (`tests/http/demo.http`, paso 7). La IA publica `anomalia.detectada` y el auto queda en revision.
8. Saga: reservar con "Hotel Falla (demo de Saga)". Mostrar la bitacora: vuelo OK -> hotel FALLO -> COMPENSAR_VUELO OK, y el mail de reserva fallida.
9. Resiliencia: `docker compose stop notificaciones`, reservar, mostrar el mensaje esperando en la cola,
   `docker compose start notificaciones` y ver que llega el mail.
