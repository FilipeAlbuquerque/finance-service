package com.example.financeservice;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * Configuração de segurança específica para testes.
 * Esta configuração substitui qualquer outra implementação de UserDetailsService
 * durante os testes.
 */
@TestConfiguration
public class TestSecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  @Primary
  public UserDetailsService testUserDetailsService() {
    // Retorna um UserDetailsService vazio para testes
    return new InMemoryUserDetailsManager();
  }
}