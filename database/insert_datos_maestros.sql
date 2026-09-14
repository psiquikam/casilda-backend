-- ============================================================================
-- SCRIPT DE INSERCIÓN DE DATOS MAESTROS (CATÁLOGOS)
-- Sistema CASILDA - FNSP
-- Generado a partir de EV-I-02-25 D1.xlsm
-- ============================================================================

-- Ejecutar después de iniciar la aplicación al menos una vez.
-- Hibernate (ddl-auto=update) crea y actualiza las columnas del esquema.

-- ============================================================================
-- 1. TIPOS DE CORREO
-- ============================================================================
INSERT INTO tipocorreo (id, nombre) VALUES
(1, 'Correo Institucional'),
(2, 'Correo Personal');

-- ============================================================================
-- 2. SEXO (Sexo biológico)
-- ============================================================================
INSERT INTO sexo (id, codigo, nombre) VALUES
(1, 'M', 'Mujer'),
(2, 'H', 'Hombre'),
(3, 'NB', 'No binario'),
(4, 'PR', 'Prefiere no responder');

-- ============================================================================
-- 3. TIPOS DE IDENTIFICACIÓN
-- ============================================================================
INSERT INTO tipoidentificacion (id, codigo, nombre) VALUES
(1, 'TI', 'Tarjeta de Identidad'),
(2, 'CC', 'Cédula de Ciudadanía'),
(3, 'CE', 'Cédula de Extranjería'),
(4, 'PA', 'Pasaporte'),
(5, 'PPT', 'PPT'),
(6, 'NUIP', 'NUIP'),
(7, 'OT', 'Otro');

-- ============================================================================
-- 4. ETNIAS
-- ============================================================================
INSERT INTO etnia (id, nombre) VALUES
(1, 'Afrodescendiente'),
(2, 'Indígena'),
(3, 'Palenquero-a'),
(4, 'Gitano-a (Rrom)'),
(5, 'Raizal'),
(6, 'Ningún grupo étnico');

-- ============================================================================
-- 5. IDENTIDADES DE GÉNERO
-- ============================================================================
INSERT INTO identidadgenero (id, nombre) VALUES
(1, 'Mujer cisgénero'),
(2, 'Hombre cisgénero'),
(3, 'Mujer trans'),
(4, 'Hombre trans'),
(5, 'No binaria'),
(6, 'Prefiere no responder'),
(7, 'Otra'),
(8, 'SD');

-- ============================================================================
-- 6. ORIENTACIÓN SEXUAL
-- ============================================================================
INSERT INTO orientacionsexual (id, nombre) VALUES
(1, 'Asexual'),
(2, 'Bisexual'),
(3, 'Heterosexual'),
(4, 'Homosexual'),
(5, 'Lesbiana'),
(6, 'Prefiere no responder'),
(7, 'Otra');

-- ============================================================================
-- 7. DEPARTAMENTOS (Principales de Colombia)
-- ============================================================================
INSERT INTO departamento (id, codigo, nombre) VALUES
(1, '05', 'Antioquia'),
(2, '08', 'Atlántico'),
(3, '11', 'Bogotá D.C.'),
(4, '13', 'Bolívar'),
(5, '15', 'Boyacá'),
(6, '17', 'Caldas'),
(7, '18', 'Caquetá'),
(8, '19', 'Cauca'),
(9, '20', 'Cesar'),
(10, '23', 'Córdoba'),
(11, '25', 'Cundinamarca'),
(12, '27', 'Chocó'),
(13, '41', 'Huila'),
(14, '44', 'La Guajira'),
(15, '47', 'Magdalena'),
(16, '50', 'Meta'),
(17, '52', 'Nariño'),
(18, '54', 'Norte de Santander'),
(19, '63', 'Quindío'),
(20, '66', 'Risaralda'),
(21, '68', 'Santander'),
(22, '70', 'Sucre'),
(23, '73', 'Tolima'),
(24, '76', 'Valle del Cauca'),
(25, '81', 'Arauca'),
(26, '85', 'Casanare'),
(27, '86', 'Putumayo'),
(28, '88', 'San Andrés'),
(29, '91', 'Amazonas'),
(30, '94', 'Guainía'),
(31, '95', 'Guaviare'),
(32, '97', 'Vaupés'),
(33, '99', 'Vichada');

