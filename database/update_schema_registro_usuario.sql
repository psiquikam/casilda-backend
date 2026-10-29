ALTER TABLE persona
    ALTER COLUMN idciudadnacimiento DROP NOT NULL;

ALTER TABLE persona
    ADD COLUMN IF NOT EXISTS idvinculoudea INTEGER REFERENCES vinculoudea(id);

ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS idpersona BIGINT REFERENCES persona(id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_usuario_idpersona
    ON usuario (idpersona);

ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS habeasdata BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS fechahabeasdata TIMESTAMP;

ALTER TABLE usuario
    ALTER COLUMN nombre TYPE VARCHAR(403);
