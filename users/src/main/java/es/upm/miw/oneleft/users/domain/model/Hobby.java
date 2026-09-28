package es.upm.miw.oneleft.users.domain.model;

public record Hobby(Activity activity, Level level) {

    public Hobby {
        if (activity == null || level == null) {
            throw new ValidationException("hobby.incomplete", "A hobby needs an activity and a level");
        }
    }
}
