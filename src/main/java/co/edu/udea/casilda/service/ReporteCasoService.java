package co.edu.udea.casilda.service;

import co.edu.udea.casilda.dto.request.ReporteCasoRequest;
import co.edu.udea.casilda.dto.response.ReporteCasoResponse;
import co.edu.udea.casilda.dto.response.RutaActivacionReporteResponse;
import co.edu.udea.casilda.exception.ResourceNotFoundException;
import co.edu.udea.casilda.exception.FieldValidationException;
import co.edu.udea.casilda.model.entity.Campus;
import co.edu.udea.casilda.model.entity.Cargo;
import co.edu.udea.casilda.model.entity.EvidenciaReporte;
import co.edu.udea.casilda.model.entity.IdentidadGenero;
import co.edu.udea.casilda.model.entity.ReporteCaso;
import co.edu.udea.casilda.model.entity.RutaActivacion;
import co.edu.udea.casilda.model.entity.TipoIdentificacion;
import co.edu.udea.casilda.model.entity.Usuario;
import co.edu.udea.casilda.model.entity.VinculoAgresorVictima;
import co.edu.udea.casilda.model.entity.VinculoUdeA;
import co.edu.udea.casilda.model.enums.EstadoReporteRutaEnum;
import co.edu.udea.casilda.model.enums.TipoReportanteEnum;
import co.edu.udea.casilda.dto.request.ReporteContactoRequest;
import co.edu.udea.casilda.dto.request.ReportePresuntoAgresorRequest;
import co.edu.udea.casilda.dto.request.ReporteVictimaRequest;
import co.edu.udea.casilda.repository.CargoRepository;
import co.edu.udea.casilda.repository.IdentidadGeneroRepository;
import co.edu.udea.casilda.repository.CampusRepository;
import co.edu.udea.casilda.repository.ReporteCasoRepository;
import co.edu.udea.casilda.repository.RutaActivacionRepository;
import co.edu.udea.casilda.repository.TipoIdentificacionRepository;
import co.edu.udea.casilda.repository.UsuarioRepository;
import co.edu.udea.casilda.repository.VinculoAgresorVictimaRepository;
import co.edu.udea.casilda.repository.VinculoUdeARepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/**
 * Servicio para crear reportes iniciales de caso.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReporteCasoService {

    private static final String CAS_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CAS_RANDOM_LENGTH = 10;
    private static final int CAS_GENERATION_ATTEMPTS = 5;
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private final SecureRandom secureRandom = new SecureRandom();
    private final ReporteCasoRepository reporteCasoRepository;
    private final RutaActivacionRepository rutaActivacionRepository;
    private final TipoIdentificacionRepository tipoIdentificacionRepository;
    private final CampusRepository campusRepository;
    private final UsuarioRepository usuarioRepository;
    private final IdentidadGeneroRepository identidadGeneroRepository;
    private final CargoRepository cargoRepository;
    private final VinculoUdeARepository vinculoUdeARepository;
    private final VinculoAgresorVictimaRepository vinculoAgresorVictimaRepository;
    private final ReporteEvidenciaStorageService evidenciaStorageService;

    @Transactional
    public ReporteCasoResponse crear(final ReporteCasoRequest request) {
        return crear(request, List.of());
    }

    @Transactional
    public ReporteCasoResponse crear(
            final ReporteCasoRequest request, final List<org.springframework.web.multipart.MultipartFile> archivos) {
        evidenciaStorageService.validarCantidadYTamanoTotal(archivos);
        validarRequest(request);

        final TipoIdentificacion tipoDocumento = obtenerTipoDocumento(
                request.getTipoDocumentoId(), request.getNumeroDocumento(), request.getFechaNacimiento());
        final Campus campus = obtenerCampus(request.getCampusId());
        final RutaActivacion ruta = rutaActivacionRepository
                .findByIdAndActivoTrue(request.getRutaActivacionId())
                .orElseThrow(() -> fieldError(
                        "rutaActivacionId", "La ruta de activación no existe o está inactiva"));

        final ReporteCaso reporte = new ReporteCaso();
        reporte.setCodigo(generarCodigoDisponible());
        reporte.setUsuario(obtenerUsuarioAutenticado());
        reporte.setTipoDocumento(tipoDocumento);
        reporte.setNumeroDocumento(normalizar(request.getNumeroDocumento()));
        reporte.setFechaNacimiento(request.getFechaNacimiento());
        reporte.setTipoLugar(normalizarTipoLugar(request.getTipoLugar()));
        reporte.setCampus(campus);
        reporte.setFacultad(normalizar(request.getFacultad()));
        reporte.setBloque(normalizar(request.getBloque()));
        reporte.setEspacio(normalizar(request.getEspacio()));
        reporte.setLugarDetalleExterno(normalizar(request.getLugarDetalleExterno()));
        reporte.setDescripcionHechos(request.getDescripcionHechos().trim());
        reporte.setRutaActivacion(ruta);
        reporte.setEstadoRuta(EstadoReporteRutaEnum.SOLICITADA);
        completarDatosAdicionales(reporte, request);

        final List<String> archivosAlmacenados = new ArrayList<>();
        try {
            for (org.springframework.web.multipart.MultipartFile archivo : archivos) {
                final ReporteEvidenciaStorageService.StoredEvidence almacenado =
                        evidenciaStorageService.guardar(archivo);
                final EvidenciaReporte evidencia = new EvidenciaReporte();
                evidencia.setReporteCaso(reporte);
                evidencia.setNombreOriginal(almacenado.nombreOriginal());
                evidencia.setNombreAlmacenado(almacenado.nombreAlmacenado());
                evidencia.setTipoContenido(almacenado.tipoContenido());
                evidencia.setTamanoBytes(almacenado.tamanoBytes());
                reporte.getEvidencias().add(evidencia);
                archivosAlmacenados.add(almacenado.nombreAlmacenado());
            }
            final ReporteCaso guardado = reporteCasoRepository.saveAndFlush(reporte);
            log.info("Reporte de caso creado con id {}", guardado.getId());
            return toResponse(guardado);
        } catch (RuntimeException ex) {
            archivosAlmacenados.forEach(evidenciaStorageService::eliminar);
            throw ex;
        }
    }

    private void validarRequest(final ReporteCasoRequest request) {
        validarDatosAdicionales(request);
        final boolean tieneTipoDocumento = request.getTipoDocumentoId() != null;
        final boolean tieneNumeroDocumento = StringUtils.hasText(request.getNumeroDocumento());
        if (tieneTipoDocumento != tieneNumeroDocumento) {
            final Map<String, String> errors = new LinkedHashMap<>();
            if (!tieneTipoDocumento) {
                errors.put("tipoDocumentoId", "El tipo de documento es obligatorio cuando se envía el número de documento");
            }

            if (!tieneNumeroDocumento) {
                errors.put("numeroDocumento", "El número de documento es obligatorio cuando se envía el tipo de documento");
            }
            throw new FieldValidationException(errors);
        }

        final String tipoLugar = normalizarTipoLugar(request.getTipoLugar());
        if (tipoLugar != null && !"interno".equals(tipoLugar) && !"externo".equals(tipoLugar)) {
            throw fieldError("tipoLugar", "El tipo de lugar debe ser interno o externo");
        }
        if ("interno".equals(tipoLugar) && request.getCampusId() == null) {
            throw fieldError("campusId", "Seleccione el campus donde ocurrieron los hechos");
        }
        if ("externo".equals(tipoLugar) && !StringUtils.hasText(request.getLugarDetalleExterno())) {
            throw fieldError("lugarDetalleExterno", "Describa el lugar externo donde ocurrieron los hechos");
        }
        if (tipoLugar == null && (request.getCampusId() != null
                || StringUtils.hasText(request.getFacultad())
                || StringUtils.hasText(request.getBloque())
                || StringUtils.hasText(request.getEspacio())
                || StringUtils.hasText(request.getLugarDetalleExterno()))) {
            throw fieldError("tipoLugar", "Seleccione si el lugar es interno o externo");
        }
        if ("interno".equals(tipoLugar) && StringUtils.hasText(request.getLugarDetalleExterno())) {
            throw fieldError("lugarDetalleExterno", "Este campo no aplica para un lugar interno");
        }
        if ("externo".equals(tipoLugar) && (request.getCampusId() != null
                || StringUtils.hasText(request.getFacultad())
                || StringUtils.hasText(request.getBloque())
                || StringUtils.hasText(request.getEspacio()))) {
            throw fieldError("campusId", "El campus no aplica para un lugar externo");
        }
        if (request.getFechaNacimiento() != null
                && !request.getFechaNacimiento().isBefore(LocalDate.now(BOGOTA))) {
            throw fieldError("fechaNacimiento", "La fecha de nacimiento debe ser anterior a hoy");
        }
    }

    private void validarDatosAdicionales(final ReporteCasoRequest request) {
        if (StringUtils.hasText(request.getTipoReportante())) {
            try {
                TipoReportanteEnum.valueOf(request.getTipoReportante().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw fieldError("tipoReportante", "El tipo de reportante debe ser AFECTADA o TERCERO");
            }
        }

        final ReporteVictimaRequest victima = request.getVictima();
        if (victima != null) {
            if (!StringUtils.hasText(victima.getPrimerNombre())) {
                throw fieldError("victima.primerNombre", "El primer nombre de la víctima es obligatorio");
            }
            if (!StringUtils.hasText(victima.getPrimerApellido())) {
                throw fieldError("victima.primerApellido", "El primer apellido de la víctima es obligatorio");
            }
            if (victima.getCargoId() == null) {
                throw fieldError("victima.cargoId", "El cargo de la víctima es obligatorio");
            }
        }

        final ReporteContactoRequest contacto = request.getContacto();
        if (contacto != null && Boolean.TRUE.equals(contacto.getDeseaContacto())
                && !StringUtils.hasText(contacto.getCorreo())
                && !StringUtils.hasText(contacto.getWhatsapp())) {
            throw fieldError("contacto", "Ingrese un correo o WhatsApp para ser contactada");
        }

        final ReportePresuntoAgresorRequest agresor = request.getPresuntoAgresor();
        if (agresor != null) {
            final VinculoUdeA vinculoUdeA = obtenerVinculoUdeA(agresor.getVinculoUniversidadId());
            final VinculoAgresorVictima vinculoVictima =
                    obtenerVinculoVictima(agresor.getVinculoVictimaId());
            validarOtroVinculo("presuntoAgresor.otroVinculoUniversidad",
                    agresor.getOtroVinculoUniversidad(), vinculoUdeA);
            validarOtroVinculo("presuntoAgresor.otroVinculoVictima",
                    agresor.getOtroVinculoVictima(), vinculoVictima);
            if (agresor.getRemitirUad() != null && vinculoUdeA == null) {
                throw fieldError("presuntoAgresor.remitirUad",
                        "La autorización para remitir a la UAD requiere un vínculo con la Universidad");
            }
        }
    }

    private void completarDatosAdicionales(
            final ReporteCaso reporte, final ReporteCasoRequest request) {
        if (StringUtils.hasText(request.getTipoReportante())) {
            reporte.setTipoReportante(TipoReportanteEnum.valueOf(
                    request.getTipoReportante().trim().toUpperCase(Locale.ROOT)));
        }

        final ReporteVictimaRequest victima = request.getVictima();
        if (victima != null) {
            reporte.setVictimaPrimerNombre(normalizar(victima.getPrimerNombre()));
            reporte.setVictimaSegundoNombre(normalizar(victima.getSegundoNombre()));
            reporte.setVictimaPrimerApellido(normalizar(victima.getPrimerApellido()));
            reporte.setVictimaSegundoApellido(normalizar(victima.getSegundoApellido()));
            reporte.setVictimaCorreo(normalizar(victima.getCorreo()));
            reporte.setVictimaIdentidadGenero(obtenerIdentidadGenero(victima.getIdentidadGeneroId()));
            reporte.setVictimaCargo(obtenerCargo(victima.getCargoId()));
        }

        final ReporteContactoRequest contacto = request.getContacto();
        if (contacto != null) {
            reporte.setDeseaContacto(contacto.getDeseaContacto());
            if (Boolean.TRUE.equals(contacto.getDeseaContacto())) {
                reporte.setContactoCorreo(normalizar(contacto.getCorreo()));
                reporte.setContactoWhatsapp(normalizar(contacto.getWhatsapp()));
            }
        }

        final ReportePresuntoAgresorRequest agresor = request.getPresuntoAgresor();
        if (agresor != null) {
            reporte.setAgresorNumeroDocumento(normalizar(agresor.getNumeroDocumento()));
            reporte.setAgresorPrimerNombre(normalizar(agresor.getPrimerNombre()));
            reporte.setAgresorSegundoNombre(normalizar(agresor.getSegundoNombre()));
            reporte.setAgresorPrimerApellido(normalizar(agresor.getPrimerApellido()));
            reporte.setAgresorSegundoApellido(normalizar(agresor.getSegundoApellido()));
            reporte.setAgresorVinculoUdeA(obtenerVinculoUdeA(agresor.getVinculoUniversidadId()));
            reporte.setAgresorVinculoVictima(obtenerVinculoVictima(agresor.getVinculoVictimaId()));
            reporte.setAgresorOtroVinculoUdeA(normalizar(agresor.getOtroVinculoUniversidad()));
            reporte.setAgresorOtroVinculoVictima(normalizar(agresor.getOtroVinculoVictima()));
            reporte.setAgresorInformacionAdicional(normalizar(agresor.getInformacionAdicional()));
            reporte.setAgresorRemitirUad(agresor.getRemitirUad());
            if (agresor.getRemitirUad() != null) {
                reporte.setAgresorRemitirUadFecha(Instant.now());
                reporte.setAgresorRemitirUadVersion("v1");
            }
        }
    }

    private IdentidadGenero obtenerIdentidadGenero(final Integer id) {
        if (id == null) {
            return null;
        }
        return identidadGeneroRepository.findById(id)
                .orElseThrow(() -> fieldError(
                        "victima.identidadGeneroId", "La identidad de género no existe"));
    }

    private Cargo obtenerCargo(final Integer id) {
        if (id == null) {
            throw fieldError("victima.cargoId", "El cargo de la víctima es obligatorio");
        }
        return cargoRepository.findById(id)
                .orElseThrow(() -> fieldError("victima.cargoId", "El cargo de la víctima no existe"));
    }

    private VinculoUdeA obtenerVinculoUdeA(final Integer id) {
        if (id == null) {
            return null;
        }
        return vinculoUdeARepository.findById(id)
                .orElseThrow(() -> fieldError(
                        "presuntoAgresor.vinculoUniversidadId",
                        "El vínculo con la Universidad no existe"));
    }

    private VinculoAgresorVictima obtenerVinculoVictima(final Integer id) {
        if (id == null) {
            return null;
        }
        return vinculoAgresorVictimaRepository.findById(id)
                .orElseThrow(() -> fieldError(
                        "presuntoAgresor.vinculoVictimaId",
                        "El vínculo con la víctima no existe"));
    }

    private void validarOtroVinculo(
            final String field, final String otherValue, final Object vinculo) {
        final boolean hasOtherValue = StringUtils.hasText(otherValue);
        final boolean isOther = vinculo != null && "otro".equalsIgnoreCase(nombreVinculo(vinculo));
        if (hasOtherValue != isOther) {
            throw fieldError(field, isOther
                    ? "Describa el otro vínculo seleccionado"
                    : "Este campo solo aplica cuando el vínculo es Otro");
        }
    }

    private String nombreVinculo(final Object vinculo) {
        return vinculo instanceof VinculoUdeA udea ? udea.getNombre()
                : ((VinculoAgresorVictima) vinculo).getNombre();
    }

    private TipoIdentificacion obtenerTipoDocumento(
            final Integer id, final String numeroDocumento, final LocalDate fechaNacimiento) {
        if (id == null) {
            return null;
        }
        final TipoIdentificacion tipo = tipoIdentificacionRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> fieldError(
                        "tipoDocumentoId", "El tipo de documento no existe o está inactivo"));
        validarTipoDocumento(tipo, numeroDocumento, fechaNacimiento);
        return tipo;
    }

    private Campus obtenerCampus(final Integer id) {
        if (id == null) {
            return null;
        }
        return campusRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> fieldError("campusId", "El campus no existe o está inactivo"));
    }

    private Usuario obtenerUsuarioAutenticado() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }

    private String generarCodigoDisponible() {
        for (int attempt = 0; attempt < CAS_GENERATION_ATTEMPTS; attempt++) {
            final String candidate = "CAS-" + randomPart();
            if (!reporteCasoRepository.existsByCodigo(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("No fue posible generar un código de reporte disponible");
    }

    private String randomPart() {
        final StringBuilder value = new StringBuilder(CAS_RANDOM_LENGTH);
        for (int index = 0; index < CAS_RANDOM_LENGTH; index++) {
            value.append(CAS_ALPHABET.charAt(secureRandom.nextInt(CAS_ALPHABET.length())));
        }
        return value.toString();
    }

    private String normalizar(final String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizarTipoLugar(final String value) {
        final String normalized = normalizar(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private void validarTipoDocumento(
            final TipoIdentificacion tipo, final String numeroDocumento, final LocalDate fechaNacimiento) {
        final String codigo = tipo.getCodigo().toUpperCase(Locale.ROOT);
        final String documento = normalizar(numeroDocumento).toUpperCase(Locale.ROOT);
        final int maxLength;
        final boolean soloDigitos;
        switch (codigo) {
            case "CC" -> {
                maxLength = 10;
                soloDigitos = true;
            }
            case "TI" -> {
                maxLength = 11;
                soloDigitos = true;
            }
            case "CE" -> {
                maxLength = 6;
                soloDigitos = false;
            }
            case "PA", "PAS" -> {
                maxLength = 16;
                soloDigitos = false;
            }
            default -> {
                maxLength = 20;
                soloDigitos = true;
            }
        }
        if (documento.length() > maxLength
                || (soloDigitos && !documento.matches("\\d+"))
                || (!soloDigitos && !documento.matches("[A-Z0-9]+"))) {
            throw fieldError("numeroDocumento",
                    "El documento no cumple el formato permitido para " + tipo.getNombre());
        }
        if ("TI".equals(codigo) && fechaNacimiento != null
                && Period.between(fechaNacimiento, LocalDate.now(BOGOTA)).getYears() >= 18) {
            throw fieldError("tipoDocumentoId",
                    "La tarjeta de identidad solo aplica a menores de 18 años. Seleccione otro tipo de documento.");
        }
    }

    private FieldValidationException fieldError(final String field, final String message) {
        return new FieldValidationException(Map.of(field, message));
    }

    private ReporteCasoResponse toResponse(final ReporteCaso reporte) {
        final RutaActivacion ruta = reporte.getRutaActivacion();
        return ReporteCasoResponse.builder()
                .id(reporte.getId())
                .codigo(reporte.getCodigo())
                .fechaRegistro(reporte.getFechaRegistro())
                .rutaActivacion(RutaActivacionReporteResponse.builder()
                        .id(ruta.getId())
                        .codigo(ruta.getCodigo())
                        .nombre(ruta.getNombre())
                        .estado(reporte.getEstadoRuta())
                        .build())
                .build();
    }
}
