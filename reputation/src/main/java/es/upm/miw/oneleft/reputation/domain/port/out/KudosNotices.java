package es.upm.miw.oneleft.reputation.domain.port.out;

import es.upm.miw.oneleft.reputation.domain.model.Kudos;

/** Tells someone, in real time, that they received a kudos (HU-060), once it is saved. */
public interface KudosNotices {

    void received(Kudos kudos, String giverName);
}
