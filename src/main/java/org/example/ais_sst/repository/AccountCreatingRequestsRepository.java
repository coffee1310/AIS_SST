package org.example.ais_sst.repository;

import org.example.ais_sst.entity.AccountCreatingRequest;
import org.example.ais_sst.entity.enums.AccountCreatingRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.Optional;

@Repository
public interface AccountCreatingRequestsRepository extends JpaRepository<AccountCreatingRequest, Long> {
    Optional<AccountCreatingRequest> findAccountCreatingRequestById(Long id);

    Page<AccountCreatingRequest> findByStatus(AccountCreatingRequestStatus status, Pageable pageable);
}