-- ============================================================================
-- 8. MUNICIPIOS DE ANTIOQUIA (Principales)
-- ============================================================================
INSERT INTO municipio (id, codigo, nombre, iddepartamento) VALUES
(1, '05001', 'Medellín', 1),
(2, '05002', 'Abejorral', 1),
(3, '05004', 'Abriaquí', 1),
(4, '05021', 'Alejandría', 1),
(5, '05030', 'Amagá', 1),
(6, '05031', 'Amalfi', 1),
(7, '05034', 'Andes', 1),
(8, '05036', 'Angelópolis', 1),
(9, '05038', 'Angostura', 1),
(10, '05040', 'Anorí', 1),
(11, '05044', 'Santafé de Antioquia', 1),
(12, '05045', 'Anza', 1),
(13, '05051', 'Apartadó', 1),
(14, '05055', 'Arboletes', 1),
(15, '05059', 'Argelia', 1),
(16, '05079', 'Barbosa', 1),
(17, '05086', 'Belmira', 1),
(18, '05088', 'Bello', 1),
(19, '05091', 'Betania', 1),
(20, '05093', 'Betulia', 1),
(21, '05101', 'Ciudad Bolívar', 1),
(22, '05107', 'Briceño', 1),
(23, '05113', 'Buriticá', 1),
(24, '05120', 'Cáceres', 1),
(25, '05125', 'Caicedo', 1),
(26, '05129', 'Caldas', 1),
(27, '05134', 'Campamento', 1),
(28, '05138', 'Cañasgordas', 1),
(29, '05142', 'Caracolí', 1),
(30, '05145', 'Caramanta', 1),
(31, '05147', 'Carepa', 1),
(32, '05148', 'El Carmen de Viboral', 1),
(33, '05150', 'Carolina', 1),
(34, '05154', 'Caucasia', 1),
(35, '05172', 'Chigorodó', 1),
(36, '05190', 'Cisneros', 1),
(37, '05197', 'Cocorná', 1),
(38, '05206', 'Concepción', 1),
(39, '05209', 'Concordia', 1),
(40, '05212', 'Copacabana', 1),
(41, '05234', 'Dabeiba', 1),
(42, '05237', 'Donmatías', 1),
(43, '05240', 'Ebéjico', 1),
(44, '05250', 'El Bagre', 1),
(45, '05264', 'Entrerríos', 1),
(46, '05266', 'Envigado', 1),
(47, '05282', 'Fredonia', 1),
(48, '05284', 'Frontino', 1),
(49, '05306', 'Giraldo', 1),
(50, '05308', 'Girardota', 1),
(51, '05310', 'Gómez Plata', 1),
(52, '05315', 'Granada', 1),
(53, '05318', 'Guadalupe', 1),
(54, '05321', 'Guarne', 1),
(55, '05347', 'Guatapé', 1),
(56, '05353', 'Heliconia', 1),
(57, '05360', 'Hispania', 1),
(58, '05361', 'Itagüí', 1),
(59, '05364', 'Ituango', 1),
(60, '05368', 'Jardín', 1),
(61, '05376', 'Jericó', 1),
(62, '05380', 'La Ceja', 1),
(63, '05390', 'La Estrella', 1),
(64, '05400', 'La Pintada', 1),
(65, '05411', 'La Unión', 1),
(66, '05425', 'Liborina', 1),
(67, '05440', 'Maceo', 1),
(68, '05467', 'Marinilla', 1),
(69, '05475', 'Montebello', 1),
(70, '05480', 'Murindó', 1),
(71, '05483', 'Mutatá', 1),
(72, '05490', 'Nariño', 1),
(73, '05495', 'Necoclí', 1),
(74, '05501', 'Nechí', 1),
(75, '05541', 'Olaya', 1),
(76, '05543', 'Peñol', 1),
(77, '05576', 'Peque', 1),
(78, '05579', 'Pueblorrico', 1),
(79, '05585', 'Puerto Berrío', 1),
(80, '05591', 'Puerto Nare', 1),
(81, '05604', 'Puerto Triunfo', 1),
(82, '05607', 'Remedios', 1),
(83, '05615', 'Retiro', 1),
(84, '05628', 'Rionegro', 1),
(85, '05631', 'Sabanalarga', 1),
(86, '05642', 'Sabaneta', 1),
(87, '05647', 'Salgar', 1),
(88, '05649', 'San Andrés de Cuerquia', 1),
(89, '05652', 'San Carlos', 1),
(90, '05656', 'San Francisco', 1),
(91, '05658', 'San Jerónimo', 1),
(92, '05659', 'San José de la Montaña', 1),
(93, '05660', 'San Juan de Urabá', 1),
(94, '05664', 'San Luis', 1),
(95, '05665', 'San Pedro', 1),
(96, '05667', 'San Pedro de Uraba', 1),
(97, '05670', 'San Rafael', 1),
(98, '05674', 'San Roque', 1),
(99, '05679', 'San Vicente', 1),
(100, '05686', 'Santa Bárbara', 1),
(101, '05690', 'Santa Rosa de Osos', 1),
(102, '05697', 'Santo Domingo', 1),
(103, '05736', 'El Santuario', 1),
(104, '05756', 'Segovia', 1),
(105, '05761', 'Sonsón', 1),
(106, '05789', 'Sopetrán', 1),
(107, '05790', 'Támesis', 1),
(108, '05792', 'Tarazá', 1),
(109, '05809', 'Tarso', 1),
(110, '05819', 'Titiribí', 1),
(111, '05837', 'Toledo', 1),
(112, '05842', 'Turbo', 1),
(113, '05847', 'Uramita', 1),
(114, '05854', 'Urrao', 1),
(115, '05856', 'Valdivia', 1),
(116, '05858', 'Valparaíso', 1),
(117, '05861', 'Vegachí', 1),
(118, '05873', 'Venecia', 1),
(119, '05885', 'Vigía del Fuerte', 1),
(120, '05887', 'Yalí', 1),
(121, '05890', 'Yarumal', 1),
(122, '05893', 'Yolombó', 1),
(123, '05895', 'Yondó', 1),
(124, '05898', 'Zaragoza', 1);

