package es.upm.miw.oneleft.users.domain.port.in;

import es.upm.miw.oneleft.users.domain.model.Identity;
import es.upm.miw.oneleft.users.domain.model.User;

/**
 * Caso de uso: obtener el usuario de OneLeft correspondiente a la identidad autenticada.
 */
public interface GetCurrentUserUseCase {

    User currentUser(Identity identity);
}
