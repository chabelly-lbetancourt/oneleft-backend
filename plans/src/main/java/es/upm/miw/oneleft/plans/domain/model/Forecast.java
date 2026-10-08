package es.upm.miw.oneleft.plans.domain.model;

import java.io.Serializable;
import java.time.Instant;

/**
 * Weather forecast at the time and place of a plan (HU-026): the hour it refers to, temperature in °C, chance of rain
 * in % and wind in km/h.
 */
public record Forecast(Instant time, double temperature, int precipitationProbability, double windSpeed)
        implements Serializable {

    /** From this chance of rain on, the plan shows a highlighted notice. */
    public static final int RAIN_LIKELY = 60;

    public boolean rainLikely() {
        return precipitationProbability >= RAIN_LIKELY;
    }
}
