package es.upm.miw.oneleft.reputation.domain.port.out;

import es.upm.miw.oneleft.reputation.domain.model.Attendance;
import es.upm.miw.oneleft.reputation.domain.model.TrackRecord;

import java.util.List;
import java.util.UUID;

/** What each person has done in the plans held, for the badges. */
public interface TrackRecords {

    /** The same person and plan twice count once */
    void record(List<Attendance> attendances);

    TrackRecord of(UUID userId);
}
