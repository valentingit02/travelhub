# Manual de instalación y despliegue

Todo lo que usa TravelHub es gratis. Para **solo ejecutarlo** alcanza con Git y Docker; el resto de las
herramientas hace falta únicamente para desarrollar.

## 1. Requisitos
- 8 GB de RAM como mínimo (16 GB si se usa el LLM local) y 15 GB libres en disco.
- Windows 10/11 con virtualización activada, macOS o Linux.
- **Git** y **Docker Desktop** (en Windows, al abrirlo por primera vez pide activar WSL 2: aceptar y reiniciar).

```powershell
winget install -e --id Git.Git
winget install -e --id Docker.DockerDesktop
```

Para desarrollar (opcional): JDK 21, Maven 3.9, Python 3.12, Node 20+ y un IDE.
```powershell
winget install -e --id EclipseAdoptium.Temurin.21.JDK
winget install -e --id Apache.Maven
winget install -e --id Python.Python.3.12
winget install -e --id OpenJS.NodeJS.LTS
```

## 2. Obtener el código
Clonar en una ruta corta (en Windows, las rutas muy largas hacen fallar el checkout):
```bash
git clone https://github.com/valentingit02/travelhub C:/Proyectos/travelhub
cd C:/Proyectos/travelhub
```
Si hay que clonar en una ruta larga: `git config --global core.longpaths true` antes de clonar.

## 3. Configurar
```bash
cp .env.example .env          # en Windows: copy .env.example .env
```
Con los valores del ejemplo todo funciona en modo demostración (datos simulados y mails en Mailpit).
Las variables más importantes:

| Variable | Para qué | Valor de ejemplo |
|---|---|---|
| `PROVEEDORES_MODO` | `mock`: datos simulados · `real`: Duffel y Hotelbeds (ver sección 6) | `mock` |
| `JWT_SECRET` | Clave para firmar las sesiones. Mínimo 32 caracteres | Generar una propia: `openssl rand -base64 48` |
| `JWT_DURACION` | Cuánto dura una sesión | `8h` |
| `ADMIN_EMAILS` | Emails de los operadores (rol ADMIN), separados por coma | `admin@travelhub.local` |
| `MAIL_*` | SMTP para mandar mails reales (ver sección 7). Sin esto van a Mailpit | — |
| `DB_USER` / `DB_PASSWORD` | Usuario de Postgres. No cambiarlos después de la primera ejecución | `travelhub` |

`.env` está en `.gitignore`: nunca se sube al repositorio.

## 4. Levantar
Con Docker Desktop abierto, desde la carpeta del proyecto:
```bash
docker compose up -d --build
```
- La primera vez tarda entre 10 y 15 minutos (descarga imágenes y dependencias de Maven, npm y Python).
- **Esperar uno o dos minutos** a que arranquen los servicios Java antes de usar la app. Mientras tanto, una búsqueda
  puede responder 502.
- Verificar: `docker compose ps` (todos en *running*; postgres, rabbitmq, mailpit y zipkin en *healthy*).

| Qué | URL |
|---|---|
| App web | http://localhost:8080 |
| Swagger de catálogo, precios, reservas, pagos y notificaciones | http://localhost:8081/swagger-ui.html (8082, 8083, 8084, 8085) |
| WSDL del servicio SOAP | http://localhost:8082/ws/cotizacion.wsdl |
| Documentación del servicio de IA | http://localhost:8000/docs |
| Consola de RabbitMQ (guest / guest) | http://localhost:15672 |
| Trazas distribuidas (Zipkin) | http://localhost:9411 |
| Bandeja de mails de prueba (Mailpit) | http://localhost:8025 |

Uso diario: `docker compose up -d` para levantar (sin `--build` si no cambió el código) y `docker compose stop` para apagar.

## 5. Primer uso
1. **Viajero:** en http://localhost:8080 buscar un destino (no hace falta cuenta), armar el paquete y reservar.
   Al reservar pide **crear una cuenta** con email y contraseña.
2. **Operador:** crear una cuenta con un email de `ADMIN_EMAILS` (por defecto `admin@travelhub.local`). Su token tiene
   el rol ADMIN y permite cambiar precios y aprobar productos en el Swagger del catálogo (botón *Authorize*).
   Para obtener el token: `POST /api/auth/login` en http://localhost:8083/swagger-ui.html.
3. **Recorrido guiado:** `tests/http/demo.http` (VS Code con la extensión *REST Client*) recorre búsqueda, cotización,
   login, reserva, Saga con compensación y detección de anomalías.

