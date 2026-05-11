package org.example.ais_sst.service.socialStatusService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.social_status.UserSocialStatusesDTO;
import org.example.ais_sst.entity.SocialStatus;
import org.example.ais_sst.entity.SocialStatusStudent;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.exception.SocialStatusDoesNotExistException;
import org.example.ais_sst.exception.UserDoesNotExistException;
import org.example.ais_sst.repository.SocialStatusRepository;
import org.example.ais_sst.repository.SocialStatusStudentsRepository;
import org.example.ais_sst.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialStatusService {

    private final SocialStatusRepository socialStatusRepository;
    private final SocialStatusStudentsRepository socialStatusStudentsRepository;
    private final UserRepository userRepository;

    public List<SocialStatus> getSocialStatuses() {
        return socialStatusRepository.findAll();
    }

    public List<SocialStatusStudent> createUserSocialStatuses(UserSocialStatusesDTO dto) {
        Long userId = dto.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserDoesNotExistException(
                        String.format("Пользователь с id: %d не существует", userId)));

        List<Long> socialStatusIds = dto.getSocial_statuses_id();

        if (socialStatusIds == null || socialStatusIds.isEmpty()) {
            log.info("No social statuses to add for user id: {}", userId);
            return List.of();
        }

        // Убираем дубликаты для проверки
        List<Long> uniqueSocialStatusIds = socialStatusIds.stream()
                .distinct()
                .collect(Collectors.toList());

        List<SocialStatus> socialStatuses = socialStatusRepository.findAllById(uniqueSocialStatusIds);

        if (socialStatuses.size() != uniqueSocialStatusIds.size()) {
            Set<Long> foundIds = socialStatuses.stream()
                    .map(SocialStatus::getId)
                    .collect(Collectors.toSet());
            List<Long> missingIds = uniqueSocialStatusIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toList());
            throw new SocialStatusDoesNotExistException(
                    String.format("Социальные статусы с ids не были найдены: %s", missingIds));
        }

        List<SocialStatusStudent> entities = socialStatuses.stream()
                .map(socialStatus -> SocialStatusStudent.builder()
                        .student(user)
                        .socialStatus(socialStatus)
                        .build())
                .collect(Collectors.toList());

        return socialStatusStudentsRepository.saveAll(entities);
    }
}
