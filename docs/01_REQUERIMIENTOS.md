# 1. Requerimientos

TravelHub es una plataforma de reservas de viajes: el usuario arma un paquete (vuelo, hotel, auto y excursiones),
el sistema cotiza con precios dinámicos, reserva en los proveedores y cobra de forma asincrónica.
Una capa de IA detecta precios anómalos, resume el viaje, recomienda excursiones y arma un kit de viaje.

## 1.1 Actores
| Actor | Descripción |
|---|---|
| Viajero | Busca, arma el paquete, reserva, paga y consulta sus viajes. Necesita cuenta para reservar |
| Invitado | Amigo de un viaje compartido: paga su parte con un link, sin cuenta |
| Operador | Revisa productos marcados por la IA y los aprueba (Swagger del catálogo) |
| Proveedores externos | Duffel (vuelos), Hotelbeds (hoteles y excursiones), Frankfurter (tipo de cambio), Open-Meteo (clima), Open Library (libros), OpenStreetMap (mapas) |

## 1.2 Requerimientos funcionales
| ID | Requerimiento | Dónde se cumple |
|---|---|---|
| RF01 | Buscar vuelos, hoteles, excursiones y autos para un destino y fechas, sin necesidad de cuenta | `catalogo-service` `GET /api/catalogo/buscar` |
| RF02 | Mostrar todos los precios en USD aunque el proveedor cotice en otra moneda | `TipoCambioService` (Frankfurter) |
| RF03 | Cotizar cada producto con precio dinámico (temporada, anticipación, ocupación, grupo) y mostrar el desglose | `precios-service` (Strategy), REST y SOAP |
| RF04 | Cotizar un paquete con descuento si combina 2 o más tipos de producto | `PaqueteCotizado` (Composite) |
| RF05 | Registrarse e ingresar con email y contraseña; editar el perfil | `reservas-service` `/api/auth/*`, `/api/viajeros/me` |
| RF06 | Reservar un paquete: el servidor verifica precio y vigencia de cada oferta (el navegador no define precios) | `ReservaFacade` + `OfertaService.verificar` |
| RF07 | Si falla un proveedor durante la reserva, deshacer lo ya reservado (compensación) | `ReservaSaga` |
| RF08 | Cobrar de forma asincrónica y confirmar la reserva al aprobarse el pago | Eventos `pago.solicitado` / `pago.aprobado` |
| RF09 | Viaje compartido: dividir el pago entre amigos, cada uno paga con un link | `compartida/*`, evento `invitacion.viaje` |
| RF10 | Cancelar una reserva, liberar todo en los proveedores y reembolsar | `DELETE /api/reservas/{id}`, `reembolso.solicitado` |
| RF11 | Enviar mails de confirmación, cancelación, invitación, crédito y alertas de precio | `notificaciones-service` |
| RF12 | Detectar precios anómalos y ocultar el producto hasta que un operador lo apruebe | `ia-service` + `catalogo.anomalia-detectada` |
| RF13 | Protección de precio: si baja el precio de algo reservado, dar crédito para la próxima reserva | `credito/*`, `credito.otorgado` |
| RF14 | Alertas: avisar por mail cuando un destino seguido baja de un precio objetivo | `seguimiento/*`, `alerta.precio` |
| RF15 | Asistente conversacional que interpreta un pedido en lenguaje natural y propone un paquete | `ia-service` `POST /api/ia/asistente` |
| RF16 | Recomendar excursiones según preferencias del viajero y clima del destino | `GET /api/ia/recomendaciones` |
| RF17 | Kit de viaje: qué llevar según el clima, itinerario día por día con mapa, libros | `GET /api/ia/kit`, `MapaExcursiones.jsx` |
| RF18 | Seguir el estado de cada reserva (bitácora de la Saga) | `GET /api/reservas/{id}` |

