package es.upm.miw.oneleft.plans.domain.model;

/** The users service did not say who the person is (HU-070): a plan with an audience cannot be joined now. */
public class AudienceUnavailableException extends RuntimeException {

    public AudienceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
