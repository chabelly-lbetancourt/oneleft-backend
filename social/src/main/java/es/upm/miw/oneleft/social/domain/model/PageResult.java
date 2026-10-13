package es.upm.miw.oneleft.social.domain.model;

import java.util.List;

/** One page of a list and how many there are in total. */
public record PageResult<T>(List<T> items, int page, int size, long total) {

    public PageResult {
        items = List.copyOf(items);
    }
}
