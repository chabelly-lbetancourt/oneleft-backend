package es.upm.miw.oneleft.reputation.domain.port.in;

import es.upm.miw.oneleft.reputation.domain.model.AttendanceFact;
import es.upm.miw.oneleft.reputation.domain.model.CancellationFact;
import es.upm.miw.oneleft.reputation.domain.model.CheckInFact;
import es.upm.miw.oneleft.reputation.domain.model.ReportFact;

/** Turns the events of the other services into entries of the ledger. The same event twice changes nothing. */
public interface RecordReputationUseCase {

    void record(CheckInFact checkIn);

    void record(AttendanceFact attendance);

    void record(CancellationFact cancellation);

    void record(ReportFact report);
}
