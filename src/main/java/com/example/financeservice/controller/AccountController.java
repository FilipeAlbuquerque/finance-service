package com.example.financeservice.controller;

import com.example.financeservice.dto.AccountDTO;
import com.example.financeservice.dto.CreateAccountDTO;
import com.example.financeservice.model.Account;
import com.example.financeservice.service.account.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

  private final AccountService accountService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<AccountDTO>> getAllAccounts() {
    return ResponseEntity.ok(accountService.getAllAccounts());
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AccountDTO> getAccountById(@PathVariable Long id) {
    return ResponseEntity.ok(accountService.getAccountById(id));
  }

  @GetMapping("/number/{accountNumber}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AccountDTO> getAccountByNumber(@PathVariable String accountNumber) {
    return ResponseEntity.ok(accountService.getAccountByNumber(accountNumber));
  }

  @GetMapping("/client/{clientId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<AccountDTO>> getAccountsByClient(@PathVariable Long clientId) {
    return ResponseEntity.ok(accountService.getAccountsByClient(clientId));
  }

  @GetMapping("/merchant/{merchantId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<AccountDTO>> getAccountsByMerchant(@PathVariable Long merchantId) {
    return ResponseEntity.ok(accountService.getAccountsByMerchant(merchantId));
  }

  @PostMapping("/client")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AccountDTO> createClientAccount(@Valid @RequestBody CreateAccountDTO createAccountDTO) {
    AccountDTO createdAccount = accountService.createClientAccount(createAccountDTO);
    return new ResponseEntity<>(createdAccount, HttpStatus.CREATED);
  }

  @PostMapping("/merchant")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AccountDTO> createMerchantAccount(@Valid @RequestBody CreateAccountDTO createAccountDTO) {
    AccountDTO createdAccount = accountService.createMerchantAccount(createAccountDTO);
    return new ResponseEntity<>(createdAccount, HttpStatus.CREATED);
  }

  @PostMapping("/{accountNumber}/deposit")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AccountDTO> deposit(
      @PathVariable String accountNumber,
      @RequestParam BigDecimal amount) {

    return ResponseEntity.ok(accountService.deposit(accountNumber, amount));
  }

  @PostMapping("/{accountNumber}/withdraw")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AccountDTO> withdraw(
      @PathVariable String accountNumber,
      @RequestParam BigDecimal amount) {

    return ResponseEntity.ok(accountService.withdraw(accountNumber, amount));
  }

  @PutMapping("/{id}/status")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AccountDTO> updateAccountStatus(
      @PathVariable Long id,
      @RequestParam Account.AccountStatus status) {

    return ResponseEntity.ok(accountService.updateAccountStatus(id, status));
  }
}
