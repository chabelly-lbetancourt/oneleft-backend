package es.upm.miw.oneleft.users.domain.port.in;

import es.upm.miw.oneleft.users.domain.model.ApproximateZone;
import es.upm.miw.oneleft.users.domain.model.Hobby;
import es.upm.miw.oneleft.users.domain.model.Identity;
import es.upm.miw.oneleft.users.domain.model.Profile;

import java.util.List;
import java.util.UUID;

/**
 * Casos de uso del perfil: consultar y editar el propio, y ver el de otro participante.
 */
public interface ManageProfileUseCase {

    /** Devuelve el perfil del usuario autenticado; si aún no existe, lo crea a partir de su identidad. */
    Profile myProfile(Identity identity);

    Profile updateMyProfile(Identity identity, String displayName, ApproximateZone zone, List<Hobby> hobbies);

    /** @throws es.upm.miw.oneleft.users.domain.model.ProfileNotFoundException si el usuario no tiene perfil */
    Profile profileOf(UUID userId);
}
