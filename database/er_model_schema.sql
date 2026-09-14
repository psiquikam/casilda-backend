CREATE TABLE pais (
    id integer NOT NULL,
    codigo character varying COLLATE pg_catalog."default" UNIQUE NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT pais_pkey PRIMARY KEY (id)
);

CREATE TABLE tipocorreo (
    id integer NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT tipocorreo_pkey PRIMARY KEY (id)
);

CREATE TABLE sexo (
    id integer NOT NULL,
    codigo character varying COLLATE pg_catalog."default" UNIQUE NOT NULL,
    nombre character varying COLLATE pg_catalog."default" UNIQUE NOT NULL,
    CONSTRAINT sexo_pkey PRIMARY KEY (id)
);

CREATE TABLE tipoidentificacion (
    id integer NOT NULL,
    codigo character varying COLLATE pg_catalog."default" UNIQUE NOT NULL,
    nombre character varying COLLATE pg_catalog."default" UNIQUE NOT NULL,
    CONSTRAINT tipoidentificacion_pkey PRIMARY KEY (id)
);

CREATE TABLE etnia (
    id integer NOT NULL,
    nombre character varying COLLATE pg_catalog."default" UNIQUE NOT NULL,
    CONSTRAINT etnia_pkey PRIMARY KEY (id)
);

CREATE TABLE identidadgenero (
    id integer NOT NULL,
    nombre character varying COLLATE pg_catalog."default" UNIQUE NOT NULL,
    CONSTRAINT identidadgenero_pkey PRIMARY KEY (id)
);

CREATE TABLE orientacionsexual (
    id integer NOT NULL,
    nombre character varying COLLATE pg_catalog."default" UNIQUE  NOT NULL,
    CONSTRAINT orientacionsexual_pkey PRIMARY KEY (id)
);

CREATE TABLE departamento (
    id integer NOT NULL,
    codigo character varying COLLATE pg_catalog."default" UNIQUE NOT NULL,
    nombre character varying COLLATE pg_catalog."default" UNIQUE NOT NULL,
    CONSTRAINT departamento_pkey PRIMARY KEY (id)
);

CREATE TABLE municipio (
    id integer NOT NULL,
    codigo character varying COLLATE pg_catalog."default" UNIQUE NOT NULL,
    nombre character varying COLLATE pg_catalog."default" UNIQUE NOT NULL,
    iddepartamento INT REFERENCES departamento(id),
    CONSTRAINT municipio_pkey PRIMARY KEY (id)
);

CREATE TABLE tipodiscapacidad (
    id int PRIMARY KEY,
    nombre character varying COLLATE pg_catalog."default" NOT NULL
);

CREATE TABLE subtipodiscapacidad (
    id int PRIMARY KEY,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    idtipo INTEGER not null,
    CONSTRAINT subtipodiscapacidad_idtipo_fkey FOREIGN KEY (idtipo) REFERENCES tipodiscapacidad(id) ON DELETE NO ACTION
);

CREATE TABLE tipotelefono (
    id integer NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT tipotelefono_pkey PRIMARY KEY (id)
);

CREATE TABLE persona (
    id bigserial NOT NULL,
    primernombre character varying COLLATE pg_catalog."default" NOT NULL,
    segundonombre character varying COLLATE pg_catalog."default",
    primerapellido character varying COLLATE pg_catalog."default" NOT NULL,
    segundoapellido character varying COLLATE pg_catalog."default",
    idtipoidentificacion INT not null,
    numerodocumento character varying COLLATE pg_catalog."default" NOT NULL,
    fechanacimiento timestamp without time zone,
    idsexo INT,
    idciudadnacimiento int not null,
    CONSTRAINT persona_pkey PRIMARY KEY (id),
    constraint persona_idtipoidentificacion_fkey FOREIGN KEY (idtipoidentificacion) REFERENCES tipoidentificacion(id) ON DELETE NO ACTION,
    constraint persona_idsexo_fkey FOREIGN KEY (idsexo) REFERENCES sexo(id) ON DELETE NO ACTION,
    constraint persona_idciudadnacimiento_fkey FOREIGN KEY (idciudadnacimiento) REFERENCES municipio(id) ON DELETE NO ACTION
);

CREATE TABLE discapacidadpersona (
    idpersona bigint NOT NULL,
    idsubtipodiscapacidad INT not null,
    descripcion character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT discapacidadpersona_pkey PRIMARY KEY (idpersona, idsubtipodiscapacidad),
    CONSTRAINT discapacidadpersona_idpersona_fkey FOREIGN KEY (idpersona) REFERENCES persona (id) ON DELETE NO ACTION,
    CONSTRAINT discapacidadpersona_idsubtipodiscapacidad_fkey FOREIGN KEY (idsubtipodiscapacidad) REFERENCES subtipodiscapacidad (id) ON DELETE NO ACTION
);

CREATE TABLE correopersona (
    id bigserial NOT NULL,
    idpersona INT NOT NULL,
    idtipo integer not null,
    correo character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT correopersona_pkey PRIMARY KEY (id),
    CONSTRAINT correopersona_idpersona_fkey FOREIGN KEY (idpersona) REFERENCES persona(id) ON DELETE NO ACTION,
    CONSTRAINT correopersona_idtipo_fkey FOREIGN KEY (idtipo) REFERENCES tipocorreo(id) ON DELETE NO ACTION
);

CREATE TABLE telefonopersona (
    id bigserial not null,
    idpersona INT NOT NULL,
    idtipo integer not null,
    telefono character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT telefonopersona_pkey PRIMARY KEY (id),
    CONSTRAINT telefonopersona_idpersona_fkey FOREIGN KEY (idpersona) REFERENCES persona(id) ON DELETE NO ACTION,
    CONSTRAINT telefonopersona_idtipo_fkey FOREIGN KEY (idtipo) REFERENCES tipotelefono(id) ON DELETE NO ACTION
);

CREATE TABLE campus (
    id integer NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT campus_pkey PRIMARY KEY (id)
);

CREATE TABLE unidadadministrativa (
    id integer NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT unidadadministrativa_pkey PRIMARY KEY (id)
);

