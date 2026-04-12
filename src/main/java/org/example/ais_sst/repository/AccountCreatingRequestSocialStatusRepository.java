package org.example.ais_sst.repository;

import org.example.ais_sst.entity.AccountCreatingRequest;
import org.example.ais_sst.entity.AccountCreatingRequestsSocialStatuses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountCreatingRequestSocialStatusRepository extends JpaRepository<AccountCreatingRequestsSocialStatuses, Long> {
    List<AccountCreatingRequestsSocialStatuses> findByAccountCreatingRequest(AccountCreatingRequest request);
}
