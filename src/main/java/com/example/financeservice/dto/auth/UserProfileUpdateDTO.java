package com.example.financeservice.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateDTO {

  @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
  private String firstName;

  @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
  private String lastName;

  @Size(max = 50, message = "Email must be less than 50 characters")
  @Email(message = "Invalid email format")
  private String email;
}