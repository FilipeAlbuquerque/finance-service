package com.example.financeservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class JwtUtils {

  @Value("${jwt.secret:mysecretkey12345mysecretkey12345mysecretkey12345}")
  private String secretString;

  @Value("${jwt.expiration:86400000}")
  private int expiration;  // 24 horas por padrão

  private String getEncodedSecret() {
    // Garantir que a chave secreta esteja em Base64
    try {
      // Verificar se já é uma string Base64 válida
      Decoders.BASE64.decode(secretString);
      return secretString;
    } catch (Exception e) {
      // Se não for Base64 válido, codificar
      return Base64.getEncoder().encodeToString(secretString.getBytes());
    }
  }

  public String generateToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    return Jwts.builder()
        .claims(claims)
        .subject(userDetails.getUsername())
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(getSignKey())
        .compact();
  }

  public boolean validateToken(String token, UserDetails userDetails) {
    final String username = extractUsername(token);
    return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
  }

  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser()
        .verifyWith(getSignKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  private Boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  private SecretKey getSignKey() {
    byte[] keyBytes = Decoders.BASE64.decode(getEncodedSecret());
    return Keys.hmacShaKeyFor(keyBytes);
  }
}
