# PR: Mapa de excursiones en el kit de viaje

## Qué hace
En **Mis viajes → Ver kit de viaje** aparece un mapa con las excursiones del itinerario:
- **Pin numerado**: la excursión de ese día. Al tocar un día del itinerario, el mapa se acerca y abre su ficha.
- **Pin "+"**: excursiones recomendadas por la IA que no entraron en los días del viaje.
- **Casita**: el destino (llegada, alojamiento y regreso).
- Cada ficha tiene un link **"Cómo llegar"** que abre Google Maps con la ruta (sin API key).

## Cambios
| Qué | Dónde | Detalle |
|---|---|---|
| Ubicaciones | `ia-service/app/recomendaciones.py` (`UBICACIONES`) | Lat/lon del punto de encuentro de las 19 excursiones. Las recomendaciones ahora las incluyen |
| Kit | `ia-service/app/kit.py` | Los días con excursión llevan `lat`/`lon`; nuevo bloque `mapa` con el centro del destino y las excursiones que no entraron en el itinerario |
| Mapa | `frontend/src/components/MapaExcursiones.jsx` | Leaflet + mapas de OpenStreetMap (gratis, sin API key). Se carga solo al abrir un kit (lazy loading) para no hacer más pesada la página inicial |
| Itinerario | `KitViaje.jsx` | Los días con excursión se pueden tocar para verlos en el mapa |

## Qué sigue igual
- El campo `mapa` y las coordenadas son **agregados**: quien no los use no se entera. Si el destino no tiene coordenadas, el kit funciona como antes y no muestra mapa.
- Si no hay internet para bajar el mapa base, se avisa y el itinerario se sigue viendo.

## API externa
**OpenStreetMap** (mapas base) se suma a Open-Meteo, Open Library, Duffel y Hotelbeds. Se respeta su política de uso: atribución visible y carga normal desde el navegador (sin descargas masivas).

## Pruebas
`ia-service/tests/test_mapa.py`:
- Todas las excursiones tienen ubicación y quedan a menos de 150 km de su destino (fórmula de haversine): evita errores de carga como un signo cambiado.
- Los días con excursión traen coordenadas; la llegada y el regreso no.
- El mapa trae el centro del destino y no repite excursiones que ya están en el itinerario.
- Un destino sin coordenadas no rompe el kit.

## Cómo probar
1. Reconstruir `ia` y `gateway`: `docker compose up -d --build ia gateway`.
2. Reservar un paquete a Bariloche, esperar a que quede CONFIRMADA y tocar **Ver kit de viaje**.
3. Tocar "Día 2 · Cerro Catedral" en el itinerario: el mapa se acerca al cerro y abre la ficha.
4. API: `GET /api/ia/kit?destino=BRC&desde=2027-07-10&hasta=2027-07-14&preferencias=aventura` → ver `itinerario[].lat/lon` y `mapa`.
