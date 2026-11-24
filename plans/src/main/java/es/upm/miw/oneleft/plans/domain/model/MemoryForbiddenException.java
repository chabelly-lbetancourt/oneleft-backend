package es.upm.miw.oneleft.plans.domain.model;

/**
 * The album of a plan is only for its group, and each photo is only removed by whoever uploaded it (HU-052). The
 * person is known but may not do this: the API answers 403 with the {@code code}.
 */
public class MemoryForbiddenException extends RuntimeException {

    public static final String NOT_PARTICIPANT = "plan.notParticipant";
    public static final String NOT_YOURS = "memory.notYours";

    private final String code;

    public MemoryForbiddenException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
