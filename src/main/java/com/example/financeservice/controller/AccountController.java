package com.example.financeservice.controller;

import com.example.financeservice.dto.AccountDTO;
import com.example.financeservice.dto.CreateAccountDTO;
import com.example.financeservice.model.Account;
import com.example.financeservice.service.account.AccountService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

  private final AccountService accountService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<AccountDTO>> getAllAccounts() {
    log.info("API Request: Fetching all accounts");

    try {
      List<AccountDTO> accounts = accountService.getAllAccounts();
      log.info("API Response: Retrieved {} accounts", accounts.size());
      return ResponseEntity.ok(accounts);
    } catch (Exception e) {
      log.error("API Error: Failed to retrieve all accounts", e);
      throw e;
    }
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AccountDTO> getAccountById(@PathVariable Long id) {
    log.info("API Request: Fetching account with ID: {}", id);

    try {
      AccountDTO account = accountService.getAccountById(id);
      log.info("API Response: Retrieved account with ID: {}, number: {}",
          id, account.getAccountNumber());
      return ResponseEntity.ok(account);
    } catch (Exception e) {
      log.error("API Error: Failed to retrieve account with ID: {}", id, e);
      throw e;
    }
  }

  @GetMapping("/number/{accountNumber}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AccountDTO> getAccountByNumber(@PathVariable String accountNumber) {
    log.info("API Request: Fetching account with number: {}", accountNumber);

    try {
      AccountDTO account = accountService.getAccountByNumber(accountNumber);
      log.info("API Response: Retrieved account with number: {}, ID: {}",
          accountNumber, account.getId());
      return ResponseEntity.ok(account);
    } catch (Exception e) {
      log.error("API Error: Failed to retrieve account with number: {}", accountNumber, e);
      throw e;
    }
  }

  @GetMapping("/client/{clientId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<AccountDTO>> getAccountsByClient(@PathVariable Long clientId) {
    log.info("API Request: Fetching accounts for client with ID: {}", clientId);

    try {
      List<AccountDTO> accounts = accountService.getAccountsByClient(clientId);
      log.info("API Response: Retrieved {} accounts for client with ID: {}",
          accounts.size(), clientId);
      return ResponseEntity.ok(accounts);
    } catch (Exception e) {
      log.error("API Error: Failed to retrieve accounts for client with ID: {}", clientId, e);
      throw e;
    }
  }

  @GetMapping("/merchant/{merchantId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<AccountDTO>> getAccountsByMerchant(@PathVariable Long merchantId) {
    log.info("API Request: Fetching accounts for merchant with ID: {}", merchantId);

    try {
      List<AccountDTO> accounts = accountService.getAccountsByMerchant(merchantId);
      log.info("API Response: Retrieved {} accounts for merchant with ID: {}",
          accounts.size(), merchantId);
      return ResponseEntity.ok(accounts);
    } catch (Exception e) {
      log.error("API Error: Failed to retrieve accounts for merchant with ID: {}", merchantId, e);
      throw e;
    }
  }

  @PostMapping("/client")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AccountDTO> createClientAccount(
      @Valid @RequestBody CreateAccountDTO createAccountDTO) {
    log.info("API Request: Creating client account for client with ID: {}, type: {}",
        createAccountDTO.getOwnerId(), createAccountDTO.getType());

    try {
      AccountDTO createdAccount = accountService.createClientAccount(createAccountDTO);
      log.info(
          "API Response: Successfully created client account with ID: {}, number: {}, for client ID: {}",
          createdAccount.getId(), createdAccount.getAccountNumber(), createAccountDTO.getOwnerId());
      return new ResponseEntity<>(createdAccount, HttpStatus.CREATED);
    } catch (Exception e) {
      log.error("API Error: Failed to create client account for client with ID: {}",
          createAccountDTO.getOwnerId(), e);
      throw e;
    }
  }

  @PostMapping("/merchant")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AccountDTO> createMerchantAccount(
      @Valid @RequestBody CreateAccountDTO createAccountDTO) {
    log.info("API Request: Creating merchant account for merchant with ID: {}, type: {}",
        createAccountDTO.getOwnerId(), createAccountDTO.getType());

    try {
      AccountDTO createdAccount = accountService.createMerchantAccount(createAccountDTO);
      log.info(
          "API Response: Successfully created merchant account with ID: {}, number: {}, for merchant ID: {}",
          createdAccount.getId(), createdAccount.getAccountNumber(), createAccountDTO.getOwnerId());
      return new ResponseEntity<>(createdAccount, HttpStatus.CREATED);
    } catch (Exception e) {
      log.error("API Error: Failed to create merchant account for merchant with ID: {}",
          createAccountDTO.getOwnerId(), e);
      throw e;
    }
  }

  @PostMapping("/{accountNumber}/deposit")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AccountDTO> deposit(
      @PathVariable String accountNumber,
      @RequestParam BigDecimal amount) {

    log.info("API Request: Depositing {} to account: {}", amount, accountNumber);

    try {
      AccountDTO updatedAccount = accountService.deposit(accountNumber, amount);
      log.info("API Response: Successfully deposited {} to account: {}, new balance: {}",
          amount, accountNumber, updatedAccount.getBalance());
      return ResponseEntity.ok(updatedAccount);
    } catch (Exception e) {
      log.error("API Error: Failed to deposit {} to account: {}", amount, accountNumber, e);
      throw e;
    }
  }

  @PostMapping("/{accountNumber}/withdraw")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AccountDTO> withdraw(
      @PathVariable String accountNumber,
      @RequestParam BigDecimal amount) {

    log.info("API Request: Withdrawing {} from account: {}", amount, accountNumber);

    try {
      AccountDTO updatedAccount = accountService.withdraw(accountNumber, amount);
      log.info("API Response: Successfully withdrew {} from account: {}, new balance: {}",
          amount, accountNumber, updatedAccount.getBalance());
      return ResponseEntity.ok(updatedAccount);
    } catch (Exception e) {
      log.error("API Error: Failed to withdraw {} from account: {}", amount, accountNumber, e);
      throw e;
    }
  }

  @PutMapping("/{id}/status")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AccountDTO> updateAccountStatus(
      @PathVariable Long id,
      @RequestParam Account.AccountStatus status) {

    log.info("API Request: Updating status of account with ID: {} to {}", id, status);

    try {
      AccountDTO updatedAccount = accountService.updateAccountStatus(id, status);
      log.info("API Response: Successfully updated status of account ID: {} to {}",
          id, updatedAccount.getStatus());
      return ResponseEntity.ok(updatedAccount);
    } catch (Exception e) {
      log.error("API Error: Failed to update status of account with ID: {} to {}", id, status, e);
      throw e;
    }
  }
}