## 1.3 Requerimientos no funcionales
| ID | Atributo | Requerimiento | Cómo se verifica | Resultado medido |
|---|---|---|---|---|
| RNF01 | Rendimiento | Búsqueda con 50 usuarios simultáneos: p95 < 500 ms, errores < 1 % | `tests/k6/busqueda.js` | p95 = 115 ms recién arrancado y 50 ms en caliente; 0 % errores (3.631 requests) |
| RNF02 | Disponibilidad | Si un proveedor externo falla, el resto del sistema sigue funcionando | Circuit breaker (Resilience4j) en catálogo, precios y tipo de cambio | Estado en `/actuator/circuitbreakers` |
| RNF03 | Confiabilidad | Ningún evento se pierde si RabbitMQ está caído; los mensajes que fallan no se descartan | Outbox + 3 reintentos + DLQ | `OutboxRelayTest`, cola `travelhub.dlq` |
| RNF04 | Consistencia | Una reserva nunca queda a medias entre proveedores | Saga con compensación | `ReservaSagaTest`, prueba funcional "Hotel Falla" |
| RNF05 | Idempotencia | Reintentar una compra o un pago no cobra dos veces | Header `Idempotency-Key`; pagos no cobra dos veces la misma parte | Prueba funcional: dos POST con la misma clave devuelven la misma reserva y generan un solo pago |
| RNF06 | Seguridad | Contraseñas con BCrypt; sesión con JWT firmado; cada viajero solo accede a sus datos | Spring Security | `SeguridadWebTest`, `AuthServiceTest`, `TokenServiceTest` |
| RNF07 | Observabilidad | Cada request se puede seguir entre servicios | `X-Correlation-Id` en logs + trazas en Zipkin | http://localhost:9411 |
| RNF08 | Mantenibilidad | Esquema versionado; configuración fuera del código | Flyway; variables de entorno (`.env`) | `db/migration/V*.sql`, `.env.example` |
| RNF09 | Portabilidad | Todo corre en un entorno de laboratorio con un solo comando | Docker Compose | `docker compose up -d --build` |
| RNF10 | Usabilidad | Interfaz web responsive en español; buscar no requiere cuenta | React + Vite | Prueba manual en celular y escritorio |

## 1.4 Requerimientos de la IA
| ID | Atributo | Requerimiento | Cómo se verifica | Resultado medido |
|---|---|---|---|---|
| RIA01 | Exactitud | Detección de anomalías con **recall ≥ 0,85** (no dejar pasar precios erróneos) | `ia-service/evaluar_modelo.py` (3 escenarios con datos etiquetados) | Recall 1,00 en los 3 escenarios; precisión 0,90 a 1,00 |
| RIA02 | Latencia | Evaluar un precio en **< 200 ms (p95)** | `evaluar_modelo.py` | p95 entre 40 y 112 ms según la carga del equipo |
| RIA03 | Latencia | Resumen del viaje con LLM en **< 5 s**; si tarda más, se usa una plantilla | `LLM_TIMEOUT_S=5` en `resumen.py` | El mail siempre sale; indica si lo generó el LLM o la plantilla |
| RIA04 | Privacidad | Ningún dato personal sale a servicios de IA de terceros | LLM **local** (Ollama), modelos propios (scikit-learn) | La IA solo recibe nombre de pila, destino, fechas, productos y total; nunca documento, email ni contraseña |
| RIA05 | Privacidad | Los documentos de identidad no se exponen completos | Se devuelven enmascarados (`****222`) | `SeguridadWebTest.elPerfilEsElDeLaSesion` |
| RIA06 | Robustez | Si la IA no responde, el negocio no se detiene | La evaluación es asincrónica (cola); el resumen tiene plantilla de respaldo | Prueba con `docker compose stop ia`: la reserva se confirmó y el mail salió con la plantilla |
| RIA07 | Explicabilidad | Cada decisión de la IA indica por qué la tomó | Campos `motivo` y `fuente` en anomalías y recomendaciones | `GET /api/ia/anomalias/historial` |
| RIA08 | Calidad de datos | Las coordenadas del mapa están a menos de 150 km de su destino | `test_mapa.py` (haversine) | Pasa para las 19 excursiones |
