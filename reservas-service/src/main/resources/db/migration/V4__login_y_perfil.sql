-- Login con email y contrasena + datos de perfil.
-- Todas las columnas son opcionales: los viajeros creados antes de esta version siguen
-- funcionando y crean su contrasena la primera vez que ingresan (POST /api/auth/activar).

ALTER TABLE viajero ADD COLUMN password_hash     VARCHAR(100);
ALTER TABLE viajero ADD COLUMN telefono          VARCHAR(30);
ALTER TABLE viajero ADD COLUMN aeropuerto_origen VARCHAR(3);
ALTER TABLE viajero ADD COLUMN ultimo_ingreso    TIMESTAMP WITH TIME ZONE;
