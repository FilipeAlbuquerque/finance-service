package com.example.financeservice.controller;

import com.example.financeservice.dto.auth.AuthRequestDTO;
import com.example.financeservice.dto.auth.PasswordResetRequestDTO;
import com.example.financeservice.dto.auth.PasswordUpdateDTO;
import com.example.financeservice.dto.auth.RegisterRequestDTO;
import com.example.financeservice.exception.InvalidTokenException;
import com.example.financeservice.exception.ResourceNotFoundException;
import com.example.financeservice.model.User;
import com.example.financeservice.security.JwtUtils;
import com.example.financeservice.security.LoginAttemptService;
import com.example.financeservice.service.MetricsService;
import com.example.financeservice.service.UserService;
import com.example.financeservice.TestSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class AuthControllerIT {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private AuthenticationManager authenticationManager;

  @MockBean
  private JwtUtils jwtUtils;

  @MockBean
  private LoginAttemptService loginAttemptService;

  @MockBean
  private MetricsService metricsService;

  @MockBean
  private UserService userService;

  private User testUser;
  private UserDetails userDetails;

  @BeforeEach
  void setUp() {
    // Create test user
    testUser = new User();
    testUser.setId(1L);
    testUser.setUsername("testuser");
    testUser.setEmail("test@example.com");
    testUser.setFirstName("Test");
    testUser.setLastName("User");

    // Create user details
    userDetails = new org.springframework.security.core.userdetails.User(
        "testuser",
        "password",
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
    );

    // Setup timer mock
    when(metricsService.startTimer()).thenReturn(null);
  }

  @Test
  void login_WithValidCredentials_ShouldReturnToken() throws Exception {
    // Arrange
    AuthRequestDTO authRequest = new AuthRequestDTO();
    authRequest.setUsername("testuser");
    authRequest.setPassword("password");

    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(userDetails);
    when(authentication.getName()).thenReturn("testuser");

    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(authentication);
    when(jwtUtils.generateToken(any(UserDetails.class))).thenReturn("test.jwt.token");
    when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
    doNothing().when(loginAttemptService).loginSucceeded(anyString());
    doNothing().when(metricsService).recordSuccessfulLogin(anyString());

    // Act & Assert
    mockMvc.perform(post("/auth/login")
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(authRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("test.jwt.token"))
        .andExpect(jsonPath("$.username").value("testuser"));

    verify(loginAttemptService, times(1)).loginSucceeded("testuser");
    verify(metricsService, times(1)).recordSuccessfulLogin("testuser");
  }

  // Outros métodos de teste continuam iguais...
  @Test
  void login_WithInvalidCredentials_ShouldReturn401() throws Exception {
    // Arrange
    AuthRequestDTO authRequest = new AuthRequestDTO();
    authRequest.setUsername("testuser");
    authRequest.setPassword("wrongpassword");

    when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenThrow(new BadCredentialsException("Invalid credentials"));
    doNothing().when(loginAttemptService).loginFailed(anyString());
    doNothing().when(metricsService).recordFailedLogin(anyString());

    // Act & Assert
    mockMvc.perform(post("/auth/login")
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(authRequest)))
        .andExpect(status().isUnauthorized())
        .andExpect(MockMvcResultMatchers.content().string("Invalid credentials"));

    verify(loginAttemptService, times(1)).loginFailed("testuser");
    verify(metricsService, times(1)).recordFailedLogin("testuser");
  }

  @Test
  void register_WithValidData_ShouldCreateUser() throws Exception {
    // Arrange
    RegisterRequestDTO registerRequest = new RegisterRequestDTO();
    registerRequest.setUsername("newuser");
    registerRequest.setPassword("password123");
    registerRequest.setEmail("new@example.com");
    registerRequest.setFirstName("New");
    registerRequest.setLastName("User");
    registerRequest.setRoles(List.of("ROLE_USER"));

    when(userService.createUser(
        eq("newuser"),
        eq("password123"),
        eq("new@example.com"),
        eq("New"),
        eq("User"),
        any(List.class)))
        .thenReturn(testUser);

    // Act & Assert
    mockMvc.perform(post("/auth/register")
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(registerRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("User registered successfully"))
        .andExpect(jsonPath("$.username").value("testuser"))
        .andExpect(jsonPath("$.id").value("1"));
  }

  @Test
  void forgotPassword_WithValidEmail_ShouldSendResetEmail() throws Exception {
    // Arrange
    PasswordResetRequestDTO resetRequest = new PasswordResetRequestDTO();
    resetRequest.setEmail("test@example.com");

    doNothing().when(userService).requestPasswordReset("test@example.com");

    // Act & Assert
    mockMvc.perform(post("/auth/forgot-password")
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(resetRequest)))
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.content().string("Password reset email sent"));

    verify(userService, times(1)).requestPasswordReset("test@example.com");
  }

  @Test
  void forgotPassword_WithInvalidEmail_ShouldStillReturn202() throws Exception {
    // Arrange
    PasswordResetRequestDTO resetRequest = new PasswordResetRequestDTO();
    resetRequest.setEmail("nonexistent@example.com");

    doThrow(new ResourceNotFoundException("User not found with email: nonexistent@example.com"))
        .when(userService).requestPasswordReset("nonexistent@example.com");

    // Act & Assert
    mockMvc.perform(post("/auth/forgot-password")
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(resetRequest)))
        .andExpect(status().isAccepted())
        .andExpect(MockMvcResultMatchers.content().string("Password reset email sent"));
  }

  @Test
  void resetPassword_WithValidToken_ShouldResetPassword() throws Exception {
    // Arrange
    PasswordUpdateDTO updateRequest = new PasswordUpdateDTO();
    updateRequest.setToken("valid-token");
    updateRequest.setPassword("newpassword123");

    doNothing().when(userService).resetPassword("valid-token", "newpassword123");

    // Act & Assert
    mockMvc.perform(post("/auth/reset-password")
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.content().string("Password reset successful"));

    verify(userService, times(1)).resetPassword("valid-token", "newpassword123");
  }

  @Test
  void resetPassword_WithInvalidToken_ShouldReturn400() throws Exception {
    // Arrange
    PasswordUpdateDTO updateRequest = new PasswordUpdateDTO();
    updateRequest.setToken("invalid-token");
    updateRequest.setPassword("newpassword123");

    doThrow(new InvalidTokenException("Password reset token has expired"))
        .when(userService).resetPassword("invalid-token", "newpassword123");

    // Act & Assert
    mockMvc.perform(post("/auth/reset-password")
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(MockMvcResultMatchers.content().string("Password reset token has expired"));
  }
}
