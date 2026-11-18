-- Solo para pruebas autorizadas. La auditoría de consultas aún no está implementada.
CREATE TABLE IF NOT EXISTS limiteconsultapreviaaph (
    idusuario BIGINT PRIMARY KEY REFERENCES usuario(id),
    inicio TIMESTAMP WITH TIME ZONE NOT NULL,
    cantidad INTEGER NOT NULL CHECK (cantidad > 0)
);

INSERT INTO endpoint (path, http_method, activo, publico) VALUES
    ('/alma/personas/consulta', 'GET', true, false)
ON CONFLICT (path, http_method) DO UPDATE SET activo = true, publico = false;

INSERT INTO endpointrole (idendpoint, idrol)
SELECT e.id, r.id FROM endpoint e
JOIN rol r ON r.codigo IN ('ADMIN', 'TECNICO_APH', 'REVISOR')
WHERE e.path = '/alma/personas/consulta' AND e.http_method = 'GET'
ON CONFLICT (idendpoint, idrol) DO NOTHING;
