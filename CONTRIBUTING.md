# Como trabajamos en TravelHub

## Regla de oro
**Nadie commitea directo a `main`.** Todo entra por Pull Request con el CI en verde.

## Flujo (GitHub Flow)
1. Tomar o crear un **issue** (una tarea = una rama = un PR).
2. Actualizar `main` y crear la rama:
   ```bash
   git checkout main && git pull
   git checkout -b feat/12-soap-cotizacion
   ```
3. Commits chicos y con mensaje claro (ver convencion abajo).
4. Antes de subir: `mvn clean verify`.
5. `git push -u origin feat/12-soap-cotizacion` y abrir el PR completando la plantilla.
6. Esperar el CI en verde y la revision (cuando haya mas de una persona).
7. Merge con **Squash and merge** y borrar la rama.

## Nombres de ramas
`tipo/numero-issue-descripcion-corta`

| Prefijo | Uso |
|---|---|
| `feat/` | Funcionalidad nueva |
| `fix/` | Correccion |
| `docs/` | Documentacion, diagramas, informe |
| `test/` | Solo pruebas |
| `refactor/` | Cambios internos sin nuevo comportamiento |
| `chore/` | Configuracion, dependencias, CI |

## Mensajes de commit (Conventional Commits)
```
feat(precios): agrega endpoint SOAP CotizarProducto
fix(catalogo): valida codigo IATA de 3 letras
docs: diagrama de componentes para entrega parcial
test(precios): cubre estrategia de anticipacion
chore(ci): cachea dependencias de Maven
```
El scope es el modulo: `common`, `catalogo`, `precios`, `reservas`, `pagos`, `notificaciones`, `ia`, `frontend`, `infra`, `ci`.

## Si main avanzo mientras trabajabas
```bash
git checkout main && git pull
git checkout mi-rama
git rebase main        # resolver conflictos si aparecen
git push --force-with-lease
```

## Que no se sube nunca
- El archivo `.env` ni claves de Duffel, Hotelbeds o Mercado Pago.
- Carpetas `target/`, `.idea/`, `node_modules/`.
