package es.upm.miw.oneleft.plans.domain.port.in;

/**
 * Moves plans along their lifecycle (HU-007): reminders before they start, «in progress» when they start and
 * «finished» when they end.
 */
public interface AdvancePlansUseCase {

    /** @return how many plans changed */
    int advance();
}
