package org.example.ais_sst.controller.applications;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.controller.base.BaseController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
public abstract class BaseApplicationController<T, C, R, F> extends BaseController {

    protected abstract ApplicationStrategy<T, C, R, F> getStrategy();
    protected abstract String getApplicationName();

    @PostMapping
    public ResponseEntity<T> createApplication(@Valid @RequestBody C createDto) {
        log.info("Creating {} application", getApplicationName());
        T application = getStrategy().createApplication(createDto);
        return new ResponseEntity<>(application, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<T> getApplicationById(@PathVariable Long id) {
        log.info("Getting {} application by id: {}", getApplicationName(), id);
        T application = getStrategy().getApplicationById(id);
        return ResponseEntity.ok(application);
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<?> acceptApplication(@PathVariable Long id) {
        log.info("Accepting {} application with id: {}", getApplicationName(), id);
        T application = getStrategy().acceptApplication(id);
        return createSuccessResponse(getApplicationName() + " принята", application);
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectApplication(
            @PathVariable Long id,
            @Valid @RequestBody R rejectDto) {
        log.info("Rejecting {} application with id: {}", getApplicationName(), id);
        T application = getStrategy().rejectApplication(id, rejectDto);
        return createSuccessResponse(getApplicationName() + " отклонена", application);
    }

    @GetMapping
    public ResponseEntity<Page<T>> getAllApplications(F filter, Pageable pageable) {
        log.info("Getting all {} applications with filters", getApplicationName());
        Page<T> applications = getStrategy().getAllApplications(filter, pageable);
        return ResponseEntity.ok(applications);
    }
}