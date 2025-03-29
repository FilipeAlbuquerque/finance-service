package com.example.financeservice.service;

import com.example.financeservice.exception.InvalidTokenException;
import com.example.financeservice.exception.ResourceNotFoundException;
import com.example.financeservice.model.User;
import com.example.financeservice.repository.UserRepository;
import io.micrometer.core.instrument.Counter;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserDetailsService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final MetricsService metricsService;
  private final EmailService emailService;

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    // Usar um timer para medir o tempo de carregamento do usuário
    return metricsService.recordRepositoryExecutionTime(
        "UserRepository", "findByUsername",
        () -> {
          User user = userRepository.findByUsername(username)
              .orElseThrow(() -> {
                log.error("User not found with username: {}", username);
                metricsService.recordExceptionOccurred("UsernameNotFoundException",
                    "loadUserByUsername");
                return new UsernameNotFoundException("User not found with username: " + username);
              });

          return org.springframework.security.core.userdetails.User.builder()
              .username(user.getUsername())
              .password(user.getPassword())
              .authorities(user.getRoles().stream()
                  .map(SimpleGrantedAuthority::new)
                  .toList())
              .disabled(!user.isEnabled())
              .accountExpired(false)
              .credentialsExpired(false)
              .accountLocked(false)
              .build();
        });
  }

  @Transactional(readOnly = true)
  public Optional<User> findByUsername(String username) {
    return userRepository.findByUsername(username);
  }

  @Transactional
  public User createUser(String username, String password, String email, String firstName,
      String lastName, List<String> roles) {
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
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setRoles(new HashSet<>(roles));

    User savedUser = userRepository.save(user);
    log.info("User created successfully: {}", username);

    // Registrar métrica
    Counter.builder("finance.users.created")
        .tag("role", String.join(",", roles))
        .register(metricsService.getRegistry())
        .increment();

    return savedUser;
  }

  @Transactional(readOnly = true)
  public boolean existsByUsername(String username) {
    return userRepository.existsByUsername(username);
  }

  @Transactional(readOnly = true)
  public long countByRole(String role) {
    return userRepository.countByRole(role);
  }

  @Transactional
  public void requestPasswordReset(String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> {
          log.error("Email not found for password reset: {}", email);
          metricsService.recordExceptionOccurred("ResourceNotFoundException",
              "requestPasswordReset");
          return new ResourceNotFoundException("User not found with email: " + email);
        });

    String token = UUID.randomUUID().toString();
    user.setPasswordResetToken(token);
    user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(24));
    userRepository.save(user);

    // Enviar email com token
    emailService.sendPasswordResetEmail(user.getEmail(), token);
    log.info("Password reset requested for email: {}", user.getEmail());
  }

  @Transactional
  public void resetPassword(String token, String newPassword) {
    User user = userRepository.findByPasswordResetToken(token)
        .orElseThrow(() -> {
          log.error("Invalid password reset token: {}", token);
          metricsService.recordExceptionOccurred("ResourceNotFoundException", "resetPassword");
          return new ResourceNotFoundException("Invalid or expired token");
        });

    if (user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
      log.error("Password reset token expired for user: {}", user.getUsername());
      metricsService.recordExceptionOccurred("InvalidTokenException", "resetPassword");
      throw new InvalidTokenException("Password reset token has expired");
    }

    user.setPassword(passwordEncoder.encode(newPassword));
    user.setPasswordResetToken(null);
    user.setPasswordResetTokenExpiry(null);
    userRepository.save(user);
    log.info("Password reset successfully for user: {}", user.getUsername());
  }

  // Metodo para atualizar informações do usuário
  @Transactional
  public void updateUser(Long userId, String firstName, String lastName, String email) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.error("User not found with id: {}", userId);
          metricsService.recordExceptionOccurred("ResourceNotFoundException", "updateUser");
          return new ResourceNotFoundException("User not found with id: " + userId);
        });

    if (email != null && !email.equals(user.getEmail())) {
      if (userRepository.existsByEmail(email)) {
        log.error("Email already in use: {}", email);
        metricsService.recordExceptionOccurred("IllegalArgumentException", "updateUser");
        throw new IllegalArgumentException("Email already in use");
      }
      user.setEmail(email);
    }

    if (firstName != null) {
      user.setFirstName(firstName);
    }

    if (lastName != null) {
      user.setLastName(lastName);
    }

    userRepository.save(user);
    log.info("User updated: {}", user.getUsername());
  }
}
