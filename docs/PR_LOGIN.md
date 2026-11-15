# PR: Login con email y contraseña, sesión con JWT y Mi perfil

## Por qué
Antes se "ingresaba" solo con el email: cualquiera que supiera el email de otra persona podía ver y cancelar sus reservas.
Además `GET /api/viajeros` listaba a todos los viajeros y `GET /api/reservas?viajeroId=N` devolvía las reservas de cualquiera.
Esto cubre el requerimiento de **privacidad de datos** del Integrador.

## Cambios
| Qué | Dónde | Detalle |
|---|---|---|
| Contraseñas | `reservas/auth/AuthService` | BCrypt. Nunca se guarda ni se devuelve en texto plano. Mínimo 8 caracteres, máximo 72 (límite de BCrypt) |
| Sesión | `reservas/seguridad` | JWT firmado con HS256 (Spring Security OAuth2 Resource Server). Dura `JWT_DURACION` (8 h por defecto). Sin estado: no hay sesiones en el servidor |
| Autorización | `ReservaController`, `CreditoController`, `SeguimientoController`, `ViajeroController` | Cada viajero solo ve y opera lo suyo: 401 sin sesión, 403 si pide datos de otro |
| Endpoints nuevos | `POST /api/auth/registro`, `POST /api/auth/login`, `POST /api/auth/activar`, `GET/PUT /api/viajeros/me`, `PUT /api/viajeros/me/password` | Documentados en Swagger, con botón *Authorize* |
| Perfil | `Viajero` + migración `V4` | Teléfono, aeropuerto habitual (precarga el buscador), preferencias (las usa la IA) y último ingreso |
| Errores | `travelhub-common/web` | `UnauthorizedException` (401) y `ForbiddenException` (403) con el mismo formato `ApiError` de siempre |
| Frontend | `LoginModal.jsx`, `Perfil.jsx`, `api.js`, `App.jsx` | Contraseña en el ingreso, pantalla "Mi perfil" (clic en tu nombre), el token viaja en `Authorization`, y si vence se pide ingresar de nuevo |
| Gateway | `gateway/nginx.conf` | Ruta `/api/auth/` hacia reservas |

## Qué sigue igual (no se rompe nada)
- **Buscar, cotizar, el asistente y el kit** no piden cuenta (son otros servicios).
- **Viaje compartido**: los amigos siguen pagando con el link, sin cuenta (`/api/reservas/compartidas/**` es público).
- **Saga, pagos y notificaciones** se comunican por RabbitMQ, así que no pasan por el filtro de seguridad.
- **Cuentas creadas antes de este cambio**: al ingresar, el sistema les pide crear su contraseña confirmando su documento (`/api/auth/activar`).
- `POST /api/viajeros` (alta sin contraseña) sigue funcionando por compatibilidad; está marcado como *deprecated* en Swagger.
- `viajeroId` en el body de una reserva o un seguimiento pasa a ser opcional: si viene, tiene que ser el de la sesión.

## Decisiones de diseño
- **La seguridad vive en `reservas-service`**, que es el dueño de los datos de los viajeros. El gateway (nginx) solo enruta: validar JWT en nginx requiere módulos extra (OpenResty/njs) y no suma para el alcance del TP.
- **HS256 con una clave compartida** en vez de un servidor de identidad (Keycloak): un solo servicio emite y valida el token, así que no hace falta criptografía asimétrica. Si otro servicio necesitara validar tokens, se pasaría a RS256 con una clave pública.
- **Mismo mensaje para "email inexistente" y "contraseña incorrecta"**, y el servidor tarda lo mismo en los dos casos: no se puede averiguar qué emails tienen cuenta.
- **Los logs enmascaran el email** (`a***@test.com`) y nunca registran contraseñas ni tokens.
- **El token se guarda en `localStorage`** porque la app es una SPA que llama a la API con `fetch`. Para producción convendría una cookie `HttpOnly` + protección CSRF.

## Configuración
```env
JWT_SECRET=una-clave-propia-de-al-menos-32-caracteres   # openssl rand -base64 48
JWT_DURACION=8h
```
Si no se define, se usa una clave de desarrollo (solo para correr local). Con menos de 32 caracteres el servicio no arranca.

## Pruebas
| Test | Qué verifica |
|---|---|
| `TokenServiceTest` | El token se valida; se rechaza si está adulterado, vencido o firmado con otra clave; la clave exige 32 caracteres |
| `AuthServiceTest` | Hash BCrypt, email duplicado, mismo error para email inexistente y contraseña mala, activación de cuentas viejas, cambio de contraseña |
| `SeguridadWebTest` | Capa HTTP con tokens reales: 401 sin sesión, 403 con datos ajenos, el link de viaje compartido sigue público, validación de la contraseña |

## Cómo probar
1. `docker compose up -d --build` (Flyway aplica `V4` solo; no hace falta `down -v`).
2. En http://localhost:8080: **Ingresar → Crear cuenta** con contraseña. Clic en tu nombre → **Mi perfil**: cambiar el aeropuerto habitual y ver que el buscador lo usa.
3. Cerrar sesión y volver a ingresar con una contraseña incorrecta: "Email o contraseña incorrectos".
4. Con una cuenta creada antes de este cambio: ingresar → pide documento y contraseña nueva.
5. `tests/http/demo.http`, pasos 3 a 3e: 401 sin token, 403 con otro `viajeroId`, perfil.
6. Swagger de reservas (http://localhost:8083/swagger-ui.html): **Authorize** con el token del login.