## 6. Proveedores reales (opcional)
1. **Duffel:** registrarse en app.duffel.com, pasar a modo *Test* y crear un *access token* (empieza con `duffel_test_`).
2. **Hotelbeds:** registrarse en developer.hotelbeds.com. Da una key y un secret para Hotels y otros para Activities.
3. Completar `DUFFEL_TOKEN` y `HOTELBEDS_*` en `.env`, poner `PROVEEDORES_MODO=real` y aplicar: `docker compose up -d`.

Notas:
- Son ambientes de prueba: no se cobra ni se reserva nada real.
- El ambiente de prueba de Hotelbeds tiene poco inventario (por ejemplo, sí tiene Madrid y no Bariloche) y una cuota
  diaria. Si una API no devuelve resultados, falla o se agota la cuota, el catálogo usa datos simulados.
- La primera búsqueda contra las APIs reales tarda unos segundos; después queda en caché 15 minutos.

## 7. Mails reales (opcional)
Por defecto los mails van a Mailpit. Para enviarlos de verdad con Gmail: activar la verificación en dos pasos,
crear una *contraseña de aplicación* y completar en `.env`:
```env
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=cuenta@gmail.com
MAIL_PASSWORD=contrasena-de-aplicacion
MAIL_SMTP_AUTH=true
MAIL_STARTTLS=true
MAIL_REMITENTE=cuenta@gmail.com
```
Aplicar con `docker compose up -d`. Prueba: `POST http://localhost:8085/api/notificaciones/prueba?email=...`.

## 8. LLM local (opcional)
```bash
docker compose --profile llm up -d
docker compose exec ollama ollama pull llama3.2:1b     # ~1.3 GB, una sola vez
```
Sin el LLM la IA funciona igual: el resumen del mail se genera con una plantilla.

## 9. Pruebas
| Prueba | Comando |
|---|---|
| Tests Java (unitarios e integración con Testcontainers; Docker abierto) | `mvn -B test` |
| Tests de la IA y validación del modelo | `cd ia-service && pip install -r requirements.txt && pytest && python evaluar_modelo.py` |
| De punta a punta, con el sistema levantado (usar `PROVEEDORES_MODO=mock`) | `python tests/e2e/prueba_funcional.py` |
| Carga con k6, sin instalarlo (usar `PROVEEDORES_MODO=mock`) | `docker run --rm -v ./tests/k6:/k6 -e GW=http://host.docker.internal:8080 grafana/k6:0.54.0 run /k6/busqueda.js` |

Resultados y evidencias: [06_PRUEBAS.md](06_PRUEBAS.md).

## 10. Desarrollar con el IDE
```bash
docker compose up -d postgres rabbitmq redis mailpit   # solo la infraestructura
mvn clean install                                      # compila y corre los tests
```
En IntelliJ: abrir la carpeta raíz y correr cada `*Application.java`.

Servicio de IA:
```bash
cd ia-service
python -m venv .venv
.venv\Scripts\activate          # macOS/Linux: source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

Frontend:
```bash
cd frontend
npm install
npm run dev                     # http://localhost:5173 (usa el gateway de Docker en :8080)
```

## 11. Problemas comunes
| Síntoma | Solución |
|---|---|
| La búsqueda da 502 o "No pude consultar la disponibilidad" | Los servicios Java todavía están arrancando: esperar uno o dos minutos |
| `port is already allocated` | Otro programa usa el puerto (por ejemplo un Postgres local): cerrarlo o cambiar el puerto en `docker-compose.yml` |
| Un servicio se reinicia en bucle | `docker compose logs -f NOMBRE`. Suele ser que Postgres o RabbitMQ todavía no arrancaron |
| Después de cambiar `JWT_SECRET` la app pide ingresar de nuevo | Es esperable: las sesiones firmadas con la clave anterior dejan de valer |
| El operador recibe 403 al cambiar un precio | Su email no está en `ADMIN_EMAILS`, o ingresó antes de agregarlo: volver a ingresar |
| Falla el build de `ia` descargando paquetes de Python | La red bloquea PyPI (`files.pythonhosted.org`): hacer el build desde otra red |
| El kit de viaje no muestra el mapa | La imagen de `ia` es vieja: `docker compose up -d --build ia` |
| Cambié código y no se ve | `docker compose up -d --build NOMBRE` |
| La base quedó con datos viejos | `docker compose down -v` (borra todos los datos) y volver a levantar |
| Testcontainers se saltea los tests de integración | Docker tiene que estar abierto. El `pom.xml` ya fija la zona horaria UTC y la versión de la API de Docker |
| `git clone` falla con *Filename too long* | Clonar en una ruta corta o `git config --global core.longpaths true` |

Para apagar todo: `docker compose stop` (conserva los datos) o `docker compose down` (borra los contenedores;
agregar `-v` borra también las bases).
