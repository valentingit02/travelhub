# PR: Asistente IA, viaje compartido, protección de precio, kit de viaje, huella de carbono y mails reales

## Cambios
| Funcionalidad | Dónde | Qué demuestra |
|---|---|---|
| **Asistente conversacional** | `ia-service/app/asistente.py`, `Asistente.jsx` | IA consumida por REST que orquesta catálogo + precios; reglas + LLM opcional |
| **Viaje compartido** | `reservas/compartida`, `PagarParte.jsx` | Saga con N pagos, invitaciones por cola, vencimiento de 24 h y reembolsos |
| **Protección de precio** | `reservas/credito` | Nuevo consumidor de `precio.cambiado`; crédito que se aplica en la próxima reserva y vuelve si se cancela |
| **Kit de viaje** | `ia-service/app/kit.py`, `libros.py`, `KitViaje.jsx` | Open-Meteo + Open Library (API externa nueva) |
| **Huella de carbono** | `catalogo/huella` | Estimación por distancia en cada vuelo |
| **Mails** | `notificaciones` | SMTP configurable (Mailpit o Gmail), registro de envíos y mail de prueba |
| **Gateway** | `gateway/nginx.conf` | Re-resolución DNS: se acaba el 502 al recrear un servicio |

## Eventos nuevos
`pago.solicitado` (reemplaza a `reserva.creada` para pagos), `reembolso.solicitado`, `invitacion.viaje`, `credito.otorgado`.
`PagoAprobado`/`PagoRechazado` ahora llevan `participanteId`.

## Migraciones
- reservas `V3`: participantes, créditos, `vence_en`, `precio_protegido`.
- pagos `V2`: un pago por participante (se quita el UNIQUE de `reserva_id`).
No hace falta `down -v`: son migraciones incrementales.

## Cómo probar
1. **Mail**: `POST /api/notificaciones/prueba?email=...` y `GET /api/notificaciones`.
2. **Asistente**: botón "Planificá con el asistente" → "Nieve en julio con mi pareja" → "Usar este paquete".
3. **Viaje compartido**: en Mi paquete, "Viajo con amigos" con 2 emails → reservar → abrir los mails de invitación
   en Mailpit → pagar cada parte → la reserva pasa a CONFIRMADA y llega el mail a todos.
   Con un email que contenga `rechazo` se ve el rechazo y el reintento.
4. **Protección de precio**: reservar el auto 2 → `PUT /api/catalogo/autos/2` bajando de 120 a 95 → crédito en
   Mis viajes y mail. En la próxima reserva individual se descuenta solo.
5. **Kit de viaje**: en una reserva confirmada, "Ver kit de viaje".
