package org.example.ais_sst.integration.services;

import org.example.ais_sst.dto.user.UserFilterDTO;
import org.example.ais_sst.dto.user.UserProfileInfoDTO;
import org.example.ais_sst.dto.user.UserResponseDTO;
import org.example.ais_sst.entity.*;
import org.example.ais_sst.entity.enums.Gender;
import org.example.ais_sst.entity.enums.SectorParticipantStatuses;
import org.example.ais_sst.integration.BaseIntegrationTest;
import org.example.ais_sst.repository.*;
import org.example.ais_sst.service.userService.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private SpecialityRepository specialityRepository;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private SectorParticipantRepository sectorParticipantRepository;

    @Autowired
    private SocialStatusStudentsRepository socialStatusStudentsRepository;

    @Autowired
    private SocialStatusRepository socialStatusRepository;

    private Role testRole;
    private Role adminRole;
    private Group testGroup;
    private Group anotherGroup;
    private Speciality testSpeciality;
    private Speciality anotherSpeciality;
    private Sector testSector;
    private Sector anotherSector;
    private User testUser;
    private User coordinatorUser;
    private User inactiveUser;
    private User bannedUser;
    private User userWithoutSocialStatuses;
    private SocialStatus socialStatus1;
    private SocialStatus socialStatus2;
    private SocialStatus socialStatus3;

    @Autowired
    private Environment env;

    @Test
    public void testActiveProfiles() {
        System.out.println("Active profiles: " + String.join(", ", env.getActiveProfiles()));
    }

    @Value("${spring.security.enabled:not set}")
    private String securityEnabled;

    @Test
    public void testSecurityProperty() {
        System.out.println("spring.security.enabled = " + securityEnabled);
    }
    @BeforeEach
    void setUp() {
        // Создаем роли
        testRole = roleRepository.findByTitle("Activist")
                .orElseGet(() -> roleRepository.save(Role.builder().title("Activist").build()));

        adminRole = roleRepository.findByTitle("Admin")
                .orElseGet(() -> roleRepository.save(Role.builder().title("Admin").build()));

        // Создаем группы
        testGroup = groupRepository.findGroupByTitle("422")
                .orElseGet(() -> groupRepository.save(Group.builder()
                        .title("422")
                        .course(3)
                        .build()));

        anotherGroup = groupRepository.save(Group.builder()
                .title("421")
                .course(4)
                .build());

        // Создаем специальности
        testSpeciality = specialityRepository.findSpecialityByTitle("Информационные системы и программирование")
                .orElseGet(() -> specialityRepository.save(Speciality.builder()
                        .title("Информационные системы и программирование")
                        .shortTitle("ИСИП")
                        .build()));

        anotherSpeciality = specialityRepository.save(Speciality.builder()
                .title("Программная инженерия")
                .shortTitle("ПИ")
                .build());

        // Создаем сектора
        testSector = sectorRepository.save(Sector.builder()
                .title("Спортивный сектор")
                .description("Описание спортивного сектора")
                .isActive(true)
                .build());

        anotherSector = sectorRepository.save(Sector.builder()
                .title("Культурный сектор")
                .description("Описание культурного сектора")
                .isActive(true)
                .build());

        // Создаем социальные статусы
        socialStatus1 = socialStatusRepository.save(SocialStatus.builder().title("Студент").build());
        socialStatus2 = socialStatusRepository.save(SocialStatus.builder().title("Активист").build());
        socialStatus3 = socialStatusRepository.save(SocialStatus.builder().title("Староста").build());

        // Активный пользователь с соц. статусами
        testUser = userRepository.save(User.builder()
                .name("Иван")
                .surname("Иванов")
                .patronymic("Иванович")
                .gender(Gender.Мужчина)
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .studentEmail("ivan@test.com")
                .phoneNumber("+79991234567")
                .password("encodedPassword")
                .studentIdNumber(12345)
                .courseNumber((short) 3)
                .role(testRole)
                .group(testGroup)
                .speciality(testSpeciality)
                .isActive(true)
                .isBanned(false)
                .vkLink("https://vk.com/ivan")
                .additionalEmail("ivan.extra@test.com")
                .pathToPhoto("/photos/ivan.jpg")
                .build());

        // Координатор сектора
        coordinatorUser = userRepository.save(User.builder()
                .name("Петр")
                .surname("Петров")
                .patronymic("Петрович")
                .gender(Gender.Мужчина)
                .dateOfBirth(LocalDate.of(1999, 5, 15))
                .studentEmail("petr@test.com")
                .phoneNumber("+79991234568")
                .password("encodedPassword")
                .studentIdNumber(12346)
                .courseNumber((short) 4)
                .role(testRole)
                .group(anotherGroup)
                .speciality(anotherSpeciality)
                .isActive(true)
                .isBanned(false)
                .build());

        // Неактивный пользователь
        inactiveUser = userRepository.save(User.builder()
                .name("Сергей")
                .surname("Сергеев")
                .patronymic("Сергеевич")
                .gender(Gender.Мужчина)
                .dateOfBirth(LocalDate.of(2001, 3, 20))
                .studentEmail("sergey@test.com")
                .phoneNumber("+79991234569")
                .password("encodedPassword")
                .studentIdNumber(12347)
                .courseNumber((short) 2)
                .role(testRole)
                .group(testGroup)
                .speciality(testSpeciality)
                .isActive(false)
                .isBanned(false)
                .build());

        // Забаненный пользователь
        bannedUser = userRepository.save(User.builder()
                .name("Дмитрий")
                .surname("Дмитриев")
                .patronymic("Дмитриевич")
                .gender(Gender.Мужчина)
                .dateOfBirth(LocalDate.of(2000, 7, 10))
                .studentEmail("dmitry@test.com")
                .phoneNumber("+79991234570")
                .password("encodedPassword")
                .studentIdNumber(12348)
                .courseNumber((short) 3)
                .role(testRole)
                .group(anotherGroup)
                .speciality(anotherSpeciality)
                .isActive(true)
                .isBanned(true)
                .build());

        // Пользователь без социальных статусов
        userWithoutSocialStatuses = userRepository.save(User.builder()
                .name("Алексей")
                .surname("Алексеев")
                .patronymic("Алексеевич")
                .gender(Gender.Мужчина)
                .dateOfBirth(LocalDate.of(2002, 11, 5))
                .studentEmail("alexey@test.com")
                .phoneNumber("+79991234571")
                .password("encodedPassword")
                .studentIdNumber(12349)
                .courseNumber((short) 1)
                .role(testRole)
                .group(testGroup)
                .speciality(testSpeciality)
                .isActive(true)
                .isBanned(false)
                .build());

        // Добавляем социальные статусы для testUser
        SocialStatusStudent statusStudent1 = SocialStatusStudent.builder()
                .student(testUser)
                .socialStatus(socialStatus1)
                .build();
        SocialStatusStudent statusStudent2 = SocialStatusStudent.builder()
                .student(testUser)
                .socialStatus(socialStatus2)
                .build();
        socialStatusStudentsRepository.saveAll(List.of(statusStudent1, statusStudent2));

        // Добавляем социальные статусы для coordinatorUser
        SocialStatusStudent statusStudent3 = SocialStatusStudent.builder()
                .student(coordinatorUser)
                .socialStatus(socialStatus3)
                .build();
        socialStatusStudentsRepository.save(statusStudent3);

        // Добавляем участников секторов
        SectorParticipant coordinatorParticipant = SectorParticipant.builder()
                .student(coordinatorUser)
                .sector(testSector)
                .isCoordinator(true)
                .entryDate(LocalDate.now())
                .status(SectorParticipantStatuses.Активный)
                .build();
        sectorParticipantRepository.save(coordinatorParticipant);

        SectorParticipant regularParticipant = SectorParticipant.builder()
                .student(testUser)
                .sector(testSector)
                .isCoordinator(false)
                .entryDate(LocalDate.now())
                .status(SectorParticipantStatuses.Активный)
                .build();
        sectorParticipantRepository.save(regularParticipant);

        SectorParticipant anotherParticipant = SectorParticipant.builder()
                .student(inactiveUser)
                .sector(anotherSector)
                .isCoordinator(false)
                .entryDate(LocalDate.now())
                .status(SectorParticipantStatuses.Активный)
                .build();
        sectorParticipantRepository.save(anotherParticipant);
    }

    // ==================== TESTS FOR getUserBasicInfo ====================

    @Nested
    class GetUserBasicInfoTests {

        @Test
        void getUserBasicInfo_Success() {
            UserProfileInfoDTO result = userService.getUserBasicInfo(testUser.getId());

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testUser.getId());
            assertThat(result.getName()).isEqualTo("Иван");
            assertThat(result.getSurname()).isEqualTo("Иванов");
            assertThat(result.getPatronymic()).isEqualTo("Иванович");
            assertThat(result.getStudentEmail()).isEqualTo("ivan@test.com");
            assertThat(result.getPhoneNumber()).isEqualTo("+79991234567");
            assertThat(result.getRoleTitle()).isEqualTo("Activist");
            // Исправлено: используем правильное значение из БД
            assertThat(result.getShortSpecialityTitle()).isEqualTo("ИСИП");
            assertThat(result.getSocialStatuses()).containsExactlyInAnyOrder("Студент", "Активист");
            assertThat(result.getGender()).isEqualTo(Gender.Мужчина);
            assertThat(result.getCourseNumber()).isEqualTo((short) 3);
            assertThat(result.getVkLink()).isEqualTo("https://vk.com/ivan");
        }

        @Test
        void getUserBasicInfo_WithAllOptionalFields_ReturnsCompleteInfo() {
            UserProfileInfoDTO result = userService.getUserBasicInfo(testUser.getId());

            assertThat(result.getAdditionalEmail()).isEqualTo("ivan.extra@test.com");
            assertThat(result.getSpecialityTitle()).isEqualTo("Информационные системы и программирование");
            assertThat(result.getGroupTitle()).isEqualTo("422");
        }

        @Test
        void getUserBasicInfo_UserNotFound_ThrowsException() {
            assertThatThrownBy(() -> userService.getUserBasicInfo(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Пользователь не найден");
        }

        @Test
        void getUserBasicInfo_WithNullUserId_ThrowsException() {
            assertThatThrownBy(() -> userService.getUserBasicInfo(null))
                    .isInstanceOf(Exception.class);
        }

        @Test
        void getUserBasicInfo_ReturnsCoordinatorInfoForCoordinator() {
            UserProfileInfoDTO result = userService.getUserBasicInfo(coordinatorUser.getId());

            assertThat(result).isNotNull();
            assertThat(result.getCoordinatorSectorId()).isEqualTo(testSector.getId());
            assertThat(result.getCoordinatorSectorTitle()).isEqualTo(testSector.getTitle());
            assertThat(result.getCoordinatorSector()).isEqualTo(testSector.getTitle());
            assertThat(result.getSocialStatuses()).containsExactly("Староста");
        }

        @Test
        void getUserBasicInfo_UserWithoutSocialStatuses_ReturnsEmptyList() {
            UserProfileInfoDTO result = userService.getUserBasicInfo(userWithoutSocialStatuses.getId());

            assertThat(result).isNotNull();
            assertThat(result.getSocialStatuses()).isNotNull();
            assertThat(result.getSocialStatuses()).isEmpty();
        }

        @Test
        void getUserBasicInfo_UserWithoutPhoto_ReturnsNullPhoto() {
            UserProfileInfoDTO result = userService.getUserBasicInfo(coordinatorUser.getId());

            assertThat(result.getPhoto()).isNull();
        }
    }

    // ==================== TESTS FOR getAllUsers ====================

    @Nested
    class GetAllUsersTests {

        @Test
        void getAllUsers_Success() {
            UserFilterDTO filter = UserFilterDTO.builder().build();
            Page<UserResponseDTO> result = userService.getAllUsers(0, 10, "id", "ASC", filter);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSizeGreaterThanOrEqualTo(5);
            assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(5);
        }

        @Test
        void getAllUsers_WithEmptyFilter_ReturnsAllUsers() {
            UserFilterDTO filter = UserFilterDTO.builder().build();
            Page<UserResponseDTO> result = userService.getAllUsers(0, 100, "id", "ASC", filter);

            long expectedCount = userRepository.count();
            assertThat(result.getTotalElements()).isEqualTo(expectedCount);
        }

        @Test
        void getAllUsers_WithRoleFilter() {
            UserFilterDTO filter = UserFilterDTO.builder()
                    .role("Activist")
                    .build();

            Page<UserResponseDTO> result = userService.getAllUsers(0, 10, "id", "ASC", filter);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isNotEmpty();
            assertThat(result.getContent()).allMatch(user -> "Activist".equals(user.getRole()));
        }

        @Test
        void getAllUsers_WithRoleFilter_NoResults() {
            UserFilterDTO filter = UserFilterDTO.builder()
                    .role("NonExistentRole")
                    .build();

            Page<UserResponseDTO> result = userService.getAllUsers(0, 10, "id", "ASC", filter);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        void getAllUsers_WithSearchFilter_ByName() {
            UserFilterDTO filter = UserFilterDTO.builder()
                    .search("Иван")
                    .build();

            Page<UserResponseDTO> result = userService.getAllUsers(0, 10, "id", "ASC", filter);

            assertThat(result.getContent()).isNotEmpty();
            assertThat(result.getContent().get(0).getName()).isEqualTo("Иван");
        }

        @Test
        void getAllUsers_WithSearchFilter_BySurname() {
            UserFilterDTO filter = UserFilterDTO.builder()
                    .search("Петров")
                    .build();

            Page<UserResponseDTO> result = userService.getAllUsers(0, 10, "id", "ASC", filter);

            assertThat(result.getContent()).isNotEmpty();
            assertThat(result.getContent().get(0).getSurname()).isEqualTo("Петров");
        }

        @Test
        void getAllUsers_WithSearchFilter_ByEmail() {
            UserFilterDTO filter = UserFilterDTO.builder()
                    .search("ivan@test.com")
                    .build();

            Page<UserResponseDTO> result = userService.getAllUsers(0, 10, "id", "ASC", filter);

            assertThat(result.getContent()).isNotEmpty();
            assertThat(result.getContent().get(0).getStudentEmail()).isEqualTo("ivan@test.com");
        }

        @Test
        void getAllUsers_WithSearchFilter_NoMatches() {
            UserFilterDTO filter = UserFilterDTO.builder()
                    .search("xyz123")
                    .build();

            Page<UserResponseDTO> result = userService.getAllUsers(0, 10, "id", "ASC", filter);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        void getAllUsers_WithSectorFilter() {
            UserFilterDTO filter = UserFilterDTO.builder()
                    .sectorId(testSector.getId())
                    .build();

            Page<UserResponseDTO> result = userService.getAllUsers(0, 10, "id", "ASC", filter);

            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        void getAllUsers_WithSectorFilter_NoResults() {
            UserFilterDTO filter = UserFilterDTO.builder()
                    .sectorId(999L)
                    .build();

            Page<UserResponseDTO> result = userService.getAllUsers(0, 10, "id", "ASC", filter);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        void getAllUsers_WithActiveFilter() {
            UserFilterDTO filter = UserFilterDTO.builder()
                    .isActive(true)
                    .build();

            Page<UserResponseDTO> result = userService.getAllUsers(0, 10, "id", "ASC", filter);

            assertThat(result.getContent()).allMatch(UserResponseDTO::getIsActive);
        }

        @Test
        void getAllUsers_WithInactiveFilter() {
            UserFilterDTO filter = UserFilterDTO.builder()
                    .isActive(false)
                    .build();

            Page<UserResponseDTO> result = userService.getAllUsers(0, 10, "id", "ASC", filter);

            assertThat(result.getContent()).allMatch(user -> !user.getIsActive());
        }

        @Test
        void getAllUsers_WithBannedFilter() {
            UserFilterDTO filter = UserFilterDTO.builder()
                    .isBanned(true)
                    .build();

            Page<UserResponseDTO> result = userService.getAllUsers(0, 10, "id", "ASC", filter);

            assertThat(result.getContent()).allMatch(user -> user.getIsBanned());
        }

        @Test
        void getAllUsers_WithNotBannedFilter() {
            UserFilterDTO filter = UserFilterDTO.builder()
                    .isBanned(false)
                    .build();

            Page<UserResponseDTO> result = userService.getAllUsers(0, 10, "id", "ASC", filter);

            assertThat(result.getContent()).allMatch(user -> !user.getIsBanned());
        }

        @Test
        void getAllUsers_WithGroupFilter() {
            UserFilterDTO filter = UserFilterDTO.builder()
                    .groupId(testGroup.getId())
                    .build();

            Page<UserResponseDTO> result = userService.getAllUsers(0, 10, "id", "ASC", filter);

            assertThat(result.getContent()).allMatch(user -> user.getGroupId().equals(testGroup.getId()));
        }

        @Test
        void getAllUsers_WithSpecialityFilter() {
            UserFilterDTO filter = UserFilterDTO.builder()
                    .specialityId(testSpeciality.getId())
                    .build();

            Page<UserResponseDTO> result = userService.getAllUsers(0, 10, "id", "ASC", filter);

            assertThat(result.getContent()).allMatch(user -> user.getSpecialityId().equals(testSpeciality.getId()));
        }

        @Test
        void getAllUsers_WithPagination() {
            UserFilterDTO filter = UserFilterDTO.builder().build();

            Page<UserResponseDTO> page1 = userService.getAllUsers(0, 2, "id", "ASC", filter);
            Page<UserResponseDTO> page2 = userService.getAllUsers(1, 2, "id", "ASC", filter);

            assertThat(page1.getContent()).hasSize(2);
            assertThat(page2.getContent()).hasSize(2);
            assertThat(page1.getContent().get(0).getId()).isNotEqualTo(page2.getContent().get(0).getId());
        }

        @Test
        void getAllUsers_WithSortingDescending() {
            UserFilterDTO filter = UserFilterDTO.builder().build();
            Page<UserResponseDTO> result = userService.getAllUsers(0, 10, "name", "DESC", filter);

            List<String> names = result.getContent().stream()
                    .map(UserResponseDTO::getName)
                    .toList();

            // DESC = обратный порядок ASC
            assertThat(names).containsExactly("Сергей", "Петр", "Иван", "Дмитрий", "Алексей");
        }

        @Test
        void getAllUsers_WithLargePageSize_ReturnsAll() {
            UserFilterDTO filter = UserFilterDTO.builder().build();
            Page<UserResponseDTO> result = userService.getAllUsers(0, 1000, "id", "ASC", filter);

            assertThat(result.getContent().size()).isEqualTo(userRepository.count());
        }

        @Test
        void getAllUsers_WithNegativePage_HandlesGracefully() {
            UserFilterDTO filter = UserFilterDTO.builder().build();
            Page<UserResponseDTO> result = userService.getAllUsers(-1, 10, "id", "ASC", filter);

            assertThat(result).isNotNull();
        }
    }

    // ==================== TESTS FOR getUsersByRole ====================

    @Nested
    class GetUsersByRoleTests {

        @Test
        void getUsersByRole_Success() {
            Page<UserResponseDTO> result = userService.getUsersByRole("Activist", 0, 10, "id", "ASC");

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSizeGreaterThanOrEqualTo(5);
            assertThat(result.getContent()).allMatch(user -> "Activist".equals(user.getRole()));
        }

        @Test
        void getUsersByRole_WithPagination() {
            Page<UserResponseDTO> page1 = userService.getUsersByRole("Activist", 0, 2, "id", "ASC");
            Page<UserResponseDTO> page2 = userService.getUsersByRole("Activist", 1, 2, "id", "ASC");

            assertThat(page1.getContent()).hasSize(2);
            assertThat(page2.getContent()).hasSize(2);
        }

        @Test
        void getUsersByRole_WithSorting() {
            Page<UserResponseDTO> resultAsc = userService.getUsersByRole("Activist", 0, 10, "name", "ASC");
            Page<UserResponseDTO> resultDesc = userService.getUsersByRole("Activist", 0, 10, "name", "DESC");

            String firstNameAsc = resultAsc.getContent().get(0).getName();
            String firstNameDesc = resultDesc.getContent().get(0).getName();

            assertThat(firstNameAsc).isNotEqualTo(firstNameDesc);
        }

        @Test
        void getUsersByRole_RoleNotFound_ReturnsEmptyPage() {
            Page<UserResponseDTO> result = userService.getUsersByRole("NonExistentRole", 0, 10, "id", "ASC");

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        void getUsersByRole_WithNullRole_ReturnsEmptyPage() {
            Page<UserResponseDTO> result = userService.getUsersByRole(null, 0, 10, "id", "ASC");

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        void getUsersByRole_WithEmptyStringRole_ReturnsEmptyPage() {
            Page<UserResponseDTO> result = userService.getUsersByRole("", 0, 10, "id", "ASC");

            assertThat(result.getContent()).isEmpty();
        }
    }

    // ==================== TESTS FOR COMPLEX SCENARIOS ====================

    @Nested
    class ComplexScenariosTests {

        @Test
        void getAllUsers_WithMultipleFilters() {
            UserFilterDTO filter = UserFilterDTO.builder()
                    .role("Activist")
                    .search("Иван")
                    .isActive(true)
                    .isBanned(false)
                    .groupId(testGroup.getId())
                    .specialityId(testSpeciality.getId())
                    .sectorId(testSector.getId())
                    .build();

            Page<UserResponseDTO> result = userService.getAllUsers(0, 10, "id", "ASC", filter);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Иван");
        }

        @Test
        void getAllUsers_WithAllFilters_NoResults() {
            UserFilterDTO filter = UserFilterDTO.builder()
                    .role("Admin")
                    .search("Nonexistent")
                    .isActive(false)
                    .isBanned(true)
                    .groupId(999L)
                    .specialityId(999L)
                    .sectorId(999L)
                    .build();

            Page<UserResponseDTO> result = userService.getAllUsers(0, 10, "id", "ASC", filter);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        void getUserBasicInfo_IncludesSocialStatuses() {
            UserProfileInfoDTO result = userService.getUserBasicInfo(testUser.getId());

            assertThat(result.getSocialStatuses()).containsExactlyInAnyOrder("Студент", "Активист");
        }

        @Test
        void getAllUsers_IncludesSocialStatuses() {
            UserFilterDTO filter = UserFilterDTO.builder().build();
            Page<UserResponseDTO> result = userService.getAllUsers(0, 10, "id", "ASC", filter);

            UserResponseDTO user = result.getContent().stream()
                    .filter(u -> u.getId().equals(testUser.getId()))
                    .findFirst()
                    .orElse(null);

            assertThat(user).isNotNull();
            assertThat(user.getSocialStatuses()).containsExactlyInAnyOrder("Студент", "Активист");
        }

        @Test
        void getUsersByRole_IncludesCoordinatorInfo() {
            Page<UserResponseDTO> result = userService.getUsersByRole("Activist", 0, 10, "id", "ASC");

            UserResponseDTO coordinator = result.getContent().stream()
                    .filter(u -> u.getId().equals(coordinatorUser.getId()))
                    .findFirst()
                    .orElse(null);

            assertThat(coordinator).isNotNull();
            assertThat(coordinator.getCoordinatorSectorId()).isEqualTo(testSector.getId());
            assertThat(coordinator.getCoordinatorSectorTitle()).isEqualTo(testSector.getTitle());
        }

        @Test
        void getUserBasicInfo_ForAdminRole_WorksCorrectly() {
            User admin = userRepository.save(User.builder()
                    .name("Admin")
                    .surname("Adminov")
                    .gender(Gender.Мужчина)
                    .dateOfBirth(LocalDate.of(1990, 1, 1))
                    .studentEmail("admin@test.com")
                    .phoneNumber("+79991234599")
                    .password("encodedPassword")
                    .studentIdNumber(99999)
                    .courseNumber((short) 5)
                    .role(adminRole)
                    .group(testGroup)
                    .speciality(testSpeciality)
                    .isActive(true)
                    .isBanned(false)
                    .build());

            UserProfileInfoDTO result = userService.getUserBasicInfo(admin.getId());

            assertThat(result.getRoleTitle()).isEqualTo("Admin");
        }
    }
}