package org.example.ais_sst.controller.applications;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ApplicationStrategy<T, C, R, F> {
    T createApplication(C createDto);
    T rejectApplication(Long id, R rejectDto);
    T acceptApplication(Long id);
    T getApplicationById(Long id);
    Page<T> getAllApplications(F filter, Pageable pageable);
}