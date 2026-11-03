-- Ajustes para bases de datos ya existentes (Hibernate ddl-auto=update no elimina restricciones NOT NULL).
-- Idempotente: puede ejecutarse varias veces.

-- La presunta persona agresora ahora puede asociarse a una solicitud sin caso.
ALTER TABLE presuntoagresor ALTER COLUMN idcaso DROP NOT NULL;
