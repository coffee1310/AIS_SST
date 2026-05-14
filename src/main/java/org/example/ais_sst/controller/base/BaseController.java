package org.example.ais_sst.controller.base;

import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.common.PageRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

@Slf4j
public abstract class BaseController {

    protected Pageable createPageable(int page, int size, String sortBy, String sortDirection) {
        Sort.Direction direction = Sort.Direction.fromString(sortDirection.toUpperCase());
        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }

    protected Pageable createPageable(int page, int size) {
        return PageRequest.of(page, size);
    }

    protected <T> ResponseEntity<Map<String, Object>> createSuccessResponse(String message, T data) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    protected <T> ResponseEntity<Map<String, Object>> createSuccessResponse(T data) {
        Map<String, Object> response = new HashMap<>();
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    protected void logInfo(String endpoint, String message, Object... args) {
        log.info("{} - {}", String.format(endpoint, args), message);
    }

    // Для сервисов с сигнатурой (filter, pageable)
    protected <T, F> Page<T> getFilteredPage(
            BiFunction<F, Pageable, Page<T>> serviceMethod,
            F filter,
            int page, int size, String sortBy, String sortDirection) {

        Pageable pageable = createPageable(page, size, sortBy, sortDirection);
        return serviceMethod.apply(filter, pageable);
    }

    // Для сервисов с сигнатурой (filter, page, size, sortBy, sortDirection)
    @FunctionalInterface
    public interface FilteredPageFunction<T, F> {
        Page<T> apply(F filter, int page, int size, String sortBy, String sortDirection);
    }

    protected <T, F> Page<T> getFilteredPageWithParams(
            FilteredPageFunction<T, F> serviceMethod,
            F filter,
            int page, int size, String sortBy, String sortDirection) {

        return serviceMethod.apply(filter, page, size, sortBy, sortDirection);
    }

    // Перегруженный метод для PageRequestDTO
    protected <T, F> Page<T> getFilteredPage(
            BiFunction<F, Pageable, Page<T>> serviceMethod,
            F filter,
            Pageable pageable) {

        return serviceMethod.apply(filter, pageable);
    }

    protected void logWarn(String endpoint, String message, Object... args) {
        log.warn("{} - {}", String.format(endpoint, args), message);
    }

    protected void logError(String endpoint, String message, Object... args) {
        log.error("{} - {}", String.format(endpoint, args), message);
    }

    protected void logDebug(String endpoint, String message, Object... args) {
        log.debug("{} - {}", String.format(endpoint, args), message);
    }
}