-- ============================================================================
-- 9. TIPOS DE DISCAPACIDAD
-- ============================================================================
INSERT INTO tipodiscapacidad (id, nombre) VALUES
(1, 'Física o Motora'),
(2, 'Sensorial'),
(3, 'Intelectual'),
(4, 'Psíquica o Psicosocial');

-- Insertar los Subtipos (relacionados por el ID del tipo)
INSERT INTO subtipodiscapacidad (id, nombre, idtipo) VALUES
-- Subtipos de Física o Motora (ID 1)
(1, 'Dificultades motoras (parálisis, amputaciones, distrofias, malformaciones)', 1),
(2, 'Afectaciones neurológicas que limitan el desplazamiento', 1),

-- Subtipos de Sensorial (ID 2)
(3, 'Visual (Baja visión a ceguera total)', 2),
(4, 'Auditiva (Sordera o hipoacusia)', 2),
(5, 'Olfativa y del gusto', 2),

-- Subtipos de Intelectual (ID 3)
(6, 'Síndrome de Down', 3),
(7, 'Retraso en el desarrollo', 3),
(8, 'Dificultades de aprendizaje', 3),

-- Subtipos de Psíquica o Psicosocial (ID 4)
(9, 'Trastornos del espectro autista (TEA)', 4),
(10, 'Esquizofrenia', 4),
(11, 'Trastorno bipolar', 4),
(12, 'Depresión mayor', 4),
(13, 'Trastornos de pánico', 4);


-- ============================================================================
-- 10. TIPOS DE TELÉFONO
-- ============================================================================
INSERT INTO tipotelefono (id, nombre) VALUES
(1, 'Celular'),
(2, 'Teléfono Fijo'),
(3, 'WhatsApp'),
(4, 'Oficina');

-- ============================================================================
-- 11. CAMPUS
-- ============================================================================
INSERT INTO campus (id, nombre) VALUES
(1, 'Campus Apartadó'),
(2, 'Campus Carepa'),
(3, 'Campus Turbo'),
(4, 'Campus Caucasia'),
(5, 'Campus Santa Fe de Antioquia'),
(6, 'Campus Yarumal'),
(7, 'Campus Amalfi'),
(8, 'Campus Segovia'),
(9, 'Campus Andes'),
(10, 'Campus La Pintada'),
(11, 'Campus El Carmen de Viboral'),
(12, 'Campus Sonsón'),
(13, 'Campus Puerto Berrío'),
(14, 'Campus Medellín');

-- ============================================================================
-- 12. UNIDADES ADMINISTRATIVAS
-- ============================================================================
INSERT INTO public.unidadadministrativa (id, nombre) VALUES
(1, 'Rectoría'),
(2, 'Secretaría General'),
(3, 'Vicerrectoría General'),
(4, 'Vicerrectoría de Docencia'),
(5, 'Vicerrectoría de Investigación'),
(6, 'Vicerrectoría de Extensión'),
(7, 'Vicerrectoría Administrativa'),
(8, 'Dirección Jurídica'),
(9, 'Dirección de Bienestar Universitario'),
(10, 'Dirección de Posgrado'),
(11, 'Dirección de Regionalización'),
(12, 'Dirección de Planeación y Desarrollo Institucional'),
(13, 'Dirección de Relaciones Internacionales'),
(14, 'Dirección de Comunicaciones'),
(15, 'Oficina de Auditoría Institucional'),
(16, 'No Aplica');

-- ============================================================================
-- 13. UNIDAD ACADÉMICA
-- ============================================================================
INSERT INTO public.unidadacademica (id, nombre) VALUES
(1, 'Escuela de Microbiología'),
(2, 'Escuela de Nutrición y Dietética'),
(3, 'Escuela de Idiomas'),
(4, 'Escuela Interamericana de Bibliotecología'),
(5, 'Facultad de Artes'),
(6, 'Facultad de Ciencias Agrarias'),
(7, 'Facultad de Ciencias Económicas'),
(8, 'Facultad de Ciencias Exactas y Naturales'),
(9, 'Facultad de Ciencias Farmacéuticas y Alimentarias'),
(10, 'Facultad de Ciencias Sociales y Humanas'),
(11, 'Facultad de Comunicaciones'),
(12, 'Facultad de Derecho y Ciencias Políticas'),
(13, 'Facultad de Educación'),
(14, 'Facultad de Enfermería'),
(15, 'Facultad de Ingeniería'),
(16, 'Facultad de Medicina'),
(17, 'Facultad de Odontología'),
(18, 'Facultad Nacional de Salud Pública'),
(19, 'Instituto de Estudios Políticos'),
(20, 'Instituto de Estudios Regionales'),
(21, 'Instituto de Filosofía'),
(22, 'Instituto Universitario de Educación Física y Deportes'),
(23, 'No Aplica');


