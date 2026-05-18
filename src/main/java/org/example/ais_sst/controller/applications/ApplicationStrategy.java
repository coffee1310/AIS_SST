package org.example.ais_sst.controller.applications;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ApplicationStrategy <T, C, R, F> {
    T createApplication(C CreateDto);
    T rejectApplication(Long id, R RejectDto);
    T acceptApplication(Long id);
    T getById(Long id);
    Page<T> getAll(F filter, Pageable page);
}
