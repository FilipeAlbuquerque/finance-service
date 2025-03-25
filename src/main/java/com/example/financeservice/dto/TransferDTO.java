package com.example.financeservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferDTO {

  @NotBlank(message = "Source account number is required")
  private String sourceAccountNumber;

  @NotBlank(message = "Destination account number is required")
  private String destinationAccountNumber;

  @NotNull(message = "Amount is required")
  @Positive(message = "Amount must be positive")
  private BigDecimal amount;

  private String description;
}