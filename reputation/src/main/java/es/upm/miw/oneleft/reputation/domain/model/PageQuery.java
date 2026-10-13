package es.upm.miw.oneleft.reputation.domain.model;

/** A page of a list: from 0, with 1 to {@link #MAX_SIZE} items. */
public record PageQuery(int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 50;

    public PageQuery {
        if (page < 0 || size < 1 || size > MAX_SIZE) {
            throw new ValidationException("page.invalid", "Pages start at 0 and have 1 to " + MAX_SIZE + " items");
        }
    }
}
