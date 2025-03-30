package com.example.financeservice.service;


import com.example.financeservice.dto.AccountDTO;
import com.example.financeservice.dto.CreateAccountDTO;
import com.example.financeservice.exception.InsufficientFundsException;
import com.example.financeservice.exception.ResourceNotFoundException;
import com.example.financeservice.model.Account;
import com.example.financeservice.model.Client;
import com.example.financeservice.model.Merchant;
import com.example.financeservice.repository.AccountRepository;
import com.example.financeservice.repository.ClientRepository;
import com.example.financeservice.repository.MerchantRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

  private static final String WITHDRAWAL = "withdrawal";
  private static final String WITHDRAWAL_METRIC = "WITHDRAWAL";
  private static final String DEPOSIT_METRIC = "DEPOSIT";
  private static final String ACCOUNT_REPOSITORY_METRIC = "AccountRepository";
  private static final String FIND_BY_ID = "findById";
  private static final String ACCOUNT_NOT_FOUND = "Service: Account not found with number: {}";
  private static final String ACCOUNT_NUMBER_NOT_FOUND = "Account not found with number: ";
  private static final String RESOURCE_NOT_FOUND = "ResourceNotFoundException";

  private final AccountRepository accountRepository;
  private final ClientRepository clientRepository;
  private final MerchantRepository merchantRepository;
  private final MetricsService metricsService;

  @Transactional(readOnly = true)
  public List<AccountDTO> getAllAccounts() {
    log.debug("Service: Getting all accounts");

    List<Account> accounts = metricsService.recordRepositoryExecutionTime(
        ACCOUNT_REPOSITORY_METRIC, "findAll",
        accountRepository::findAll);

    log.debug("Service: Found {} accounts in database", accounts.size());

    return accounts.stream()
        .map(this::convertToDTO)
        .toList();
  }

  @Transactional(readOnly = true)
  public AccountDTO getAccountById(Long id) {
    log.debug("Service: Getting account with ID: {}", id);

    Account account = metricsService.recordRepositoryExecutionTime(
        ACCOUNT_REPOSITORY_METRIC, FIND_BY_ID,
        () -> accountRepository.findById(id)
            .orElseThrow(() -> {
              log.error("Service: Account not found with ID: {}", id);
              metricsService.recordExceptionOccurred(RESOURCE_NOT_FOUND, "getAccountById");
              return new ResourceNotFoundException("Account not found with id: " + id);
            }));

    log.debug("Service: Found account with ID: {}, number: {}", id, account.getAccountNumber());
    return convertToDTO(account);
  }

  @Transactional(readOnly = true)
  public AccountDTO getAccountByNumber(String accountNumber) {
    log.debug("Service: Getting account with number: {}", accountNumber);

    Account account = metricsService.recordRepositoryExecutionTime(
        ACCOUNT_REPOSITORY_METRIC, "findByAccountNumber",
        () -> accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> {
              log.error(ACCOUNT_NOT_FOUND, accountNumber);
              metricsService.recordExceptionOccurred(RESOURCE_NOT_FOUND,
                  "getAccountByNumber");
              return new ResourceNotFoundException(
                  ACCOUNT_NUMBER_NOT_FOUND + accountNumber);
            }));

    log.debug("Service: Found account with number: {}, ID: {}", accountNumber, account.getId());
    return convertToDTO(account);
  }

  @Transactional(readOnly = true)
  public List<AccountDTO> getAccountsByClient(Long clientId) {
    log.debug("Service: Getting accounts for client with ID: {}", clientId);

    Client client = metricsService.recordRepositoryExecutionTime(
        "ClientRepository", FIND_BY_ID,
        () -> clientRepository.findById(clientId)
            .orElseThrow(() -> {
              log.error("Service: Client not found with ID: {}", clientId);
              metricsService.recordExceptionOccurred(RESOURCE_NOT_FOUND,
                  "getAccountsByClient");
              return new ResourceNotFoundException("Client not found with id: " + clientId);
            }));

    List<Account> accounts = metricsService.recordRepositoryExecutionTime(
        ACCOUNT_REPOSITORY_METRIC, "findByClient",
        () -> accountRepository.findByClient(client));

    log.debug("Service: Found {} accounts for client with ID: {}", accounts.size(), clientId);

    return accounts.stream()
        .map(this::convertToDTO)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<AccountDTO> getAccountsByMerchant(Long merchantId) {
    log.debug("Service: Getting accounts for merchant with ID: {}", merchantId);

    Merchant merchant = metricsService.recordRepositoryExecutionTime(
        "MerchantRepository", FIND_BY_ID,
        () -> merchantRepository.findById(merchantId)
            .orElseThrow(() -> {
              log.error("Service: Merchant not found with ID: {}", merchantId);
              metricsService.recordExceptionOccurred(RESOURCE_NOT_FOUND,
                  "getAccountsByMerchant");
              return new ResourceNotFoundException("Merchant not found with id: " + merchantId);
            }));

    List<Account> accounts = metricsService.recordRepositoryExecutionTime(
        ACCOUNT_REPOSITORY_METRIC, "findByMerchant",
        () -> accountRepository.findByMerchant(merchant));

    log.debug("Service: Found {} accounts for merchant with ID: {}", accounts.size(), merchantId);

    return accounts.stream()
        .map(this::convertToDTO)
        .toList();
  }

  @Transactional
  public AccountDTO createClientAccount(CreateAccountDTO createAccountDTO) {
    log.debug("Service: Creating account for client with ID: {}", createAccountDTO.getOwnerId());

    Client client = metricsService.recordRepositoryExecutionTime(
        "ClientRepository", FIND_BY_ID,
        () -> clientRepository.findById(createAccountDTO.getOwnerId())
            .orElseThrow(() -> {
              log.error("Service: Client not found with ID: {}", createAccountDTO.getOwnerId());
              metricsService.recordExceptionOccurred(RESOURCE_NOT_FOUND,
                  "createClientAccount");
              return new ResourceNotFoundException(
                  "Client not found with id: " + createAccountDTO.getOwnerId());
            }));

    Account account = new Account();
    account.setType(createAccountDTO.getType());
    account.setBalance(BigDecimal.ZERO);
    account.setClient(client);

    if (createAccountDTO.getInitialDeposit() != null
        && createAccountDTO.getInitialDeposit().compareTo(BigDecimal.ZERO) > 0) {
      account.setBalance(createAccountDTO.getInitialDeposit());
      log.debug("Service: Setting initial deposit of {} for new account",
          createAccountDTO.getInitialDeposit());
    }

    if (createAccountDTO.getAvailableLimit() != null) {
      account.setAvailableLimit(createAccountDTO.getAvailableLimit());
      log.debug("Service: Setting available limit of {} for new account",
          createAccountDTO.getAvailableLimit());
    }

    Account savedAccount = metricsService.recordRepositoryExecutionTime(
        ACCOUNT_REPOSITORY_METRIC, "save",
        () -> accountRepository.save(account));

    log.info(
        "Service: Account created successfully for client ID: {}, account number: {}, type: {}",
        client.getId(), savedAccount.getAccountNumber(), savedAccount.getType());

    // Registrar métricas
    metricsService.recordAccountCreated(savedAccount.getType().toString(), "CLIENT");

    // Registrar o depósito inicial se houver
    if (createAccountDTO.getInitialDeposit() != null
        && createAccountDTO.getInitialDeposit().compareTo(BigDecimal.ZERO) > 0) {
      metricsService.recordTransactionProcessed(DEPOSIT_METRIC,
          createAccountDTO.getInitialDeposit(), true);
      metricsService.recordDailyFinancialVolume(DEPOSIT_METRIC,
          createAccountDTO.getInitialDeposit());
    }

    return convertToDTO(savedAccount);
  }

  @Transactional
  public AccountDTO createMerchantAccount(CreateAccountDTO createAccountDTO) {
    log.debug("Service: Creating account for merchant with ID: {}", createAccountDTO.getOwnerId());

    Merchant merchant = metricsService.recordRepositoryExecutionTime(
        "MerchantRepository", FIND_BY_ID,
        () -> merchantRepository.findById(createAccountDTO.getOwnerId())
            .orElseThrow(() -> {
              log.error("Service: Merchant not found with ID: {}", createAccountDTO.getOwnerId());
              metricsService.recordExceptionOccurred(RESOURCE_NOT_FOUND,
                  "createMerchantAccount");
              return new ResourceNotFoundException(
                  "Merchant not found with id: " + createAccountDTO.getOwnerId());
            }));

    Account account = new Account();
    account.setType(createAccountDTO.getType());
    account.setBalance(BigDecimal.ZERO);
    account.setMerchant(merchant);

    if (createAccountDTO.getInitialDeposit() != null
        && createAccountDTO.getInitialDeposit().compareTo(BigDecimal.ZERO) > 0) {
      account.setBalance(createAccountDTO.getInitialDeposit());
      log.debug("Service: Setting initial deposit of {} for new account",
          createAccountDTO.getInitialDeposit());
    }

    if (createAccountDTO.getAvailableLimit() != null) {
      account.setAvailableLimit(createAccountDTO.getAvailableLimit());
      log.debug("Service: Setting available limit of {} for new account",
          createAccountDTO.getAvailableLimit());
    }

    Account savedAccount = metricsService.recordRepositoryExecutionTime(
        ACCOUNT_REPOSITORY_METRIC, "save",
        () -> accountRepository.save(account));

    log.info(
        "Service: Account created successfully for merchant ID: {}, account number: {}, type: {}",
        merchant.getId(), savedAccount.getAccountNumber(), savedAccount.getType());

    // Registrar métricas
    metricsService.recordAccountCreated(savedAccount.getType().toString(), "MERCHANT");

    // Registrar o depósito inicial se houver
    if (createAccountDTO.getInitialDeposit() != null
        && createAccountDTO.getInitialDeposit().compareTo(BigDecimal.ZERO) > 0) {
      metricsService.recordTransactionProcessed(DEPOSIT_METRIC,
          createAccountDTO.getInitialDeposit(), true);
      metricsService.recordDailyFinancialVolume(DEPOSIT_METRIC,
          createAccountDTO.getInitialDeposit());
    }

    return convertToDTO(savedAccount);
  }

  @Transactional
  public AccountDTO deposit(String accountNumber, BigDecimal amount) {
    log.debug("Service: Processing deposit of {} to account: {}", amount, accountNumber);

    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      log.warn("Service: Invalid deposit amount: {}", amount);
      metricsService.recordExceptionOccurred("IllegalArgumentException", "deposit");
      throw new IllegalArgumentException("Deposit amount must be positive");
    }

    Account account = metricsService.recordRepositoryExecutionTime(
        ACCOUNT_REPOSITORY_METRIC, "findByAccountNumberWithLock",
        () -> accountRepository.findByAccountNumberWithLock(accountNumber)
            .orElseThrow(() -> {
              log.error(ACCOUNT_NOT_FOUND, accountNumber);
              metricsService.recordExceptionOccurred(RESOURCE_NOT_FOUND, "deposit");
              return new ResourceNotFoundException(
                  ACCOUNT_NUMBER_NOT_FOUND + accountNumber);
            }));

    BigDecimal previousBalance = account.getBalance();
    account.setBalance(previousBalance.add(amount));
    Account updatedAccount = metricsService.recordRepositoryExecutionTime(
        ACCOUNT_REPOSITORY_METRIC, "save",
        () -> accountRepository.save(account));

    log.info(
        "Service: Deposit successful for account: {}, amount: {}, previous balance: {}, new balance: {}",
        accountNumber, amount, previousBalance, updatedAccount.getBalance());

    // Registrar métricas de depósito
    metricsService.recordTransactionProcessed(DEPOSIT_METRIC, amount, true);
    metricsService.recordDailyFinancialVolume(DEPOSIT_METRIC, amount);

    return convertToDTO(updatedAccount);
  }

  @Transactional
  public AccountDTO withdraw(String accountNumber, BigDecimal amount) {
    log.debug("Service: Processing withdrawal of {} from account: {}", amount, accountNumber);

    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      log.warn("Service: Invalid withdrawal amount: {}", amount);
      metricsService.recordExceptionOccurred("IllegalArgumentException", WITHDRAWAL);
      throw new IllegalArgumentException("Withdrawal amount must be positive");
    }

    Account account = metricsService.recordRepositoryExecutionTime(
        ACCOUNT_REPOSITORY_METRIC, "findByAccountNumberWithLock",
        () -> accountRepository.findByAccountNumberWithLock(accountNumber)
            .orElseThrow(() -> {
              log.error(ACCOUNT_NOT_FOUND, accountNumber);
              metricsService.recordExceptionOccurred(RESOURCE_NOT_FOUND, WITHDRAWAL);
              return new ResourceNotFoundException(
                  ACCOUNT_NUMBER_NOT_FOUND + accountNumber);
            }));

    if (account.getBalance().compareTo(amount) < 0) {
      log.warn(
          "Service: Insufficient funds for withdrawal. Account: {}, balance: {}, requested amount: {}",
          accountNumber, account.getBalance(), amount);
      metricsService.recordExceptionOccurred("InsufficientFundsException", WITHDRAWAL);
      metricsService.recordTransactionProcessed(WITHDRAWAL_METRIC, amount, false);
      throw new InsufficientFundsException("Insufficient funds for withdrawal");
    }

    BigDecimal previousBalance = account.getBalance();
    account.setBalance(previousBalance.subtract(amount));
    Account updatedAccount = metricsService.recordRepositoryExecutionTime(
        ACCOUNT_REPOSITORY_METRIC, "save",
        () -> accountRepository.save(account));

    log.info(
        "Service: Withdrawal successful for account: {}, amount: {}, previous balance: {}, new balance: {}",
        accountNumber, amount, previousBalance, updatedAccount.getBalance());

    // Registrar métricas de saque
    metricsService.recordTransactionProcessed(WITHDRAWAL_METRIC, amount, true);
    metricsService.recordDailyFinancialVolume(WITHDRAWAL_METRIC, amount);

    return convertToDTO(updatedAccount);
  }

  @Transactional
  public AccountDTO updateAccountStatus(Long id, Account.AccountStatus status) {
    log.debug("Service: Updating status of account with ID: {} to {}", id, status);

    Account account = metricsService.recordRepositoryExecutionTime(
        ACCOUNT_REPOSITORY_METRIC, FIND_BY_ID,
        () -> accountRepository.findById(id)
            .orElseThrow(() -> {
              log.error("Service: Account not found with ID: {}", id);
              metricsService.recordExceptionOccurred(RESOURCE_NOT_FOUND,
                  "updateAccountStatus");
              return new ResourceNotFoundException("Account not found with id: " + id);
            }));

    Account.AccountStatus previousStatus = account.getStatus();
    account.setStatus(status);
    Account updatedAccount = metricsService.recordRepositoryExecutionTime(
        ACCOUNT_REPOSITORY_METRIC, "save",
        () -> accountRepository.save(account));

    log.info(
        "Service: Account status updated successfully. Account ID: {}, previous status: {}, new status: {}",
        id, previousStatus, status);

    // Registrar métrica de atualização de status
    metricsService.recordAccountStatusUpdate(previousStatus.toString(), status.toString());

    return convertToDTO(updatedAccount);
  }

  // Helper methods for DTO conversion
  private AccountDTO convertToDTO(Account account) {
    AccountDTO dto = new AccountDTO();
    dto.setId(account.getId());
    dto.setAccountNumber(account.getAccountNumber());
    dto.setType(account.getType());
    dto.setBalance(account.getBalance());
    dto.setAvailableLimit(account.getAvailableLimit());
    dto.setStatus(account.getStatus());
    dto.setCreatedAt(account.getCreatedAt());

    if (account.getClient() != null) {
      dto.setOwnerId(account.getClient().getId());
      dto.setOwnerName(account.getClient().getName());
      dto.setOwnerType("CLIENT");
    } else if (account.getMerchant() != null) {
      dto.setOwnerId(account.getMerchant().getId());
      dto.setOwnerName(account.getMerchant().getBusinessName());
      dto.setOwnerType("MERCHANT");
    }

    return dto;
  }
}
