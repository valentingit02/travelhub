# Guia de instalacion y puesta en marcha (paso a paso)

Todo lo que se usa es gratis. Tiempo estimado: 1 a 2 horas la primera vez (casi todo es descarga).

## 0. Requisitos de la computadora
- 16 GB de RAM recomendados (con 8 GB funciona, pero sin el LLM local).
- 15 GB libres en disco.
- Windows 10/11 con virtualizacion activada (Docker la necesita), macOS o Linux.

## 1. Instalar las herramientas

### Windows (PowerShell como administrador)
```powershell
winget install -e --id Git.Git
winget install -e --id EclipseAdoptium.Temurin.21.JDK
winget install -e --id Apache.Maven
winget install -e --id Docker.DockerDesktop
winget install -e --id Python.Python.3.12
winget install -e --id OpenJS.NodeJS.LTS
winget install -e --id JetBrains.IntelliJIDEA.Community
```
Si `winget install Apache.Maven` falla, no pasa nada: IntelliJ trae Maven incluido
(panel **Maven** a la derecha), o se puede bajar el zip de maven.apache.org y agregar `bin` al PATH.

Docker Desktop: al abrirlo por primera vez te pide activar **WSL 2**. Acepta y reinicia.

### macOS (con Homebrew)
```bash
brew install git openjdk@21 maven python@3.12 node
brew install --cask docker intellij-idea-ce
```

### Opcionales (utiles para la demo y el informe)
- **Postman** o **SoapUI** (probar REST y SOAP): `winget install -e --id Postman.Postman`
- **k6** (prueba de carga): `winget install -e --id GrafanaLabs.k6`
- **VS Code** + extension *REST Client* (para correr `tests/http/demo.http`)

## 2. Verificar que todo quedo instalado
Cerrar y abrir la terminal, y correr:
```bash
git --version        # 2.x
java -version        # 21
mvn -version         # 3.9.x (y que diga Java 21)
docker --version     # y Docker Desktop abierto
python --version     # 3.12
node --version       # 20 o 22
```
Si `java -version` muestra otra version, configurar `JAVA_HOME` apuntando al JDK 21.

## 3. Crear el repositorio publico en GitHub
1. Crear cuenta en github.com (si tenes mail de la facu, aplica al GitHub Student Developer Pack).
2. **New repository** -> nombre `travelhub` -> **Public** -> sin README (ya lo trae el proyecto).
3. En la carpeta del proyecto:
```bash
git init -b main
git add .
git commit -m "chore: estructura inicial de TravelHub"
git remote add origin https://github.com/TU_USUARIO/travelhub.git
git push -u origin main
```
4. Reemplazar `USUARIO` en el badge del README por tu usuario.

## 4. Configurar GitHub para trabajar con PR
**Settings -> General -> Pull Requests**
- Dejar solo **Allow squash merging**.
- Activar **Automatically delete head branches**.

**Settings -> Branches -> Add branch protection rule** (patron `main`), *despues* de que el CI corrio una vez:
- Require a pull request before merging (aprobaciones: 0 mientras estes solo, 1 cuando se sumen).
- Require status checks to pass: `java-build-y-tests`, `ia-tests`, `frontend-build`.
- Require branches to be up to date before merging.
- Do not allow bypassing the above settings.

**Issues -> Milestones**: crear *Entrega parcial 12/10*, *Diseño 26/10*, *TPO 16/11*.

## 5. Primera ejecucion (todo en Docker)
```bash
cp .env.example .env          # en Windows: copy .env.example .env
docker compose up -d --build  # la primera vez tarda 10-15 min (descarga imagenes y dependencias)
docker compose ps             # todos en "running" / "healthy"
```

| Que | URL |
|---|---|
| App web (frontend + gateway) | http://localhost:8080 |
| Swagger catalogo / precios / reservas / pagos | http://localhost:8081/swagger-ui.html (8082, 8083, 8084) |
| WSDL del SOAP | http://localhost:8082/ws/cotizacion.wsdl |
| Docs del servicio de IA | http://localhost:8000/docs |
| Consola RabbitMQ (guest / guest) | http://localhost:15672 |
| Bandeja de correo (Mailpit) | http://localhost:8025 |

Para apagar: `docker compose down` (agregar `-v` borra tambien las bases de datos).

## 6. Activar el LLM local (opcional)
```bash
docker compose --profile llm up -d
docker compose exec ollama ollama pull llama3.2:1b     # ~1.3 GB, una sola vez
```
Sin esto la IA funciona igual: el resumen se genera con plantilla (degradacion elegante).

## 7. Activar las APIs reales (opcional, cuando quieras mostrarlo)
1. **Duffel**: registrarse en app.duffel.com, entrar en modo *Test* y crear un *access token* (empieza con `duffel_test_`).
2. **Hotelbeds**: registrarse en developer.hotelbeds.com. Te da una key y un secret para Hotel y otra para Activities (50 requests por dia).
3. Completar el `.env`, poner `PROVEEDORES_MODO=real` y reiniciar el catalogo:
```bash
docker compose up -d catalogo
```
Si una API falla o se agota la cuota, el catalogo cae solo a datos simulados.

## 8. Desarrollar en el dia a dia (con el IDE)
```bash
docker compose up -d postgres rabbitmq redis mailpit   # solo infraestructura
mvn clean install                                      # compila y corre los tests
```
En IntelliJ: **File -> Open** la carpeta raiz; correr cada `*Application.java` con el boton verde.

IA en local:
```bash
cd ia-service
python -m venv .venv
.venv\Scripts\activate          # macOS/Linux: source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
python -m pytest -q
python evaluar_modelo.py        # matriz de confusion para el informe
```

Frontend en local:
```bash
cd frontend
npm install
npm run dev                     # http://localhost:5173 (usa el gateway de Docker en :8080)
```

## 9. Flujo con Pull Requests
```bash
git checkout main && git pull
git checkout -b feat/12-mi-cambio
# ...cambios...
mvn clean verify
git commit -m "feat(precios): descripcion corta"
git push -u origin feat/12-mi-cambio
```
Abrir el PR en GitHub, completar la plantilla, esperar el CI en verde y hacer *Squash and merge*.
Mas detalle en `CONTRIBUTING.md`.

## 10. Problemas comunes
| Sintoma | Solucion |
|---|---|
| `port is already allocated` | Otro programa usa el puerto (ej. un Postgres local). Cerrarlo o cambiar el puerto en `docker-compose.yml`. |
| Un servicio se reinicia en loop | `docker compose logs -f NOMBRE` y ver el error. Suele ser que Postgres o RabbitMQ todavia no arrancaron: esperar 1 minuto. |
| `mvn` usa Java 17 | Configurar `JAVA_HOME` al JDK 21 y abrir otra terminal. |
| Testcontainers se saltea | Es normal si Docker no esta abierto; en GitHub Actions si corre. |
| Cambie codigo y en Docker no se ve | `docker compose up -d --build NOMBRE` |
| La base quedo con datos viejos | `docker compose down -v` y volver a levantar. |
