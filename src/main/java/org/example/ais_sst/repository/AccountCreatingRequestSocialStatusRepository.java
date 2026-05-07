package org.example.ais_sst.repository;

import org.example.ais_sst.entity.AccountCreatingRequest;
import org.example.ais_sst.entity.AccountCreatingRequestsSocialStatuses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountCreatingRequestSocialStatusRepository extends JpaRepository<AccountCreatingRequestsSocialStatuses, Long> {
    List<AccountCreatingRequestsSocialStatuses> findByAccountCreatingRequest(AccountCreatingRequest request);

    List<AccountCreatingRequestsSocialStatuses> findByAccountCreatingRequestId(Long requestId);

    @Query("SELECT acrss.socialStatus.title FROM account_creating_requests_social_statuses acrss WHERE acrss.accountCreatingRequest.id = :requestId")
    List<String> findSocialStatusTitlesByRequestId(@Param("requestId") Long requestId);
}
