BEGIN;

CREATE TABLE IF NOT EXISTS instanciaremision (
    id integer NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    idtiporemision integer NOT NULL,
    CONSTRAINT instanciaremision_pkey PRIMARY KEY (id),
    CONSTRAINT fk_instanciaremision_tiporemision FOREIGN KEY (idtiporemision) REFERENCES tiporemision(id) ON DELETE NO ACTION
);

CREATE TEMPORARY TABLE instancias_remision_iniciales (
    id integer PRIMARY KEY,
    nombre text NOT NULL,
    idtiporemision integer NOT NULL
) ON COMMIT DROP;

INSERT INTO instancias_remision_iniciales (id, nombre, idtiporemision) VALUES
    (1, 'Seguridad a personas y bienes', 1),
    (2, 'Policía', 2),
    (3, 'Fiscalía General de la Nación', 2),
    (4, 'IPS-Salud', 2),
    (5, 'EPS-Salud', 2);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM tiporemision WHERE id = 1 AND nombre = 'Interna')
       OR NOT EXISTS (SELECT 1 FROM tiporemision WHERE id = 2 AND nombre = 'Externa') THEN
        RAISE EXCEPTION 'Los tipos de remisión esperados son 1: Interna y 2: Externa';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM instanciaremision actual
        JOIN instancias_remision_iniciales esperado ON actual.id = esperado.id
        WHERE actual.nombre IS DISTINCT FROM esperado.nombre
           OR actual.idtiporemision IS DISTINCT FROM esperado.idtiporemision
    ) THEN
        RAISE EXCEPTION 'Hay IDs de instancias de remisión con datos diferentes; revisar antes de continuar';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM instanciaremision actual
        JOIN instancias_remision_iniciales esperado
            ON actual.nombre = esperado.nombre AND actual.idtiporemision = esperado.idtiporemision
        WHERE actual.id <> esperado.id
    ) THEN
        RAISE EXCEPTION 'Hay instancias de remisión con el mismo nombre y otro ID; revisar antes de continuar';
    END IF;
END $$;

INSERT INTO instanciaremision (id, nombre, idtiporemision)
SELECT esperado.id, esperado.nombre, esperado.idtiporemision
FROM instancias_remision_iniciales esperado
WHERE NOT EXISTS (SELECT 1 FROM instanciaremision actual WHERE actual.id = esperado.id);

COMMIT;
