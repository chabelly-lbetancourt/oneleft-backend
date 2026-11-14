package es.upm.miw.oneleft.notifications.application;

import es.upm.miw.oneleft.notifications.domain.model.MemberAudience;
import es.upm.miw.oneleft.notifications.domain.port.in.KeepAudiencesUseCase;
import es.upm.miw.oneleft.notifications.domain.port.out.Audiences;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * The gender and age of people (HU-070), copied here so that the notices of nearby plans leave out who the plan is not
 * for without asking the users service for each person.
 */
@Service
@Transactional
public class AudienceCopyService implements KeepAudiencesUseCase {

    private final Audiences audiences;

    public AudienceCopyService(Audiences audiences) {
        this.audiences = audiences;
    }

    @Override
    public void changed(UUID userId, String gender, int age) {
        audiences.save(new MemberAudience(userId, gender, age));
    }
}
