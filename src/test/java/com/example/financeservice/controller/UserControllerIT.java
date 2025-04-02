package com.example.financeservice.controller;

import com.example.financeservice.dto.auth.UserProfileUpdateDTO;
import com.example.financeservice.exception.ResourceNotFoundException;
import com.example.financeservice.repository.UserRepository;
import com.example.financeservice.security.JwtUtils;
import com.example.financeservice.security.SecurityService;
import com.example.financeservice.service.EmailService;
import com.example.financeservice.service.MetricsService;
import com.example.financeservice.service.UserService;
import com.example.financeservice.TestSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class UserControllerIT {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private SecurityService securityService;

  @MockBean
  private UserService userService;

  // Mocking beans relacionados ao UserService para evitar conflitos
  @MockBean
  private UserRepository userRepository;

  @MockBean
  private MetricsService metricsService;

  @MockBean
  private EmailService emailService;

  // Mocks necessários para segurança
  @MockBean
  private JwtUtils jwtUtils;

  @MockBean
  private AuthenticationManager authenticationManager;

  @BeforeEach
  void setUp() {
    // No need for additional setup
  }

  @Test
  @WithMockUser(username = "testuser", roles = {"USER"})
  void updateUserProfile_WithValidData_ShouldUpdateProfile() throws Exception {
    // Arrange
    UserProfileUpdateDTO profileDTO = new UserProfileUpdateDTO();
    profileDTO.setFirstName("Updated");
    profileDTO.setLastName("User");
    profileDTO.setEmail("updated@example.com");

    when(securityService.getCurrentUserId()).thenReturn(1L);
    doNothing().when(userService).updateUser(
        1L,
        "Updated",
        "User",
        "updated@example.com");

    // Act & Assert
    mockMvc.perform(put("/api/users/profile")
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(profileDTO)))
        .andExpect(status().isOk())
        .andExpect(content().string("Profile updated successfully"));

    verify(userService, times(1)).updateUser(1L, "Updated", "User", "updated@example.com");
  }

  @Test
  @WithMockUser(username = "testuser", roles = {"USER"})
  void updateUserProfile_WithEmailAlreadyInUse_ShouldReturnBadRequest() throws Exception {
    // Arrange
    UserProfileUpdateDTO profileDTO = new UserProfileUpdateDTO();
    profileDTO.setFirstName("Updated");
    profileDTO.setLastName("User");
    profileDTO.setEmail("existing@example.com");

    when(securityService.getCurrentUserId()).thenReturn(1L);
    doThrow(new IllegalArgumentException("Email already in use"))
        .when(userService)
        .updateUser(1L, "Updated", "User", "existing@example.com");

    // Act & Assert
    mockMvc.perform(put("/api/users/profile")
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(profileDTO)))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("Email already in use"));
  }

  @Test
  void updateUserProfile_WithoutAuthentication_ShouldBeAllowedInTestButWouldFailInProduction()
      throws Exception {
    // Note: This test is allowed to pass because we disabled security filters with @AutoConfigureMockMvc(addFilters = false)
    // In a real application, this would return 401 Unauthorized

    UserProfileUpdateDTO profileDTO = new UserProfileUpdateDTO();
    profileDTO.setFirstName("Updated");
    profileDTO.setLastName("User");
    profileDTO.setEmail("updated@example.com");

    when(securityService.getCurrentUserId()).thenThrow(
        new ResourceNotFoundException("No authenticated user found"));

    // Act & Assert
    mockMvc.perform(put("/api/users/profile")
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(profileDTO)))
        .andExpect(status().isBadRequest()); // Bad request devido à ResourceNotFoundException
  }
}
