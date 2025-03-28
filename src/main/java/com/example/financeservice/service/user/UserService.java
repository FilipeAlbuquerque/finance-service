package com.example.financeservice.service.user;

import com.example.financeservice.model.User;
import com.example.financeservice.repository.UserRepository;
import com.example.financeservice.service.metrics.MetricsService;
import io.micrometer.core.instrument.Counter;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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
                  .collect(Collectors.toList()))
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
}
