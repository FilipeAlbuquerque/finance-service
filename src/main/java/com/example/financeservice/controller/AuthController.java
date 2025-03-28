package com.example.financeservice.controller;

import com.example.financeservice.dto.auth.AuthRequestDTO;
import com.example.financeservice.dto.auth.AuthResponseDTO;
import com.example.financeservice.security.JwtUtils;
import com.example.financeservice.security.LoginAttemptService;
import com.example.financeservice.service.metrics.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final JwtUtils jwtUtils;
  private final LoginAttemptService loginAttemptService;
  private final MetricsService metricsService;

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody AuthRequestDTO request) {
    log.info("Authentication attempt for user: {}", request.getUsername());

    // Verificar se o usuário está bloqueado por tentativas excessivas
    if (loginAttemptService.isBlocked(request.getUsername())) {
      log.warn("User account blocked due to multiple failed login attempts: {}", request.getUsername());
      return ResponseEntity.status(429).body("Account temporarily locked due to multiple failed login attempts");
    }

    try {
      // Autenticar usuário
      Authentication authentication = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(
              request.getUsername(),
              request.getPassword())
      );

      log.info("User authenticated: {}", request.getUsername());

      // Atualizar SecurityContext
      SecurityContextHolder.getContext().setAuthentication(authentication);

      // Gerar token JWT
      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      String jwt = jwtUtils.generateToken(userDetails);

      // Extrair roles
      String[] roles = userDetails.getAuthorities().stream()
          .map(GrantedAuthority::getAuthority)
          .toArray(String[]::new);

      log.info("Generated token for: {}, with roles: {}", userDetails.getUsername(), String.join(", ", roles));

      // Registrar login bem-sucedido
      loginAttemptService.loginSucceeded(request.getUsername());
      metricsService.recordSuccessfulLogin(request.getUsername());

      // Retornar resposta com token
      return ResponseEntity.ok(new AuthResponseDTO(jwt, userDetails.getUsername(), roles));
    } catch (BadCredentialsException e) {
      log.warn("Authentication failed for user: {}", request.getUsername());

      // Registrar falha de login
      loginAttemptService.loginFailed(request.getUsername());
      metricsService.recordFailedLogin(request.getUsername());

      return ResponseEntity.status(401).body("Invalid credentials");
    } catch (Exception e) {
      log.error("Error during authentication: {}", e.getMessage(), e);
      return ResponseEntity.status(500).body("Authentication error: " + e.getMessage());
    }
  }

  @PostMapping("/test-token")
  public ResponseEntity<?> testToken() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth != null && auth.isAuthenticated()) {
      log.info("Token test successful for user: {}", auth.getName());
      return ResponseEntity.ok("Token válido para usuário: " + auth.getName() +
          " com autoridades: " + auth.getAuthorities());
    }

    log.warn("Token test failed: No authenticated user found");
    return ResponseEntity.status(401).body("Não autenticado");
  }
}