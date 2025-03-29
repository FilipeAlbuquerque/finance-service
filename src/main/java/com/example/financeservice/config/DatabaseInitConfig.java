package com.example.financeservice.config;

import com.example.financeservice.service.UserService;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class DatabaseInitConfig {

  private final UserService userService;
  private final Environment environment;

  @Bean
  public CommandLineRunner initDatabase() {
    return args -> {
      // Verificar se estamos em ambiente de desenvolvimento
      boolean isDev = Arrays.asList(environment.getActiveProfiles()).contains("dev");

      // Verificar se já existe algum admin
      if (userService.countByRole("ROLE_ADMIN") == 0) {
        // Gerar senha segura ou usar a configurada em ambiente de desenvolvimento
        String adminPassword = isDev ? "admin" : generateSecurePassword();

        userService.createUser(
            "admin",
            adminPassword,
            "admin@example.com",
            "Admin",
            "Albuquerque",
            Collections.singletonList("ROLE_ADMIN")
        );

        log.info("Admin user created successfully");

        if (!isDev) {
          log.info("IMPORTANT: Generated admin password: {}", adminPassword);
          log.info("Please change this password after first login!");
        } else {
          log.info("DEV MODE: Admin credentials - username: admin, password: admin");
        }
      } else {
        log.info("Admin user already exists, skipping initialization");
      }
    };
  }

  private String generateSecurePassword() {
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+";
    SecureRandom random = new SecureRandom();
    return IntStream.range(0, 16)
        .map(i -> random.nextInt(chars.length()))
        .mapToObj(i -> String.valueOf(chars.charAt(i)))
        .collect(Collectors.joining());
  }
}