package es.upm.miw.oneleft.users.domain.model;

import java.util.List;

/** The age band other people see instead of the date of birth (HU-070): 18–24, 25–34, 35–44, 45–54, 55–64 and 65+. */
public record AgeBand(int from, Integer to) {

    private static final List<AgeBand> BANDS = List.of(new AgeBand(18, 24), new AgeBand(25, 34),
            new AgeBand(35, 44), new AgeBand(45, 54), new AgeBand(55, 64), new AgeBand(65, null));

    /** The band of an age, which must be of age. */
    public static AgeBand of(int age) {
        return BANDS.stream().filter(band -> age >= band.from && (band.to == null || age <= band.to)).findFirst()
                .orElseThrow(() -> new ValidationException("demographics.minor", "Only people of age can use OneLeft"));
    }

    public String label() {
        return to == null ? from + "+" : from + "–" + to;
    }
}
