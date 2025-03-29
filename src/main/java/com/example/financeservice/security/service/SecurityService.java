package com.example.financeservice.security.service;

import com.example.financeservice.exception.ResourceNotFoundException;
import com.example.financeservice.model.User;
import com.example.financeservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityService {

  private final UserRepository userRepository;

  // Metodo extraído para permitir substituição em testes
  protected SecurityContext getSecurityContext() {
    return SecurityContextHolder.getContext();
  }

  public Long getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new ResourceNotFoundException("No authenticated user found");
    }

    String username = authentication.getName();
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

    return user.getId();
  }

  public boolean isCurrentUser(Long userId) {
    try {
      return getCurrentUserId().equals(userId);
    } catch (Exception e) {
      return false;
    }
  }
}
