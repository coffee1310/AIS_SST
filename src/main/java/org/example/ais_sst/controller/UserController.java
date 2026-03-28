package org.example.ais_sst.controller;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.entity.CustomUserDetails;
import org.example.ais_sst.dto.user.UserProfileInfoDTO;
import org.example.ais_sst.service.userService.UserService;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Сделать подсчет кол-ва баллов!
    @Transactional
    @GetMapping()
    public ResponseEntity<?> getCurrentUserInfo() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return new ResponseEntity<>("", HttpStatusCode.valueOf(403));
            }

            CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
            Long id = user.getId();
            UserProfileInfoDTO userProfileInfoDTO = userService.getUserBasicInfo(id);

            return ResponseEntity.ok(userProfileInfoDTO);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

//    @Transactional
//    @PostMapping
//    public ResponseEntity<?> createAccountCreatingRequests(@Valid ) {
//
//    }
}
