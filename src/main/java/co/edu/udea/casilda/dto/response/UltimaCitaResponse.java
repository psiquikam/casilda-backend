package co.edu.udea.casilda.dto.response;

import java.time.LocalDateTime;

public record UltimaCitaResponse(LocalDateTime fechaCita, String estadoCita, boolean tieneCitaProgramada) {}
