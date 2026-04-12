package org.example.ais_sst.service.socialStatusService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestsSummaryDTO;
import org.example.ais_sst.entity.AccountCreatingRequest;
import org.example.ais_sst.entity.AccountCreatingRequestsSocialStatuses;
import org.example.ais_sst.entity.SocialStatus;
import org.example.ais_sst.exception.SocialStatusDoesNotExistException;
import org.example.ais_sst.repository.AccountCreatingRequestSocialStatusRepository;
import org.example.ais_sst.repository.AccountCreatingRequestsRepository;
import org.example.ais_sst.repository.SocialStatusRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class AccountCreatingRequestsSocialStatusService {

    private final AccountCreatingRequestsRepository accountCreatingRequestsRepository;
    private final AccountCreatingRequestSocialStatusRepository accountCreatingRequestSocialStatusRepository;
    private final SocialStatusRepository socialStatusRepository;

    public List<Long> getSocialStatusIdsByRequestId(Long requestId) {
        AccountCreatingRequest request = accountCreatingRequestsRepository
                .findAccountCreatingRequestById(requestId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Request with id: %d not found", requestId)));

        // Предполагаем, что у вас есть репозиторий для получения связей
        List<AccountCreatingRequestsSocialStatuses> relations =
                accountCreatingRequestSocialStatusRepository.findByAccountCreatingRequest(request);

        return relations.stream()
                .map(relation -> relation.getSocialStatus().getId())
                .collect(Collectors.toList());
    }

    public List<AccountCreatingRequestsSocialStatuses> createAccountCreatingRequestSocialStatus(
            AccountCreatingRequestsSummaryDTO dto) {

        Long requestId = dto.getId();
        AccountCreatingRequest request = accountCreatingRequestsRepository
                .findAccountCreatingRequestById(requestId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Request with id: %d not found", requestId)));

        List<Long> socialStatusIds = dto.getSocial_statuses_id();

        List<SocialStatus> socialStatuses = socialStatusRepository.findAllById(socialStatusIds);

        if (socialStatuses.size() != socialStatusIds.size()) {
            Set<Long> foundIds = socialStatuses.stream()
                    .map(SocialStatus::getId)
                    .collect(Collectors.toSet());
            List<Long> missingIds = socialStatusIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();
            throw new SocialStatusDoesNotExistException(
                    String.format("Social statuses with ids not found: %s", missingIds));
        }

        List<AccountCreatingRequestsSocialStatuses> entities = socialStatuses.stream()
                .map(socialStatus -> AccountCreatingRequestsSocialStatuses.builder()
                        .accountCreatingRequest(request)
                        .socialStatus(socialStatus)
                        .build())
                .collect(Collectors.toList());

        return accountCreatingRequestSocialStatusRepository.saveAll(entities);

    }
}
