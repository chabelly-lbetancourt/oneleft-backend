package es.upm.miw.oneleft.users.infrastructure.rest;

import es.upm.miw.oneleft.users.domain.model.Role;
import es.upm.miw.oneleft.users.domain.model.User;

import java.util.List;
import java.util.UUID;

public record UserResponse(UUID id, String name, String email, List<String> roles) {

    public static UserResponse of(User user) {
        var roles = user.roles().stream().map(Role::name).sorted().toList();
        return new UserResponse(user.id(), user.name(), user.email(), roles);
    }
}
