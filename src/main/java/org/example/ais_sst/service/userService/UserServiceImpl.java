package org.example.ais_sst.service.userService;

import org.example.ais_sst.dto.user.UserProfileInfoDTO;

public interface UserServiceImpl {
    public UserProfileInfoDTO getUserBasicInfo(Long userId);
}
