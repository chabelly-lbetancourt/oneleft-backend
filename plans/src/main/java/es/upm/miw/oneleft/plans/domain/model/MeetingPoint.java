package es.upm.miw.oneleft.plans.domain.model;

/**
 * Lugar de encuentro del plan (una pista, un bar, una puerta del parque...). A diferencia de la zona del
 * usuario, es un lugar público y se guarda con precisión para poder buscar planes cercanos.
 */
public record MeetingPoint(String name, double latitude, double longitude) {

    public static final int MAX_NAME_LENGTH = 100;

    public MeetingPoint {
        if (name == null || name.isBlank() || name.strip().length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("El lugar necesita un nombre de hasta " + MAX_NAME_LENGTH + " caracteres");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Coordenadas del lugar fuera de rango");
        }
        name = name.strip();
    }
}
