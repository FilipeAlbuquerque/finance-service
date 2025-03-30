package com.example.financeservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtRequestFilter extends OncePerRequestFilter {

  private UserDetailsService userDetailsService;

  private JwtUtils jwtUtils;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain)
      throws ServletException, IOException {

    final String authorizationHeader = request.getHeader("Authorization");
    log.info("Processing request: {} {}, Auth header: {}",
        request.getMethod(), request.getRequestURI(),
        authorizationHeader != null ? "present" : "absent");

    String username = null;
    String jwt = null;

    if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
      jwt = authorizationHeader.substring(7);
      try {
        username = jwtUtils.extractUsername(jwt);
        log.info("Extracted username from token: {}", username);
      } catch (Exception e) {
        log.error("Error extracting username from token: {}", e.getMessage());
      }
    }

    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      try {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        log.info("Loaded user details: {}, authorities: {}",
            userDetails.getUsername(), userDetails.getAuthorities());

        if (jwtUtils.validateToken(jwt, userDetails)) {
          UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
              userDetails, null, userDetails.getAuthorities());

          authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authentication);

          log.info("Authentication set in SecurityContext for user: {}", username);
        } else {
          log.warn("Token validation failed for user: {}", username);
        }
      } catch (Exception e) {
        log.error("Error during authentication: {}", e.getMessage());
      }
    }

    chain.doFilter(request, response);
  }
}
