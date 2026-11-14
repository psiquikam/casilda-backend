-- Si existen notas con más de 2000 caracteres, PostgreSQL rechaza el cambio sin truncarlas.
ALTER TABLE atencionaph ALTER COLUMN notaaph TYPE character varying(2000);
