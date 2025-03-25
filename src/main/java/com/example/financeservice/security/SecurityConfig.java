package com.example.financeservice.security;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

  @Bean
  public AuthenticationManager authenticationManager(
      UserDetailsService userDetailsService,
      PasswordEncoder passwordEncoder) {
    logger.info("Configuring AuthenticationManager with UserDetailsService");
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return new ProviderManager(provider);
  }

  @Bean
  @Primary
  public SecurityFilterChain securityFilterChain(HttpSecurity http,
      JwtRequestFilter jwtRequestFilter) throws Exception {
    logger.info("Configuring primary SecurityFilterChain");

    http
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> {
          auth.requestMatchers("/h2-console/**").permitAll()
              .requestMatchers("/actuator/**").permitAll()
              .requestMatchers("/auth/**").permitAll()
              .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
              .anyRequest().authenticated();

          logger.info("Security paths configured");
        })
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
        .headers(headers -> headers
            .frameOptions(FrameOptionsConfig::sameOrigin)
        )
        .exceptionHandling(exHandling -> exHandling.authenticationEntryPoint(
            (request, response, authException) -> {
              logger.error("Unauthorized error: {}", authException.getMessage());
              response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
              response.getWriter().write("Unauthorized: " + authException.getMessage());
            }));

    return http.build();
  }
}