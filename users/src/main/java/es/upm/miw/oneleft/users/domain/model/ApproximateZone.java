package es.upm.miw.oneleft.users.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Zona habitual del usuario. Por privacidad, OneLeft nunca guarda la ubicación exacta:
 * las coordenadas se redondean a dos decimales (una cuadrícula de aproximadamente 1,1 km).
 */
public record ApproximateZone(String name, double latitude, double longitude) {

    public static final int DECIMALS = 2;
    public static final int MAX_NAME_LENGTH = 60;

    public ApproximateZone {
        if (name == null || name.isBlank() || name.strip().length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("La zona necesita un nombre de hasta " + MAX_NAME_LENGTH + " caracteres");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Coordenadas fuera de rango");
        }
        name = name.strip();
        latitude = round(latitude);
        longitude = round(longitude);
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(DECIMALS, RoundingMode.HALF_UP).doubleValue();
    }
}
