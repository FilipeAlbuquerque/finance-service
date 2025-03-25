package com.example.financeservice.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AuthResponseDTO {
  private String token;
  private String username;
  private String[] roles;

  @ToString.Include(name = "token")
  private String getTokenForLogging() {
    if (token != null && token.length() > 15) {
      return token.substring(0, 10) + "...";
    }
    return token;
  }
}