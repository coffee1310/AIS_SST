package org.example.ais_sst.services.UsersService;

import org.example.ais_sst.dto.user.UserFilterDTO;
import org.example.ais_sst.dto.user.UserProfileInfoDTO;
import org.example.ais_sst.dto.user.UserResponseDTO;
import org.example.ais_sst.entity.*;
import org.example.ais_sst.entity.enums.Gender;
import org.example.ais_sst.exception.UserDoesNotExistException;
import org.example.ais_sst.mapper.UserMapper;
import org.example.ais_sst.repository.SectorParticipantRepository;
import org.example.ais_sst.repository.SocialStatusStudentsRepository;
import org.example.ais_sst.repository.UserRepository;
import org.example.ais_sst.service.userService.UserService;
import org.example.ais_sst.utils.ImageUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private SocialStatusStudentsRepository socialStatusStudentRepository;

    @Mock
    private SectorParticipantRepository sectorParticipantRepository;

    @InjectMocks
    private UserService userService;

    private User user;
    private Role role;
    private Group group;
    private Speciality speciality;
    private Sector sector;
    private SectorParticipant sectorParticipant;
    private UserFilterDTO filterDTO;

    @BeforeEach
    void setUp() {
        speciality = Speciality.builder()
                .id(1L)
                .title("Информационные системы и программирование")
                .shortTitle("ИСП")
                .build();

        group = Group.builder()
                .id(1L)
                .title("ПИ-101")
                .build();

        role = Role.builder()
                .id(1L)
                .title("Activist")
                .build();

        sector = Sector.builder()
                .id(1L)
                .title("Спортивный сектор")
                .build();

        user = User.builder()
                .id(1L)
                .name("Иван")
                .surname("Иванов")
                .patronymic("Иванович")
                .gender(Gender.Мужчина)
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .studentEmail("ivan@test.com")
                .phoneNumber("+79991234567")
                .studentIdNumber(12345)
                .courseNumber((short) 3)
                .role(role)
                .group(group)
                .speciality(speciality)
                .isActive(true)
                .isBanned(false)
                .vkLink("vk.com/ivan")
                .additionalEmail("ivan.extra@test.com")
                .photo(new byte[]{1, 2, 3})
                .build();

        sectorParticipant = SectorParticipant.builder()
                .id(1L)
                .sector(sector)
                .student(user)
                .isCoordinator(true)
                .build();

        filterDTO = UserFilterDTO.builder()
                .id(1L)
                .role("Activist")
                .search("Иван")
                .isActive(true)
                .isBanned(false)
                .groupId(1L)
                .specialityId(1L)
                .sectorId(1L)
                .build();
    }

    // ==================== TESTS FOR getUserBasicInfo ====================

    @Test
    void getUserBasicInfo_Success() {
        // given
        when(userRepository.findUserById(1L)).thenReturn(Optional.of(user));
        when(socialStatusStudentRepository.findSocialStatusTitlesByStudentId(1L))
                .thenReturn(List.of("Студент", "Активист"));

        // ИСПРАВЛЕНО: создаем список Object[] правильно
        List<Object[]> coordinatorInfo = new ArrayList<>();
        coordinatorInfo.add(new Object[]{1L, "Спортивный сектор"});
        when(sectorParticipantRepository.findCoordinatorSectorInfoByUserId(1L))
                .thenReturn(coordinatorInfo);

        // when
        UserProfileInfoDTO result = userService.getUserBasicInfo(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Иван");
        assertThat(result.getSurname()).isEqualTo("Иванов");
        assertThat(result.getStudentEmail()).isEqualTo("ivan@test.com");
        assertThat(result.getRoleTitle()).isEqualTo("Activist");
        assertThat(result.getShortSpecialityTitle()).isEqualTo("ИСП");
        assertThat(result.getCoordinatorSectorId()).isEqualTo(1L);
        assertThat(result.getCoordinatorSectorTitle()).isEqualTo("Спортивный сектор");
        assertThat(result.getSocialStatuses()).contains("Студент", "Активист");

        verify(userRepository).findUserById(1L);
        verify(socialStatusStudentRepository).findSocialStatusTitlesByStudentId(1L);
        verify(sectorParticipantRepository).findCoordinatorSectorInfoByUserId(1L);
    }

    @Test
    void getUserBasicInfo_UserNotFound_ThrowsException() {
        // given
        when(userRepository.findUserById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getUserBasicInfo(1L))
                .isInstanceOf(UserDoesNotExistException.class)
                .hasMessageContaining("Пользователь не найден");
    }

    @Test
    void getUserBasicInfo_UserWithoutPhoto_ReturnsNullPhoto() {
        // given
        User userWithoutPhoto = User.builder()
                .id(2L)
                .name("Петр")
                .surname("Петров")
                .role(role)
                .speciality(speciality)
                .photo(null)
                .build();

        when(userRepository.findUserById(2L)).thenReturn(Optional.of(userWithoutPhoto));
        when(socialStatusStudentRepository.findSocialStatusTitlesByStudentId(2L))
                .thenReturn(List.of());
        when(sectorParticipantRepository.findCoordinatorSectorInfoByUserId(2L))
                .thenReturn(new ArrayList<>());

        // when
        UserProfileInfoDTO result = userService.getUserBasicInfo(2L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPhoto()).isNull();
    }

    @Test
    void getUserBasicInfo_UserWithoutCoordinatorSector_ReturnsNullCoordinatorFields() {
        // given
        when(userRepository.findUserById(1L)).thenReturn(Optional.of(user));
        when(socialStatusStudentRepository.findSocialStatusTitlesByStudentId(1L))
                .thenReturn(List.of());
        when(sectorParticipantRepository.findCoordinatorSectorInfoByUserId(1L))
                .thenReturn(new ArrayList<>());

        // when
        UserProfileInfoDTO result = userService.getUserBasicInfo(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getCoordinatorSectorId()).isNull();
        assertThat(result.getCoordinatorSectorTitle()).isNull();
    }

    // ==================== TESTS FOR getAllUsers ====================

    @Test
    void getAllUsers_Success() {
        // given
        Object[] row = createUserRow();
        List<Object[]> results = new ArrayList<>();
        results.add(row);
        long total = 1L;

        when(userRepository.findAllWithFiltersNative(any(), any(), any(), any(), any(),
                any(), any(), any(), anyInt(), anyInt())).thenReturn(results);
        when(userRepository.countAllWithFiltersNative(any(), any(), any(), any(), any(),
                any(), any(), any())).thenReturn(total);
        when(sectorParticipantRepository.findCoordinatorByUserId(1L))
                .thenReturn(Optional.of(sectorParticipant));

        // when
        Page<UserResponseDTO> result = userService.getAllUsers(0, 10, "id", "ASC", filterDTO);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);

        verify(userRepository).findAllWithFiltersNative(any(), any(), any(), any(), any(),
                any(), any(), any(), eq(0), eq(10));
        verify(userRepository).countAllWithFiltersNative(any(), any(), any(), any(), any(),
                any(), any(), any());
    }

    @Test
    void getAllUsers_EmptyResult_ReturnsEmptyPage() {
        // given
        when(userRepository.findAllWithFiltersNative(any(), any(), any(), any(), any(),
                any(), any(), any(), anyInt(), anyInt())).thenReturn(new ArrayList<>());
        when(userRepository.countAllWithFiltersNative(any(), any(), any(), any(), any(),
                any(), any(), any())).thenReturn(0L);

        // when
        Page<UserResponseDTO> result = userService.getAllUsers(0, 10, "id", "ASC", filterDTO);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    // ==================== TESTS FOR getUsersByRole ====================

    @Test
    void getUsersByRole_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        Page<User> userPage = new PageImpl<>(List.of(user));
        UserResponseDTO responseDTO = UserResponseDTO.builder().id(1L).build();

        when(userRepository.findByRole("Activist", pageable)).thenReturn(userPage);
        when(userMapper.toResponseDto(any(User.class))).thenReturn(responseDTO);

        // ИСПРАВЛЕНО: создаем список Object[] правильно
        List<Object[]> coordinatorInfo = new ArrayList<>();
        coordinatorInfo.add(new Object[]{1L, "Сектор"});
        when(sectorParticipantRepository.findCoordinatorSectorInfoByUserId(1L))
                .thenReturn(coordinatorInfo);

        // when
        Page<UserResponseDTO> result = userService.getUsersByRole("Activist", 0, 10, "id", "ASC");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(userRepository).findByRole("Activist", pageable);
        verify(userMapper).toResponseDto(user);
    }

    @Test
    void getUsersByRole_EmptyResult_ReturnsEmptyPage() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        Page<User> emptyPage = new PageImpl<>(List.of());

        when(userRepository.findByRole("Activist", pageable)).thenReturn(emptyPage);

        // when
        Page<UserResponseDTO> result = userService.getUsersByRole("Activist", 0, 10, "id", "ASC");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }

    // ==================== HELPER METHODS ====================

    private Object[] createUserRow() {
        return new Object[]{
                1L,                                                       // id
                "Иван",                                                   // name
                "Иванов",                                                 // surname
                "Иванович",                                               // patronymic
                "Мужчина",                                                // gender
                Date.valueOf("2000-01-01"),                               // date_of_birth
                3,                                                        // course_number
                12345,                                                    // student_id_number
                "ivan@test.com",                                          // student_email
                "ivan.extra@test.com",                                    // additional_email
                "+79991234567",                                           // phone_number
                "vk.com/ivan",                                            // vk_link
                true,                                                     // is_active
                false,                                                    // is_banned
                "Activist",                                               // role
                1L,                                                       // group_id
                "ПИ-101",                                                 // group_name
                1L,                                                       // speciality_id
                "Информационные системы и программирование",              // speciality_name
                new byte[]{1, 2, 3},                                      // photo
                "ИСП"                                                     // short_title
        };
    }
}