package com.example.financeservice.dto;

import com.example.financeservice.model.Account;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountDTO {

  @NotNull(message = "Owner ID is required")
  private Long ownerId;

  @NotNull(message = "Account type is required")
  private Account.AccountType type;

  private BigDecimal initialDeposit;

  private BigDecimal availableLimit;
}
