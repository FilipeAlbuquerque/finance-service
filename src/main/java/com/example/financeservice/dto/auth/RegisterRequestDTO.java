package com.example.financeservice.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDTO {

  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
  private String username;

  @NotBlank(message = "Password is required")
  @Size(min = 6, max = 40, message = "Password must be between 6 and 40 characters")
  private String password;

  @NotBlank(message = "Email is required")
  @Size(max = 50, message = "Email must be less than 50 characters")
  @Email(message = "Invalid email format")
  private String email;

  private String firstName;

  private String lastName;

  private List<String> roles;
}