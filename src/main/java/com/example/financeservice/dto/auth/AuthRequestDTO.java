package com.example.financeservice.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AuthRequestDTO {
  private String username;

  @ToString.Exclude  // Para não mostrar a senha em logs
  private String password;
}