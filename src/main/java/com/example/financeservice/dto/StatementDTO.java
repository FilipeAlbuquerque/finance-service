package com.example.financeservice.dto;

import com.example.financeservice.model.Account;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementDTO {

  private String accountNumber;
  private Account.AccountType accountType;
  private BigDecimal currentBalance;
  private LocalDateTime statementStartDate;
  private LocalDateTime statementEndDate;
  private LocalDateTime generatedAt;

  private List<TransactionDTO> transactions;
}
