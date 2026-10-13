package es.upm.miw.oneleft.plans.domain.port.in;

/** Closes the attendance of the plans whose check-in has closed (HU-025, HU-009). */
public interface CloseAttendanceUseCase {

    /** @return how many plans had their attendance closed */
    int closeDue();
}
