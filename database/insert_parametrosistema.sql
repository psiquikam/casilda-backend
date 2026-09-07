-- Script: Inicialización de la tabla parametrosistema
-- Parámetros configurables del sistema CASILDA

-- Insertar parámetro de máximo de llamadas de contacto telefónico
-- Valor por defecto: 2 (segunda llamada activa asignación unilateral)
INSERT INTO parametrosistema (clave, valor)
VALUES ('MAX_LLAMADAS_CONTACTO', '2')
ON CONFLICT (clave) DO NOTHING;