-- ============================================================================
-- 14. PROGRAMAS
-- ============================================================================

INSERT INTO public.programa (id, nombre, idunidadacademica, aplicapregrado, aplicaposgrado) VALUES
(1, 'Doctorado en Ciencias Microbiológicas', 1, false, true),
(2, 'Especialización en Microbiología Clínica', 1, false, true),
(3, 'Maestría en Microbiología', 1, false, true),
(4, 'Microbiología Industrial y Ambiental', 1, true, false),    
(5, 'Microbiología y Bioanálisis', 1, true, false),
(6, 'Especialización en Alimentación y Nutrición Humana', 2, false, true),
(7, 'Maestría en Ciencias de la Alimentación y Nutrición', 2, false, true),
(8, 'Nutrición y Dietética', 2, true, false),
(9, 'Especialización en Traducción', 3, false, true),
(10, 'Licenciatura en Lenguas Extranjeras con Énfasis en Inglés', 3, true, false),
(11, 'Licenciatura en Lenguas Extranjeras con Énfasis en Inglés y Francés', 3, true, false),
(12, 'Maestría en Enseñanza y Aprendizaje de Idiomas', 3, false, true),
(13, 'Traducción Inglés- Francés- Español', 3, true, false),
(14, 'Archivística', 4, true, false),
(15, 'Bibliotecología', 4, true, false),
(16, 'Arte Dramático', 5, true, false),
(17, 'Artes Plásticas', 5, true, false),
(18, 'Creación Digital', 5, true, false),
(19, 'Doctorado en Artes', 5, false, true),
(20, 'Gestión Cultural', 5, true, false),
(21, 'Licenciatura en Artes Escénicas', 5, true, false),
(22, 'Licenciatura en Artes Plásticas', 5, true, false),
(23, 'Licenciatura en Danza', 5, true, false),
(24, 'Licenciatura en Música', 5, true, false),
(25, 'Maestría en Dramaturgia y Dirección', 5, false, true),
(26, 'Maestría en Gestión Cultural', 5, false, true),
(27, 'Maestría en Historia del Arte', 5, false, true),
(28, 'Maestría en Músicas de América Latina y el Caribe', 5, false, true),
(29, 'Música', 5, true, false),
(30, 'Música y Canto', 5, true, false),
(31, 'Doctorado en Ciencias Agrarias', 6, false, true),
(32, 'Especialización en Medicina de Pequeñas Especies', 6, false, true),
(33, 'Ingeniería Agropecuaria', 6, true, false),
(34, 'Maestría en Ciencias Animales', 6, false, true),
(35, 'Medicina Veterinaria', 6, true, false),
(36, 'Tecnología en Gestión de Insumos Agropecuarios', 6, true, false),
(37, 'Zootecnia', 6, true, false),
(38, 'Administración de Empresas', 7, true, false),
(39, 'Contaduría Pública', 7, true, false),
(40, 'Doctorado en Ciencias Económicas', 7, false, true),
(41, 'Economía', 7, true, false),
(42, 'Especialización en Finanzas', 7, false, true),
(43, 'Maestría en Administración', 7, false, true),
(44, 'Maestría en Ciencias Económicas', 7, false, true),
(45, 'Astronomía', 8, true, false),
(46, 'Biología', 8, true, false),
(47, 'Doctorado en Ciencias - Biología', 8, false, true),
(48, 'Doctorado en Ciencias - Física', 8, false, true),
(49, 'Doctorado en Ciencias - Química', 8, false, true),
(50, 'Física', 8, true, false),
(51, 'Maestría en Matemáticas', 8, false, true),
(52, 'Matemáticas', 8, true, false),
(53, 'Química', 8, true, false),
(54, 'Ciencias Culinarias', 9, true, false),
(55, 'Ingeniería de Alimentos', 9, true, false),
(56, 'Química Farmacéutica', 9, true, false),
(57, 'Tecnología en Regencia de Farmacia', 9, true, false),
(58, 'Antropología', 10, true, false),
(59, 'Doctorado en Ciencias Sociales', 10, false, true),
(60, 'Historia', 10, true, false),
(61, 'Maestría en Antropología', 10, false, true),
(62, 'Maestría en Psicología', 10, false, true),
(63, 'Maestría en Sociología', 10, false, true),
(64, 'Psicología', 10, true, false),
(65, 'Sociología', 10, true, false),
(66, 'Trabajo Social', 10, true, false),
(67, 'Comunicación Audiovisual y Multimedial', 11, true, false),
(68, 'Comunicación Social - Periodismo', 11, true, false),
(69, 'Comunicaciones', 11, true, false),
(70, 'Creación Digital', 11, true, false),
(71, 'Español como Lengua Extranjera', 11, true, false),
(72, 'Filología Hispánica', 11, true, false),
(73, 'Periodismo', 11, true, false),
(74, 'Ciencia Política', 12, true, false),
(75, 'Derecho', 12, true, false),
(76, 'Doctorado en Derecho', 12, false, true),
(77, 'Maestría en Derecho', 12, false, true),
(78, 'Doctorado en Educación', 13, false, true),
(79, 'Licenciatura en Ciencias Naturales', 13, true, false),
(80, 'Licenciatura en Ciencias Sociales', 13, true, false),
(81, 'Licenciatura en Educación Básica Primaria', 13, true, false),
(82, 'Licenciatura en Educación Especial', 13, true, false),
(83, 'Licenciatura en Educación Infantil', 13, true, false),
(84, 'Licenciatura en Física', 13, true, false),
(85, 'Licenciatura en Literature y Lengua Castellana', 13, true, false),
(86, 'Licenciatura en Matemáticas', 13, true, false),
(87, 'Licenciatura en Pedagogía de la Madre Tierra', 13, true, false),
(88, 'Maestría en Educación', 13, false, true),
(89, 'Doctorado en Enfermería', 14, false, true),
(90, 'Enfermería', 14, true, false),
(91, 'Maestría en Enfermería', 14, false, true),
(92, 'Bioingeniería', 15, true, false),
(93, 'Doctorado en Ingeniería', 15, false, true),
(94, 'Ingeniería Aeroespacial', 15, true, false),
(95, 'Ingeniería Agroindustrial', 15, true, false),
(96, 'Ingeniería Ambiental', 15, true, false),
(97, 'Ingeniería Bioquímica', 15, true, false),
(98, 'Ingeniería Civil', 15, true, false),
(99, 'Ingeniería de Materiales', 15, true, false),
(100, 'Ingeniería de Sistemas', 15, true, false),
(101, 'Ingeniería de Telecomunicaciones', 15, true, false),
(102, 'Ingeniería Eléctrica', 15, true, false),
(103, 'Ingeniería Electrónica', 15, true, false),
(104, 'Ingeniería en Equipos Biomédicos', 15, true, false),
(105, 'Ingeniería Energética', 15, true, false),
(106, 'Ingeniería Industrial', 15, true, false),
(107, 'Ingeniería Mecánica', 15, true, false),
(108, 'Ingeniería Oceanográfica', 15, true, false),
(109, 'Ingeniería Química', 15, true, false),
(110, 'Ingeniería Sanitaria', 15, true, false),
(111, 'Ingeniería Urbana', 15, true, false),
(112, 'Maestría en Ingeniería', 15, false, true),
(113, 'Doctorado en Ciencias Médicas', 16, false, true),
(114, 'Especialización en Pediatría', 16, false, true),
(115, 'Instrumentación Quirúrgica', 16, true, false),
(116, 'Medicina', 16, true, false),
(117, 'Técnica Profesional en Atención Prehospitalaria', 16, true, false),
(118, 'Odontología', 17, true, false),
(119, 'Administración Ambiental y Sanitaria', 18, true, false),
(120, 'Administración en Salud', 18, true, false),
(121, 'Doctorado en Salud Pública', 18, false, true),
(122, 'Gerencia en Sistemas de Información en Salud', 18, true, false),
(123, 'Maestría en Salud Pública', 18, false, true),
(124, 'Tecnología en Administración de Servicios de Salud', 18, true, false),
(125, 'Tecnología en Saneamiento Ambiental', 18, true, false),
(126, 'Tecnología en Sistemas de Información en Salud', 18, true, false),
(127, 'Doctorado en Filosofía', 21, false, true),
(128, 'Filosofía', 21, true, false),
(129, 'Licenciatura en Filosofía', 21, true, false),
(130, 'Maestría en Filosofía', 21, false, true),
(131, 'Entrenamiento Deportivo', 22, true, false),
(132, 'Licenciatura en Educación Física', 22, true, false),
(133, 'Maestría en Ciencias del Deporte', 22, false, true),
(134, 'No Aplica', 23, true, true);

