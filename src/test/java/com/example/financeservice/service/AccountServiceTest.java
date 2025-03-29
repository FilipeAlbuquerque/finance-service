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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountServiceTest {

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private ClientRepository clientRepository;

  @Mock
  private MerchantRepository merchantRepository;

  @Mock
  private MetricsService metricsService;

  @InjectMocks
  private AccountService accountService;

  private Account testAccount;
  private Client testClient;
  private Merchant testMerchant;
  private CreateAccountDTO createClientAccountDTO;
  private CreateAccountDTO createMerchantAccountDTO;

  @BeforeEach
  void setUp() {
    // Configurar comportamento padrão do MetricsService
    doAnswer(invocation -> {
      Supplier<?> supplier = invocation.getArgument(2);
      return supplier.get();
    }).when(metricsService).recordRepositoryExecutionTime(anyString(), anyString(), any(Supplier.class));

    doNothing().when(metricsService).recordAccountCreated(anyString(), anyString());
    doNothing().when(metricsService).recordTransactionProcessed(anyString(), any(BigDecimal.class), anyBoolean());
    doNothing().when(metricsService).recordDailyFinancialVolume(anyString(), any(BigDecimal.class));
    doNothing().when(metricsService).recordAccountStatusUpdate(anyString(), anyString());
    doNothing().when(metricsService).recordExceptionOccurred(anyString(), anyString());

    // Configurar dados de teste
    testClient = new Client();
    testClient.setId(1L);
    testClient.setName("Test Client");
    testClient.setEmail("client@example.com");

    testMerchant = new Merchant();
    testMerchant.setId(2L);
    testMerchant.setBusinessName("Test Merchant");
    testMerchant.setEmail("merchant@example.com");

    testAccount = new Account();
    testAccount.setId(1L);
    testAccount.setAccountNumber("ACC123456");
    testAccount.setType(Account.AccountType.CHECKING);
    testAccount.setBalance(new BigDecimal("1000.00"));
    testAccount.setAvailableLimit(new BigDecimal("2000.00"));
    testAccount.setStatus(Account.AccountStatus.ACTIVE);
    testAccount.setCreatedAt(LocalDateTime.now());
    testAccount.setClient(testClient);

    createClientAccountDTO = new CreateAccountDTO();
    createClientAccountDTO.setOwnerId(1L);
    createClientAccountDTO.setType(Account.AccountType.CHECKING);
    createClientAccountDTO.setInitialDeposit(new BigDecimal("500.00"));
    createClientAccountDTO.setAvailableLimit(new BigDecimal("1000.00"));

    createMerchantAccountDTO = new CreateAccountDTO();
    createMerchantAccountDTO.setOwnerId(2L);
    createMerchantAccountDTO.setType(Account.AccountType.BUSINESS);
    createMerchantAccountDTO.setInitialDeposit(new BigDecimal("1000.00"));
    createMerchantAccountDTO.setAvailableLimit(new BigDecimal("5000.00"));
  }

  @Test
  void getAllAccounts_ShouldReturnAllAccounts() {
    // Arrange
    Account account2 = new Account();
    account2.setId(2L);
    account2.setAccountNumber("ACC654321");
    account2.setType(Account.AccountType.SAVINGS);
    account2.setBalance(new BigDecimal("2000.00"));
    account2.setStatus(Account.AccountStatus.ACTIVE);
    account2.setMerchant(testMerchant);

    when(accountRepository.findAll()).thenReturn(Arrays.asList(testAccount, account2));

    // Act
    List<AccountDTO> result = accountService.getAllAccounts();

    // Assert
    assertNotNull(result);
    assertEquals(2, result.size());

    // Verify first account
    assertEquals(1L, result.get(0).getId());
    assertEquals("ACC123456", result.get(0).getAccountNumber());
    assertEquals(Account.AccountType.CHECKING, result.get(0).getType());
    assertEquals(new BigDecimal("1000.00"), result.get(0).getBalance());
    assertEquals("CLIENT", result.get(0).getOwnerType());
    assertEquals("Test Client", result.get(0).getOwnerName());

    // Verify second account
    assertEquals(2L, result.get(1).getId());
    assertEquals("ACC654321", result.get(1).getAccountNumber());
    assertEquals(Account.AccountType.SAVINGS, result.get(1).getType());
    assertEquals(new BigDecimal("2000.00"), result.get(1).getBalance());
    assertEquals("MERCHANT", result.get(1).getOwnerType());
    assertEquals("Test Merchant", result.get(1).getOwnerName());

    verify(accountRepository, times(1)).findAll();
  }

  @Test
  void getAccountById_WithValidId_ShouldReturnAccount() {
    // Arrange
    when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

    // Act
    AccountDTO result = accountService.getAccountById(1L);

    // Assert
    assertNotNull(result);
    assertEquals(1L, result.getId());
    assertEquals("ACC123456", result.getAccountNumber());
    assertEquals(Account.AccountType.CHECKING, result.getType());
    assertEquals(new BigDecimal("1000.00"), result.getBalance());
    assertEquals("CLIENT", result.getOwnerType());
    assertEquals("Test Client", result.getOwnerName());

    verify(accountRepository, times(1)).findById(1L);
  }

  @Test
  void getAccountById_WithInvalidId_ShouldThrowException() {
    // Arrange
    when(accountRepository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
        () -> accountService.getAccountById(999L));

    assertEquals("Account not found with id: 999", exception.getMessage());
    verify(accountRepository, times(1)).findById(999L);
    verify(metricsService, times(1)).recordExceptionOccurred("ResourceNotFoundException", "getAccountById");
  }

  @Test
  void getAccountByNumber_WithValidNumber_ShouldReturnAccount() {
    // Arrange
    when(accountRepository.findByAccountNumber("ACC123456")).thenReturn(Optional.of(testAccount));

    // Act
    AccountDTO result = accountService.getAccountByNumber("ACC123456");

    // Assert
    assertNotNull(result);
    assertEquals(1L, result.getId());
    assertEquals("ACC123456", result.getAccountNumber());

    verify(accountRepository, times(1)).findByAccountNumber("ACC123456");
  }

  @Test
  void getAccountByNumber_WithInvalidNumber_ShouldThrowException() {
    // Arrange
    when(accountRepository.findByAccountNumber("INVALID")).thenReturn(Optional.empty());

    // Act & Assert
    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
        () -> accountService.getAccountByNumber("INVALID"));

    assertEquals("Account not found with number: INVALID", exception.getMessage());
    verify(accountRepository, times(1)).findByAccountNumber("INVALID");
    verify(metricsService, times(1)).recordExceptionOccurred("ResourceNotFoundException", "getAccountByNumber");
  }

  @Test
  void getAccountsByClient_WithValidClientId_ShouldReturnAccounts() {
    // Arrange
    Account account2 = new Account();
    account2.setId(3L);
    account2.setAccountNumber("ACC789012");
    account2.setType(Account.AccountType.SAVINGS);
    account2.setClient(testClient);

    when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));
    when(accountRepository.findByClient(testClient)).thenReturn(Arrays.asList(testAccount, account2));

    // Act
    List<AccountDTO> result = accountService.getAccountsByClient(1L);

    // Assert
    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals("ACC123456", result.get(0).getAccountNumber());
    assertEquals("ACC789012", result.get(1).getAccountNumber());

    verify(clientRepository, times(1)).findById(1L);
    verify(accountRepository, times(1)).findByClient(testClient);
  }

  @Test
  void getAccountsByClient_WithInvalidClientId_ShouldThrowException() {
    // Arrange
    when(clientRepository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
        () -> accountService.getAccountsByClient(999L));

    assertEquals("Client not found with id: 999", exception.getMessage());
    verify(clientRepository, times(1)).findById(999L);
    verify(accountRepository, never()).findByClient(any(Client.class));
    verify(metricsService, times(1)).recordExceptionOccurred("ResourceNotFoundException", "getAccountsByClient");
  }

  @Test
  void createClientAccount_WithValidData_ShouldCreateAccount() {
    // Arrange
    when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));
    when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
      Account savedAccount = invocation.getArgument(0);
      savedAccount.setId(5L);
      savedAccount.setAccountNumber("ACC555555");
      return savedAccount;
    });

    // Act
    AccountDTO result = accountService.createClientAccount(createClientAccountDTO);

    // Assert
    assertNotNull(result);
    assertEquals(5L, result.getId());
    assertEquals("ACC555555", result.getAccountNumber());
    assertEquals(Account.AccountType.CHECKING, result.getType());
    assertEquals(new BigDecimal("500.00"), result.getBalance());
    assertEquals(new BigDecimal("1000.00"), result.getAvailableLimit());
    assertEquals("CLIENT", result.getOwnerType());
    assertEquals("Test Client", result.getOwnerName());

    verify(clientRepository, times(1)).findById(1L);
    verify(accountRepository, times(1)).save(any(Account.class));
    verify(metricsService, times(1)).recordAccountCreated("CHECKING", "CLIENT");
    verify(metricsService, times(1)).recordTransactionProcessed("DEPOSIT", new BigDecimal("500.00"), true);
    verify(metricsService, times(1)).recordDailyFinancialVolume("DEPOSIT", new BigDecimal("500.00"));
  }

  @Test
  void createClientAccount_WithInvalidClientId_ShouldThrowException() {
    // Arrange
    when(clientRepository.findById(999L)).thenReturn(Optional.empty());
    createClientAccountDTO.setOwnerId(999L);

    // Act & Assert
    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
        () -> accountService.createClientAccount(createClientAccountDTO));

    assertEquals("Client not found with id: 999", exception.getMessage());
    verify(clientRepository, times(1)).findById(999L);
    verify(accountRepository, never()).save(any(Account.class));
    verify(metricsService, times(1)).recordExceptionOccurred("ResourceNotFoundException", "createClientAccount");
  }

  @Test
  void createMerchantAccount_WithValidData_ShouldCreateAccount() {
    // Arrange
    when(merchantRepository.findById(2L)).thenReturn(Optional.of(testMerchant));
    when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
      Account savedAccount = invocation.getArgument(0);
      savedAccount.setId(6L);
      savedAccount.setAccountNumber("ACC666666");
      return savedAccount;
    });

    // Act
    AccountDTO result = accountService.createMerchantAccount(createMerchantAccountDTO);

    // Assert
    assertNotNull(result);
    assertEquals(6L, result.getId());
    assertEquals("ACC666666", result.getAccountNumber());
    assertEquals(Account.AccountType.BUSINESS, result.getType());
    assertEquals(new BigDecimal("1000.00"), result.getBalance());
    assertEquals(new BigDecimal("5000.00"), result.getAvailableLimit());
    assertEquals("MERCHANT", result.getOwnerType());
    assertEquals("Test Merchant", result.getOwnerName());

    verify(merchantRepository, times(1)).findById(2L);
    verify(accountRepository, times(1)).save(any(Account.class));
    verify(metricsService, times(1)).recordAccountCreated("BUSINESS", "MERCHANT");
    verify(metricsService, times(1)).recordTransactionProcessed("DEPOSIT", new BigDecimal("1000.00"), true);
    verify(metricsService, times(1)).recordDailyFinancialVolume("DEPOSIT", new BigDecimal("1000.00"));
  }

  @Test
  void deposit_WithValidData_ShouldIncreaseBalance() {
    // Arrange
    Account account = new Account();
    account.setId(1L);
    account.setAccountNumber("ACC123456");
    account.setBalance(new BigDecimal("1000.00"));
    account.setStatus(Account.AccountStatus.ACTIVE);
    account.setClient(testClient);

    when(accountRepository.findByAccountNumberWithLock("ACC123456")).thenReturn(Optional.of(account));
    when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

    BigDecimal depositAmount = new BigDecimal("500.00");

    // Act
    AccountDTO result = accountService.deposit("ACC123456", depositAmount);

    // Assert
    assertNotNull(result);
    assertEquals(new BigDecimal("1500.00"), result.getBalance());

    verify(accountRepository, times(1)).findByAccountNumberWithLock("ACC123456");
    verify(accountRepository, times(1)).save(any(Account.class));
    verify(metricsService, times(1)).recordTransactionProcessed("DEPOSIT", depositAmount, true);
    verify(metricsService, times(1)).recordDailyFinancialVolume("DEPOSIT", depositAmount);
  }

  @Test
  void deposit_WithNegativeAmount_ShouldThrowException() {
    // Arrange
    BigDecimal negativeAmount = new BigDecimal("-100.00");

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> accountService.deposit("ACC123456", negativeAmount));

    assertEquals("Deposit amount must be positive", exception.getMessage());
    verify(accountRepository, never()).findByAccountNumberWithLock(anyString());
    verify(accountRepository, never()).save(any(Account.class));
    verify(metricsService, times(1)).recordExceptionOccurred("IllegalArgumentException", "deposit");
  }

  @Test
  void deposit_WithInvalidAccountNumber_ShouldThrowException() {
    // Arrange
    when(accountRepository.findByAccountNumberWithLock("INVALID")).thenReturn(Optional.empty());
    BigDecimal amount = new BigDecimal("500.00");

    // Act & Assert
    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
        () -> accountService.deposit("INVALID", amount));

    assertEquals("Account not found with number: INVALID", exception.getMessage());
    verify(accountRepository, times(1)).findByAccountNumberWithLock("INVALID");
    verify(accountRepository, never()).save(any(Account.class));
    verify(metricsService, times(1)).recordExceptionOccurred("ResourceNotFoundException", "deposit");
  }

  @Test
  void withdraw_WithValidData_ShouldDecreaseBalance() {
    // Arrange
    Account account = new Account();
    account.setId(1L);
    account.setAccountNumber("ACC123456");
    account.setBalance(new BigDecimal("1000.00"));
    account.setStatus(Account.AccountStatus.ACTIVE);
    account.setClient(testClient);

    when(accountRepository.findByAccountNumberWithLock("ACC123456")).thenReturn(Optional.of(account));
    when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

    BigDecimal withdrawAmount = new BigDecimal("300.00");

    // Act
    AccountDTO result = accountService.withdraw("ACC123456", withdrawAmount);

    // Assert
    assertNotNull(result);
    assertEquals(new BigDecimal("700.00"), result.getBalance());

    verify(accountRepository, times(1)).findByAccountNumberWithLock("ACC123456");
    verify(accountRepository, times(1)).save(any(Account.class));
    verify(metricsService, times(1)).recordTransactionProcessed("WITHDRAWAL", withdrawAmount, true);
    verify(metricsService, times(1)).recordDailyFinancialVolume("WITHDRAWAL", withdrawAmount);
  }

  @Test
  void withdraw_WithInsufficientFunds_ShouldThrowException() {
    // Arrange
    Account account = new Account();
    account.setId(1L);
    account.setAccountNumber("ACC123456");
    account.setBalance(new BigDecimal("500.00"));
    account.setStatus(Account.AccountStatus.ACTIVE);

    when(accountRepository.findByAccountNumberWithLock("ACC123456")).thenReturn(Optional.of(account));

    BigDecimal withdrawAmount = new BigDecimal("1000.00");

    // Act & Assert
    InsufficientFundsException exception = assertThrows(InsufficientFundsException.class,
        () -> accountService.withdraw("ACC123456", withdrawAmount));

    assertEquals("Insufficient funds for withdrawal", exception.getMessage());
    verify(accountRepository, times(1)).findByAccountNumberWithLock("ACC123456");
    verify(accountRepository, never()).save(any(Account.class));
    verify(metricsService, times(1)).recordExceptionOccurred("InsufficientFundsException", "withdraw");
    verify(metricsService, times(1)).recordTransactionProcessed("WITHDRAWAL", withdrawAmount, false);
  }

  @Test
  void updateAccountStatus_WithValidData_ShouldUpdateStatus() {
    // Arrange
    Account account = new Account();
    account.setId(1L);
    account.setAccountNumber("ACC123456");
    account.setStatus(Account.AccountStatus.ACTIVE);
    account.setClient(testClient);

    when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
    when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    AccountDTO result = accountService.updateAccountStatus(1L, Account.AccountStatus.BLOCKED);

    // Assert
    assertNotNull(result);
    assertEquals(Account.AccountStatus.BLOCKED, result.getStatus());

    verify(accountRepository, times(1)).findById(1L);
    verify(accountRepository, times(1)).save(account);
    verify(metricsService, times(1)).recordAccountStatusUpdate(
        Account.AccountStatus.ACTIVE.toString(),
        Account.AccountStatus.BLOCKED.toString());
  }
}
