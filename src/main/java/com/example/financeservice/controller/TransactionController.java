package com.example.financeservice.controller;

import com.example.financeservice.dto.StatementDTO;
import com.example.financeservice.dto.TransactionDTO;
import com.example.financeservice.dto.TransferDTO;
import com.example.financeservice.service.transaction.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

  private final TransactionService transactionService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<TransactionDTO>> getAllTransactions() {
    return ResponseEntity.ok(transactionService.getAllTransactions());
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TransactionDTO> getTransactionById(@PathVariable Long id) {
    return ResponseEntity.ok(transactionService.getTransactionById(id));
  }

  @GetMapping("/transaction-id/{transactionId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TransactionDTO> getTransactionByTransactionId(@PathVariable String transactionId) {
    return ResponseEntity.ok(transactionService.getTransactionByTransactionId(transactionId));
  }

  @GetMapping("/account/{accountNumber}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<TransactionDTO>> getTransactionsByAccount(@PathVariable String accountNumber) {
    return ResponseEntity.ok(transactionService.getTransactionsByAccount(accountNumber));
  }

  @GetMapping("/account/{accountNumber}/paginated")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Page<TransactionDTO>> getTransactionsByAccountPaginated(
      @PathVariable String accountNumber,
      Pageable pageable) {

    return ResponseEntity.ok(transactionService.getTransactionsByAccountPaginated(accountNumber, pageable));
  }

  @GetMapping("/account/{accountNumber}/statement")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<StatementDTO> getAccountStatement(
      @PathVariable String accountNumber,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

    return ResponseEntity.ok(transactionService.getAccountStatement(accountNumber, startDate, endDate));
  }

  @PostMapping("/transfer")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TransactionDTO> transfer(@Valid @RequestBody TransferDTO transferDTO) {
    TransactionDTO transaction = transactionService.transfer(transferDTO);
    return new ResponseEntity<>(transaction, HttpStatus.CREATED);
  }

  @PostMapping("/deposit")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TransactionDTO> deposit(
      @RequestParam String accountNumber,
      @RequestParam BigDecimal amount,
      @RequestParam(required = false) String description) {

    TransactionDTO transaction = transactionService.deposit(accountNumber, amount, description);
    return new ResponseEntity<>(transaction, HttpStatus.CREATED);
  }

  @PostMapping("/withdraw")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TransactionDTO> withdraw(
      @RequestParam String accountNumber,
      @RequestParam BigDecimal amount,
      @RequestParam(required = false) String description) {

    TransactionDTO transaction = transactionService.withdraw(accountNumber, amount, description);
    return new ResponseEntity<>(transaction, HttpStatus.CREATED);
  }
}
