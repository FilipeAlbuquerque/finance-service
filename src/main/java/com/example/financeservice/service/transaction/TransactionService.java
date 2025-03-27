package com.example.financeservice.service.transaction;

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

  @Transactional(readOnly = true)
  public List<TransactionDTO> getAllTransactions() {
    log.debug("Service: Getting all transactions");

    List<Transaction> transactions = transactionRepository.findAll();
    log.debug("Service: Found {} transactions in database", transactions.size());

    return transactions.stream()
        .map(this::convertToDTO)
        .toList();
  }

  @Transactional(readOnly = true)
  public TransactionDTO getTransactionById(Long id) {
    log.debug("Service: Getting transaction with ID: {}", id);

    Transaction transaction = transactionRepository.findById(id)
        .orElseThrow(() -> {
          log.error("Service: Transaction not found with ID: {}", id);
          return new ResourceNotFoundException("Transaction not found with id: " + id);
        });

    log.debug("Service: Found transaction with ID: {}, type: {}, amount: {}",
        id, transaction.getType(), transaction.getAmount());
    return convertToDTO(transaction);
  }

  @Transactional(readOnly = true)
  public TransactionDTO getTransactionByTransactionId(String transactionId) {
    log.debug("Service: Getting transaction with transaction ID: {}", transactionId);

    Transaction transaction = transactionRepository.findByTransactionId(transactionId)
        .orElseThrow(() -> {
          log.error("Service: Transaction not found with transaction ID: {}", transactionId);
          return new ResourceNotFoundException(
              "Transaction not found with transaction id: " + transactionId);
        });

    log.debug("Service: Found transaction with transaction ID: {}, type: {}, amount: {}",
        transactionId, transaction.getType(), transaction.getAmount());
    return convertToDTO(transaction);
  }

  @Transactional(readOnly = true)
  public List<TransactionDTO> getTransactionsByAccount(String accountNumber) {
    log.debug("Service: Getting transactions for account: {}", accountNumber);

    Account account = accountRepository.findByAccountNumber(accountNumber)
        .orElseThrow(() -> {
          log.error("Service: Account not found with number: {}", accountNumber);
          return new ResourceNotFoundException("Account not found with number: " + accountNumber);
        });

    List<Transaction> transactions = transactionRepository.findByAccount(account);
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

    Account account = accountRepository.findByAccountNumber(accountNumber)
        .orElseThrow(() -> {
          log.error("Service: Account not found with number: {}", accountNumber);
          return new ResourceNotFoundException("Account not found with number: " + accountNumber);
        });

    Page<Transaction> transactions = transactionRepository.findByAccountPaginated(account,
        pageable);
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

    Account account = accountRepository.findByAccountNumber(accountNumber)
        .orElseThrow(() -> {
          log.error("Service: Account not found with number: {}", accountNumber);
          return new ResourceNotFoundException("Account not found with number: " + accountNumber);
        });

    List<Transaction> transactions = transactionRepository.findByAccountAndDateRange(account,
        startDate, endDate);
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

    // Validations
    if (transferDTO.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
      log.warn("Service: Invalid transfer amount: {}", transferDTO.getAmount());
      throw new InvalidTransactionException("Transfer amount must be positive");
    }

    if (transferDTO.getSourceAccountNumber().equals(transferDTO.getDestinationAccountNumber())) {
      log.warn("Service: Source and destination accounts are the same: {}",
          transferDTO.getSourceAccountNumber());
      throw new InvalidTransactionException("Source and destination accounts cannot be the same");
    }

    // Get accounts with lock to prevent concurrent modifications
    Account sourceAccount = accountRepository.findByAccountNumberWithLock(
            transferDTO.getSourceAccountNumber())
        .orElseThrow(() -> {
          log.error("Service: Source account not found with number: {}",
              transferDTO.getSourceAccountNumber());
          return new ResourceNotFoundException(
              "Source account not found with number: " + transferDTO.getSourceAccountNumber());
        });

    Account destinationAccount = accountRepository.findByAccountNumberWithLock(
            transferDTO.getDestinationAccountNumber())
        .orElseThrow(() -> {
          log.error("Service: Destination account not found with number: {}",
              transferDTO.getDestinationAccountNumber());
          return new ResourceNotFoundException("Destination account not found with number: "
              + transferDTO.getDestinationAccountNumber());
        });

    // Check if accounts are active
    if (sourceAccount.getStatus() != Account.AccountStatus.ACTIVE) {
      log.warn("Service: Source account is not active. Account: {}, Status: {}",
          transferDTO.getSourceAccountNumber(), sourceAccount.getStatus());
      throw new InvalidTransactionException("Source account is not active");
    }

    if (destinationAccount.getStatus() != Account.AccountStatus.ACTIVE) {
      log.warn("Service: Destination account is not active. Account: {}, Status: {}",
          transferDTO.getDestinationAccountNumber(), destinationAccount.getStatus());
      throw new InvalidTransactionException("Destination account is not active");
    }

    // Check if source account has sufficient funds
    if (sourceAccount.getBalance().compareTo(transferDTO.getAmount()) < 0) {
      log.warn(
          "Service: Insufficient funds in source account. Account: {}, Balance: {}, Requested amount: {}",
          transferDTO.getSourceAccountNumber(), sourceAccount.getBalance(),
          transferDTO.getAmount());
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
    Transaction savedTransaction = transactionRepository.save(transaction);
    log.debug("Service: Created pending transaction with ID: {}, transaction ID: {}",
        savedTransaction.getId(), savedTransaction.getTransactionId());

    try {
      // Update account balances
      BigDecimal sourceBalanceBefore = sourceAccount.getBalance();
      BigDecimal destBalanceBefore = destinationAccount.getBalance();

      sourceAccount.setBalance(sourceAccount.getBalance().subtract(transferDTO.getAmount()));
      destinationAccount.setBalance(destinationAccount.getBalance().add(transferDTO.getAmount()));

      accountRepository.save(sourceAccount);
      accountRepository.save(destinationAccount);

      log.debug(
          "Service: Updated account balances. Source account: {} ({} -> {}), Destination account: {} ({} -> {})",
          sourceAccount.getAccountNumber(), sourceBalanceBefore, sourceAccount.getBalance(),
          destinationAccount.getAccountNumber(), destBalanceBefore,
          destinationAccount.getBalance());

      // Update transaction status
      savedTransaction.setStatus(Transaction.TransactionStatus.COMPLETED);
      savedTransaction = transactionRepository.save(savedTransaction);

      log.info(
          "Service: Transfer completed successfully. Transaction ID: {}, Amount: {}, From: {}, To: {}",
          savedTransaction.getTransactionId(), transferDTO.getAmount(),
          transferDTO.getSourceAccountNumber(), transferDTO.getDestinationAccountNumber());

      return convertToDTO(savedTransaction);
    } catch (Exception e) {
      // If any exception occurs, mark transaction as failed
      savedTransaction.setStatus(Transaction.TransactionStatus.FAILED);
      transactionRepository.save(savedTransaction);

      log.error("Service: Transfer failed. Transaction ID: {}, Amount: {}, From: {}, To: {}",
          savedTransaction.getTransactionId(), transferDTO.getAmount(),
          transferDTO.getSourceAccountNumber(), transferDTO.getDestinationAccountNumber(), e);

      throw e;
    }
  }

  @Transactional
  public TransactionDTO deposit(String accountNumber, BigDecimal amount, String description) {
    log.debug("Service: Processing deposit of {} to account: {}", amount, accountNumber);

    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      log.warn("Service: Invalid deposit amount: {}", amount);
      throw new InvalidTransactionException("Deposit amount must be positive");
    }

    Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
        .orElseThrow(() -> {
          log.error("Service: Account not found with number: {}", accountNumber);
          return new ResourceNotFoundException("Account not found with number: " + accountNumber);
        });

    if (account.getStatus() != Account.AccountStatus.ACTIVE) {
      log.warn("Service: Account is not active. Account: {}, Status: {}", accountNumber,
          account.getStatus());
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
    Transaction savedTransaction = transactionRepository.save(transaction);
    log.debug("Service: Created pending deposit transaction with ID: {}, transaction ID: {}",
        savedTransaction.getId(), savedTransaction.getTransactionId());

    try {
      // Update account balance
      BigDecimal balanceBefore = account.getBalance();
      account.setBalance(account.getBalance().add(amount));
      accountRepository.save(account);

      log.debug("Service: Updated account balance. Account: {} ({} -> {})",
          account.getAccountNumber(), balanceBefore, account.getBalance());

      // Update transaction status
      savedTransaction.setStatus(Transaction.TransactionStatus.COMPLETED);
      savedTransaction = transactionRepository.save(savedTransaction);

      log.info(
          "Service: Deposit completed successfully. Transaction ID: {}, Amount: {}, To account: {}",
          savedTransaction.getTransactionId(), amount, accountNumber);

      return convertToDTO(savedTransaction);
    } catch (Exception e) {
      // If any exception occurs, mark transaction as failed
      savedTransaction.setStatus(Transaction.TransactionStatus.FAILED);
      transactionRepository.save(savedTransaction);

      log.error("Service: Deposit failed. Transaction ID: {}, Amount: {}, To account: {}",
          savedTransaction.getTransactionId(), amount, accountNumber, e);

      throw e;
    }
  }

  @Transactional
  public TransactionDTO withdraw(String accountNumber, BigDecimal amount, String description) {
    log.debug("Service: Processing withdrawal of {} from account: {}", amount, accountNumber);

    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      log.warn("Service: Invalid withdrawal amount: {}", amount);
      throw new InvalidTransactionException("Withdrawal amount must be positive");
    }

    Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
        .orElseThrow(() -> {
          log.error("Service: Account not found with number: {}", accountNumber);
          return new ResourceNotFoundException("Account not found with number: " + accountNumber);
        });

    if (account.getStatus() != Account.AccountStatus.ACTIVE) {
      log.warn("Service: Account is not active. Account: {}, Status: {}", accountNumber,
          account.getStatus());
      throw new InvalidTransactionException("Account is not active");
    }

    if (account.getBalance().compareTo(amount) < 0) {
      log.warn(
          "Service: Insufficient funds for withdrawal. Account: {}, Balance: {}, Requested amount: {}",
          accountNumber, account.getBalance(), amount);
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
    Transaction savedTransaction = transactionRepository.save(transaction);
    log.debug("Service: Created pending withdrawal transaction with ID: {}, transaction ID: {}",
        savedTransaction.getId(), savedTransaction.getTransactionId());

    try {
      // Update account balance
      BigDecimal balanceBefore = account.getBalance();
      account.setBalance(account.getBalance().subtract(amount));
      accountRepository.save(account);

      log.debug("Service: Updated account balance. Account: {} ({} -> {})",
          account.getAccountNumber(), balanceBefore, account.getBalance());

      // Update transaction status
      savedTransaction.setStatus(Transaction.TransactionStatus.COMPLETED);
      savedTransaction = transactionRepository.save(savedTransaction);

      log.info(
          "Service: Withdrawal completed successfully. Transaction ID: {}, Amount: {}, From account: {}",
          savedTransaction.getTransactionId(), amount, accountNumber);

      return convertToDTO(savedTransaction);
    } catch (Exception e) {
      // If any exception occurs, mark transaction as failed
      savedTransaction.setStatus(Transaction.TransactionStatus.FAILED);
      transactionRepository.save(savedTransaction);

      log.error("Service: Withdrawal failed. Transaction ID: {}, Amount: {}, From account: {}",
          savedTransaction.getTransactionId(), amount, accountNumber, e);

      throw e;
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
