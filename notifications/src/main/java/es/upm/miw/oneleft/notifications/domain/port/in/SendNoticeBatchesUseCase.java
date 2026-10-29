package es.upm.miw.oneleft.notifications.domain.port.in;

/**
 * Sends the next batch of notices of the plans that still have free spots (HU-012).
 */
public interface SendNoticeBatchesUseCase {

    /** @return how many people were notified */
    int sendDue();
}
