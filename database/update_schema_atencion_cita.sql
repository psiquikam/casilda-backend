-- Relaciona cada registro de atención con la cita que lo originó.
-- Permite varias atenciones por caso y evita duplicar la misma cita.
ALTER TABLE atencion
    ADD COLUMN IF NOT EXISTS idcita bigint;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'atencion_idcita_fkey'
    ) THEN
        ALTER TABLE atencion
            ADD CONSTRAINT atencion_idcita_fkey
            FOREIGN KEY (idcita) REFERENCES cita(id) ON DELETE NO ACTION;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS atencion_cita_unique_idx
    ON atencion (idcita)
    WHERE idcita IS NOT NULL;
