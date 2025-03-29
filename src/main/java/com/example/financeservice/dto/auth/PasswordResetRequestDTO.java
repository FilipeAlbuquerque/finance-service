package com.example.financeservice.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequestDTO {

  @NotBlank(message = "Email is required")
  @Email(message = "Invalid email format")
  private String email;
}
