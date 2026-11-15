# 6. Pruebas y evidencias

Todas las salidas están guardadas en [`docs/evidencias/`](evidencias/) y se pueden regenerar con los comandos de cada sección.

## 6.1 Resumen
| Tipo | Herramienta | Qué cubre | Resultado | Evidencia |
|---|---|---|---|---|
| Unitarias Java | JUnit 5 + Mockito | Estrategias de precio, Factory, Saga, Outbox, login, tokens, ofertas, reparto del viaje compartido | Todas OK | [tests-java.txt](evidencias/tests-java.txt) |
| Integración Java | Testcontainers (Postgres, RabbitMQ) | Repository contra Postgres real; pagos consumiendo y publicando en RabbitMQ real | OK: 76 tests Java, 0 salteados | [tests-java.txt](evidencias/tests-java.txt) |
| Seguridad (capa HTTP) | MockMvc + JWT reales | 401 sin sesión, 403 con datos ajenos, rutas públicas, validaciones; rol ADMIN en el catálogo | 12/12 y 9/9 OK | `SeguridadWebTest`, `SeguridadCatalogoWebTest` |
| Local vs remota | JUnit | Misma cotización por llamada directa, REST y SOAP, con tiempos | OK | [invocacion-local-vs-remota.txt](evidencias/invocacion-local-vs-remota.txt) |
| IA | pytest | Anomalías, consumidor de la cola, resumen con plantilla, privacidad del prompt, recomendaciones, asistente, kit, mapa | 25/25 OK | [tests-ia.txt](evidencias/tests-ia.txt) |
| Validación del modelo | `evaluar_modelo.py` | Matriz de confusión, precisión, recall y latencia en 3 escenarios | Cumple RIA01 y RIA02 | [evaluacion-modelo-ia.txt](evidencias/evaluacion-modelo-ia.txt) |
| Funcional e integración de punta a punta | `tests/e2e/prueba_funcional.py` | Todo el sistema levantado: REST, SOAP, login, rol ADMIN, Saga, cola, mails, viaje compartido, IA sincrónica y asincrónica, APIs externas, DLQ, Zipkin | 53/53 OK | [prueba-funcional-e2e.txt](evidencias/prueba-funcional-e2e.txt) |
| Carga | k6 | Búsqueda con 50 usuarios simultáneos | p95 de 115 a 406 ms recién arrancado y 60 ms en caliente (meta < 500 ms), 0 % errores | [k6-carga-50-usuarios.txt](evidencias/k6-carga-50-usuarios.txt) |
| Manuales | `tests/http/demo.http`, `tests/soap/*.xml` | Recorrido de la demo con REST Client / SoapUI | — | — |
| CI | GitHub Actions | Compila, corre los tests Java (con Testcontainers), los de la IA, valida el modelo y compila el frontend en cada PR | Verde en `main` | `.github/workflows/ci.yml` |

## 6.2 Cómo correr cada prueba
```bash
# Unitarias e integración Java (Testcontainers necesita Docker corriendo)
mvn -B test

# IA (desde ia-service/)
pip install -r requirements.txt && pytest && python evaluar_modelo.py

# De punta a punta (con el sistema levantado)
docker compose up -d --build
python tests/e2e/prueba_funcional.py

# Carga (sin instalar k6, con Docker)
docker run --rm -v ./tests/k6:/k6 -e GW=http://host.docker.internal:8080 grafana/k6:0.54.0 run /k6/busqueda.js
```

## 6.3 Ejecución local y remota
- **Local:** dentro de un servicio, las capas se invocan directamente (ej. `CotizadorService` desde el controller). El test
  `InvocacionLocalVsRemotaTest` mide 0,17 ms por llamada.
- **Remota sincrónica:** entre servicios por REST (15,8 ms) o SOAP (17,1 ms), siempre a través de contratos publicados.
- **Remota asincrónica:** por RabbitMQ. La prueba de punta a punta muestra una reserva que pasa de PENDIENTE a CONFIRMADA
  sola, en unos 3 segundos, cuando `pagos` procesa el evento.

## 6.4 Prueba de carga y mejora de rendimiento
La primera medición **no cumplía** el RNF01: con 50 usuarios, p95 = 9,1 s recién arrancado y 935 ms en caliente.

**Causa:** cada búsqueda registraba sus ~11 ofertas con un SELECT y un UPDATE por oferta, cada uno en su propia transacción,
y reescribía las filas aunque nada hubiera cambiado. Con 50 usuarios buscando los mismos 4 destinos, competían por los mismos
bloqueos de fila en Postgres. Además, el hotel de la demo de la Saga usaba el mismo id en todos los destinos, así que esa fila
se reescribía en cada búsqueda.

**Cambios:**
1. `OfertaService.publicar` lee todas las ofertas en una sola consulta (`findAllById`) y solo escribe las nuevas, las que
   cambiaron algún dato o las que tienen menos de media vigencia por delante. El resto es solo lectura.
2. El hotel de la demo pasa a tener un id único por destino (`MOCK-HOTEL-BRC-FALLA`), como cualquier oferta.
3. El script de k6 sube la carga en 20 s (en vez de 50 usuarios en el segundo 0), como en un uso real.

| Medición (50 usuarios) | Antes | Después |
|---|---|---|
| p95 recién arrancado | 9,1 s | 115 a 406 ms (varía entre corridas) |
| p95 en caliente | 935 ms | 50 a 60 ms |
| Mediana en caliente | 95 ms | 14 ms |
| Errores | 0 % | 0 % |

Pruebas que cubren el cambio: `OfertaServiceTest` (7 casos) y `MockProveedoresTest` (3 casos).
Medido en una notebook con Docker Desktop y 3,8 GB de memoria para los 12 contenedores.

## 6.5 Validación del componente de IA
Ver [05_IA.md](05_IA.md#51-detección-de-anomalías-de-precio). Resumen: recall 1,00 en los tres escenarios (meta ≥ 0,85),
precisión entre 0,90 y 1,00, latencia p95 entre 36 y 112 ms según la carga del equipo (meta < 200 ms). La validación corre en CI en cada PR.

## 6.6 Capturas para el informe
Estas evidencias son visuales y conviene sacarlas con el sistema levantado (no se pueden generar desde la consola):

| Captura | Dónde |
|---|---|
| Swagger de reservas con el botón Authorize | http://localhost:8083/swagger-ui.html |
| Llamada SOAP con su respuesta | SoapUI o Postman con `tests/soap/cotizar-paquete.xml` |
| Exchange, colas y DLQ | http://localhost:15672 (guest / guest) |
| Traza de una reserva atravesando varios servicios | http://localhost:9411 |
| Mail de confirmación con el resumen de la IA | http://localhost:8025 |
| Mis viajes con la bitácora de la Saga y el kit con el mapa | http://localhost:8080 |
| Salida de k6 y de `evaluar_modelo.py` | Consola |