CREATE TABLE unidadacademica (
    id integer NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT unidadacademica_pkey PRIMARY KEY (id)
);


CREATE TABLE vinculoagresorvictima (
    id int NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT vinculoagresorvictima_pkey PRIMARY KEY (id)
);

CREATE TABLE vinculoudea (
    id int NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT vinculoudea_pkey PRIMARY KEY (id)
);

CREATE TABLE tiempoocurridounidad (
    id integer NOT NULL,
    nombre character varying NOT NULL,
    CONSTRAINT tiempoocurridounidad_pkey PRIMARY KEY (id)
);

CREATE TABLE formaocurrencia
(
    id integer NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT formaocurrencia_pkey PRIMARY KEY (id)
);

CREATE TABLE lugarocurrencia
(
    id integer NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT lugarocurrencia_pkey PRIMARY KEY (id)
);

CREATE TABLE actividadmisional
(
    id integer NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT actividadmisional_pkey PRIMARY KEY (id)
);

CREATE TABLE tipoviolencia
(
    id integer NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT tipoviolencia_pkey PRIMARY KEY (id)
);

CREATE TABLE modalidadviolencia
(
    id integer NOT NULL,
    nombre character varying(200) COLLATE pg_catalog."default" NOT NULL,
    idtipoviolencia integer NOT NULL,
    CONSTRAINT modalidadviolencia_pkey PRIMARY KEY (id),
    CONSTRAINT modalidadviolencia_idtipoviolencia_fkey FOREIGN KEY (idtipoviolencia)
        REFERENCES public.tipoviolencia (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE TABLE hecho (
    id bigserial NOT NULL,
    idcaso bigint not null,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP not null,
    lugar character varying COLLATE pg_catalog."default" NOT NULL,
    descripcion character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT hecho_pkey PRIMARY KEY (id),
    constraint hecho_idcaso_fkey FOREIGN KEY (idcaso) REFERENCES caso(id) ON DELETE NO ACTION 
);

CREATE TABLE modalidadviolenciacaso (
    idcaso INT not null,
    idmodalidadviolencia  integer NOT NULL,
    CONSTRAINT modalidadviolenciacaso_pkey PRIMARY KEY (idcaso, idmodalidadviolencia),
    CONSTRAINT modalidadviolenciacaso_idcaso_fkey FOREIGN KEY (idcaso) REFERENCES caso(id) ON DELETE NO ACTION,
    CONSTRAINT modalidadviolenciacaso_idmodalidadviolencia_fkey FOREIGN KEY (idmodalidadviolencia) REFERENCES modalidadviolencia(id) ON DELETE NO ACTION
);

CREATE TABLE apreciacion (
    id integer NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT apreciacion_pkey PRIMARY KEY (id)
);

CREATE TABLE tipoapreciacion (
    id integer NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    idapreciacion integer NOT NULL,
    CONSTRAINT tipoapreciacion_pkey PRIMARY KEY (id),
    CONSTRAINT tipoapreciacion_idapreciacion_fkey FOREIGN KEY (idapreciacion) REFERENCES apreciacion(id) ON DELETE NO ACTION
);

CREATE TABLE tiposolicitud (
    id  integer NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT tipo_solicitud_pkey PRIMARY KEY (id)
);


CREATE TABLE estadosolicitud
(
    id integer NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT estadosolicitud_pkey PRIMARY KEY (id)
);

CREATE TABLE estadocita
(
    id integer NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT estadocita_pkey PRIMARY KEY (id)
);

CREATE TABLE programa
(
    id integer NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    idunidadacademica integer NOT NULL,
    aplicapregrado boolean NOT NULL,
    aplicaposgrado boolean NOT NULL,
    CONSTRAINT programa_pkey PRIMARY KEY (id),
    CONSTRAINT programa_idunidadacademica_fkey FOREIGN KEY (idunidadacademica)
        REFERENCES public.unidadacademica (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE TABLE programacaso (
    idcaso int not null,
    idprograma INT not null,
    constraint programacaso_pkey PRIMARY KEY (idcaso, idprograma),
    constraint programacaso_idcaso_fkey FOREIGN KEY (idcaso) REFERENCES caso(id) ON DELETE no action,
    constraint programacaso_idprograma_fkey FOREIGN KEY (idprograma) REFERENCES programa(id) ON DELETE no action  
);

CREATE TABLE rol (
    id int NOT NULL,
    nombre character varying COLLATE pg_catalog."default" UNIQUE NOT NULL,
    codigo character varying(50) COLLATE pg_catalog."default" UNIQUE NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    constraint rol_pkey PRIMARY KEY (id)
);

CREATE TABLE usuario (
    id bigserial not null,
    email character varying COLLATE pg_catalog."default" UNIQUE NOT NULL,
    password character varying COLLATE pg_catalog."default" NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    idrol INT not null,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fechacreacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fechaactualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    constraint usuario_pkey PRIMARY KEY (id),
    constraint usuario_idrol_fkey FOREIGN KEY (idrol) REFERENCES rol(id) ON DELETE NO ACTION
);

CREATE TABLE cargo (
    id int NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    constraint cargo_pkey PRIMARY KEY (id)
);

CREATE TABLE remision (
    id bigserial not null,
    idremitente bigserial not null,
    idcargo INT not null,
    idunidadadministrativa INT not null,
    idunidadacademica INT not null,
    idcampus INT not null,
    fechacreacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idusuariocreacion bigint,
    fechaactualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idusuarioactualizacion bigint,
    constraint remision_pkey PRIMARY KEY (id),
    constraint remision_idremitente_fkey FOREIGN KEY (idremitente) REFERENCES persona(id) ON DELETE NO ACTION,
    constraint remision_idcargo_fkey FOREIGN KEY (idcargo) REFERENCES cargo(id) ON DELETE NO ACTION,
    constraint remision_idunidadadministrativa_fkey FOREIGN KEY (idunidadadministrativa) REFERENCES unidadadministrativa(id) ON DELETE NO ACTION,
    constraint remision_idunidadacademica_fkey FOREIGN KEY (idunidadacademica) REFERENCES unidadacademica(id) ON DELETE NO ACTION,
    constraint remision_idcampus_fkey FOREIGN KEY (idcampus) REFERENCES campus(id) ON DELETE NO ACTION,
    constraint remision_idusuariocreacion_fkey FOREIGN KEY (idusuariocreacion) REFERENCES usuario(id) ON DELETE NO ACTION,
    constraint remision_idusuarioactualizacion_fkey FOREIGN KEY (idusuarioactualizacion) REFERENCES usuario(id) ON DELETE NO ACTION
);


CREATE TABLE mediosolicitud (
    id integer NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT mediosolicitud_pkey PRIMARY KEY (id)
);

CREATE TABLE solicitudatencion (
    id bigserial NOT NULL,
    idsolicitante bigint not null,
    idremision bigint,
    ididentidadgenero INT,
    idusuarioactualizacion bigint,
    idtiposolicitud INT not null,
    idestadosolicitud INT not null,
    idmediosolicitud INT not null,
    observacionestelefono character varying COLLATE pg_catalog."default",
    observacionescorreo character varying COLLATE pg_catalog."default",
    fechacreacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idusuariocreacion bigint,
    fechaactualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    constraint solicitudatencion_pkey PRIMARY KEY (id),
    constraint solicitudatencion_idsolicitante_fkey FOREIGN KEY (idsolicitante) REFERENCES persona(id) ON DELETE NO ACTION,
    constraint solicitudatencion_idremision_fkey FOREIGN KEY (idremision) REFERENCES remision(id) ON DELETE NO ACTION,
    constraint solicitudatencion_idtiposolicitud_fkey FOREIGN KEY (idtiposolicitud) REFERENCES tiposolicitud(id) ON DELETE NO ACTION,
    constraint solicitudatencion_idestadosolicitud_fkey FOREIGN KEY (idestadosolicitud) REFERENCES estadosolicitud(id) ON DELETE NO ACTION,
    constraint solicitudatencion_ididentidadgenero_fkey FOREIGN KEY (ididentidadgenero) REFERENCES identidadgenero(id) ON DELETE NO ACTION,
    constraint solicitudatencion_idusuariocreacion_fkey FOREIGN KEY (idusuariocreacion) REFERENCES usuario(id) ON DELETE NO ACTION,
    constraint solicitudatencion_idusuarioactualizacion_fkey FOREIGN KEY (idusuarioactualizacion) REFERENCES usuario(id) ON DELETE NO ACTION,
    constraint solicitudatencion_idmediosolicitud_fkey FOREIGN KEY (idmediosolicitud) REFERENCES mediosolicitud(id) ON DELETE NO ACTION
);


CREATE TABLE cita (
    id bigserial NOT NULL,
    idsolicitudatencion bigint not null,
    fecha timestamp without time zone NOT NULL,
    idestadocita INT not null,
    idmotivoestadocita int,
    observaciones character varying COLLATE pg_catalog."default",
    fechacreacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idusuariocreacion bigint,
    fechaactualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idusuarioactualizacion bigint,
    constraint cita_pkey PRIMARY KEY (id),
    constraint cita_idsolicitudatencion_fkey FOREIGN KEY (idsolicitudatencion) REFERENCES solicitudatencion(id) ON DELETE NO ACTION,
    constraint cita_idestadocita_fkey FOREIGN KEY (idestadocita) REFERENCES estadocita(id) ON DELETE NO ACTION,
    constraint cita_idmotivoestadocita_fkey FOREIGN KEY (idmotivoestadocita) REFERENCES motivoestadocita(id) ON DELETE NO ACTION,
    constraint cita_idusuariocreacion_fkey FOREIGN KEY (idusuariocreacion) REFERENCES usuario(id) ON DELETE NO ACTION,
    constraint cita_idusuarioactualizacion_fkey FOREIGN KEY (idusuarioactualizacion) REFERENCES usuario(id) ON DELETE NO ACTION
);

CREATE TABLE estadocaso (
    id int not null,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    constraint estadocaso_pkey PRIMARY KEY (id)
);

CREATE TABLE caso (
    id bigserial NOT NULL,
    idcita bigint not null,
    codigo character varying COLLATE pg_catalog."default" UNIQUE NOT NULL,

    idregimen int not null,
    ideps int not null,
    idciudadresidencia int,
    direccionresidencia character varying COLLATE pg_catalog."default",
    ididentidadgenero int not null,
    idorientacionsexual INT,
    idetnia INT,
    idvinculoudea INT,
    otrovinculo character varying COLLATE pg_catalog."default",
    idprograma int,
    idunidadacademica INT,
    idunidadadministrativa INT,
    idcampus INT,

    hacecuantooccurrio integer,
    idtiempoocurridounidad INT,
    idformaocurrencia INT,
    idciudadhechos INT,
    idlugarocurrencia INT,
    violenciabasadagenero boolean,
    hechoviolenciaocurrioactividadesmisionales boolean,
    idactivadmisional int,
    tipoviolenciapsicologica boolean,
    tipoviolenciafisica boolean,
    tipoviolenciasexual boolean,
    tipoviolenciainstitucional boolean,
    tipoviolenciaeconomicapatrimonial boolean,
    tipoviolenciasexualinformatica boolean,
    tipoviolenciaporprejuicio boolean,
    
    idestadocaso int,

    fechacreacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idusuariocreacion bigint,
    fechaactualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idusuarioactualizacion bigint,

    CONSTRAINT caso_pkey PRIMARY KEY (id),
    constraint caso_idcita_fkey FOREIGN KEY (idcita) REFERENCES cita(id) ON DELETE NO ACTION,
    constraint caso_idvinculoudea_fkey FOREIGN KEY (idvinculoudea) REFERENCES vinculoudea(id) ON DELETE NO ACTION,
    constraint caso_idprograma_fkey FOREIGN KEY (idprograma) REFERENCES programa(id) ON DELETE NO ACTION,
    constraint caso_idunidadacademica_fkey FOREIGN KEY (idunidadacademica) REFERENCES unidadacademica(id) ON DELETE NO ACTION,
    constraint caso_idunidadadministrativa_fkey FOREIGN KEY (idunidadadministrativa) REFERENCES unidadadministrativa(id) ON DELETE NO ACTION,
    constraint caso_idcampus_fkey FOREIGN KEY (idcampus) REFERENCES campus(id) ON DELETE NO ACTION,
    constraint caso_idorientacionsexual_fkey FOREIGN KEY (idorientacionsexual) REFERENCES orientacionsexual(id) ON DELETE NO ACTION,
    constraint caso_ididentidadgenero_fkey FOREIGN KEY (ididentidadgenero) REFERENCES identidadgenero(id) ON DELETE NO ACTION,
    constraint caso_idciudadhechos_fkey FOREIGN KEY (idciudadhechos) REFERENCES ciudad(id) ON DELETE NO ACTION,
    constraint caso_idformaocurrencia_fkey FOREIGN KEY (idformaocurrencia) REFERENCES formaocurrencia(id) ON DELETE NO ACTION,
    constraint caso_idlugarocurrencia_fkey FOREIGN KEY (idlugarocurrencia) REFERENCES lugarocurrencia(id) ON DELETE NO ACTION,
    constraint caso_idactivadmisional_fkey FOREIGN KEY (idactivadmisional) REFERENCES actividadmisional(id) ON DELETE NO ACTION,
    constraint caso_idtiempoocurridounidad_fkey FOREIGN KEY (idtiempoocurridounidad) REFERENCES tiempoocurridounidad(id) ON DELETE NO ACTION,
    constraint caso_idestadocaso_fkey FOREIGN KEY  (idestadocaso) REFERENCES estadocaso(id) ON DELETE NO ACTION, 
    CONSTRAINT caso_idusuariocreacion_fkey FOREIGN KEY (idusuariocreacion) REFERENCES usuario(id) ON DELETE NO ACTION,
    CONSTRAINT caso_idusuarioactualizacion_fkey FOREIGN KEY (idusuarioactualizacion) REFERENCES usuario(id) ON DELETE NO ACTION
);

CREATE TABLE resultadocontactotelefonico (
    id int NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    constraint resultadocontactotelefonico_pkey PRIMARY KEY (id)
);

CREATE TABLE contactotelefonico (
    id bigserial NOT NULL,
    idsolicitudatencion bigint not null,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fechacreacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idusuariocreacion bigint,
    fechaactualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idusuarioactualizacion bigint,
    idresultado INT not null,
    CONSTRAINT contactotelefonico_pkey PRIMARY KEY (id),
    CONSTRAINT contactotelefonico_idsolicitudatencion_fkey FOREIGN KEY (idsolicitudatencion) REFERENCES solicitudatencion(id) ON DELETE NO ACTION,
    CONSTRAINT contactotelefonico_idresultado_fkey FOREIGN KEY (idresultado) REFERENCES resultadocontactotelefonico(id) ON DELETE NO ACTION,
    CONSTRAINT contactotelefonico_idusuariocreacion_fkey FOREIGN KEY (idusuariocreacion) REFERENCES usuario(id) ON DELETE NO ACTION,
    CONSTRAINT contactotelefonico_idusuarioactualizacion_fkey FOREIGN KEY (idusuarioactualizacion) REFERENCES usuario(id) ON DELETE NO ACTION
);


CREATE TABLE profesional (
    idpersona bigint not null,
    idcargo int  not null,
    constraint profesional_pkey PRIMARY KEY (idpersona),
    constraint profesional_idpersona_fkey FOREIGN KEY (idpersona) REFERENCES persona(id) ON DELETE NO ACTION,
    constraint profesional_idcargo_fkey FOREIGN KEY (idcargo) REFERENCES cargo(id) ON DELETE NO ACTION
);

CREATE TABLE grupoprofesional (
    id int NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    constraint grupoprofesional_pkey PRIMARY KEY (id)
);

CREATE TABLE profesionalgrupoprofesional (
    idgrupoprofesional int not null,
    idprofesional bigint not null,
    constraint profesionalgrupoprofesional_pkey PRIMARY KEY (idgrupoprofesional, idprofesional),
    constraint profesionalgrupoprofesional_idgrupoprofesional_fkey FOREIGN KEY (idgrupoprofesional) REFERENCES grupoprofesional(id) ON DELETE NO ACTION,
    constraint profesionalgrupoprofesional_idprofesional_fkey FOREIGN KEY (idprofesional) REFERENCES profesional(idpersona) ON DELETE NO ACTION
);

CREATE TABLE tipoasignacion (
    id int not null,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    constraint tipoasignacion_pkey PRIMARY KEY (id)
);

CREATE TABLE tiposervicio (
    id int not null,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    constraint tiposervicio_pkey PRIMARY KEY (id)
);

CREATE TABLE asignacion (
    id bigserial NOT NULL,
    fechacreacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idusuariocreacion bigint,
    fechaactualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idusuarioactualizacion bigint,
    idsolicitudatencion bigint not null,
    idgrupoprofesional int not null,
    idtipoasignacion int not null,
    idtiposervicio int not null,
    constraint asignacion_pkey PRIMARY KEY (id),
    constraint asignacion_idsolicitudatencion_fkey FOREIGN KEY (idsolicitudatencion) REFERENCES solicitudatencion(id) ON DELETE NO ACTION,
    constraint asignacion_idgrupoprofesional_fkey FOREIGN KEY (idgrupoprofesional) REFERENCES grupoprofesional(id) ON DELETE NO ACTION,
    constraint asignacion_idtipoasignacion_fkey FOREIGN KEY (idtipoasignacion) REFERENCES tipoasignacion(id) ON DELETE NO ACTION,
    constraint asignacion_idtiposervicio_fkey FOREIGN KEY (idtiposervicio) REFERENCES tiposervicio(id) ON DELETE NO ACTION,
    constraint asignacion_idusuariocreacion_fkey FOREIGN KEY (idusuariocreacion) REFERENCES usuario(id) ON DELETE NO ACTION,
    constraint asignacion_idusuarioactualizacion_fkey FOREIGN KEY (idusuarioactualizacion) REFERENCES usuario(id) ON DELETE NO ACTION
);

CREATE TABLE regimen (
    id int NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    constraint regimen_pkey PRIMARY KEY (id)
);

CREATE TABLE eps (
    id int not null,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    constraint eps_pkey PRIMARY KEY (id)
);

CREATE TABLE motivoestadocita (
    id int not null,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    constraint motivoestadocita_pkey PRIMARY KEY (id)
);

CREATE TABLE estadoatencion (
    id int not null,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    constraint estadoatencion_pkey PRIMARY KEY (id)
);

CREATE TABLE atencion (
    id bigserial NOT NULL,
    idestadoatencion int not null,
    idcaso bigint not null,
    idtiposervicio int not null,
    idlugarentrevista int not null,
    logroacuerdo boolean NOT NULL,
    fechacreacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idusuariocreacion bigint,
    fechaactualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idusuarioactualizacion bigint,
    constraint atencion_pkey PRIMARY KEY (id),
    constraint atencion_idcaso_fkey FOREIGN KEY (idcaso) REFERENCES caso(id) ON DELETE NO ACTION,
    constraint atencion_idestadoatencion_fkey FOREIGN KEY (idestadoatencion) REFERENCES estadoatencion(id) ON DELETE NO ACTION,
    constraint atencion_idtiposervicio_fkey FOREIGN KEY (idtiposervicio) REFERENCES tiposervicio(id) ON DELETE NO ACTION,
    constraint atencion_idlugarentrevista_fkey FOREIGN KEY (idlugarentrevista) REFERENCES lugarentrevista(id) ON DELETE NO ACTION,   
    constraint atencion_idusuariocreacion_fkey FOREIGN KEY (idusuariocreacion) REFERENCES usuario(id) ON DELETE NO ACTION,
    constraint atencion_idusuarioactualizacion_fkey FOREIGN KEY (idusuarioactualizacion) REFERENCES usuario(id) ON DELETE NO ACTION
);

CREATE TABLE apreciacionatencion (
    idatencion INT not null,
    idtipoapreciacion integer NOT NULL,
    descripcion character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT apreciacionatencion_pkey PRIMARY KEY (idatencion, idtipoapreciacion),
    CONSTRAINT apreciacionatencion_idatencion_fkey FOREIGN KEY (idatencion) REFERENCES atencion(id) ON DELETE NO ACTION,
    CONSTRAINT apreciacionatencion_idtipoapreciacion_fkey FOREIGN KEY (idtipoapreciacion) REFERENCES tipoapreciacion(id) ON DELETE NO ACTION
);

CREATE TABLE archivoconsentimiento (
    id bigserial not null,
    idatencion bigint not null,
    contenido bytea NOT NULL,
    tipocontenido character varying(200) COLLATE pg_catalog."default" NOT NULL,
    nombre character varying(500) COLLATE pg_catalog."default" NOT NULL,
    constraint archivoconsentimiento_pkey PRIMARY KEY (id),
    constraint archivoconsentimiento_idatencion_fkey FOREIGN KEY (idatencion) REFERENCES atencion(id) ON DELETE NO ACTION
);

CREATE TABLE parametrosistema (
    id serial NOT NULL,
    clave character varying NOT NULL,
    valor character varying NOT NULL,
    CONSTRAINT parametrosistema_pkey PRIMARY KEY (id),
    CONSTRAINT parametrosistema_clave_unique UNIQUE (clave)
);

CREATE TABLE tiporutaactivacion (
    id int not null,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    constraint tiporutaactivacion_pkey PRIMARY KEY (id)
);

CREATE TABLE rutaactivacion (
    id int not null,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    constraint rutaactivacion_pkey PRIMARY KEY (id)
); 

CREATE TABLE rutaatencion (
    idatencion bigint not null,
    idtiporutaactivacion int not null,
    idrutaactivacion int not null,
    constraint rutaatencion_pkey PRIMARY KEY (idatencion, idtiporutaactivacion, idrutaactivacion),
    constraint rutaatencion_idatencion_fkey FOREIGN KEY (idatencion) REFERENCES atencion(id) ON DELETE NO ACTION,
    constraint rutaatencion_idtiporutaactivacion_fkey FOREIGN KEY (idtiporutaactivacion) REFERENCES tiporutaactivacion(id) ON DELETE NO ACTION,
    constraint rutaatencion_idrutaactivacion_fkey FOREIGN KEY (idrutaactivacion) REFERENCES rutaactivacion(id) ON DELETE NO ACTION
);

CREATE TABLE tiporemision (
    id int not null,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    constraint tiporemision_pkey PRIMARY KEY (id)
);

CREATE TABLE instanciaremision (
    id integer NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    idtiporemision integer NOT NULL,
    CONSTRAINT instanciaremision_pkey PRIMARY KEY (id),
    CONSTRAINT fk_instanciaremision_tiporemision FOREIGN KEY (idtiporemision) REFERENCES tiporemision(id) ON DELETE NO ACTION
);

CREATE TABLE remisionatencion (
    idatencion bigint not null,
    idtiporemision int not null,
    cual character varying COLLATE pg_catalog."default",
    fecha timestamp without time zone NOT NULL,
    constraint remisionatencion_pkey PRIMARY KEY (idatencion, idtiporemision),
    constraint remisionatencion_idatencion_fkey FOREIGN KEY (idatencion) REFERENCES atencion(id) ON DELETE NO ACTION,
    constraint remisionatencion_idtiporemision_fkey FOREIGN KEY (idtiporemision) REFERENCES tiporemision(id) ON DELETE NO ACTION

);

CREATE TABLE grupoatencion (
    id int not null,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    constraint grupoatencion_pkey PRIMARY KEY (id)
);

CREATE TABLE tipocompromiso (
    id int not null,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    constraint tipocompromiso_pkey PRIMARY KEY (id)
);

CREATE TABLE compromisopersonaatendida (
    idatencion bigint not null,
    fechacompromiso timestamp without time zone NOT NULL,
    idtipocompromiso int not null,
    constraint compromisopersonaatendida_pkey PRIMARY KEY (idatencion, idtipocompromiso),
    constraint compromisopersonaatendida_idatencion_fkey FOREIGN KEY (idatencion) REFERENCES atencion(id) ON DELETE NO ACTION,
    constraint compromisopersonaatendida_idtipocompromiso_fkey FOREIGN KEY (idtipocompromiso) REFERENCES tipocompromiso(id) ON DELETE NO ACTION
);

CREATE TABLE compromisoprofesional (
    idatencion bigint not null,
    fechacompromiso timestamp without time zone NOT NULL,
    idgrupoprofesional int not null,
    idtipocompromiso int not null,
    constraint compromisoprofesional_pkey PRIMARY KEY (idatencion, idtipocompromiso),
    constraint compromisoprofesional_idatencion_fkey FOREIGN KEY (idatencion) REFERENCES atencion(id) ON DELETE NO ACTION,
    constraint compromisoprofesional_idtipocompromiso_fkey FOREIGN KEY (idtipocompromiso) REFERENCES tipocompromiso(id) ON DELETE NO ACTION,
    constraint compromisoprofesional_idgrupoprofesional_fkey FOREIGN KEY (idgrupoprofesional) REFERENCES grupoatencion(id) ON DELETE NO ACTION
);

CREATE TABLE tipocompromiso (
    id int not null,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    constraint tipocompromiso_pkey PRIMARY KEY (id)
);


CREATE TABLE tiposeguimiento (
    id int not null,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    constraint tiposeguimiento_pkey PRIMARY KEY (id)
);


CREATE TABLE accion (
    id int not null,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    constraint accion_pkey PRIMARY KEY (id)
);

CREATE TABLE actividad (
    id int not null,
    idaccion int not null,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    constraint actividad_pkey PRIMARY KEY (id),
    constraint actividad_idaccion_fkey FOREIGN KEY (idaccion) REFERENCES accion(id) ON DELETE NO ACTION
);

CREATE TABLE estadoseguimiento (
    id int not null,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    constraint estadoseguimiento_pkey PRIMARY KEY (id)
);

CREATE TABLE motivoestadoseguimiento (
    id int not null,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    constraint motivoestadoseguimiento_pkey PRIMARY KEY (id)
);

CREATE TABLE seguimientoatencion (
    id bigserial NOT NULL,
    idatencion bigint not null,
    idtiposeguimiento int not null,
    fecha timestamp without time zone NOT NULL,
    idaccion int not null,
    idactividad int not null,
    descripcion character varying COLLATE pg_catalog."default" NOT NULL,
    idestadoseguimiento int not null,
    idmotivoestado int not null,
    constraint seguimientoatencion_pkey PRIMARY KEY (id),
    constraint seguimientoatencion_idatencion_fkey FOREIGN KEY (idatencion) REFERENCES atencion(id) ON DELETE NO ACTION,
    constraint seguimientoatencion_idtiposeguimiento_fkey FOREIGN KEY (idtiposeguimiento) REFERENCES tiposeguimiento(id) ON DELETE NO ACTION,
    constraint seguimientoatencion_idaccion_fkey FOREIGN KEY (idaccion) REFERENCES accion(id) ON DELETE NO ACTION,
    constraint seguimientoatencion_idactividad_fkey FOREIGN KEY (idactividad) REFERENCES actividad(id) ON DELETE NO ACTION,
    constraint seguimientoatencion_idestadoseguimiento_fkey FOREIGN KEY (idestadoseguimiento) REFERENCES estadoseguimiento(id) ON DELETE NO ACTION,
    constraint seguimientoatencion_idmotivoestado_fkey FOREIGN KEY (idmotivoestado) REFERENCES motivoestadoseguimiento(id) ON DELETE NO ACTION
);

CREATE TABLE archivoseguimientoatencion (
    id bigserial not null,
    idseguimientoatencion bigint not null,
    contenido bytea NOT NULL,
    tipocontenido character varying(200) COLLATE pg_catalog."default" NOT NULL,
    nombre character varying(500) COLLATE pg_catalog."default" NOT NULL,
    constraint archivoseguimientoatencion_pkey PRIMARY KEY (id),
    constraint archivoseguimientoatencion_idseguimientoatencion_fkey FOREIGN KEY (idseguimientoatencion) REFERENCES seguimientoatencion(id) ON DELETE NO ACTION
);

CREATE TABLE presuntoagresor (
    id bigserial NOT NULL,
    idcaso bigint NOT NULL,
    primernombre character varying(255),
    segundonombre character varying(255),
    primerapellido character varying(255),
    segundoapellido character varying(255),
    idvinculoudea integer,
    idvinculoagresorvictima integer,
    CONSTRAINT presuntoagresor_pkey PRIMARY KEY (id),
    CONSTRAINT presuntoagresor_idcaso_fkey FOREIGN KEY (idcaso) REFERENCES caso(id) ON DELETE CASCADE,
    CONSTRAINT presuntoagresor_idvinculoudea_fkey FOREIGN KEY (idvinculoudea) REFERENCES vinculoudea(id),
    CONSTRAINT presuntoagresor_idvinculoagresorvictima_fkey FOREIGN KEY (idvinculoagresorvictima) REFERENCES vinculoagresorvictima(id)
);

CREATE TABLE tipomedida (
    id serial NOT NULL,
    nombre character varying(255) NOT NULL,
    CONSTRAINT tipomedida_pkey PRIMARY KEY (id)
);

CREATE TABLE subtipomedida (
    id serial NOT NULL,
    nombre character varying(255) NOT NULL,
    idtipomedida integer NOT NULL,
    CONSTRAINT subtipomedida_pkey PRIMARY KEY (id),
    CONSTRAINT subtipomedida_idtipomedida_fkey FOREIGN KEY (idtipomedida) REFERENCES tipomedida(id) ON DELETE CASCADE
);

CREATE TABLE responsablemedidaproteccion (
    id serial NOT NULL,
    nombre character varying(255) NOT NULL,
    CONSTRAINT responsablemedidaproteccion_pkey PRIMARY KEY (id)
);

CREATE TABLE medidaproteccion (
    id bigserial NOT NULL,
    idatencion bigint NOT NULL,
    idtipomedida integer NOT NULL,
    idsubtipomedida integer NOT NULL,
    idresponsablemedidaproteccion integer NOT NULL,
    fecharegistro timestamp without time zone NOT NULL,
    descripcion character varying(1000) NOT NULL,
    CONSTRAINT medidaproteccion_pkey PRIMARY KEY (id),
    CONSTRAINT medidaproteccion_idatencion_fkey FOREIGN KEY (idatencion) REFERENCES atencion(id) ON DELETE CASCADE,
    CONSTRAINT medidaproteccion_idtipomedida_fkey FOREIGN KEY (idtipomedida) REFERENCES tipomedida(id),
    CONSTRAINT medidaproteccion_idsubtipomedida_fkey FOREIGN KEY (idsubtipomedida) REFERENCES subtipomedida(id),
    CONSTRAINT medidaproteccion_idresponsable_fkey FOREIGN KEY (idresponsablemedidaproteccion) REFERENCES responsablemedidaproteccion(id)
);

-- ===========================================================================
-- LÍNEA ALMA — APH (Atención Pre-Hospitalaria)
-- Sección añadida para soportar el componente atencion-pr
-- Convención: tablas en minúsculas sin guiones bajos, FKs id<entidad>
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- 1. Catálogos específicos de Línea ALMA
-- ---------------------------------------------------------------------------

-- Tipo de reporte: Directa | Por remisión
CREATE TABLE tiporeportealma (
    id integer NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT tiporeportealma_pkey PRIMARY KEY (id)
);

CREATE TABLE actorremitente (
    id integer NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT actorremitente_pkey PRIMARY KEY (id)
);

-- Canal de contacto general: WhatsApp, Llamada, Remisión
CREATE TABLE canalcontacto (
    id integer NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT canalcontacto_pkey PRIMARY KEY (id)
);

-- Lugar de entrevista: Presencial, Virtual, Telefónica
-- Reutilizable también por el flujo estándar de atencion (registro-atencion)
CREATE TABLE lugarentrevista (
    id integer NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT lugarentrevista_pkey PRIMARY KEY (id)
);
-- Protocolo APH: protocolo clínico aplicado durante la atención
CREATE TABLE protocoloaph (
    id integer NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT protocoloaph_pkey PRIMARY KEY (id)
);

-- Resultado del triage: Alta, Media, Baja
CREATE TABLE resultadotriage (
    id integer NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT resultadotriage_pkey PRIMARY KEY (id)
);


-- ---------------------------------------------------------------------------
-- 2. Registro principal de Línea ALMA
--    Entidad raíz independiente del flujo solicitudatencion → cita → atencion.
--    Cubre los tabs: "Registro de atención de caso", "Datos de la persona",
--    "Datos complementarios" del componente atencion-pr.
-- ---------------------------------------------------------------------------

CREATE TABLE registrolinealma (
    id bigserial NOT NULL,
    -- Persona atendida
    idpersona bigint NOT NULL,
    -- Datos del tab "Registro de atención de caso"
    idtiporeporte integer NOT NULL,          -- directa / por remisión
    idcanalcontacto integer NOT NULL,        -- WhatsApp, Llamada, Remisión
    idquienremite integer,                   -- FK actorremitente (solo cuando indirecta)
    fechahoraatencion timestamp without time zone NOT NULL,
    idpersonaatiende integer NOT NULL,       -- grupo profesional que atiende
    idtiposervicio integer NOT NULL,         -- FK tiposervicio (fijo: Atención APH)
    idpersonaregistra bigint NOT NULL,       -- usuario que registra
    ididentidadgenero int not null,
    idorientacionsexual INT,
    idetnia INT,
    idciudadresidencia int,
    direccionresidencia character varying COLLATE pg_catalog."default",
    -- Datos complementarios (tab "Datos complementarios")
    idvinculoudea integer,
    idunidadacademica integer,
    idprograma integer,
    idunidadadministrativa integer,
    idcampus integer,
    -- Campos de auditoría
    fechacreacion timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idusuariocreacion bigint,
    fechaactualizacion timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idusuarioactualizacion bigint,
    CONSTRAINT registrolinealma_pkey PRIMARY KEY (id),
    CONSTRAINT registrolinealma_idpersona_fkey FOREIGN KEY (idpersona)
        REFERENCES persona(id) ON DELETE NO ACTION,
    CONSTRAINT registrolinealma_idtiporeporte_fkey FOREIGN KEY (idtiporeporte)
        REFERENCES tiporeportealma(id) ON DELETE NO ACTION,
    CONSTRAINT registrolinealma_idquienremite_fkey FOREIGN KEY (idquienremite)
        REFERENCES actorremitente(id) ON DELETE NO ACTION,
    CONSTRAINT registrolinealma_idcanalcontacto_fkey FOREIGN KEY (idcanalcontacto)
        REFERENCES canalcontacto(id) ON DELETE NO ACTION,
    CONSTRAINT registrolinealma_idtiposervicio_fkey FOREIGN KEY (idtiposervicio)
        REFERENCES tiposervicio(id) ON DELETE NO ACTION,
    CONSTRAINT registrolinealma_idpersonaatiende_fkey FOREIGN KEY (idpersonaatiende)
        REFERENCES grupoprofesional(id) ON DELETE NO ACTION,
    CONSTRAINT registrolinealma_idpersonaregistra_fkey FOREIGN KEY (idpersonaregistra)
        REFERENCES usuario(id) ON DELETE NO ACTION,
    CONSTRAINT registrolinealma_idorientacionsexual_fkey FOREIGN KEY (idorientacionsexual)
        REFERENCES orientacionsexual(id) ON DELETE NO ACTION,
    CONSTRAINT registrolinealma_ididentidadgenero_fkey FOREIGN KEY (ididentidadgenero)
        REFERENCES identidadgenero(id) ON DELETE NO ACTION,
    CONSTRAINT registrolinealma_idetnia_fkey FOREIGN KEY (idetnia)
        REFERENCES etnia(id) ON DELETE NO ACTION,
    CONSTRAINT registrolinealma_idciudadresidencia_fkey FOREIGN KEY (idciudadresidencia)
        REFERENCES municipio(id) ON DELETE NO ACTION,    
    CONSTRAINT registrolinealma_idvinculoudea_fkey FOREIGN KEY (idvinculoudea)
        REFERENCES vinculoudea(id) ON DELETE NO ACTION,
    CONSTRAINT registrolinealma_idunidadacademica_fkey FOREIGN KEY (idunidadacademica)
        REFERENCES unidadacademica(id) ON DELETE NO ACTION,
    CONSTRAINT registrolinealma_idprograma_fkey FOREIGN KEY (idprograma)
        REFERENCES programa(id) ON DELETE NO ACTION,
    CONSTRAINT registrolinealma_idunidadadministrativa_fkey FOREIGN KEY (idunidadadministrativa)
        REFERENCES unidadadministrativa(id) ON DELETE NO ACTION,
    CONSTRAINT registrolinealma_idcampus_fkey FOREIGN KEY (idcampus)
        REFERENCES campus(id) ON DELETE NO ACTION,
    CONSTRAINT registrolinealma_idusuariocreacion_fkey FOREIGN KEY (idusuariocreacion)
        REFERENCES usuario(id) ON DELETE NO ACTION,
    CONSTRAINT registrolinealma_idusuarioactualizacion_fkey FOREIGN KEY (idusuarioactualizacion)
        REFERENCES usuario(id) ON DELETE NO ACTION
);

-- ---------------------------------------------------------------------------
-- 3. Datos clínicos APH
--    Cubre el tab "Atención APH" del componente atencion-pr.
--    Relación 1:1 con registrolinealma.
-- ---------------------------------------------------------------------------

CREATE TABLE atencionaph (
    id bigserial NOT NULL,
    idregistrolinealma bigint NOT NULL,
    fechahora timestamp without time zone NOT NULL,   -- aphFecha + aphHora
    idprotocoloaph integer NOT NULL,
    practicotriage boolean NOT NULL DEFAULT FALSE,
    idresultadotriage integer,                        -- NULL cuando practicotriage = false
    notaaph character varying COLLATE pg_catalog."default",
    motivonotriage character varying COLLATE pg_catalog."default",
    aceptapsicologia boolean NOT NULL DEFAULT TRUE,
    requiereremision boolean NOT NULL DEFAULT FALSE,
    fechacreacion timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idusuariocreacion bigint,
    fechaactualizacion timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idusuarioactualizacion bigint,
    CONSTRAINT atencionaph_pkey PRIMARY KEY (id),
    CONSTRAINT atencionaph_idregistrolinealma_fkey FOREIGN KEY (idregistrolinealma)
        REFERENCES registrolinealma(id) ON DELETE NO ACTION,
    CONSTRAINT atencionaph_idprotocoloaph_fkey FOREIGN KEY (idprotocoloaph)
        REFERENCES protocoloaph(id) ON DELETE NO ACTION,
    CONSTRAINT atencionaph_idresultadotriage_fkey FOREIGN KEY (idresultadotriage)
        REFERENCES resultadotriage(id) ON DELETE NO ACTION,
    CONSTRAINT atencionaph_idusuariocreacion_fkey FOREIGN KEY (idusuariocreacion)
        REFERENCES usuario(id) ON DELETE NO ACTION,
    CONSTRAINT atencionaph_idusuarioactualizacion_fkey FOREIGN KEY (idusuarioactualizacion)
        REFERENCES usuario(id) ON DELETE NO ACTION
);

-- ---------------------------------------------------------------------------
-- 4. Remisiones asociadas a un registro Línea ALMA
--    Cubre el tab "Remision" del componente atencion-pr.
--    Análoga a remisionatencion pero vinculada a registrolinealma.
-- ---------------------------------------------------------------------------

CREATE TABLE remisionregistroalma (
    idregistrolinealma bigint NOT NULL,
    idtiporemision integer NOT NULL,
    cual character varying COLLATE pg_catalog."default",
    fecha timestamp without time zone NOT NULL,
    CONSTRAINT remisionregistroalma_pkey PRIMARY KEY (idregistrolinealma, idtiporemision),
    CONSTRAINT remisionregistroalma_idregistrolinealma_fkey FOREIGN KEY (idregistrolinealma)
        REFERENCES registrolinealma(id) ON DELETE NO ACTION,
    CONSTRAINT remisionregistroalma_idtiporemision_fkey FOREIGN KEY (idtiporemision)
        REFERENCES tiporemision(id) ON DELETE NO ACTION
);

-- ---------------------------------------------------------------------------
-- 5. Seguimiento de contactos de Línea ALMA
--    Cubre el tab "Contacto" del componente atencion-pr (en construcción).
--    Análoga a contactotelefonico pero vinculada a registrolinealma.
-- ---------------------------------------------------------------------------

CREATE TABLE contactolinealma (
    id bigserial NOT NULL,
    idregistrolinealma bigint NOT NULL,
    fecha timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idresultado integer NOT NULL,
    fechacreacion timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idusuariocreacion bigint,
    fechaactualizacion timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idusuarioactualizacion bigint,
    CONSTRAINT contactolinealma_pkey PRIMARY KEY (id),
    CONSTRAINT contactolinealma_idregistrolinealma_fkey FOREIGN KEY (idregistrolinealma)
        REFERENCES registrolinealma(id) ON DELETE NO ACTION,
    CONSTRAINT contactolinealma_idresultado_fkey FOREIGN KEY (idresultado)
        REFERENCES resultadocontactotelefonico(id) ON DELETE NO ACTION,
    CONSTRAINT contactolinealma_idusuariocreacion_fkey FOREIGN KEY (idusuariocreacion)
        REFERENCES usuario(id) ON DELETE NO ACTION,
    CONSTRAINT contactolinealma_idusuarioactualizacion_fkey FOREIGN KEY (idusuarioactualizacion)
        REFERENCES usuario(id) ON DELETE NO ACTION
);

CREATE TABLE tiempoocurridounidad (
    id integer NOT NULL,
    nombre character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT tiempoocurridounidad_pkey PRIMARY KEY (id)
);