package com.example.financeservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true)
  private String transactionId;

  @NotNull(message = "Amount cannot be null")
  @Column(precision = 19, scale = 2)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  private TransactionType type;

  private String description;

  @Enumerated(EnumType.STRING)
  private TransactionStatus status = TransactionStatus.PENDING;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "processed_at")
  private LocalDateTime processedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_account_id")
  private Account sourceAccount;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "destination_account_id")
  private Account destinationAccount;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();

    if (transactionId == null) {
      // Generate a unique transaction ID
      transactionId = generateTransactionId();
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();

    if (status == TransactionStatus.COMPLETED && processedAt == null) {
      processedAt = LocalDateTime.now();
    }
  }

  // Enum for transaction types
  public enum TransactionType {
    DEPOSIT, WITHDRAWAL, TRANSFER, PAYMENT, REFUND, FEE
  }

  // Enum for transaction status
  public enum TransactionStatus {
    PENDING, PROCESSING, COMPLETED, FAILED, REVERSED
  }

  // Helper method to generate transaction ID
  private String generateTransactionId() {
    return "TX" + System.currentTimeMillis();
  }
}
