package es.upm.miw.oneleft.users.infrastructure.rest;

import es.upm.miw.oneleft.users.domain.model.Role;
import es.upm.miw.oneleft.users.domain.model.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Usuario de OneLeft")
public record UserResponse(
        @Schema(description = "Identificador (el mismo que en Keycloak)") UUID id,
        @Schema(description = "Nombre visible", example = "Ana Pruebas") String name,
        @Schema(description = "Email", example = "ana@oneleft.dev") String email,
        @Schema(description = "Roles en OneLeft", example = "[\"USER\"]") List<String> roles) {

    public static UserResponse of(User user) {
        var roles = user.roles().stream().map(Role::name).sorted().toList();
        return new UserResponse(user.id(), user.name(), user.email(), roles);
    }
}
