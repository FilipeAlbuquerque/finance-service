package com.example.financeservice.security;

import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
public class UserDetailsServiceConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public UserDetailsService userDetailsService() {
    PasswordEncoder encoder = passwordEncoder();

    // Usando a abordagem padrão do Spring Security
    UserDetails adminUser = User.builder()
        .username("admin")
        .password(encoder.encode("admin"))
        .roles("ADMIN") // Será convertido para ROLE_ADMIN
        .build();

    UserDetails regularUser = User.builder()
        .username("user")
        .password(encoder.encode("user"))
        .roles("USER") // Será convertido para ROLE_USER
        .build();

    return new InMemoryUserDetailsManager(Arrays.asList(adminUser, regularUser));
  }
}