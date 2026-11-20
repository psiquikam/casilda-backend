-- No se infiere la fecha de cierre de los casos históricos.
ALTER TABLE caso ADD COLUMN IF NOT EXISTS fechacierre timestamp with time zone;
