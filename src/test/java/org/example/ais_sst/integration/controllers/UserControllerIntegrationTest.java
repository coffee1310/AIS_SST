package org.example.ais_sst.integration.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ais_sst.dto.user.UserProfileInfoDTO;
import org.example.ais_sst.dto.user.UserResponseDTO;
import org.example.ais_sst.entity.*;
import org.example.ais_sst.entity.enums.Gender;
import org.example.ais_sst.entity.enums.SectorParticipantStatuses;
import org.example.ais_sst.integration.BaseIntegrationTest;
import org.example.ais_sst.repository.*;
import org.example.ais_sst.security.jwt.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class UserControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;  // ← Добавьте эту зависимость

    @Autowired
    private ObjectMapper objectMapper;

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

    @Autowired
    private JwtUtils jwtUtils;

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
    private String testUserToken;
    private String coordinatorToken;

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
                .description("Описание")
                .isActive(true)
                .build());

        anotherSector = sectorRepository.save(Sector.builder()
                .title("Культурный сектор")
                .description("Описание культурного сектора")
                .isActive(true)
                .build());

        // Создаем социальные статусы
        SocialStatus socialStatus1 = socialStatusRepository.save(SocialStatus.builder().title("Студент").build());
        SocialStatus socialStatus2 = socialStatusRepository.save(SocialStatus.builder().title("Активист").build());

        // Активный пользователь
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
                .pathToPhoto("/photos/ivan.jpg")
                .vkLink("https://vk.com/ivan")
                .additionalEmail("ivan.extra@test.com")
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

        // Генерируем токены
        CustomUserDetails testUserDetails = CustomUserDetails.fromUser(testUser);
        UsernamePasswordAuthenticationToken testAuth =
                new UsernamePasswordAuthenticationToken(testUserDetails, null, testUserDetails.getAuthorities());
        testUserToken = jwtUtils.generateJwtToken(testAuth);

        CustomUserDetails coordinatorDetails = CustomUserDetails.fromUser(coordinatorUser);
        UsernamePasswordAuthenticationToken coordinatorAuth =
                new UsernamePasswordAuthenticationToken(coordinatorDetails, null, coordinatorDetails.getAuthorities());
        coordinatorToken = jwtUtils.generateJwtToken(coordinatorAuth);

        SecurityContextHolder.getContext().setAuthentication(testAuth);
    }

    private <T> Page<T> parsePage(MvcResult result, Class<T> contentClass) throws Exception {
        String responseBody = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(responseBody);

        List<T> content = new ArrayList<>();
        for (JsonNode node : root.get("content")) {
            content.add(objectMapper.treeToValue(node, contentClass));
        }

        int number = root.get("number").asInt();
        int size = root.get("size").asInt();
        long totalElements = root.get("totalElements").asLong();

        return new PageImpl<>(content, PageRequest.of(number, size), totalElements);
    }

    // ==================== TESTS FOR /api/users/me ====================

    @Nested
    class GetCurrentUserInfoTests {

        @Test
        void getCurrentUserInfo_Success() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/users/me")
                            .header("Authorization", "Bearer " + testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            UserProfileInfoDTO userInfo = objectMapper.readValue(responseBody, UserProfileInfoDTO.class);

            assertThat(userInfo).isNotNull();
            assertThat(userInfo.getId()).isEqualTo(testUser.getId());
            assertThat(userInfo.getName()).isEqualTo("Иван");
            assertThat(userInfo.getSurname()).isEqualTo("Иванов");
            assertThat(userInfo.getStudentEmail()).isEqualTo("ivan@test.com");
            assertThat(userInfo.getSocialStatuses()).containsExactlyInAnyOrder("Студент", "Активист");
            assertThat(userInfo.getAdditionalEmail()).isEqualTo("ivan.extra@test.com");
            assertThat(userInfo.getVkLink()).isEqualTo("https://vk.com/ivan");
        }

        @Test
        void getCurrentUserInfo_WithoutAuthentication_Returns403() throws Exception {
            SecurityContextHolder.clearContext();

            mockMvc.perform(get("/api/users/me"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getCurrentUserInfo_WithInvalidToken_Returns403() throws Exception {
            mockMvc.perform(get("/api/users/me")
                            .header("Authorization", "Bearer invalid_token"))
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== TESTS FOR /api/users/all ====================

    @Nested
    class GetAllUsersTests {

        @Test
        void getAllUsers_DefaultParameters_Success() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/users/all"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            Page<UserResponseDTO> page = parsePage(result, UserResponseDTO.class);

            assertThat(page.getContent()).isNotEmpty();
            assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(5);
        }

        @Test
        void getAllUsers_WithPagination_Success() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/users/all")
                            .param("page", "0")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andReturn();

            Page<UserResponseDTO> page = parsePage(result, UserResponseDTO.class);

            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getNumber()).isZero();
            assertThat(page.getSize()).isEqualTo(2);
        }

        @Test
        void getAllUsers_WithNegativePage_UsesDefault() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/users/all")
                            .param("page", "-1"))
                    .andExpect(status().isOk())
                    .andReturn();

            Page<UserResponseDTO> page = parsePage(result, UserResponseDTO.class);

            assertThat(page.getNumber()).isZero();
        }

        @Test
        void getAllUsers_WithTooLargeSize_LimitsTo100() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/users/all")
                            .param("size", "1000"))
                    .andExpect(status().isOk())
                    .andReturn();

            Page<UserResponseDTO> page = parsePage(result, UserResponseDTO.class);

            assertThat(page.getSize()).isLessThanOrEqualTo(100);
        }

        @Test
        void getAllUsers_WithRoleFilter_Success() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/users/all")
                            .param("role", "Activist"))
                    .andExpect(status().isOk())
                    .andReturn();

            Page<UserResponseDTO> page = parsePage(result, UserResponseDTO.class);

            assertThat(page.getContent()).isNotEmpty();
            assertThat(page.getContent()).allMatch(user -> "Activist".equals(user.getRole()));
        }

        @Test
        void getAllUsers_WithRoleFilter_NoResults() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/users/all")
                            .param("role", "NonExistentRole"))
                    .andExpect(status().isOk())
                    .andReturn();

            Page<UserResponseDTO> page = parsePage(result, UserResponseDTO.class);

            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isZero();
        }

        @Test
        void getAllUsers_WithSearchFilter_ByName() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/users/all")
                            .param("search", "Иван"))
                    .andExpect(status().isOk())
                    .andReturn();

            Page<UserResponseDTO> page = parsePage(result, UserResponseDTO.class);

            assertThat(page.getContent()).isNotEmpty();
            assertThat(page.getContent().get(0).getName()).isEqualTo("Иван");
        }

        @Test
        void getAllUsers_WithSearchFilter_BySurname() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/users/all")
                            .param("search", "Петров"))
                    .andExpect(status().isOk())
                    .andReturn();

            Page<UserResponseDTO> page = parsePage(result, UserResponseDTO.class);

            assertThat(page.getContent()).isNotEmpty();
            assertThat(page.getContent().get(0).getSurname()).isEqualTo("Петров");
        }

        @Test
        void getAllUsers_WithSearchFilter_ByEmail() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/users/all")
                            .param("search", "ivan@test.com"))
                    .andExpect(status().isOk())
                    .andReturn();

            Page<UserResponseDTO> page = parsePage(result, UserResponseDTO.class);

            assertThat(page.getContent()).isNotEmpty();
            assertThat(page.getContent().get(0).getStudentEmail()).isEqualTo("ivan@test.com");
        }

        @Test
        void getAllUsers_WithSearchFilter_NoResults() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/users/all")
                            .param("search", "xyz123"))
                    .andExpect(status().isOk())
                    .andReturn();

            Page<UserResponseDTO> page = parsePage(result, UserResponseDTO.class);

            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isZero();
        }

        @Test
        void getAllUsers_WithActiveFilter() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/users/all")
                            .param("isActive", "true"))
                    .andExpect(status().isOk())
                    .andReturn();

            Page<UserResponseDTO> page = parsePage(result, UserResponseDTO.class);

            assertThat(page.getContent()).allMatch(UserResponseDTO::getIsActive);
        }

        @Test
        void getAllUsers_WithInactiveFilter() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/users/all")
                            .param("isActive", "false"))
                    .andExpect(status().isOk())
                    .andReturn();

            Page<UserResponseDTO> page = parsePage(result, UserResponseDTO.class);

            assertThat(page.getContent()).allMatch(user -> !user.getIsActive());
        }

        @Test
        void getAllUsers_WithBannedFilter() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/users/all")
                            .param("isBanned", "true"))
                    .andExpect(status().isOk())
                    .andReturn();

            Page<UserResponseDTO> page = parsePage(result, UserResponseDTO.class);

            assertThat(page.getContent()).allMatch(UserResponseDTO::getIsBanned);
        }

        @Test
        void getAllUsers_WithNotBannedFilter() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/users/all")
                            .param("isBanned", "false"))
                    .andExpect(status().isOk())
                    .andReturn();

            Page<UserResponseDTO> page = parsePage(result, UserResponseDTO.class);

            assertThat(page.getContent()).allMatch(user -> !user.getIsBanned());
        }

        @Test
        void getAllUsers_WithGroupFilter() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/users/all")
                            .param("groupId", testGroup.getId().toString()))
                    .andExpect(status().isOk())
                    .andReturn();

            Page<UserResponseDTO> page = parsePage(result, UserResponseDTO.class);

            assertThat(page.getContent()).allMatch(user -> user.getGroupId().equals(testGroup.getId()));
        }

        @Test
        void getAllUsers_WithSpecialityFilter() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/users/all")
                            .param("specialityId", testSpeciality.getId().toString()))
                    .andExpect(status().isOk())
                    .andReturn();

            Page<UserResponseDTO> page = parsePage(result, UserResponseDTO.class);

            assertThat(page.getContent()).allMatch(user -> user.getSpecialityId().equals(testSpeciality.getId()));
        }

        @Test
        void getAllUsers_WithSectorFilter() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/users/all")
                            .param("sectorId", testSector.getId().toString()))
                    .andExpect(status().isOk())
                    .andReturn();

            Page<UserResponseDTO> page = parsePage(result, UserResponseDTO.class);

            assertThat(page.getContent()).hasSize(2);
        }

        @Test
        void getAllUsers_WithSectorFilter_NoResults() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/users/all")
                            .param("sectorId", "99999"))
                    .andExpect(status().isOk())
                    .andReturn();

            Page<UserResponseDTO> page = parsePage(result, UserResponseDTO.class);

            assertThat(page.getContent()).isEmpty();
        }

        @Test
        void getAllUsers_WithMultipleFilters() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/users/all")
                            .param("role", "Activist")
                            .param("search", "Иван")
                            .param("isActive", "true")
                            .param("isBanned", "false")
                            .param("groupId", testGroup.getId().toString())
                            .param("specialityId", testSpeciality.getId().toString())
                            .param("sectorId", testSector.getId().toString()))
                    .andExpect(status().isOk())
                    .andReturn();

            Page<UserResponseDTO> page = parsePage(result, UserResponseDTO.class);

            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getName()).isEqualTo("Иван");
        }

        @Test
        void getAllUsers_WithAllFilters_NoResults() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/users/all")
                            .param("role", "Admin")
                            .param("search", "Nonexistent")
                            .param("isActive", "false")
                            .param("isBanned", "true")
                            .param("groupId", "99999")
                            .param("specialityId", "99999")
                            .param("sectorId", "99999"))
                    .andExpect(status().isOk())
                    .andReturn();

            Page<UserResponseDTO> page = parsePage(result, UserResponseDTO.class);

            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isZero();
        }

        @Test
        void getAllUsers_WithSQLInjectionAttempt_ShouldBeSafe() throws Exception {
            String maliciousSearch = "' OR '1'='1'; DROP TABLE users; --";

            mockMvc.perform(get("/api/users/all")
                            .param("search", maliciousSearch))
                    .andExpect(status().isOk());
        }

        @Test
        void getAllUsers_WithXSSAttempt_ShouldBeSafe() throws Exception {
            String xssAttempt = "<script>alert('XSS')</script>";

            mockMvc.perform(get("/api/users/all")
                            .param("search", xssAttempt))
                    .andExpect(status().isOk());
        }

        @Test
        void getAllUsers_WithInvalidParameterTypes_ReturnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/users/all")
                            .param("page", "invalid"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void getAllUsers_WithNullParameters_ReturnsSuccess() throws Exception {
            mockMvc.perform(get("/api/users/all")
                            .param("role", (String) null)
                            .param("search", (String) null))
                    .andExpect(status().isOk());
        }

        @Test
        void getAllUsers_WithEmptyStringParameters_ReturnsSuccess() throws Exception {
            mockMvc.perform(get("/api/users/all")
                            .param("role", "")
                            .param("search", ""))
                    .andExpect(status().isOk());
        }
    }

    // ==================== TESTS FOR /api/users/role/{role} ====================

    @Nested
    class GetUsersByRoleTests {

        @Test
        void getUsersByRole_Success() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/users/role/Activist"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            Page<UserResponseDTO> page = parsePage(result, UserResponseDTO.class);

            assertThat(page.getContent()).isNotEmpty();
            assertThat(page.getContent()).allMatch(user -> "Activist".equals(user.getRole()));
        }

        @Test
        void getUsersByRole_WithPagination() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/users/role/Activist")
                            .param("page", "0")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andReturn();

            Page<UserResponseDTO> page = parsePage(result, UserResponseDTO.class);

            assertThat(page.getContent()).hasSize(2);
        }

        @Test
        void getUsersByRole_WithSortingAscending() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/users/role/Activist")
                            .param("sortBy", "name")
                            .param("sortDirection", "ASC"))
                    .andExpect(status().isOk())
                    .andReturn();

            Page<UserResponseDTO> page = parsePage(result, UserResponseDTO.class);

            assertThat(page.getContent()).isNotEmpty();
            List<String> names = page.getContent().stream()
                    .map(UserResponseDTO::getName)
                    .toList();
            assertThat(names).isSorted();
        }

        @Test
        void getUsersByRole_WithSortingDescending() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/users/role/Activist")
                            .param("sortBy", "name")
                            .param("sortDirection", "DESC"))
                    .andExpect(status().isOk())
                    .andReturn();

            Page<UserResponseDTO> page = parsePage(result, UserResponseDTO.class);

            assertThat(page.getContent()).isNotEmpty();
        }

        @Test
        void getUsersByRole_RoleNotFound_ReturnsEmptyPage() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/users/role/NonExistentRole"))
                    .andExpect(status().isOk())
                    .andReturn();

            Page<UserResponseDTO> page = parsePage(result, UserResponseDTO.class);

            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isZero();
        }

        @Test
        void getUsersByRole_WithEmptyRole_ReturnsNotFound() throws Exception {
            mockMvc.perform(get("/api/users/role/"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void getUsersByRole_WithSpecialCharactersInRole_ReturnsEmptyPage() throws Exception {
            mockMvc.perform(get("/api/users/role/Admin%27%20OR%20%271%27%3D%271"))
                    .andExpect(status().isOk());
        }
    }

    // ==================== RESPONSE STRUCTURE TESTS ====================

    @Nested
    class ResponseStructureTests {

        @Test
        void getAllUsers_ResponseContainsCorrectFields() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/users/all"))
                    .andExpect(status().isOk())
                    .andReturn();

            Page<UserResponseDTO> page = parsePage(result, UserResponseDTO.class);
            UserResponseDTO user = page.getContent().get(0);

            assertThat(user.getId()).isNotNull();
            assertThat(user.getName()).isNotNull();
            assertThat(user.getSurname()).isNotNull();
            assertThat(user.getRole()).isNotNull();
            assertThat(user.getSocialStatuses()).isNotNull();
        }

        @Test
        void getUserBasicInfo_ResponseContainsAllFields() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/users/me")
                            .header("Authorization", "Bearer " + testUserToken))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            UserProfileInfoDTO userInfo = objectMapper.readValue(responseBody, UserProfileInfoDTO.class);

            assertThat(userInfo.getId()).isNotNull();
            assertThat(userInfo.getName()).isNotNull();
            assertThat(userInfo.getSurname()).isNotNull();
            assertThat(userInfo.getStudentEmail()).isNotNull();
            assertThat(userInfo.getRoleTitle()).isNotNull();
            assertThat(userInfo.getSocialStatuses()).isNotNull();
        }
    }

    // ==================== SECURITY TESTS ====================

    @Nested
    class SecurityTests {

        @Test
        void getAllUsers_WithoutAuthentication_Returns403() throws Exception {
            SecurityContextHolder.clearContext();

            mockMvc.perform(get("/api/users/all"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getUsersByRole_WithoutAuthentication_Returns403() throws Exception {
            SecurityContextHolder.clearContext();

            mockMvc.perform(get("/api/users/role/Activist"))
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== PERFORMANCE TESTS ====================

    @Nested
    class PerformanceTests {

        @Test
        void getAllUsers_WithLargePage_RespondsWithinTime() throws Exception {
            long startTime = System.currentTimeMillis();

            mockMvc.perform(get("/api/users/all")
                            .param("size", "100"))
                    .andExpect(status().isOk());

            long duration = System.currentTimeMillis() - startTime;
            assertThat(duration).isLessThan(5000);
        }

        @Test
        void getAllUsers_WithManyFilters_RespondsWithinTime() throws Exception {
            long startTime = System.currentTimeMillis();

            mockMvc.perform(get("/api/users/all")
                            .param("role", "Activist")
                            .param("search", "Иван")
                            .param("isActive", "true")
                            .param("isBanned", "false")
                            .param("groupId", testGroup.getId().toString())
                            .param("specialityId", testSpeciality.getId().toString())
                            .param("sectorId", testSector.getId().toString()))
                    .andExpect(status().isOk());

            long duration = System.currentTimeMillis() - startTime;
            assertThat(duration).isLessThan(3000);
        }
    }
}