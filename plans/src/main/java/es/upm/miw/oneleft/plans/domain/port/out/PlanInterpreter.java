package es.upm.miw.oneleft.plans.domain.port.out;

import es.upm.miw.oneleft.plans.domain.model.RawDraft;

import java.time.Instant;
import java.time.ZoneId;

/** The language model that understands a sentence (HU-010). */
public interface PlanInterpreter {

    /**
     * @param now  the current time, to resolve «in an hour» or «tonight»
     * @param zone the zone of the person, to resolve «at 7»
     * @throws es.upm.miw.oneleft.plans.domain.model.DraftUnavailableException if it cannot answer
     */
    RawDraft interpret(String text, Instant now, ZoneId zone);
}