-- ============================================================================
-- 15. VÍNCULO AGRESOR CON VÍCTIMA
-- ============================================================================
INSERT INTO vinculoagresorvictima (id, nombre) VALUES
(1, 'Pareja/expareja'),
(2, 'Familiar'),
(3, 'Compañeros de estudio'),
(4, 'Compañeros de trabajo'),
(5, 'Docente'),
(6, 'Estudiante'),
(7, 'Jefe'),
(8, 'Comparten lugar de trabajo'),
(9, 'Comparten lugar de estudio'),
(10, 'Amigo/amiga'),
(11, 'Conocido'),
(12, 'Desconocido'),
(13, 'Otro');

-- ============================================================================
-- 16. VÍNCULO CON LA UDEA
-- ============================================================================
INSERT INTO vinculoudea (id, nombre) VALUES
(1, 'Estudiante de Pregrado'),
(2, 'Estudiante de Posgrado'),
(3, 'Egresado Pregrado'),
(4, 'Egresado Posgrado'),
(5, 'Personal Administrativo'),
(6, 'Docente Vinculado'),
(7, 'Docente Ocasional'),
(8, 'Docente de Cátedra'),
(9, 'Contratista'),
(10, 'Otro tipo de vínculo'),
(11, 'Docente Cátedra 50'),
(12, 'Jubilado / Pensionado'),
(13, 'Prestador de Servicios'),
(14, 'Externo')
ON CONFLICT (id) DO UPDATE SET nombre = EXCLUDED.nombre;

