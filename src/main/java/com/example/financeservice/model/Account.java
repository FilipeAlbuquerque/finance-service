package com.example.financeservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true)
  private String accountNumber;

  @Enumerated(EnumType.STRING)
  private AccountType type;

  @NotNull(message = "Balance cannot be null")
  @Column(precision = 19, scale = 2)
  private BigDecimal balance = BigDecimal.ZERO;

  @Column(name = "available_limit", precision = 19, scale = 2)
  private BigDecimal availableLimit;

  @Enumerated(EnumType.STRING)
  private AccountStatus status = AccountStatus.ACTIVE;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "client_id")
  private Client client;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "merchant_id")
  private Merchant merchant;

  @OneToMany(mappedBy = "sourceAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Transaction> outgoingTransactions = new ArrayList<>();

  @OneToMany(mappedBy = "destinationAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Transaction> incomingTransactions = new ArrayList<>();

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();

    if (accountNumber == null) {
      // Generate a random account number
      accountNumber = generateAccountNumber();
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  // Enum for account types
  public enum AccountType {
    CHECKING, SAVINGS, BUSINESS, INVESTMENT
  }

  // Enum for account status
  public enum AccountStatus {
    ACTIVE, INACTIVE, BLOCKED, CLOSED
  }

  // Helper method to generate account number
  private String generateAccountNumber() {
    return String.format("%010d", System.currentTimeMillis() % 10000000000L);
  }
}