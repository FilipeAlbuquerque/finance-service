package com.example.financeservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtils {

  private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

  // Token expiration time in milliseconds (24 hours)
  private static final long JWT_TOKEN_VALIDITY = 24 * 60 * 60 * 1000L;

  // Chave secreta forte para assinatura de tokens
  private final SecretKey key = Keys.hmacShaKeyFor(
      Decoders.BASE64.decode("YWRzZnNhZGZzYWRmc2FkZnNhZGZzYWRmc2FkYXNkZnNhZGZhc2Rmc2FkZmFzZGZzYWRmYXNkZnNhZA==")
  );

  // Generate token for user
  public String generateToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("authorities", userDetails.getAuthorities());

    String token = createToken(claims, userDetails.getUsername());
    logger.info("Generated token for user: {}", userDetails.getUsername());
    return token;
  }

  // Validate token
  public Boolean validateToken(String token, UserDetails userDetails) {
    try {
      final String username = extractUsername(token);
      boolean isValid = username.equals(userDetails.getUsername()) && !isTokenExpired(token);
      logger.info("Token validation for user {}: {}", username, isValid ? "valid" : "invalid");
      return isValid;
    } catch (Exception e) {
      logger.error("Error validating token: {}", e.getMessage());
      return false;
    }
  }

  // Extract username from token
  public String extractUsername(String token) {
    try {
      String username = extractClaim(token, Claims::getSubject);
      logger.info("Extracted username from token: {}", username);
      return username;
    } catch (Exception e) {
      logger.error("Error extracting username: {}", e.getMessage());
      throw e;
    }
  }

  // Extract expiration date from token
  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  // Extract claim from token
  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  // Extract all claims from token
  private Claims extractAllClaims(String token) {
    try {
      return Jwts.parser()
          .verifyWith(key)
          .build()
          .parseSignedClaims(token)
          .getPayload();
    } catch (Exception e) {
      logger.error("Error parsing JWT claims: {}", e.getMessage());
      throw e;
    }
  }

  // Check if token is expired
  private Boolean isTokenExpired(String token) {
    try {
      final Date expiration = extractExpiration(token);
      boolean isExpired = expiration.before(new Date());
      if (isExpired) {
        logger.info("Token is expired. Expiration: {}, Current: {}", expiration, new Date());
      }
      return isExpired;
    } catch (Exception e) {
      logger.error("Error checking token expiration: {}", e.getMessage());
      return true;
    }
  }

  // Create token
  private String createToken(Map<String, Object> claims, String subject) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + JWT_TOKEN_VALIDITY);

    return Jwts.builder()
        .claims(claims)
        .subject(subject)
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(key)
        .compact();
  }
}