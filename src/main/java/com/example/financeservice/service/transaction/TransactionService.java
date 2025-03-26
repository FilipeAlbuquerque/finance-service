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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionService {

  private final TransactionRepository transactionRepository;
  private final AccountRepository accountRepository;

  @Transactional(readOnly = true)
  public List<TransactionDTO> getAllTransactions() {
    return transactionRepository.findAll().stream()
        .map(this::convertToDTO)
        .toList();
  }

  @Transactional(readOnly = true)
  public TransactionDTO getTransactionById(Long id) {
    Transaction transaction = transactionRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
    return convertToDTO(transaction);
  }

  @Transactional(readOnly = true)
  public TransactionDTO getTransactionByTransactionId(String transactionId) {
    Transaction transaction = transactionRepository.findByTransactionId(transactionId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Transaction not found with transaction id: " + transactionId));
    return convertToDTO(transaction);
  }

  @Transactional(readOnly = true)
  public List<TransactionDTO> getTransactionsByAccount(String accountNumber) {
    Account account = accountRepository.findByAccountNumber(accountNumber)
        .orElseThrow(
            () -> new ResourceNotFoundException("Account not found with number: " + accountNumber));

    return transactionRepository.findByAccount(account).stream()
        .map(this::convertToDTO)
        .toList();
  }

  @Transactional(readOnly = true)
  public Page<TransactionDTO> getTransactionsByAccountPaginated(String accountNumber,
      Pageable pageable) {
    Account account = accountRepository.findByAccountNumber(accountNumber)
        .orElseThrow(
            () -> new ResourceNotFoundException("Account not found with number: " + accountNumber));

    return transactionRepository.findByAccountPaginated(account, pageable)
        .map(this::convertToDTO);
  }

  @Transactional(readOnly = true)
  public StatementDTO getAccountStatement(String accountNumber, LocalDateTime startDate,
      LocalDateTime endDate) {
    Account account = accountRepository.findByAccountNumber(accountNumber)
        .orElseThrow(
            () -> new ResourceNotFoundException("Account not found with number: " + accountNumber));

    List<Transaction> transactions = transactionRepository.findByAccountAndDateRange(account,
        startDate, endDate);

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

    return statement;
  }

  @Transactional
  public TransactionDTO transfer(TransferDTO transferDTO) {
    // Validations
    if (transferDTO.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
      throw new InvalidTransactionException("Transfer amount must be positive");
    }

    if (transferDTO.getSourceAccountNumber().equals(transferDTO.getDestinationAccountNumber())) {
      throw new InvalidTransactionException("Source and destination accounts cannot be the same");
    }

    // Get accounts with lock to prevent concurrent modifications
    Account sourceAccount = accountRepository.findByAccountNumberWithLock(
            transferDTO.getSourceAccountNumber())
        .orElseThrow(() -> new ResourceNotFoundException(
            "Source account not found with number: " + transferDTO.getSourceAccountNumber()));

    Account destinationAccount = accountRepository.findByAccountNumberWithLock(
            transferDTO.getDestinationAccountNumber())
        .orElseThrow(() -> new ResourceNotFoundException(
            "Destination account not found with number: "
                + transferDTO.getDestinationAccountNumber()));

    // Check if accounts are active
    if (sourceAccount.getStatus() != Account.AccountStatus.ACTIVE) {
      throw new InvalidTransactionException("Source account is not active");
    }

    if (destinationAccount.getStatus() != Account.AccountStatus.ACTIVE) {
      throw new InvalidTransactionException("Destination account is not active");
    }

    // Check if source account has sufficient funds
    if (sourceAccount.getBalance().compareTo(transferDTO.getAmount()) < 0) {
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

    try {
      // Update account balances
      sourceAccount.setBalance(sourceAccount.getBalance().subtract(transferDTO.getAmount()));
      destinationAccount.setBalance(destinationAccount.getBalance().add(transferDTO.getAmount()));

      accountRepository.save(sourceAccount);
      accountRepository.save(destinationAccount);

      // Update transaction status
      savedTransaction.setStatus(Transaction.TransactionStatus.COMPLETED);
      savedTransaction = transactionRepository.save(savedTransaction);

      return convertToDTO(savedTransaction);
    } catch (Exception e) {
      // If any exception occurs, mark transaction as failed
      savedTransaction.setStatus(Transaction.TransactionStatus.FAILED);
      transactionRepository.save(savedTransaction);
      throw e;
    }
  }

  @Transactional
  public TransactionDTO deposit(String accountNumber, BigDecimal amount, String description) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new InvalidTransactionException("Deposit amount must be positive");
    }

    Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
        .orElseThrow(
            () -> new ResourceNotFoundException("Account not found with number: " + accountNumber));

    if (account.getStatus() != Account.AccountStatus.ACTIVE) {
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

    try {
      // Update account balance
      account.setBalance(account.getBalance().add(amount));
      accountRepository.save(account);

      // Update transaction status
      savedTransaction.setStatus(Transaction.TransactionStatus.COMPLETED);
      savedTransaction = transactionRepository.save(savedTransaction);

      return convertToDTO(savedTransaction);
    } catch (Exception e) {
      // If any exception occurs, mark transaction as failed
      savedTransaction.setStatus(Transaction.TransactionStatus.FAILED);
      transactionRepository.save(savedTransaction);
      throw e;
    }
  }

  @Transactional
  public TransactionDTO withdraw(String accountNumber, BigDecimal amount, String description) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new InvalidTransactionException("Withdrawal amount must be positive");
    }

    Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
        .orElseThrow(
            () -> new ResourceNotFoundException("Account not found with number: " + accountNumber));

    if (account.getStatus() != Account.AccountStatus.ACTIVE) {
      throw new InvalidTransactionException("Account is not active");
    }

    if (account.getBalance().compareTo(amount) < 0) {
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

    try {
      // Update account balance
      account.setBalance(account.getBalance().subtract(amount));
      accountRepository.save(account);

      // Update transaction status
      savedTransaction.setStatus(Transaction.TransactionStatus.COMPLETED);
      savedTransaction = transactionRepository.save(savedTransaction);

      return convertToDTO(savedTransaction);
    } catch (Exception e) {
      // If any exception occurs, mark transaction as failed
      savedTransaction.setStatus(Transaction.TransactionStatus.FAILED);
      transactionRepository.save(savedTransaction);
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