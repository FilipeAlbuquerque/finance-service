package com.example.financeservice.controller;


import com.example.financeservice.dto.auth.UserProfileUpdateDTO;
import com.example.financeservice.security.service.SecurityService;
import com.example.financeservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

  private final UserService userService;
  private final SecurityService securityService;

  @PutMapping("/profile")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<?> updateUserProfile(@Valid @RequestBody UserProfileUpdateDTO profileDTO) {
    try {
      Long userId = securityService.getCurrentUserId();
      userService.updateUser(
          userId,
          profileDTO.getFirstName(),
          profileDTO.getLastName(),
          profileDTO.getEmail()
      );

      return ResponseEntity.ok("Profile updated successfully");
    } catch (Exception e) {
      log.error("Error updating user profile: {}", e.getMessage(), e);
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }
}