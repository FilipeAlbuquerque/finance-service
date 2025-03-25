package com.example.financeservice.controller.auth;

import com.example.financeservice.dto.auth.AuthRequestDTO;
import com.example.financeservice.dto.auth.AuthResponseDTO;
import com.example.financeservice.security.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
public class AuthController {

  private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

  @Autowired
  private AuthenticationManager authenticationManager;

  @Autowired
  private JwtUtils jwtUtils;

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody AuthRequestDTO request) {
    logger.info("Login attempt with username: {}", request.getUsername());

    try {
      // Autenticar usuário
      Authentication authentication = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(
              request.getUsername(),
              request.getPassword())
      );

      logger.info("User authenticated: {}", request.getUsername());

      // Atualizar SecurityContext
      SecurityContextHolder.getContext().setAuthentication(authentication);

      // Gerar token JWT
      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      String jwt = jwtUtils.generateToken(userDetails);

      // Extrair roles
      String[] roles = userDetails.getAuthorities().stream()
          .map(GrantedAuthority::getAuthority)
          .toArray(String[]::new);

      logger.info("Generated token for: {}, with roles: {}", userDetails.getUsername(), String.join(", ", roles));

      // Retornar resposta com token
      return ResponseEntity.ok(new AuthResponseDTO(jwt, userDetails.getUsername(), roles));
    } catch (BadCredentialsException e) {
      logger.warn("Authentication failed for user: {}", request.getUsername());
      return ResponseEntity.status(401).body("Invalid credentials");
    } catch (Exception e) {
      logger.error("Error during authentication: {}", e.getMessage(), e);
      return ResponseEntity.status(500).body("Authentication error: " + e.getMessage());
    }
  }

  @PostMapping("/test-token")
  public ResponseEntity<?> testToken() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth != null && auth.isAuthenticated()) {
      return ResponseEntity.ok("Token válido para usuário: " + auth.getName() +
          " com autoridades: " + auth.getAuthorities());
    }

    return ResponseEntity.status(401).body("Não autenticado");
  }
}