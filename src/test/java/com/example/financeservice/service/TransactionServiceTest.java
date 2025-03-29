package com.example.financeservice.service;

import com.example.financeservice.dto.StatementDTO;
import com.example.financeservice.dto.TransactionDTO;
import com.example.financeservice.dto.TransferDTO;
import com.example.financeservice.exception.InsufficientFundsException;
import com.example.financeservice.exception.InvalidTransactionException;
import com.example.financeservice.exception.ResourceNotFoundException;
import com.example.financeservice.model.Account;
import com.example.financeservice.model.Transaction;
import com.example.financeservice.model.Transaction.TransactionStatus;
import com.example.financeservice.repository.AccountRepository;
import com.example.financeservice.repository.TransactionRepository;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionServiceTest {

  @Mock
  private TransactionRepository transactionRepository;

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private MetricsService metricsService;

  @Mock
  private Timer.Sample timerSample;

  @InjectMocks
  private TransactionService transactionService;

  private Account sourceAccount;
  private Account destinationAccount;
  private Transaction transaction;
  private TransferDTO transferDTO;

  @BeforeEach
  void setUp() {
    // Configurar mock do MetricsService
    doAnswer(invocation -> {
      Supplier<?> supplier = invocation.getArgument(2);
      return supplier.get();
    }).when(metricsService).recordRepositoryExecutionTime(anyString(), anyString(), any(Supplier.class));

    when(metricsService.startTimer()).thenReturn(timerSample);
    doNothing().when(metricsService).stopTimer(any(Timer.Sample.class), anyString(), any());
    doNothing().when(metricsService).recordExceptionOccurred(anyString(), anyString());
    doNothing().when(metricsService).recordTransactionProcessed(anyString(), any(BigDecimal.class), anyBoolean());
    doNothing().when(metricsService).recordDailyFinancialVolume(anyString(), any(BigDecimal.class));

    // Configurar dados de teste
    sourceAccount = new Account();
    sourceAccount.setId(1L);
    sourceAccount.setAccountNumber("SOURCE-ACC-123");
    sourceAccount.setBalance(new BigDecimal("1000.00"));
    sourceAccount.setStatus(Account.AccountStatus.ACTIVE);

    destinationAccount = new Account();
    destinationAccount.setId(2L);
    destinationAccount.setAccountNumber("DEST-ACC-456");
    destinationAccount.setBalance(new BigDecimal("500.00"));
    destinationAccount.setStatus(Account.AccountStatus.ACTIVE);

    transaction = new Transaction();
    transaction.setId(1L);
    transaction.setTransactionId(UUID.randomUUID().toString());
    transaction.setAmount(new BigDecimal("200.00"));
    transaction.setType(Transaction.TransactionType.TRANSFER);
    transaction.setDescription("Test transfer");
    transaction.setSourceAccount(sourceAccount);
    transaction.setDestinationAccount(destinationAccount);
    transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
    transaction.setCreatedAt(LocalDateTime.now());
    transaction.setProcessedAt(LocalDateTime.now());

    transferDTO = new TransferDTO();
    transferDTO.setSourceAccountNumber("SOURCE-ACC-123");
    transferDTO.setDestinationAccountNumber("DEST-ACC-456");
    transferDTO.setAmount(new BigDecimal("200.00"));
    transferDTO.setDescription("Test transfer");
  }

  @Test
  void getAllTransactions_ShouldReturnAllTransactions() {
    // Arrange
    Transaction transaction2 = new Transaction();
    transaction2.setId(2L);
    transaction2.setTransactionId(UUID.randomUUID().toString());
    transaction2.setAmount(new BigDecimal("100.00"));
    transaction2.setType(Transaction.TransactionType.DEPOSIT);

    when(transactionRepository.findAll()).thenReturn(Arrays.asList(transaction, transaction2));

    // Act
    List<TransactionDTO> result = transactionService.getAllTransactions();

    // Assert
    assertNotNull(result);
    assertEquals(2, result.size());
    verify(transactionRepository, times(1)).findAll();
  }

  @Test
  void getTransactionById_WithValidId_ShouldReturnTransaction() {
    // Arrange
    when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

    // Act
    TransactionDTO result = transactionService.getTransactionById(1L);

    // Assert
    assertNotNull(result);
    assertEquals(transaction.getId(), result.getId());
    assertEquals(transaction.getTransactionId(), result.getTransactionId());
    assertEquals(transaction.getAmount(), result.getAmount());
    verify(transactionRepository, times(1)).findById(1L);
  }

  @Test
  void getTransactionById_WithInvalidId_ShouldThrowException() {
    // Arrange
    when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
        () -> transactionService.getTransactionById(999L));

    assertEquals("Transaction not found with id: 999", exception.getMessage());
    verify(transactionRepository, times(1)).findById(999L);
    verify(metricsService, times(1)).recordExceptionOccurred("ResourceNotFoundException", "getTransactionById");
  }

  @Test
  void getTransactionByTransactionId_WithValidId_ShouldReturnTransaction() {
    // Arrange
    String transactionId = transaction.getTransactionId();
    when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(transaction));

    // Act
    TransactionDTO result = transactionService.getTransactionByTransactionId(transactionId);

    // Assert
    assertNotNull(result);
    assertEquals(transactionId, result.getTransactionId());
    verify(transactionRepository, times(1)).findByTransactionId(transactionId);
  }

  @Test
  void getTransactionsByAccount_WithValidAccountNumber_ShouldReturnTransactions() {
    // Arrange
    when(accountRepository.findByAccountNumber("SOURCE-ACC-123")).thenReturn(Optional.of(sourceAccount));
    when(transactionRepository.findByAccount(sourceAccount)).thenReturn(List.of(transaction));

    // Act
    List<TransactionDTO> result = transactionService.getTransactionsByAccount("SOURCE-ACC-123");

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(transaction.getId(), result.get(0).getId());
    verify(accountRepository, times(1)).findByAccountNumber("SOURCE-ACC-123");
    verify(transactionRepository, times(1)).findByAccount(sourceAccount);
  }

  @Test
  void getTransactionsByAccountPaginated_WithValidParams_ShouldReturnPaginatedTransactions() {
    // Arrange
    Pageable pageable = PageRequest.of(0, 10);
    Page<Transaction> transactionPage = new PageImpl<>(List.of(transaction), pageable, 1);

    when(accountRepository.findByAccountNumber("SOURCE-ACC-123")).thenReturn(Optional.of(sourceAccount));
    when(transactionRepository.findByAccountPaginated(sourceAccount, pageable)).thenReturn(transactionPage);

    // Act
    Page<TransactionDTO> result = transactionService.getTransactionsByAccountPaginated("SOURCE-ACC-123", pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    assertEquals(transaction.getId(), result.getContent().get(0).getId());
    verify(accountRepository, times(1)).findByAccountNumber("SOURCE-ACC-123");
    verify(transactionRepository, times(1)).findByAccountPaginated(sourceAccount, pageable);
  }

  @Test
  void getAccountStatement_WithValidParams_ShouldReturnStatement() {
    // Arrange
    LocalDateTime startDate = LocalDateTime.now().minusDays(30);
    LocalDateTime endDate = LocalDateTime.now();

    when(accountRepository.findByAccountNumber("SOURCE-ACC-123")).thenReturn(Optional.of(sourceAccount));
    when(transactionRepository.findByAccountAndDateRange(sourceAccount, startDate, endDate)).thenReturn(List.of(transaction));

    // Act
    StatementDTO result = transactionService.getAccountStatement("SOURCE-ACC-123", startDate, endDate);

    // Assert
    assertNotNull(result);
    assertEquals("SOURCE-ACC-123", result.getAccountNumber());
    assertEquals(sourceAccount.getType(), result.getAccountType());
    assertEquals(sourceAccount.getBalance(), result.getCurrentBalance());
    assertEquals(1, result.getTransactions().size());
    verify(accountRepository, times(1)).findByAccountNumber("SOURCE-ACC-123");
    verify(transactionRepository, times(1)).findByAccountAndDateRange(sourceAccount, startDate, endDate);
  }

  @Test
  void transfer_WithValidParams_ShouldCompleteTransferSuccessfully() {
    // Arrange
    when(accountRepository.findByAccountNumberWithLock("SOURCE-ACC-123")).thenReturn(Optional.of(sourceAccount));
    when(accountRepository.findByAccountNumberWithLock("DEST-ACC-456")).thenReturn(Optional.of(destinationAccount));

    Transaction pendingTransaction = new Transaction();
    pendingTransaction.setId(1L);
    pendingTransaction.setTransactionId(UUID.randomUUID().toString());
    pendingTransaction.setAmount(new BigDecimal("200.00"));
    pendingTransaction.setType(Transaction.TransactionType.TRANSFER);
    pendingTransaction.setStatus(Transaction.TransactionStatus.PENDING);

    when(transactionRepository.save(any(Transaction.class))).thenReturn(pendingTransaction);
    when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    TransactionDTO result = transactionService.transfer(transferDTO);

    // Assert
    assertNotNull(result);
    assertEquals(pendingTransaction.getTransactionId(), result.getTransactionId());
    assertEquals(Transaction.TransactionType.TRANSFER, result.getType());
    assertEquals(TransactionStatus.COMPLETED, result.getStatus());

    verify(accountRepository, times(1)).findByAccountNumberWithLock("SOURCE-ACC-123");
    verify(accountRepository, times(1)).findByAccountNumberWithLock("DEST-ACC-456");
    verify(transactionRepository, times(2)).save(any(Transaction.class));
    verify(accountRepository, times(1)).save(sourceAccount);
    verify(accountRepository, times(1)).save(destinationAccount);
    verify(metricsService, times(1)).recordTransactionProcessed("TRANSFER", new BigDecimal("200.00"), true);
    verify(metricsService, times(1)).recordDailyFinancialVolume("TRANSFER", new BigDecimal("200.00"));
  }

  @Test
  void transfer_WithNegativeAmount_ShouldThrowException() {
    // Arrange
    transferDTO.setAmount(new BigDecimal("-100.00"));

    // Act & Assert
    InvalidTransactionException exception = assertThrows(InvalidTransactionException.class,
        () -> transactionService.transfer(transferDTO));

    assertEquals("Transfer amount must be positive", exception.getMessage());
    verify(accountRepository, never()).findByAccountNumberWithLock(anyString());
    verify(transactionRepository, never()).save(any(Transaction.class));
    verify(metricsService, times(1)).recordExceptionOccurred("InvalidTransactionException", "transfer");
    verify(metricsService, times(1)).recordTransactionProcessed("TRANSFER", new BigDecimal("-100.00"), false);
  }

  @Test
  void transfer_WithSameSourceAndDestination_ShouldThrowException() {
    // Arrange
    transferDTO.setDestinationAccountNumber("SOURCE-ACC-123");

    // Act & Assert
    InvalidTransactionException exception = assertThrows(InvalidTransactionException.class,
        () -> transactionService.transfer(transferDTO));

    assertEquals("Source and destination accounts cannot be the same", exception.getMessage());
    verify(accountRepository, never()).findByAccountNumberWithLock(anyString());
    verify(transactionRepository, never()).save(any(Transaction.class));
    verify(metricsService, times(1)).recordExceptionOccurred("InvalidTransactionException", "transfer");
    verify(metricsService, times(1)).recordTransactionProcessed("TRANSFER", new BigDecimal("200.00"), false);
  }

  @Test
  void transfer_WithNonExistentSourceAccount_ShouldThrowException() {
    // Arrange
    when(accountRepository.findByAccountNumberWithLock("SOURCE-ACC-123")).thenReturn(Optional.empty());

    // Act & Assert
    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
        () -> transactionService.transfer(transferDTO));

    assertEquals("Source account not found with number: SOURCE-ACC-123", exception.getMessage());
    verify(accountRepository, times(1)).findByAccountNumberWithLock("SOURCE-ACC-123");
    verify(accountRepository, never()).findByAccountNumberWithLock("DEST-ACC-456");
    verify(transactionRepository, never()).save(any(Transaction.class));
    verify(metricsService, times(1)).recordExceptionOccurred("ResourceNotFoundException", "transfer");
  }

  @Test
  void transfer_WithInactiveSourceAccount_ShouldThrowException() {
    // Arrange
    sourceAccount.setStatus(Account.AccountStatus.BLOCKED);
    when(accountRepository.findByAccountNumberWithLock("SOURCE-ACC-123")).thenReturn(Optional.of(sourceAccount));
    when(accountRepository.findByAccountNumberWithLock("DEST-ACC-456")).thenReturn(Optional.of(destinationAccount));

    // Act & Assert
    InvalidTransactionException exception = assertThrows(InvalidTransactionException.class,
        () -> transactionService.transfer(transferDTO));

    assertEquals("Source account is not active", exception.getMessage());
    verify(accountRepository, times(1)).findByAccountNumberWithLock("SOURCE-ACC-123");
    verify(accountRepository, times(1)).findByAccountNumberWithLock("DEST-ACC-456");
    verify(transactionRepository, never()).save(any(Transaction.class));
    verify(metricsService, times(1)).recordExceptionOccurred("InvalidTransactionException", "transfer");
    verify(metricsService, times(1)).recordTransactionProcessed("TRANSFER", new BigDecimal("200.00"), false);
  }

  @Test
  void transfer_WithInsufficientFunds_ShouldThrowException() {
    // Arrange
    sourceAccount.setBalance(new BigDecimal("100.00"));
    when(accountRepository.findByAccountNumberWithLock("SOURCE-ACC-123")).thenReturn(Optional.of(sourceAccount));
    when(accountRepository.findByAccountNumberWithLock("DEST-ACC-456")).thenReturn(Optional.of(destinationAccount));

    // Act & Assert
    InsufficientFundsException exception = assertThrows(InsufficientFundsException.class,
        () -> transactionService.transfer(transferDTO));

    assertEquals("Insufficient funds in source account", exception.getMessage());
    verify(accountRepository, times(1)).findByAccountNumberWithLock("SOURCE-ACC-123");
    verify(accountRepository, times(1)).findByAccountNumberWithLock("DEST-ACC-456");
    verify(transactionRepository, never()).save(any(Transaction.class));
    verify(metricsService, times(1)).recordExceptionOccurred("InsufficientFundsException", "transfer");
    verify(metricsService, times(1)).recordTransactionProcessed("TRANSFER", new BigDecimal("200.00"), false);
  }

  @Test
  void deposit_WithValidParams_ShouldCompleteDepositSuccessfully() {
    // Arrange
    when(accountRepository.findByAccountNumberWithLock("DEST-ACC-456")).thenReturn(Optional.of(destinationAccount));

    Transaction pendingTransaction = new Transaction();
    pendingTransaction.setId(1L);
    pendingTransaction.setTransactionId(UUID.randomUUID().toString());
    pendingTransaction.setAmount(new BigDecimal("300.00"));
    pendingTransaction.setType(Transaction.TransactionType.DEPOSIT);
    pendingTransaction.setStatus(Transaction.TransactionStatus.PENDING);

    when(transactionRepository.save(any(Transaction.class))).thenReturn(pendingTransaction);
    when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    TransactionDTO result = transactionService.deposit("DEST-ACC-456", new BigDecimal("300.00"), "Test deposit");

    // Assert
    assertNotNull(result);
    assertEquals(pendingTransaction.getTransactionId(), result.getTransactionId());
    assertEquals(Transaction.TransactionType.DEPOSIT, result.getType());

    verify(accountRepository, times(1)).findByAccountNumberWithLock("DEST-ACC-456");
    verify(transactionRepository, times(2)).save(any(Transaction.class));
    verify(accountRepository, times(1)).save(destinationAccount);
    verify(metricsService, times(1)).recordTransactionProcessed("DEPOSIT", new BigDecimal("300.00"), true);
    verify(metricsService, times(1)).recordDailyFinancialVolume("DEPOSIT", new BigDecimal("300.00"));
  }

  @Test
  void withdraw_WithValidParams_ShouldCompleteWithdrawalSuccessfully() {
    // Arrange
    when(accountRepository.findByAccountNumberWithLock("SOURCE-ACC-123")).thenReturn(Optional.of(sourceAccount));

    Transaction pendingTransaction = new Transaction();
    pendingTransaction.setId(1L);
    pendingTransaction.setTransactionId(UUID.randomUUID().toString());
    pendingTransaction.setAmount(new BigDecimal("200.00"));
    pendingTransaction.setType(Transaction.TransactionType.WITHDRAWAL);
    pendingTransaction.setStatus(Transaction.TransactionStatus.PENDING);

    when(transactionRepository.save(any(Transaction.class))).thenReturn(pendingTransaction);
    when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    TransactionDTO result = transactionService.withdraw("SOURCE-ACC-123", new BigDecimal("200.00"), "Test withdrawal");

    // Assert
    assertNotNull(result);
    assertEquals(pendingTransaction.getTransactionId(), result.getTransactionId());
    assertEquals(Transaction.TransactionType.WITHDRAWAL, result.getType());

    verify(accountRepository, times(1)).findByAccountNumberWithLock("SOURCE-ACC-123");
    verify(transactionRepository, times(2)).save(any(Transaction.class));
    verify(accountRepository, times(1)).save(sourceAccount);
    verify(metricsService, times(1)).recordTransactionProcessed("WITHDRAWAL", new BigDecimal("200.00"), true);
    verify(metricsService, times(1)).recordDailyFinancialVolume("WITHDRAWAL", new BigDecimal("200.00"));
  }

  @Test
  void withdraw_WithInsufficientFunds_ShouldThrowException() {
    // Arrange
    sourceAccount.setBalance(new BigDecimal("100.00"));
    when(accountRepository.findByAccountNumberWithLock("SOURCE-ACC-123")).thenReturn(Optional.of(sourceAccount));

    // Act & Assert
    InsufficientFundsException exception = assertThrows(InsufficientFundsException.class,
        () -> transactionService.withdraw("SOURCE-ACC-123", new BigDecimal("200.00"), "Test withdrawal"));

    assertEquals("Insufficient funds for withdrawal", exception.getMessage());
    verify(accountRepository, times(1)).findByAccountNumberWithLock("SOURCE-ACC-123");
    verify(transactionRepository, never()).save(any(Transaction.class));
    verify(metricsService, times(1)).recordExceptionOccurred("InsufficientFundsException", "withdraw");
    verify(metricsService, times(1)).recordTransactionProcessed("WITHDRAWAL", new BigDecimal("200.00"), false);
  }
}
