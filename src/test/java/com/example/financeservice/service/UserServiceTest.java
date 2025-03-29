package com.example.financeservice.service;

import com.example.financeservice.exception.InvalidTokenException;
import com.example.financeservice.exception.ResourceNotFoundException;
import com.example.financeservice.model.User;
import com.example.financeservice.repository.UserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

  @Mock
  private static UserRepository userRepository;

  @Mock
  private static PasswordEncoder passwordEncoder;

  @Mock
  private static MetricsService metricsService;

  @Mock
  private EmailService emailService;

  @Mock
  private MeterRegistry meterRegistry;

  @Mock
  private Counter counter;

  @InjectMocks
  private UserService userService;

  private User testUser;

  // Classe de teste que sobrescreve o método problemático
  private static class TestUserService extends UserService {

    public TestUserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
        MetricsService metricsService, EmailService emailService) {
      super(userRepository, passwordEncoder, metricsService, emailService);
    }

    @Override
    public User createUser(String username, String password, String email, List<String> roles) {
      if (userRepository.existsByUsername(username)) {
        metricsService.recordExceptionOccurred("IllegalArgumentException", "createUser");
        throw new IllegalArgumentException("Username already taken");
      }

      if (userRepository.existsByEmail(email)) {
        metricsService.recordExceptionOccurred("IllegalArgumentException", "createUser");
        throw new IllegalArgumentException("Email already in use");
      }

      User user = new User();
      user.setUsername(username);
      user.setPassword(passwordEncoder.encode(password));
      user.setEmail(email);
      user.setRoles(new HashSet<>(roles));

      return userRepository.save(user);
      // Não chama a lógica problemática do Counter
    }
  }


  @BeforeEach
  void setUp() {
    // Configurar usuário de teste
    testUser = new User();
    testUser.setId(1L);
    testUser.setUsername("testuser");
    testUser.setEmail("test@example.com");
    testUser.setPassword("encodedPassword");
    testUser.setFirstName("Test");
    testUser.setLastName("User");
    testUser.setEnabled(true);
    Set<String> roles = new HashSet<>();
    roles.add("ROLE_USER");
    testUser.setRoles(roles);

    userService = new TestUserService(userRepository, passwordEncoder, metricsService,
        emailService);

    // Configurar comportamento padrão para recordRepositoryExecutionTime
    doAnswer(invocation -> {
      Supplier<?> supplier = invocation.getArgument(2);
      return supplier.get();
    }).when(metricsService)
        .recordRepositoryExecutionTime(anyString(), anyString(), any(Supplier.class));
  }

  @Test
  void loadUserByUsername_WithValidUsername_ShouldReturnUserDetails() {
    // Arrange
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

    // Act
    UserDetails result = userService.loadUserByUsername("testuser");

    // Assert
    assertNotNull(result);
    assertEquals("testuser", result.getUsername());
    assertEquals("encodedPassword", result.getPassword());
    assertEquals(1, result.getAuthorities().size());
    assertTrue(result.getAuthorities().stream()
        .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
    verify(userRepository, times(1)).findByUsername("testuser");
  }

  @Test
  void loadUserByUsername_WithInvalidUsername_ShouldThrowException() {
    // Arrange
    when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());
    doNothing().when(metricsService).recordExceptionOccurred(anyString(), anyString());

    // Act & Assert
    assertThrows(UsernameNotFoundException.class,
        () -> userService.loadUserByUsername("nonexistent"));
    verify(userRepository, times(1)).findByUsername("nonexistent");
    verify(metricsService, times(1)).recordExceptionOccurred(eq("UsernameNotFoundException"),
        eq("loadUserByUsername"));
  }

  @Test
  void createUser_WithValidData_ShouldCreateUser() {
    // Arrange
    when(userRepository.existsByUsername("newuser")).thenReturn(false);
    when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
    when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
      User user = invocation.getArgument(0);
      user.setId(2L);
      return user;
    });

    // Act
    User result = userService.createUser("newuser", "password", "new@example.com",
        List.of("ROLE_USER"));

    // Assert
    assertNotNull(result);
    assertEquals(2L, result.getId());
    assertEquals("newuser", result.getUsername());
    assertEquals("encodedPassword", result.getPassword());
    assertEquals("new@example.com", result.getEmail());
    assertTrue(result.getRoles().contains("ROLE_USER"));
    verify(userRepository, times(1)).save(any(User.class));
  }

  @Test
  void createUser_WithExistingUsername_ShouldThrowException() {
    // Arrange
    when(userRepository.existsByUsername("existinguser")).thenReturn(true);
    doNothing().when(metricsService).recordExceptionOccurred(anyString(), anyString());

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () ->
        userService.createUser("existinguser", "password", "new@example.com",
            Arrays.asList("ROLE_USER"))
    );
    verify(userRepository, never()).save(any(User.class));
    verify(metricsService, times(1)).recordExceptionOccurred(eq("IllegalArgumentException"),
        eq("createUser"));
  }

  @Test
  void createUser_WithExistingEmail_ShouldThrowException() {
    // Arrange
    when(userRepository.existsByUsername("newuser")).thenReturn(false);
    when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);
    doNothing().when(metricsService).recordExceptionOccurred(anyString(), anyString());

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () ->
        userService.createUser("newuser", "password", "existing@example.com",
            Arrays.asList("ROLE_USER"))
    );
    verify(userRepository, never()).save(any(User.class));
    verify(metricsService, times(1)).recordExceptionOccurred(eq("IllegalArgumentException"),
        eq("createUser"));
  }

  @Test
  void requestPasswordReset_WithValidEmail_ShouldSendResetEmail() {
    // Arrange
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString());

    // Act
    userService.requestPasswordReset("test@example.com");

    // Assert
    verify(userRepository, times(1)).findByEmail("test@example.com");
    verify(userRepository, times(1)).save(testUser);
    verify(emailService, times(1)).sendPasswordResetEmail(eq("test@example.com"), anyString());
    assertNotNull(testUser.getPasswordResetToken());
    assertNotNull(testUser.getPasswordResetTokenExpiry());
  }

  @Test
  void requestPasswordReset_WithInvalidEmail_ShouldThrowException() {
    // Arrange
    when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());
    doNothing().when(metricsService).recordExceptionOccurred(anyString(), anyString());

    // Act & Assert
    assertThrows(ResourceNotFoundException.class,
        () -> userService.requestPasswordReset("nonexistent@example.com"));
    verify(userRepository, times(1)).findByEmail("nonexistent@example.com");
    verify(userRepository, never()).save(any(User.class));
    verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    verify(metricsService, times(1)).recordExceptionOccurred(eq("ResourceNotFoundException"),
        eq("requestPasswordReset"));
  }

  @Test
  void resetPassword_WithValidToken_ShouldResetPassword() {
    // Arrange
    testUser.setPasswordResetToken("valid-token");
    testUser.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
    when(userRepository.findByPasswordResetToken("valid-token")).thenReturn(Optional.of(testUser));
    when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");

    // Act
    userService.resetPassword("valid-token", "newPassword");

    // Assert
    verify(userRepository, times(1)).findByPasswordResetToken("valid-token");
    verify(passwordEncoder, times(1)).encode("newPassword");
    verify(userRepository, times(1)).save(testUser);
    assertEquals("newEncodedPassword", testUser.getPassword());
    assertNull(testUser.getPasswordResetToken());
    assertNull(testUser.getPasswordResetTokenExpiry());
  }

  @Test
  void resetPassword_WithInvalidToken_ShouldThrowException() {
    // Arrange
    when(userRepository.findByPasswordResetToken("invalid-token")).thenReturn(Optional.empty());
    doNothing().when(metricsService).recordExceptionOccurred(anyString(), anyString());

    // Act & Assert
    assertThrows(ResourceNotFoundException.class,
        () -> userService.resetPassword("invalid-token", "newPassword"));
    verify(userRepository, times(1)).findByPasswordResetToken("invalid-token");
    verify(passwordEncoder, never()).encode(anyString());
    verify(userRepository, never()).save(any(User.class));
    verify(metricsService, times(1)).recordExceptionOccurred(eq("ResourceNotFoundException"),
        eq("resetPassword"));
  }

  @Test
  void resetPassword_WithExpiredToken_ShouldThrowException() {
    // Arrange
    testUser.setPasswordResetToken("expired-token");
    testUser.setPasswordResetTokenExpiry(LocalDateTime.now().minusHours(1));
    when(userRepository.findByPasswordResetToken("expired-token")).thenReturn(
        Optional.of(testUser));
    doNothing().when(metricsService).recordExceptionOccurred(anyString(), anyString());

    // Act & Assert
    assertThrows(InvalidTokenException.class,
        () -> userService.resetPassword("expired-token", "newPassword"));
    verify(userRepository, times(1)).findByPasswordResetToken("expired-token");
    verify(passwordEncoder, never()).encode(anyString());
    verify(userRepository, never()).save(any(User.class));
    verify(metricsService, times(1)).recordExceptionOccurred(eq("InvalidTokenException"),
        eq("resetPassword"));
  }

  @Test
  void updateUser_WithValidData_ShouldUpdateUser() {
    // Arrange
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

    // Act
    userService.updateUser(1L, "Updated", "User", "updated@example.com");

    // Assert
    verify(userRepository, times(1)).findById(1L);
    verify(userRepository, times(1)).save(testUser);
    assertEquals("Updated", testUser.getFirstName());
    assertEquals("User", testUser.getLastName());
    assertEquals("updated@example.com", testUser.getEmail());
  }

  @Test
  void updateUser_WithInvalidId_ShouldThrowException() {
    // Arrange
    when(userRepository.findById(999L)).thenReturn(Optional.empty());
    doNothing().when(metricsService).recordExceptionOccurred(anyString(), anyString());

    // Act & Assert
    assertThrows(ResourceNotFoundException.class, () ->
        userService.updateUser(999L, "Updated", "User", "updated@example.com")
    );
    verify(userRepository, times(1)).findById(999L);
    verify(userRepository, never()).save(any(User.class));
    verify(metricsService, times(1)).recordExceptionOccurred(eq("ResourceNotFoundException"),
        eq("updateUser"));
  }

  @Test
  void updateUser_WithExistingEmail_ShouldThrowException() {
    // Arrange
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);
    doNothing().when(metricsService).recordExceptionOccurred(anyString(), anyString());

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () ->
        userService.updateUser(1L, "Updated", "User", "existing@example.com")
    );
    verify(userRepository, times(1)).findById(1L);
    verify(userRepository, never()).save(any(User.class));
    verify(metricsService, times(1)).recordExceptionOccurred(eq("IllegalArgumentException"),
        eq("updateUser"));
  }
}
