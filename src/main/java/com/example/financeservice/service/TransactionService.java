package com.example.financeservice.service;

import com.example.financeservice.dto.StatementDTO;
import com.example.financeservice.dto.TransactionDTO;
import com.example.financeservice.dto.TransferDTO;
import com.example.financeservice.exception.InsufficientFundsException;
import com.example.financeservice.exception.InvalidTransactionException;
import com.example.financeservice.exception.ResourceNotFoundException;
import com.example.financeservice.model.Account;
import com.example.financeservice.model.Transaction;
import com.example.financeservice.repository.AccountRepository;
import com.example.financeservice.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

  private final TransactionRepository transactionRepository;
  private final AccountRepository accountRepository;
  private final MetricsService metricsService;

  @Transactional(readOnly = true)
  public List<TransactionDTO> getAllTransactions() {
    log.debug("Service: Getting all transactions");

    List<Transaction> transactions = metricsService.recordRepositoryExecutionTime(
        "TransactionRepository", "findAll",
        () -> transactionRepository.findAll());

    log.debug("Service: Found {} transactions in database", transactions.size());

    return transactions.stream()
        .map(this::convertToDTO)
        .toList();
  }

  @Transactional(readOnly = true)
  public TransactionDTO getTransactionById(Long id) {
    log.debug("Service: Getting transaction with ID: {}", id);

    Transaction transaction = metricsService.recordRepositoryExecutionTime(
        "TransactionRepository", "findById",
        () -> transactionRepository.findById(id)
            .orElseThrow(() -> {
              log.error("Service: Transaction not found with ID: {}", id);
              metricsService.recordExceptionOccurred("ResourceNotFoundException", "getTransactionById");
              return new ResourceNotFoundException("Transaction not found with id: " + id);
            }));

    log.debug("Service: Found transaction with ID: {}, type: {}, amount: {}",
        id, transaction.getType(), transaction.getAmount());
    return convertToDTO(transaction);
  }

  @Transactional(readOnly = true)
  public TransactionDTO getTransactionByTransactionId(String transactionId) {
    log.debug("Service: Getting transaction with transaction ID: {}", transactionId);

    Transaction transaction = metricsService.recordRepositoryExecutionTime(
        "TransactionRepository", "findByTransactionId",
        () -> transactionRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> {
              log.error("Service: Transaction not found with transaction ID: {}", transactionId);
              metricsService.recordExceptionOccurred("ResourceNotFoundException", "getTransactionByTransactionId");
              return new ResourceNotFoundException(
                  "Transaction not found with transaction id: " + transactionId);
            }));

    log.debug("Service: Found transaction with transaction ID: {}, type: {}, amount: {}",
        transactionId, transaction.getType(), transaction.getAmount());
    return convertToDTO(transaction);
  }

  @Transactional(readOnly = true)
  public List<TransactionDTO> getTransactionsByAccount(String accountNumber) {
    log.debug("Service: Getting transactions for account: {}", accountNumber);

    Account account = metricsService.recordRepositoryExecutionTime(
        "AccountRepository", "findByAccountNumber",
        () -> accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> {
              log.error("Service: Account not found with number: {}", accountNumber);
              metricsService.recordExceptionOccurred("ResourceNotFoundException", "getTransactionsByAccount");
              return new ResourceNotFoundException("Account not found with number: " + accountNumber);
            }));

    List<Transaction> transactions = metricsService.recordRepositoryExecutionTime(
        "TransactionRepository", "findByAccount",
        () -> transactionRepository.findByAccount(account));

    log.debug("Service: Found {} transactions for account: {}", transactions.size(), accountNumber);

    return transactions.stream()
        .map(this::convertToDTO)
        .toList();
  }

  @Transactional(readOnly = true)
  public Page<TransactionDTO> getTransactionsByAccountPaginated(String accountNumber,
      Pageable pageable) {
    log.debug("Service: Getting paginated transactions for account: {}, page: {}, size: {}",
        accountNumber, pageable.getPageNumber(), pageable.getPageSize());

    Account account = metricsService.recordRepositoryExecutionTime(
        "AccountRepository", "findByAccountNumber",
        () -> accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> {
              log.error("Service: Account not found with number: {}", accountNumber);
              metricsService.recordExceptionOccurred("ResourceNotFoundException", "getTransactionsByAccountPaginated");
              return new ResourceNotFoundException("Account not found with number: " + accountNumber);
            }));

    Page<Transaction> transactions = metricsService.recordRepositoryExecutionTime(
        "TransactionRepository", "findByAccountPaginated",
        () -> transactionRepository.findByAccountPaginated(account, pageable));

    log.debug("Service: Found page {} of {} for account: {}, total elements: {}",
        pageable.getPageNumber(), pageable.getPageSize(), accountNumber,
        transactions.getTotalElements());

    return transactions.map(this::convertToDTO);
  }

  @Transactional(readOnly = true)
  public StatementDTO getAccountStatement(String accountNumber, LocalDateTime startDate,
      LocalDateTime endDate) {
    log.debug("Service: Generating statement for account: {} from {} to {}",
        accountNumber, startDate, endDate);

    Account account = metricsService.recordRepositoryExecutionTime(
        "AccountRepository", "findByAccountNumber",
        () -> accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> {
              log.error("Service: Account not found with number: {}", accountNumber);
              metricsService.recordExceptionOccurred("ResourceNotFoundException", "getAccountStatement");
              return new ResourceNotFoundException("Account not found with number: " + accountNumber);
            }));

    List<Transaction> transactions = metricsService.recordRepositoryExecutionTime(
        "TransactionRepository", "findByAccountAndDateRange",
        () -> transactionRepository.findByAccountAndDateRange(account, startDate, endDate));

    log.debug("Service: Found {} transactions for account: {} between {} and {}",
        transactions.size(), accountNumber, startDate, endDate);

    StatementDTO statement = new StatementDTO();
    statement.setAccountNumber(account.getAccountNumber());
    statement.setAccountType(account.getType());
    statement.setCurrentBalance(account.getBalance());
    statement.setStatementStartDate(startDate);
    statement.setStatementEndDate(endDate);
    statement.setGeneratedAt(LocalDateTime.now());

    statement.setTransactions(transactions.stream()
        .map(this::convertToDTO)
        .toList());

    log.info("Service: Successfully generated statement for account: {} with {} transactions",
        accountNumber, transactions.size());

    return statement;
  }

  @Transactional
  public TransactionDTO transfer(TransferDTO transferDTO) {
    log.debug("Service: Processing transfer of {} from account: {} to account: {}",
        transferDTO.getAmount(), transferDTO.getSourceAccountNumber(),
        transferDTO.getDestinationAccountNumber());

    // Iniciar timer para acompanhar tempo total da transferência
    var timer = metricsService.startTimer();

    try {
      // Validações
      if (transferDTO.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
        log.warn("Service: Invalid transfer amount: {}", transferDTO.getAmount());
        metricsService.recordExceptionOccurred("InvalidTransactionException", "transfer");
        metricsService.recordTransactionProcessed("TRANSFER", transferDTO.getAmount(), false);
        throw new InvalidTransactionException("Transfer amount must be positive");
      }

      if (transferDTO.getSourceAccountNumber().equals(transferDTO.getDestinationAccountNumber())) {
        log.warn("Service: Source and destination accounts are the same: {}",
            transferDTO.getSourceAccountNumber());
        metricsService.recordExceptionOccurred("InvalidTransactionException", "transfer");
        metricsService.recordTransactionProcessed("TRANSFER", transferDTO.getAmount(), false);
        throw new InvalidTransactionException("Source and destination accounts cannot be the same");
      }

      // Get accounts with lock to prevent concurrent modifications
      Account sourceAccount = metricsService.recordRepositoryExecutionTime(
          "AccountRepository", "findByAccountNumberWithLock",
          () -> accountRepository.findByAccountNumberWithLock(
                  transferDTO.getSourceAccountNumber())
              .orElseThrow(() -> {
                log.error("Service: Source account not found with number: {}",
                    transferDTO.getSourceAccountNumber());
                metricsService.recordExceptionOccurred("ResourceNotFoundException", "transfer");
                return new ResourceNotFoundException(
                    "Source account not found with number: " + transferDTO.getSourceAccountNumber());
              }));

      Account destinationAccount = metricsService.recordRepositoryExecutionTime(
          "AccountRepository", "findByAccountNumberWithLock",
          () -> accountRepository.findByAccountNumberWithLock(
                  transferDTO.getDestinationAccountNumber())
              .orElseThrow(() -> {
                log.error("Service: Destination account not found with number: {}",
                    transferDTO.getDestinationAccountNumber());
                metricsService.recordExceptionOccurred("ResourceNotFoundException", "transfer");
                return new ResourceNotFoundException("Destination account not found with number: "
                    + transferDTO.getDestinationAccountNumber());
              }));

      // Check if accounts are active
      if (sourceAccount.getStatus() != Account.AccountStatus.ACTIVE) {
        log.warn("Service: Source account is not active. Account: {}, Status: {}",
            transferDTO.getSourceAccountNumber(), sourceAccount.getStatus());
        metricsService.recordExceptionOccurred("InvalidTransactionException", "transfer");
        metricsService.recordTransactionProcessed("TRANSFER", transferDTO.getAmount(), false);
        throw new InvalidTransactionException("Source account is not active");
      }

      if (destinationAccount.getStatus() != Account.AccountStatus.ACTIVE) {
        log.warn("Service: Destination account is not active. Account: {}, Status: {}",
            transferDTO.getDestinationAccountNumber(), destinationAccount.getStatus());
        metricsService.recordExceptionOccurred("InvalidTransactionException", "transfer");
        metricsService.recordTransactionProcessed("TRANSFER", transferDTO.getAmount(), false);
        throw new InvalidTransactionException("Destination account is not active");
      }

      // Check if source account has sufficient funds
      if (sourceAccount.getBalance().compareTo(transferDTO.getAmount()) < 0) {
        log.warn(
            "Service: Insufficient funds in source account. Account: {}, Balance: {}, Requested amount: {}",
            transferDTO.getSourceAccountNumber(), sourceAccount.getBalance(),
            transferDTO.getAmount());
        metricsService.recordExceptionOccurred("InsufficientFundsException", "transfer");
        metricsService.recordTransactionProcessed("TRANSFER", transferDTO.getAmount(), false);
        throw new InsufficientFundsException("Insufficient funds in source account");
      }

      // Create transaction
      Transaction transaction = new Transaction();
      transaction.setAmount(transferDTO.getAmount());
      transaction.setType(Transaction.TransactionType.TRANSFER);
      transaction.setDescription(transferDTO.getDescription());
      transaction.setSourceAccount(sourceAccount);
      transaction.setDestinationAccount(destinationAccount);
      transaction.setStatus(Transaction.TransactionStatus.PENDING);

      // Save transaction
      Transaction savedTransaction = metricsService.recordRepositoryExecutionTime(
          "TransactionRepository", "save",
          () -> transactionRepository.save(transaction));

      log.debug("Service: Created pending transaction with ID: {}, transaction ID: {}",
          savedTransaction.getId(), savedTransaction.getTransactionId());

      try {
        // Update account balances
        BigDecimal sourceBalanceBefore = sourceAccount.getBalance();
        BigDecimal destBalanceBefore = destinationAccount.getBalance();

        sourceAccount.setBalance(sourceAccount.getBalance().subtract(transferDTO.getAmount()));
        destinationAccount.setBalance(destinationAccount.getBalance().add(transferDTO.getAmount()));

        metricsService.recordRepositoryExecutionTime(
            "AccountRepository", "save",
            () -> accountRepository.save(sourceAccount));

        metricsService.recordRepositoryExecutionTime(
            "AccountRepository", "save",
            () -> accountRepository.save(destinationAccount));

        log.debug(
            "Service: Updated account balances. Source account: {} ({} -> {}), Destination account: {} ({} -> {})",
            sourceAccount.getAccountNumber(), sourceBalanceBefore, sourceAccount.getBalance(),
            destinationAccount.getAccountNumber(), destBalanceBefore,
            destinationAccount.getBalance());

        // Update transaction status
        savedTransaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        Transaction finalSavedTransaction = savedTransaction;
        savedTransaction = metricsService.recordRepositoryExecutionTime(
            "TransactionRepository", "save",
            () -> transactionRepository.save(finalSavedTransaction));

        log.info(
            "Service: Transfer completed successfully. Transaction ID: {}, Amount: {}, From: {}, To: {}",
            savedTransaction.getTransactionId(), transferDTO.getAmount(),
            transferDTO.getSourceAccountNumber(), transferDTO.getDestinationAccountNumber());

        // Registrar métricas para transferência bem-sucedida
        metricsService.recordTransactionProcessed("TRANSFER", transferDTO.getAmount(), true);
        metricsService.recordDailyFinancialVolume("TRANSFER", transferDTO.getAmount());

        // Parar o timer e registrar o tempo total da operação
        metricsService.stopTimer(timer, "finance.operations.transfer.time",
            "source", transferDTO.getSourceAccountNumber(),
            "destination", transferDTO.getDestinationAccountNumber());

        return convertToDTO(savedTransaction);
      } catch (Exception e) {
        // If any exception occurs, mark transaction as failed
        savedTransaction.setStatus(Transaction.TransactionStatus.FAILED);
        transactionRepository.save(savedTransaction);

        log.error("Service: Transfer failed. Transaction ID: {}, Amount: {}, From: {}, To: {}",
            savedTransaction.getTransactionId(), transferDTO.getAmount(),
            transferDTO.getSourceAccountNumber(), transferDTO.getDestinationAccountNumber(), e);

        // Registrar métricas para transferência com falha
        metricsService.recordTransactionProcessed("TRANSFER", transferDTO.getAmount(), false);
        metricsService.recordExceptionOccurred(e.getClass().getSimpleName(), "transfer");

        throw e;
      }
    } finally {
      // Garantir que o timer seja parado mesmo em caso de exceção
      if (timer != null) {
        metricsService.stopTimer(timer, "finance.operations.transfer.time");
      }
    }
  }

  @Transactional
  public TransactionDTO deposit(String accountNumber, BigDecimal amount, String description) {
    log.debug("Service: Processing deposit of {} to account: {}", amount, accountNumber);

    // Iniciar timer para acompanhar tempo total do depósito
    var timer = metricsService.startTimer();

    try {
      if (amount.compareTo(BigDecimal.ZERO) <= 0) {
        log.warn("Service: Invalid deposit amount: {}", amount);
        metricsService.recordExceptionOccurred("InvalidTransactionException", "deposit");
        metricsService.recordTransactionProcessed("DEPOSIT", amount, false);
        throw new InvalidTransactionException("Deposit amount must be positive");
      }

      Account account = metricsService.recordRepositoryExecutionTime(
          "AccountRepository", "findByAccountNumberWithLock",
          () -> accountRepository.findByAccountNumberWithLock(accountNumber)
              .orElseThrow(() -> {
                log.error("Service: Account not found with number: {}", accountNumber);
                metricsService.recordExceptionOccurred("ResourceNotFoundException", "deposit");
                return new ResourceNotFoundException("Account not found with number: " + accountNumber);
              }));

      if (account.getStatus() != Account.AccountStatus.ACTIVE) {
        log.warn("Service: Account is not active. Account: {}, Status: {}", accountNumber,
            account.getStatus());
        metricsService.recordExceptionOccurred("InvalidTransactionException", "deposit");
        metricsService.recordTransactionProcessed("DEPOSIT", amount, false);
        throw new InvalidTransactionException("Account is not active");
      }

      // Create transaction
      Transaction transaction = new Transaction();
      transaction.setAmount(amount);
      transaction.setType(Transaction.TransactionType.DEPOSIT);
      transaction.setDescription(description);
      transaction.setDestinationAccount(account);
      transaction.setStatus(Transaction.TransactionStatus.PENDING);

      // Save transaction
      Transaction savedTransaction = metricsService.recordRepositoryExecutionTime(
          "TransactionRepository", "save",
          () -> transactionRepository.save(transaction));

      log.debug("Service: Created pending deposit transaction with ID: {}, transaction ID: {}",
          savedTransaction.getId(), savedTransaction.getTransactionId());

      try {
        // Update account balance
        BigDecimal balanceBefore = account.getBalance();
        account.setBalance(account.getBalance().add(amount));

        metricsService.recordRepositoryExecutionTime(
            "AccountRepository", "save",
            () -> accountRepository.save(account));

        log.debug("Service: Updated account balance. Account: {} ({} -> {})",
            account.getAccountNumber(), balanceBefore, account.getBalance());

        // Update transaction status
        savedTransaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        Transaction finalSavedTransaction = savedTransaction;
        savedTransaction = metricsService.recordRepositoryExecutionTime(
            "TransactionRepository", "save",
            () -> transactionRepository.save(finalSavedTransaction));

        log.info(
            "Service: Deposit completed successfully. Transaction ID: {}, Amount: {}, To account: {}",
            savedTransaction.getTransactionId(), amount, accountNumber);

        // Registrar métricas para depósito bem-sucedido
        metricsService.recordTransactionProcessed("DEPOSIT", amount, true);
        metricsService.recordDailyFinancialVolume("DEPOSIT", amount);

        // Parar o timer e registrar o tempo total da operação
        metricsService.stopTimer(timer, "finance.operations.deposit.time",
            "account", accountNumber);

        return convertToDTO(savedTransaction);
      } catch (Exception e) {
        // If any exception occurs, mark transaction as failed
        savedTransaction.setStatus(Transaction.TransactionStatus.FAILED);
        transactionRepository.save(savedTransaction);

        log.error("Service: Deposit failed. Transaction ID: {}, Amount: {}, To account: {}",
            savedTransaction.getTransactionId(), amount, accountNumber, e);

        // Registrar métricas para depósito com falha
        metricsService.recordTransactionProcessed("DEPOSIT", amount, false);
        metricsService.recordExceptionOccurred(e.getClass().getSimpleName(), "deposit");

        throw e;
      }
    } finally {
      // Garantir que o timer seja parado mesmo em caso de exceção
      if (timer != null) {
        metricsService.stopTimer(timer, "finance.operations.deposit.time");
      }
    }
  }

  @Transactional
  public TransactionDTO withdraw(String accountNumber, BigDecimal amount, String description) {
    log.debug("Service: Processing withdrawal of {} from account: {}", amount, accountNumber);

    // Iniciar timer para acompanhar tempo total do saque
    var timer = metricsService.startTimer();

    try {
      if (amount.compareTo(BigDecimal.ZERO) <= 0) {
        log.warn("Service: Invalid withdrawal amount: {}", amount);
        metricsService.recordExceptionOccurred("InvalidTransactionException", "withdraw");
        metricsService.recordTransactionProcessed("WITHDRAWAL", amount, false);
        throw new InvalidTransactionException("Withdrawal amount must be positive");
      }

      Account account = metricsService.recordRepositoryExecutionTime(
          "AccountRepository", "findByAccountNumberWithLock",
          () -> accountRepository.findByAccountNumberWithLock(accountNumber)
              .orElseThrow(() -> {
                log.error("Service: Account not found with number: {}", accountNumber);
                metricsService.recordExceptionOccurred("ResourceNotFoundException", "withdraw");
                return new ResourceNotFoundException("Account not found with number: " + accountNumber);
              }));

      if (account.getStatus() != Account.AccountStatus.ACTIVE) {
        log.warn("Service: Account is not active. Account: {}, Status: {}", accountNumber,
            account.getStatus());
        metricsService.recordExceptionOccurred("InvalidTransactionException", "withdraw");
        metricsService.recordTransactionProcessed("WITHDRAWAL", amount, false);
        throw new InvalidTransactionException("Account is not active");
      }

      if (account.getBalance().compareTo(amount) < 0) {
        log.warn(
            "Service: Insufficient funds for withdrawal. Account: {}, Balance: {}, Requested amount: {}",
            accountNumber, account.getBalance(), amount);
        metricsService.recordExceptionOccurred("InsufficientFundsException", "withdraw");
        metricsService.recordTransactionProcessed("WITHDRAWAL", amount, false);
        throw new InsufficientFundsException("Insufficient funds for withdrawal");
      }

      // Create transaction
      Transaction transaction = new Transaction();
      transaction.setAmount(amount);
      transaction.setType(Transaction.TransactionType.WITHDRAWAL);
      transaction.setDescription(description);
      transaction.setSourceAccount(account);
      transaction.setStatus(Transaction.TransactionStatus.PENDING);

      // Save transaction
      Transaction savedTransaction = metricsService.recordRepositoryExecutionTime(
          "TransactionRepository", "save",
          () -> transactionRepository.save(transaction));

      log.debug("Service: Created pending withdrawal transaction with ID: {}, transaction ID: {}",
          savedTransaction.getId(), savedTransaction.getTransactionId());

      try {
        // Update account balance
        BigDecimal balanceBefore = account.getBalance();
        account.setBalance(account.getBalance().subtract(amount));

        metricsService.recordRepositoryExecutionTime(
            "AccountRepository", "save",
            () -> accountRepository.save(account));

        log.debug("Service: Updated account balance. Account: {} ({} -> {})",
            account.getAccountNumber(), balanceBefore, account.getBalance());

        // Update transaction status
        savedTransaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        Transaction finalSavedTransaction = savedTransaction;
        savedTransaction = metricsService.recordRepositoryExecutionTime(
            "TransactionRepository", "save",
            () -> transactionRepository.save(finalSavedTransaction));

        log.info(
            "Service: Withdrawal completed successfully. Transaction ID: {}, Amount: {}, From account: {}",
            savedTransaction.getTransactionId(), amount, accountNumber);

        // Registrar métricas para saque bem-sucedido
        metricsService.recordTransactionProcessed("WITHDRAWAL", amount, true);
        metricsService.recordDailyFinancialVolume("WITHDRAWAL", amount);

        // Parar o timer e registrar o tempo total da operação
        metricsService.stopTimer(timer, "finance.operations.withdrawal.time",
            "account", accountNumber);

        return convertToDTO(savedTransaction);
      } catch (Exception e) {
        // If any exception occurs, mark transaction as failed
        savedTransaction.setStatus(Transaction.TransactionStatus.FAILED);
        transactionRepository.save(savedTransaction);

        log.error("Service: Withdrawal failed. Transaction ID: {}, Amount: {}, From account: {}",
            savedTransaction.getTransactionId(), amount, accountNumber, e);

        // Registrar métricas para saque com falha
        metricsService.recordTransactionProcessed("WITHDRAWAL", amount, false);
        metricsService.recordExceptionOccurred(e.getClass().getSimpleName(), "withdraw");

        throw e;
      }
    } finally {
      // Garantir que o timer seja parado mesmo em caso de exceção
      if (timer != null) {
        metricsService.stopTimer(timer, "finance.operations.withdrawal.time");
      }
    }
  }

  // Helper methods for DTO conversion
  private TransactionDTO convertToDTO(Transaction transaction) {
    TransactionDTO dto = new TransactionDTO();
    dto.setId(transaction.getId());
    dto.setTransactionId(transaction.getTransactionId());
    dto.setAmount(transaction.getAmount());
    dto.setType(transaction.getType());
    dto.setDescription(transaction.getDescription());
    dto.setStatus(transaction.getStatus());
    dto.setCreatedAt(transaction.getCreatedAt());
    dto.setProcessedAt(transaction.getProcessedAt());

    if (transaction.getSourceAccount() != null) {
      dto.setSourceAccountNumber(transaction.getSourceAccount().getAccountNumber());
    }

    if (transaction.getDestinationAccount() != null) {
      dto.setDestinationAccountNumber(transaction.getDestinationAccount().getAccountNumber());
    }

    return dto;
  }
}