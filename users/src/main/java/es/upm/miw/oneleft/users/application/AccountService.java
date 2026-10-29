package es.upm.miw.oneleft.users.application;

import es.upm.miw.oneleft.users.domain.model.Identity;
import es.upm.miw.oneleft.users.domain.model.PhotoKey;
import es.upm.miw.oneleft.users.domain.port.in.ManageAccountUseCase;
import es.upm.miw.oneleft.users.domain.port.out.AccountEvents;
import es.upm.miw.oneleft.users.domain.port.out.Identities;
import es.upm.miw.oneleft.users.domain.port.out.PersonalData;
import es.upm.miw.oneleft.users.domain.port.out.PhotoStorage;
import es.upm.miw.oneleft.users.domain.port.out.ServicesData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * My data and my account (HU-045). Deleting goes in an order that can be retried: my data here and my photos first,
 * then the account in Keycloak; if either fails nothing is announced and the person can try again. Only when it is all
 * saved do the other services hear about it, each one to erase its part.
 */
@Service
@Transactional
public class AccountService implements ManageAccountUseCase {

    private final PersonalData data;
    private final ServicesData services;
    private final PhotoStorage storage;
    private final Identities identities;
    private final AccountEvents events;
    private final Clock clock;

    @SuppressWarnings("java:S107")
    public AccountService(PersonalData data, ServicesData services, PhotoStorage storage, Identities identities,
                          AccountEvents events, Clock clock) {
        this.data = data;
        this.services = services;
        this.storage = storage;
        this.identities = identities;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> exportMyData(Identity identity) {
        var userId = UUID.fromString(identity.subject());
        var copy = new LinkedHashMap<String, Object>();
        copy.put("exportedAt", clock.instant());
        copy.put("account", accountOf(identity));
        copy.put("users", data.of(userId));
        copy.putAll(services.of(userId));
        return copy;
    }

    @Override
    public void deleteMyAccount(Identity identity) {
        var userId = UUID.fromString(identity.subject());
        data.erase(userId);
        storage.deleteUnder(PhotoKey.prefixOf(userId), null);
        identities.delete(userId);
        events.deleted(userId, clock.instant());
    }

    private static Map<String, Object> accountOf(Identity identity) {
        var account = new LinkedHashMap<String, Object>();
        account.put("userId", identity.subject());
        account.put("name", identity.name());
        account.put("email", identity.email());
        account.put("roles", identity.roles());
        return account;
    }
}
