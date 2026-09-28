package es.upm.miw.oneleft.users.application;

import es.upm.miw.oneleft.users.domain.model.ApproximateZone;
import es.upm.miw.oneleft.users.domain.model.Hobby;
import es.upm.miw.oneleft.users.domain.model.Identity;
import es.upm.miw.oneleft.users.domain.model.Profile;
import es.upm.miw.oneleft.users.domain.model.ProfileNotFoundException;
import es.upm.miw.oneleft.users.domain.port.in.GetCurrentUserUseCase;
import es.upm.miw.oneleft.users.domain.port.in.ManageProfileUseCase;
import es.upm.miw.oneleft.users.domain.port.out.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProfileService implements ManageProfileUseCase {

    private final GetCurrentUserUseCase currentUser;
    private final ProfileRepository profiles;

    public ProfileService(GetCurrentUserUseCase currentUser, ProfileRepository profiles) {
        this.currentUser = currentUser;
        this.profiles = profiles;
    }

    @Override
    @Transactional
    public Profile myProfile(Identity identity) {
        var user = currentUser.currentUser(identity);
        return profiles.findById(user.id()).orElseGet(() -> profiles.save(Profile.initial(user)));
    }

    @Override
    @Transactional
    public Profile updateMyProfile(Identity identity, String displayName, ApproximateZone zone, List<Hobby> hobbies) {
        return profiles.save(myProfile(identity).update(displayName, zone, hobbies));
    }

    @Override
    @Transactional(readOnly = true)
    public Profile profileOf(UUID userId) {
        return profiles.findById(userId).orElseThrow(() -> new ProfileNotFoundException(userId));
    }
}
