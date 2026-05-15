package org.example.ais_sst.service.base;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Slf4j
public abstract class BaseEntityService {

    protected <T, ID> T findEntityOrThrow(
            ID id,
            Function<ID, Optional<T>> finder,
            Supplier<? extends RuntimeException> exceptionSupplier,
            String entityName) {

        return finder.apply(id)
                .orElseThrow(() -> {
                    log.warn("{} with id {} not found", entityName, id);
                    return exceptionSupplier.get();
                });
    }

    protected <T> void validateEntity(
            T entity,
            Predicate<T> condition,
            Supplier<? extends RuntimeException> exceptionSupplier,
            String errorMessage) {

        if (!condition.test(entity)) {
            log.warn("Validation failed: {}", errorMessage);
            throw exceptionSupplier.get();
        }
    }

    protected void validateState(
            boolean condition,
            Supplier<? extends RuntimeException> exceptionSupplier,
            String errorMessage) {

        if (!condition) {
            log.warn("Validation failed: {}", errorMessage);
            throw exceptionSupplier.get();
        }
    }

    protected <T> T executeWithLogging(
            Supplier<T> action,
            String operation,
            Object... params) {

        log.info("Executing {} with params: {}", operation, params);
        long startTime = System.currentTimeMillis();

        try {
            T result = action.get();
            long duration = System.currentTimeMillis() - startTime;
            log.info("{} completed successfully in {} ms", operation, duration);
            return result;
        } catch (Exception e) {
            log.error("{} failed: {}", operation, e.getMessage());
            throw e;
        }
    }

    protected void executeVoidWithLogging(
            Runnable action,
            String operation,
            Object... params) {

        log.info("Executing {} with params: {}", operation, params);
        long startTime = System.currentTimeMillis();

        try {
            action.run();
            long duration = System.currentTimeMillis() - startTime;
            log.info("{} completed successfully in {} ms", operation, duration);
        } catch (Exception e) {
            log.error("{} failed: {}", operation, e.getMessage());
            throw e;
        }
    }
}