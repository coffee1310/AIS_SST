package org.example.ais_sst.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.CustomUserDetails;
import org.example.ais_sst.dto.user.UserProfileInfoDTO;
import org.example.ais_sst.service.userService.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Сделать подсчет кол-ва баллов!
    @GetMapping()
    public ResponseEntity<?> getCurrentUserInfo() throws JsonProcessingException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>("", HttpStatusCode.valueOf(403));
        }

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        Long id = user.getId();
        UserProfileInfoDTO userProfileInfoDTO = userService.getUserBasicInfo(id);

        return ResponseEntity.ok(userProfileInfoDTO);
    }
}
