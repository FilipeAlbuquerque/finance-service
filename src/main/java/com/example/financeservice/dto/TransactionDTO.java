package com.example.financeservice.dto;

import com.example.financeservice.model.Transaction;
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
public class TransactionDTO {

  private Long id;
  private String transactionId;
  private BigDecimal amount;
  private Transaction.TransactionType type;
  private String description;
  private Transaction.TransactionStatus status;
  private LocalDateTime createdAt;
  private LocalDateTime processedAt;

  private String sourceAccountNumber;
  private String destinationAccountNumber;
}
