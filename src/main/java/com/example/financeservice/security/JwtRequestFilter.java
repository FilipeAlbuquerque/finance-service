package com.example.financeservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

  @Autowired
  private UserDetailsService userDetailsService;

  @Autowired
  private JwtUtils jwtUtils;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain)
      throws ServletException, IOException {

    final String authorizationHeader = request.getHeader("Authorization");
    logger.info("Processing request: {} {}, Auth header: {}",
        request.getMethod(), request.getRequestURI(),
        authorizationHeader != null ? "present" : "absent");

    String username = null;
    String jwt = null;

    if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
      jwt = authorizationHeader.substring(7);
      try {
        username = jwtUtils.extractUsername(jwt);
        logger.info("Extracted username from token: {}", username);
      } catch (Exception e) {
        logger.error("Error extracting username from token: {}", e.getMessage());
      }
    }

    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      try {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        logger.info("Loaded user details: {}, authorities: {}",
            userDetails.getUsername(), userDetails.getAuthorities());

        if (jwtUtils.validateToken(jwt, userDetails)) {
          UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
              userDetails, null, userDetails.getAuthorities());

          authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authentication);

          logger.info("Authentication set in SecurityContext for user: {}", username);
        } else {
          logger.warn("Token validation failed for user: {}", username);
        }
      } catch (Exception e) {
        logger.error("Error during authentication: {}", e.getMessage());
      }
    }

    chain.doFilter(request, response);
  }
}