-- ============================================================================
-- 17. TIPOS DE SOLICITUD
-- ============================================================================
INSERT INTO tiposolicitud (id, nombre) VALUES
(1, 'Directa'),
(2, 'Indirecta');

-- ============================================================================
-- 18. CARGOS
-- ============================================================================
INSERT INTO cargo (id, nombre) VALUES
(1, 'Psicóloga'),
(2, 'Abogada'),
(3, 'Trabajadora Social'),
(4, 'Psicoorientadora'),
(5, 'Coordinadora de Prevención'),
(6, 'Coordinadora de Atención'),
(7, 'Docente'),
(8, 'Administrativo'),
(9, 'Otro');

-- ============================================================================
-- 19. RESULTADO CONTACTO TELEFÓNICO
-- ============================================================================
INSERT INTO resultadocontactotelefonico (id, nombre) VALUES
(1, 'Contesta y se concerta cita'),
(2, 'Contesta y no se concerta cita'),
(3, 'Contesta y desiste'),
(4, 'No contesta'),
(5, 'Número errado'),
(6, 'N/A');

-- ============================================================================
-- 20. RÉGIMEN DE SALUD
-- ============================================================================
INSERT INTO regimen (id, nombre) VALUES
(1, 'Contributivo'),
(2, 'Subsidiado'),
(3, 'Especial'),
(4, 'No afiliado');

-- ============================================================================
-- 21. EPS (Entidades Promotoras de Salud)
-- ============================================================================
INSERT INTO eps (id, nombre) VALUES
(1, 'Sura EPS'),
(2, 'Salud Total'),
(3, 'Sanitas'),
(4, 'Compensar'),
(5, 'Nueva EPS'),
(6, 'Coomeva EPS'),
(7, 'Famisanar'),
(8, 'Cafesalud'),
(9, 'SOS EPS'),
(10, 'Medimás'),
(11, 'Capital Salud'),
(12, 'Aliansalud'),
(13, 'EPS SAVIA SALUD'),
(14, 'Mutual SER'),
(15, 'Otra'),
(16, 'No tiene');

-- ============================================================================
-- 22. ROLES DE USUARIO
-- ============================================================================
INSERT INTO rol (id, codigo, nombre) VALUES
(1, 'ADMIN', 'Admin'),
(2, 'COORDINADOR', 'Coordinador'),
(3, 'PROFESIONAL', 'Profesional'),
(4, 'REVISOR', 'Revisor'),
(5, 'USUARIO', 'Usuario');


-- ============================================================================
-- 23. ESTADOS DE SOLICITUD
-- ============================================================================
insert into estadosolicitud values(1, 'Sin asignar');
insert into estadosolicitud values(2, 'Asignada');


-- ============================================================================
-- 24. ESTADOS DE CITA
-- ============================================================================
insert into estadocita values(1, 'Creada');
insert into estadocita values(2, 'Cancelada');
insert into estadocita values(3, 'Reprogramada');

-- ============================================================================
-- 25. MOTIVO ESTADO CITA
-- ============================================================================
INSERT INTO public.motivoestadocita (id, nombre) VALUES 
    (1, 'Inasistencia injustificada'),
    (2,'Cambio de agenda de la dupla o la profesional'),
    (3,'Circunstancias externas'),
    (4,'Solicitud de persona a atender'),
    (5,'NA');

-- ============================================================================
-- 26. TIPO ASIGNACIÓN
-- ============================================================================
INSERT INTO tipoasignacion (id, nombre) VALUES
    (1, 'Prioritaria'),
    (2, 'Ordinaria'),
    (3, 'Seguimiento')
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- 27. TIPO SERVICIO
-- ============================================================================
INSERT INTO tiposervicio (id, nombre) VALUES
    (1, 'Psicología'),
    (2, 'Asesoría Jurídica'),
    (3, 'Trabajo Social'),
    (4, 'Dupla Psicojurídica'),
    (5, 'Atención APH')
