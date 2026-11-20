-- Ejecutar después de iniciar la versión que crea datosremision.
-- Las remisiones pendientes no tienen técnico asignado.
ALTER TABLE atencionalmaaph ALTER COLUMN idusuariotomador DROP NOT NULL;

CREATE TABLE IF NOT EXISTS limitebandejaaph (
    idusuario BIGINT PRIMARY KEY REFERENCES usuario(id),
    inicio TIMESTAMP WITH TIME ZONE NOT NULL,
    cantidad INTEGER NOT NULL CHECK (cantidad > 0)
);

INSERT INTO endpoint (path, http_method, activo, publico) VALUES
    ('/alma/remisiones-aph', 'POST', true, false),
    ('/alma/bandeja/aph', 'GET', true, false),
    ('/alma/bandeja/aph/{idAtencion}/tomar', 'POST', true, false)
ON CONFLICT (path, http_method) DO UPDATE SET activo = true, publico = false;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id FROM endpoint e
JOIN rol r ON (e.path = '/alma/remisiones-aph' AND e.http_method = 'POST'
    AND r.codigo IN ('ADMIN', 'EA_COORDINACION', 'PROFESIONAL', 'COORDINADOR',
                     'SPB_ANALISTA', 'UAD_COORDINACION', 'UAD_EQUIPO_3', 'UAD_EQUIPO_4'))
    OR (e.path = '/alma/bandeja/aph' AND e.http_method = 'GET'
    AND r.codigo IN ('ADMIN', 'COORDINADOR', 'REVISOR', 'TECNICO_APH'))
    OR (e.path = '/alma/bandeja/aph/{idAtencion}/tomar' AND e.http_method = 'POST'
    AND r.codigo = 'TECNICO_APH')
ON CONFLICT (idendpoint, idrol) DO NOTHING;
