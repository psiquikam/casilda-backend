package co.edu.udea.casilda.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public final class SlaPsicologia {
    private SlaPsicologia() {
    }

    public static int horas(String triaje) {
        return switch (triaje) {
            case "ALTO" -> 12;
            case "MEDIO" -> 24;
            case "BAJO" -> 48;
            default -> throw new IllegalStateException("Triaje no válido para calcular SLA");
        };
    }

    public static Map<String, Object> calcular(String triaje, Instant derivacion, Instant ahora) {
        int horas = horas(triaje);
        return Map.of("horasLimite", horas, "venceEn", derivacion.plus(horas, ChronoUnit.HOURS),
                "ahoraServidor", ahora);
    }
}