ON CONFLICT (id) DO NOTHING;    


INSERT INTO formaocurrencia (id, nombre) VALUES
 (1, 'Individual'),
 (2, 'Colectiva'),
 (3, 'Otra');

 insert INTO lugarocurrencia (id, nombre) VALUES
 (1, 'Campus Principal'),
 (2, 'Campus Salud'),
 (3, 'Entorno Virtual'),
 (4, 'Fuera de la Universidad');
 
 
 insert INTO actividadmisional (id, nombre) VALUES
 (1, 'Docencia'),
 (2, 'Investigación'),
 (3, 'Extensión'),
 (4, 'Administrativa');

insert INTO tipoviolencia (id, nombre) VALUES
 (1, 'Psicológica'),
 (2, 'Física'),
 (3, 'Sexual'),
 (4, 'Institucional'),
 (5, 'Económica o patrimonial'),
 (6, 'Sexual informática'),
 (7, 'Por prejuicio');

 --Tipo de violencia psicológica
insert INTO modalidadviolencia (id, nombre, idtipoviolencia) VALUES
(1, 'Difusión de contenido íntimo', 1),
(2, 'Constreñimiento ilegal', 1),
(3, 'Lenguaje misógino, sexista o discursos de odio', 1),
(4, 'Intimidación y amenazas', 1),
(5, 'Aislamiento forzado', 1),
(6, 'Abuso de poder y/o confianza', 1),
(7, 'Injurias por vías de hecho o calumnia', 1);

--Tipo de violencia física 
insert INTO modalidadviolencia (id, nombre, idtipoviolencia) VALUES
(8, 'Violencia intrafamiliar', 2),
(9, 'Violencia de pareja/expareja', 2),
(10, 'Violencia interpersonal', 2),
(11, 'Lesiones personales', 2),
(12, 'Feminicidio (Tentativa o comisión)', 2);

--Tipo de violencia sexual 
insert INTO modalidadviolencia (id, nombre, idtipoviolencia) VALUES
(13, 'Acoso sexual', 3),
(14, 'Acceso carnal', 3),
(15, 'Actos sexuales', 3),
(16, 'Violencia sexual correctiva', 3);

--Tipo de violencia Institucional
insert INTO modalidadviolencia (id, nombre, idtipoviolencia) VALUES
(17, 'Omision del deber de denuncia', 4),
(18, 'Revictimización', 4),
(19, 'Omisión al deber de debida diligencia', 4);

--Tipo de violencia económica/Patrimonial
insert INTO modalidadviolencia (id, nombre, idtipoviolencia) VALUES
(20, 'Inasistencia alimentaria', 5),
(21, 'Hurto', 5),
(22, 'Control económico', 5),
(23, 'Daño en bien ajeno', 5);


--Tipo de violencia sexual informática
insert INTO modalidadviolencia (id, nombre, idtipoviolencia) VALUES
(24, 'Grooming', 6),
(25, 'Pornografía', 6),
(26, 'Sexting', 6),
(27, 'Chantaje sexual o extorsión sexual', 6),
(28, 'Violación de datos personales', 6);

--Tipo de violencia por prejuicio
insert INTO modalidadviolencia (id, nombre, idtipoviolencia) VALUES
(29, 'Discriminación por género u orientación sexual o identidad de género', 7);

--Apreciación de la violencia
insert into apreciacion (id, nombre) values
(1, 'Jurídica'),
(2, 'Psicológica');

insert into tipoapreciacion (id, nombre, idapreciacion) values
(1, 'Penal', 1),
(2, 'Civil', 1),
(3, 'Laboral', 1),
(4, 'Administrativa', 1),
(5, 'Derechos Humanos', 1);

insert into tipoapreciacion (id, nombre, idapreciacion) values
(6, 'Clínica', 2),
(7, 'Familiar', 2),
(8, 'Educativa', 2),
(9, 'Social', 2),
(10, 'Otra', 2);

insert into tiporutaactivacion (id, nombre) values
(1, 'Interna'),
(2, 'Externa');

insert into rutaactivacion (id, nombre) values
(1, 'UAD 3 y 4 (Unidad de asuntos disciplinarios)'),
(2, 'URC (Unidad de resolución de conflictos)'),
(3, 'Defensa técnica'),
(4, 'Línea Alma'),
(5, 'Seguridad a personas y bienes (vigilancia)'),
(6, 'Ruta de atención por amenaza'),
(7, 'Medidas de protección (académicas)'),
(8, 'Medidas de protección (laborales)'),
(9, 'No acepta/No toma ninguna decisión en esta sesión'),
(10, 'No aplica'),
(11, 'Otras');

insert into tiporemision (id, nombre) values
(1, 'Interna'),
(2, 'Externa');

insert into grupoatencion (id, nombre) values
(1, 'Grupo de atención psicológica'),
(2, 'Grupo de atención jurídica'),
(3, 'Grupo de atención en trabajo social'),
(4, 'Grupo de atención en dupla psicosocial');


