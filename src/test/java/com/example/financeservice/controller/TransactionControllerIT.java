package com.example.financeservice.controller;

import com.example.financeservice.dto.StatementDTO;
import com.example.financeservice.dto.TransactionDTO;
import com.example.financeservice.dto.TransferDTO;
import com.example.financeservice.model.Account;
import com.example.financeservice.model.Transaction;
import com.example.financeservice.repository.AccountRepository;
import com.example.financeservice.repository.TransactionRepository;
import com.example.financeservice.security.JwtRequestFilter;
import com.example.financeservice.security.JwtUtils;
import com.example.financeservice.service.MetricsService;
import com.example.financeservice.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TransactionController.class)
@Import(JwtRequestFilter.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class TransactionControllerIT {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private TransactionService transactionService;

  @MockBean
  private UserDetailsService userDetailsService;

  @MockBean
  private JwtUtils jwtUtils;

  @MockBean
  private AuthenticationManager authenticationManager;

  // Beans necessários para evitar erros de dependência
  @MockBean
  private TransactionRepository transactionRepository;

  @MockBean
  private AccountRepository accountRepository;

  @MockBean
  private MetricsService metricsService;

  private TransactionDTO sampleTransaction;
  private List<TransactionDTO> transactionList;
  private TransferDTO transferDTO;
  private StatementDTO statementDTO;
  private LocalDateTime now;

  @BeforeEach
  void setUp() {
    // Current timestamp for test data
    now = LocalDateTime.now();

    // Setup user details
    UserDetails userDetails = new User("admin", "admin",
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));

    // Configure userDetailsService mock
    when(userDetailsService.loadUserByUsername("admin")).thenReturn(userDetails);

    // Create sample transaction
    sampleTransaction = TransactionDTO.builder()
        .id(1L)
        .transactionId(UUID.randomUUID().toString())
        .amount(new BigDecimal("1000.00"))
        .type(Transaction.TransactionType.TRANSFER)
        .status(Transaction.TransactionStatus.COMPLETED)
        .sourceAccountNumber("ACC-123456")
        .destinationAccountNumber("ACC-789012")
        .createdAt(now)
        .processedAt(now)
        .build();

    // Create second transaction
    TransactionDTO secondTransaction = TransactionDTO.builder()
        .id(2L)
        .transactionId(UUID.randomUUID().toString())
        .amount(new BigDecimal("500.00"))
        .type(Transaction.TransactionType.DEPOSIT)
        .status(Transaction.TransactionStatus.COMPLETED)
        .destinationAccountNumber("ACC-123456")
        .createdAt(now)
        .processedAt(now)
        .build();

    // Create transaction list
    transactionList = Arrays.asList(sampleTransaction, secondTransaction);

    // Create transfer DTO
    transferDTO = TransferDTO.builder()
        .sourceAccountNumber("ACC-123456")
        .destinationAccountNumber("ACC-789012")
        .amount(new BigDecimal("500.00"))
        .description("Test transfer")
        .build();

    // Create statement DTO
    statementDTO = new StatementDTO();
    statementDTO.setAccountNumber("ACC-123456");
    statementDTO.setAccountType(Account.AccountType.CHECKING);
    statementDTO.setCurrentBalance(new BigDecimal("1500.00"));
    statementDTO.setStatementStartDate(now.minusDays(30));
    statementDTO.setStatementEndDate(now);
    statementDTO.setGeneratedAt(now);
    statementDTO.setTransactions(transactionList);
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void getAllTransactions_ShouldReturnTransactions() throws Exception {
    // Arrange
    when(transactionService.getAllTransactions()).thenReturn(transactionList);

    // Act & Assert
    mockMvc.perform(get("/transactions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].id").value(1L))
        .andExpect(jsonPath("$[0].type").value("TRANSFER"))
        .andExpect(jsonPath("$[1].id").value(2L))
        .andExpect(jsonPath("$[1].type").value("DEPOSIT"));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void getTransactionById_ShouldReturnTransaction() throws Exception {
    // Arrange
    when(transactionService.getTransactionById(1L)).thenReturn(sampleTransaction);

    // Act & Assert
    mockMvc.perform(get("/transactions/{id}", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.amount").value(1000.00))
        .andExpect(jsonPath("$.type").value("TRANSFER"))
        .andExpect(jsonPath("$.status").value("COMPLETED"));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void getTransactionByTransactionId_ShouldReturnTransaction() throws Exception {
    // Arrange
    String transactionId = sampleTransaction.getTransactionId();
    when(transactionService.getTransactionByTransactionId(transactionId)).thenReturn(sampleTransaction);

    // Act & Assert
    mockMvc.perform(get("/transactions/transaction-id/{transactionId}", transactionId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.transactionId").value(transactionId))
        .andExpect(jsonPath("$.amount").value(1000.00));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void getTransactionsByAccount_ShouldReturnTransactions() throws Exception {
    // Arrange
    when(transactionService.getTransactionsByAccount("ACC-123456")).thenReturn(transactionList);

    // Act & Assert
    mockMvc.perform(get("/transactions/account/{accountNumber}", "ACC-123456"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].sourceAccountNumber").value("ACC-123456"))
        .andExpect(jsonPath("$[1].destinationAccountNumber").value("ACC-123456"));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void getTransactionsByAccountPaginated_ShouldReturnPaginatedTransactions() throws Exception {
    // Arrange
    Page<TransactionDTO> transactionPage = new PageImpl<>(
        transactionList,
        PageRequest.of(0, 10),
        2
    );

    when(transactionService.getTransactionsByAccountPaginated(eq("ACC-123456"), any(Pageable.class)))
        .thenReturn(transactionPage);

    // Act & Assert
    mockMvc.perform(get("/transactions/account/{accountNumber}/paginated", "ACC-123456")
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.totalPages").value(1));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void getAccountStatement_ShouldReturnStatement() throws Exception {
    // Arrange
    LocalDateTime startDate = now.minusDays(30);
    LocalDateTime endDate = now;

    when(transactionService.getAccountStatement(
        eq("ACC-123456"),
        any(LocalDateTime.class),
        any(LocalDateTime.class)))
        .thenReturn(statementDTO);

    // Act & Assert
    mockMvc.perform(get("/transactions/account/{accountNumber}/statement", "ACC-123456")
            .param("startDate", startDate.toString())
            .param("endDate", endDate.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountNumber").value("ACC-123456"))
        .andExpect(jsonPath("$.currentBalance").value(1500.00))
        .andExpect(jsonPath("$.transactions", hasSize(2)));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void transfer_ShouldCreateTransaction() throws Exception {
    // Arrange
    when(transactionService.transfer(any(TransferDTO.class))).thenReturn(sampleTransaction);

    // Act & Assert
    mockMvc.perform(post("/transactions/transfer")
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(transferDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("TRANSFER"))
        .andExpect(jsonPath("$.sourceAccountNumber").value("ACC-123456"))
        .andExpect(jsonPath("$.destinationAccountNumber").value("ACC-789012"));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void deposit_ShouldCreateTransaction() throws Exception {
    // Arrange
    TransactionDTO depositTransaction = TransactionDTO.builder()
        .id(3L)
        .transactionId(UUID.randomUUID().toString())
        .amount(new BigDecimal("500.00"))
        .type(Transaction.TransactionType.DEPOSIT)
        .status(Transaction.TransactionStatus.COMPLETED)
        .destinationAccountNumber("ACC-123456")
        .createdAt(now)
        .processedAt(now)
        .build();

    when(transactionService.deposit(
        anyString(),
        any(BigDecimal.class),
        anyString()))
        .thenReturn(depositTransaction);

    // Act & Assert
    mockMvc.perform(post("/transactions/deposit")
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .param("accountNumber", "ACC-123456")
            .param("amount", "500.00")
            .param("description", "Test deposit"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("DEPOSIT"))
        .andExpect(jsonPath("$.amount").value(500.00))
        .andExpect(jsonPath("$.destinationAccountNumber").value("ACC-123456"));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void withdraw_ShouldCreateTransaction() throws Exception {
    // Arrange
    TransactionDTO withdrawalTransaction = TransactionDTO.builder()
        .id(4L)
        .transactionId(UUID.randomUUID().toString())
        .amount(new BigDecimal("300.00"))
        .type(Transaction.TransactionType.WITHDRAWAL)
        .status(Transaction.TransactionStatus.COMPLETED)
        .sourceAccountNumber("ACC-123456")
        .createdAt(now)
        .processedAt(now)
        .build();

    when(transactionService.withdraw(
        anyString(),
        any(BigDecimal.class),
        anyString()))
        .thenReturn(withdrawalTransaction);

    // Act & Assert
    mockMvc.perform(post("/transactions/withdraw")
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .param("accountNumber", "ACC-123456")
            .param("amount", "300.00")
            .param("description", "Test withdrawal"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("WITHDRAWAL"))
        .andExpect(jsonPath("$.amount").value(300.00))
        .andExpect(jsonPath("$.sourceAccountNumber").value("ACC-123456"));
  }
}
