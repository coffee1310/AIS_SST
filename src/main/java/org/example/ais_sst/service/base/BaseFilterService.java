package org.example.ais_sst.service.base;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

@Slf4j
public abstract class BaseFilterService {

    protected <T, F> Page<T> getFilteredPage(
            BiFunction<F, Pageable, List<T>> findMethod,
            Function<F, Long> countMethod,
            F filter,
            Pageable pageable) {

        log.info("Getting filtered page with filter: {}", filter);

        List<T> content = findMethod.apply(filter, pageable);
        long total = countMethod.apply(filter);

        return new PageImpl<>(content, pageable, total);
    }

    protected <T, F> Page<T> getFilteredPageWithNative(
            BiFunction<F, Integer, List<T>> findMethod,
            Function<F, Long> countMethod,
            F filter,
            Pageable pageable) {

        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();

        List<T> content = findMethod.apply(filter, offset);
        long total = countMethod.apply(filter);

        return new PageImpl<>(content, pageable, total);
    }

    protected Sort createSort(String sortBy, String sortDirection) {
        Sort.Direction direction = Sort.Direction.fromString(sortDirection.toUpperCase());
        return Sort.by(direction, sortBy);
    }

}
