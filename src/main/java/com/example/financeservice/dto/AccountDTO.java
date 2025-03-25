package com.example.financeservice.dto;

import com.example.financeservice.model.Account;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDTO {

  private Long id;
  private String accountNumber;
  private Account.AccountType type;
  private BigDecimal balance;
  private BigDecimal availableLimit;
  private Account.AccountStatus status;
  private LocalDateTime createdAt;

  // Owner information
  private Long ownerId;
  private String ownerName;
  private String ownerType; // CLIENT or MERCHANT
}
