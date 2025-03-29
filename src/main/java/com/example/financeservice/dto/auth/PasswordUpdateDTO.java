package com.example.financeservice.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordUpdateDTO {

  @NotBlank(message = "Token is required")
  private String token;

  @NotBlank(message = "Password is required")
  @Size(min = 6, max = 40, message = "Password must be between 6 and 40 characters")
  private String password;
}