package es.upm.miw.oneleft.plans.domain.model;

/** An activity someone free would do, with their level in it ({@code null} if they did not say). */
public record Interest(Activity activity, Level level) {

    public Interest {
        if (activity == null) {
            throw new ValidationException("availability.activity", "An interest needs an activity");
        }
    }
}
