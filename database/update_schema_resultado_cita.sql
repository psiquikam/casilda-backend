-- Estados requeridos para registrar si una cita fue atendida o hubo inasistencia.
-- Idempotente para poder ejecutarse sobre QA y al reconstruir una base local.
INSERT INTO estadocita (id, nombre) VALUES
    (1, 'Programada'),
    (4, 'Atendida'),
    (5, 'Inasistencia')
ON CONFLICT (id) DO UPDATE SET nombre = EXCLUDED.nombre;
