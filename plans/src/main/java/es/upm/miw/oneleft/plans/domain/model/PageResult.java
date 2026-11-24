package es.upm.miw.oneleft.plans.domain.model;

import java.util.List;
import java.util.function.Function;

/** One page of a list and how many there are in total. */
public record PageResult<T>(List<T> items, int page, int size, long total) {

    public PageResult {
        items = List.copyOf(items);
    }

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(items.stream().map(mapper).toList(), page, size, total);
    }
}
