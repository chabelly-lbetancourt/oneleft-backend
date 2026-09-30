package es.upm.miw.oneleft.notifications.domain.port.out;

import es.upm.miw.oneleft.notifications.domain.model.NearbyPlanNotice;

/**
 * Delivers a notice to the app while it is open (real-time stream).
 */
public interface NoticePublisher {

    void publish(NearbyPlanNotice notice);
}
