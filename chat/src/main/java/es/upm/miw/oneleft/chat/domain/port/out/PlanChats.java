package es.upm.miw.oneleft.chat.domain.port.out;

import es.upm.miw.oneleft.chat.domain.model.PlanChat;

import java.util.UUID;

/**
 * Output port to the plans service: who is in a plan and until when (HU-008). It fails with
 * {@code plan.notFound} or, when the service does not answer, {@code chat.unavailable}.
 */
public interface PlanChats {

    PlanChat find(UUID planId);
}