insert into tiposeguimiento (id, nombre) values
(1, 'Presencial'),
(2, 'Telefónico'),
(3, 'Virtual'),
(4, 'Visita Domiciliaria');

insert into accion (id, nombre) values
(1, 'Seguimiento por psicología'),
(2, 'Seguimiento a activación de ruta interna acordada'),
(3, 'Seguimiento a remisión interna acordada'),
(4, 'Seguimiento a activación de ruta externa acordada'),
(5, 'Seguimiento a remision externa acordada');

insert into actividad (id, idaccion, nombre) values
(1, 1, 'Seguimiento psicológico'),
(2, 2, 'Unidad de Asuntos Disciplinarios - UAD'),
(3, 2, 'Unidad de Resolución de Conflictos - URC'),
(4, 2, 'Seguridad a Personas y Bienes'),
(5, 2, 'Protocolo de Amenaza'),
(6, 2, 'Medidas de Protección Académicas'),
(7, 2, 'Medidas de Protección Laborales'),
(8, 3, 'Asesoría psicojurídica y representación - Convenio de Dirección de Bienestar Universitario'),
(9, 3, 'Línea Violeta te Orienta'),
(10, 3, 'Psiquiatría'),
(11, 3, 'Psicoterapia'),
(12, 3, 'Toxicología'),
(13, 3, 'Nutrición'),
(14, 3, 'Ginecología'),
(15, 3, 'Exámenes ITS'),
(16, 4, 'Ruta de salud'),
(17, 4, 'Fiscalía General de la Nación - FGN'),
(18, 4, 'Comisaría de Familia - CDF'),
(19, 4, 'Inspección de Policía'),
(20, 5, 'Línea 123 Agencia Mujer Medellín - Secretaría de las Mujeres del Distrito de Medellín'),
(21, 5, 'Línea 123 Mujer Metropolitana - Secretaría de las Mujeres del Departamento de Antioquia'),
(22, 5, 'Atención Psicojurídica en Territorio - Secretaría de las Mujeres del Distrito de Medellín'),
(23, 5, 'Defensa Técnica - Secretaría de las Mujeres del Distrito de Medellín'),
(24, 5, 'Defensoría del Pueblo'),
(25, 5, 'Módulo Diverso Línea 123 Social - Secretaría de Inclusión Social, Familia y Derechos Humanos del Distrito de Medellín'),
(26, 5, 'Gerencia de Diversidades Sexuales e Identidades de Género - Distrito de Medellín');

insert into estadoseguimiento (id, nombre) values
(1, 'En Proceso'),
(2, 'Cerrado'),
(3, 'Pendiente');

insert into motivoestadoseguimiento (id, nombre) values
(1, 'Motivo 1'),
(2, 'Motivo 2'),
(3, 'Motivo 3');

insert into estadocaso (id, nombre) values
(1, 'Abierto activo'),
(2, 'Abierto en transición'),
(3, 'Cerrado');

insert into estadoatencion (id, nombre) values
(1, 'Abierto activo'),
(2, 'Abierto en transición'),
(3, 'Cerrado');

-- ============================================================================
-- MEDIO SOLICITUD
-- ============================================================================
INSERT INTO mediosolicitud (id, nombre) VALUES
(1, 'Presencial'),
(2, 'Virtual');

-- ============================================================================
-- TIEMPO OCURRIDO UNIDAD
-- ============================================================================
INSERT INTO tiempoocurridounidad (id, nombre) VALUES
(1, 'días'),
(2, 'semanas'),
(3, 'meses'),
(4, 'años')
ON CONFLICT (id) DO NOTHING;

-- Seed Data
INSERT INTO tipomedida (id, nombre) VALUES
(1, 'Medida de protección académica'),
(2, 'Medida de protección laboral'),
(3, 'Medida de protección personal'),
(4, 'Medida cautelar'),
(5, 'Otra');

INSERT INTO subtipomedida (id, nombre, idtipomedida) VALUES
(1, 'Cambio de grupo', 1),
(2, 'Cambio de horario', 1),
(3, 'Cambio de docente', 1),
(4, 'Traslado de programa', 1),
(5, 'Cambio de área', 2),
(6, 'Cambio de jornada', 2),
(7, 'Comisión de servicio', 2),
(8, 'Acompañamiento institucional', 3),
(9, 'Restricción de acceso', 3),
(10, 'Orden de alejamiento', 4),
(11, 'Prohibición de acercamiento', 4),
(12, 'Otra', 5);

INSERT INTO responsablemedidaproteccion (id, nombre) VALUES
(1, 'Bienestar Universitario'),
(2, 'Dirección de Personal'),
(3, 'Decanatura'),
(4, 'Unidad de Género'),
(5, 'Dirección de Docencia'),
(6, 'Otra unidad administrativa');
