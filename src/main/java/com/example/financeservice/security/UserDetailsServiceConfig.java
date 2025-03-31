package com.example.financeservice.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import java.util.Arrays;

@Configuration
public class UserDetailsServiceConfig {

  @Value("${app.security.admin.username:admin}")
  private String adminUsername;

  @Value("${app.security.admin.password:#{environment.APP_ADMIN_PASSWORD}}")
  private String adminPassword;

  @Value("${app.security.user.username:user}")
  private String userUsername;

  @Value("${app.security.user.password:#{environment.APP_USER_PASSWORD}}")
  private String userPassword;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public UserDetailsService userDetailsService() {
    PasswordEncoder encoder = passwordEncoder();

    // Usando valores de configuração externalizados
    UserDetails adminUser = User.builder()
        .username(adminUsername)
        .password(encoder.encode(adminPassword))
        .roles("ADMIN")
        .build();

    UserDetails regularUser = User.builder()
        .username(userUsername)
        .password(encoder.encode(userPassword))
        .roles("USER")
        .build();

    return new InMemoryUserDetailsManager(Arrays.asList(adminUser, regularUser));
  }
}
