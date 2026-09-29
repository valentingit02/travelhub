-- Viaje compartido: una reserva puede tener varios pagos (uno por participante) y reintentos.
ALTER TABLE pago DROP CONSTRAINT IF EXISTS pago_reserva_id_key;
ALTER TABLE pago ADD COLUMN participante_id BIGINT;
CREATE INDEX ix_pago_reserva_participante ON pago (reserva_id, participante_id);
