package es.upm.miw.oneleft.notifications.domain.port.in;

import java.util.UUID;

/** Keeping the copy of the gender and age of people, from the users service (HU-070). */
public interface KeepAudiencesUseCase {

    void changed(UUID userId, String gender, int age);
}
