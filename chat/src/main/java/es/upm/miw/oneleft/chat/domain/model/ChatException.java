package es.upm.miw.oneleft.chat.domain.model;

/**
 * Why the chat of a plan cannot be read or written, with a stable code for the clients:
 * {@code chat.notMember}, {@code chat.closed}, {@code plan.notFound} or {@code chat.unavailable}.
 */
public class ChatException extends RuntimeException {

    public enum Reason {
        /** Only the organizer and the participants are in the chat. */
        NOT_MEMBER("chat.notMember"),
        /** The plan is over or cancelled: the chat can be read but nobody writes any more. */
        CLOSED("chat.closed"),
        PLAN_NOT_FOUND("plan.notFound"),
        /** The plans service did not answer, so it is not known who is in the plan. */
        UNAVAILABLE("chat.unavailable");

        private final String code;

        Reason(String code) {
            this.code = code;
        }

        public String code() {
            return code;
        }
    }

    private final Reason reason;

    public ChatException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }

    public String code() {
        return reason.code();
    }
}
