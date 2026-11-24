package es.upm.miw.oneleft.notifications.application;

import es.upm.miw.oneleft.notifications.domain.model.MemberPerks;
import es.upm.miw.oneleft.notifications.domain.port.in.KeepPerksUseCase;
import es.upm.miw.oneleft.notifications.domain.port.out.PerkCopies;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * What the level of each person is worth (HU-063), copied here so that ordering the notices of a plan and saving an
 * alert do not ask the reputation service for each person.
 */
@Service
@Transactional
public class PerkCopyService implements KeepPerksUseCase {

    private final PerkCopies perks;

    public PerkCopyService(PerkCopies perks) {
        this.perks = perks;
    }

    @Override
    public void changed(MemberPerks changed) {
        perks.save(changed);
    }
}
