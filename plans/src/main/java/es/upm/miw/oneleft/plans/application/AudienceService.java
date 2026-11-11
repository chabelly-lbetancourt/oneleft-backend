package es.upm.miw.oneleft.plans.application;

import es.upm.miw.oneleft.plans.domain.model.AudienceMember;
import es.upm.miw.oneleft.plans.domain.model.AudienceUnavailableException;
import es.upm.miw.oneleft.plans.domain.model.JoinRejectedException;
import es.upm.miw.oneleft.plans.domain.model.Plan;
import es.upm.miw.oneleft.plans.domain.port.in.AudienceQueryUseCase;
import es.upm.miw.oneleft.plans.domain.port.out.AudienceDirectory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Decides whether a person fits the audience of a plan (HU-070), in the backend and not only in the interface. A plan for
 * everyone needs no data; one with an audience needs the gender and age of the person, which the users service gives (never
 * the date of birth). Without them, or if they cannot be known, the person does not join.
 */
@Service
public class AudienceService implements AudienceCheck, AudienceQueryUseCase {

    private final AudienceDirectory directory;

    public AudienceService(AudienceDirectory directory) {
        this.directory = directory;
    }

    @Override
    public void check(Plan plan, UUID userId) {
        if (plan.audience().isEveryone()) {
            return;
        }
        var member = memberOf(userId).orElseThrow(() -> new JoinRejectedException("demographics.required",
                "This plan has an audience: set your date of birth and gender in your profile"));
        if (!plan.audience().fits(member)) {
            throw new JoinRejectedException("plan.audienceMismatch", "This plan is not for you");
        }
    }

    /** Gender and age of a person for the search of nearby plans; empty if unknown or the users service is down. */
    @Override
    public Optional<AudienceMember> viewer(UUID userId) {
        try {
            return directory.memberOf(userId);
        } catch (AudienceUnavailableException unavailable) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> ageBandOf(UUID userId) {
        return viewer(userId).map(AudienceMember::ageBand);
    }

    private Optional<AudienceMember> memberOf(UUID userId) {
        try {
            return directory.memberOf(userId);
        } catch (AudienceUnavailableException unavailable) {
            throw new JoinRejectedException("demographics.unavailable", "Your profile cannot be checked right now");
        }
    }
}
