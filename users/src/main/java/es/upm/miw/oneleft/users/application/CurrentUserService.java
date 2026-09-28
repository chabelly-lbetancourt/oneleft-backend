package es.upm.miw.oneleft.users.application;

import es.upm.miw.oneleft.users.domain.model.Identity;
import es.upm.miw.oneleft.users.domain.model.Role;
import es.upm.miw.oneleft.users.domain.model.User;
import es.upm.miw.oneleft.users.domain.port.in.GetCurrentUserUseCase;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CurrentUserService implements GetCurrentUserUseCase {

    @Override
    public User currentUser(Identity identity) {
        var roles = identity.roles().stream()
                .map(Role::fromName)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
        var name = identity.name() == null || identity.name().isBlank() ? identity.email() : identity.name();
        return new User(UUID.fromString(identity.subject()), name, identity.email(), roles);
    }
}
