package org.example.ais_sst.service.userService;

import org.example.ais_sst.exception.UserDoesNotExistException;
import lombok.RequiredArgsConstructor;
import org.example.ais_sst.dto.user.UserProfileInfoDTO;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService implements UserServiceImpl {

    private final UserRepository userRepository;

    @Override
    public UserProfileInfoDTO getUserBasicInfo(Long userId) {
        UserProfileInfoDTO userProfileInfoDTO;
        Optional<User> user_optional = userRepository.findUserById(userId);

        if (!user_optional.isPresent()) throw new UserDoesNotExistException("Пользователь не найден");

        User user = user_optional.get();
        userProfileInfoDTO = UserProfileInfoDTO.builder()
                .id(userId)
                .name(user.getName())
                .surname(user.getSurname())
                .patronymic(user.getPatronymic())
                .dateOfBirth(user.getDateOfBirth())
                .rank(0)
                .specialityTitle(user.getSpeciality().getTitle())
                .groupTitle(user.getGroup().getTitle())
                .courseNumber(user.getCourseNumber())
                .additionalEmail(user.getAdditionalEmail())
                .studentEmail(user.getStudentEmail())
                .roleTitle(user.getRole().getTitle())
                .vkLink(user.getVkLink())
                .courseNumber(user.getCourseNumber())
                .phoneNumber(user.getPhoneNumber())
                .build();

        return userProfileInfoDTO;
    }

}
