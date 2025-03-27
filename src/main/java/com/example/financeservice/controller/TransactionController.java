package com.example.financeservice.controller;

import com.example.financeservice.dto.StatementDTO;
import com.example.financeservice.dto.TransactionDTO;
import com.example.financeservice.dto.TransferDTO;
import com.example.financeservice.service.transaction.TransactionService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

  private final TransactionService transactionService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<TransactionDTO>> getAllTransactions() {
    log.info("API Request: Fetching all transactions");

    try {
      List<TransactionDTO> transactions = transactionService.getAllTransactions();
      log.info("API Response: Retrieved {} transactions", transactions.size());
      return ResponseEntity.ok(transactions);
    } catch (Exception e) {
      log.error("API Error: Failed to retrieve all transactions", e);
      throw e;
    }
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TransactionDTO> getTransactionById(@PathVariable Long id) {
    log.info("API Request: Fetching transaction with ID: {}", id);

    try {
      TransactionDTO transaction = transactionService.getTransactionById(id);
      log.info("API Response: Retrieved transaction with ID: {}, type: {}, amount: {}",
          id, transaction.getType(), transaction.getAmount());
      return ResponseEntity.ok(transaction);
    } catch (Exception e) {
      log.error("API Error: Failed to retrieve transaction with ID: {}", id, e);
      throw e;
    }
  }

  @GetMapping("/transaction-id/{transactionId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TransactionDTO> getTransactionByTransactionId(
      @PathVariable String transactionId) {
    log.info("API Request: Fetching transaction with transaction ID: {}", transactionId);

    try {
      TransactionDTO transaction = transactionService.getTransactionByTransactionId(transactionId);
      log.info("API Response: Retrieved transaction with transaction ID: {}, type: {}, amount: {}",
          transactionId, transaction.getType(), transaction.getAmount());
      return ResponseEntity.ok(transaction);
    } catch (Exception e) {
      log.error("API Error: Failed to retrieve transaction with transaction ID: {}", transactionId,
          e);
      throw e;
    }
  }

  @GetMapping("/account/{accountNumber}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<TransactionDTO>> getTransactionsByAccount(
      @PathVariable String accountNumber) {
    log.info("API Request: Fetching transactions for account: {}", accountNumber);

    try {
      List<TransactionDTO> transactions = transactionService.getTransactionsByAccount(
          accountNumber);
      log.info("API Response: Retrieved {} transactions for account: {}",
          transactions.size(), accountNumber);
      return ResponseEntity.ok(transactions);
    } catch (Exception e) {
      log.error("API Error: Failed to retrieve transactions for account: {}", accountNumber, e);
      throw e;
    }
  }

  @GetMapping("/account/{accountNumber}/paginated")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Page<TransactionDTO>> getTransactionsByAccountPaginated(
      @PathVariable String accountNumber,
      Pageable pageable) {

    log.info("API Request: Fetching paginated transactions for account: {}, page: {}, size: {}",
        accountNumber, pageable.getPageNumber(), pageable.getPageSize());

    try {
      Page<TransactionDTO> transactions = transactionService.getTransactionsByAccountPaginated(
          accountNumber, pageable);
      log.info("API Response: Retrieved page {} of {} for account: {}, total elements: {}",
          pageable.getPageNumber(), pageable.getPageSize(), accountNumber,
          transactions.getTotalElements());
      return ResponseEntity.ok(transactions);
    } catch (Exception e) {
      log.error("API Error: Failed to retrieve paginated transactions for account: {}",
          accountNumber, e);
      throw e;
    }
  }

  @GetMapping("/account/{accountNumber}/statement")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<StatementDTO> getAccountStatement(
      @PathVariable String accountNumber,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

    log.info("API Request: Generating statement for account: {} from {} to {}",
        accountNumber, startDate, endDate);

    try {
      StatementDTO statement = transactionService.getAccountStatement(accountNumber, startDate,
          endDate);
      log.info("API Response: Generated statement for account: {} with {} transactions",
          accountNumber, statement.getTransactions().size());
      return ResponseEntity.ok(statement);
    } catch (Exception e) {
      log.error("API Error: Failed to generate statement for account: {} from {} to {}",
          accountNumber, startDate, endDate, e);
      throw e;
    }
  }

  @PostMapping("/transfer")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TransactionDTO> transfer(@Valid @RequestBody TransferDTO transferDTO) {
    log.info("API Request: Processing transfer of {} from account: {} to account: {}",
        transferDTO.getAmount(), transferDTO.getSourceAccountNumber(),
        transferDTO.getDestinationAccountNumber());

    try {
      TransactionDTO transaction = transactionService.transfer(transferDTO);
      log.info("API Response: Transfer successful, transaction ID: {}, amount: {}, status: {}",
          transaction.getTransactionId(), transaction.getAmount(), transaction.getStatus());
      return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    } catch (Exception e) {
      log.error("API Error: Transfer failed from account: {} to account: {}, amount: {}",
          transferDTO.getSourceAccountNumber(), transferDTO.getDestinationAccountNumber(),
          transferDTO.getAmount(), e);
      throw e;
    }
  }

  @PostMapping("/deposit")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TransactionDTO> deposit(
      @RequestParam String accountNumber,
      @RequestParam BigDecimal amount,
      @RequestParam(required = false) String description) {

    log.info("API Request: Processing deposit of {} to account: {}", amount, accountNumber);

    try {
      TransactionDTO transaction = transactionService.deposit(accountNumber, amount, description);
      log.info("API Response: Deposit successful, transaction ID: {}, amount: {}, status: {}",
          transaction.getTransactionId(), transaction.getAmount(), transaction.getStatus());
      return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    } catch (Exception e) {
      log.error("API Error: Deposit failed to account: {}, amount: {}", accountNumber, amount, e);
      throw e;
    }
  }

  @PostMapping("/withdraw")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TransactionDTO> withdraw(
      @RequestParam String accountNumber,
      @RequestParam BigDecimal amount,
      @RequestParam(required = false) String description) {

    log.info("API Request: Processing withdrawal of {} from account: {}", amount, accountNumber);

    try {
      TransactionDTO transaction = transactionService.withdraw(accountNumber, amount, description);
      log.info("API Response: Withdrawal successful, transaction ID: {}, amount: {}, status: {}",
          transaction.getTransactionId(), transaction.getAmount(), transaction.getStatus());
      return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    } catch (Exception e) {
      log.error("API Error: Withdrawal failed from account: {}, amount: {}", accountNumber, amount,
          e);
      throw e;
    }
  }
}
