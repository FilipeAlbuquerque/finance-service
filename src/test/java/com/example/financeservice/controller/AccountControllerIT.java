package com.example.financeservice.controller;

import com.example.financeservice.dto.AccountDTO;
import com.example.financeservice.dto.CreateAccountDTO;
import com.example.financeservice.model.Account;
import com.example.financeservice.model.Account.AccountType;
import com.example.financeservice.security.JwtRequestFilter;
import com.example.financeservice.security.JwtUtils;
import com.example.financeservice.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import({JwtRequestFilter.class})
class AccountControllerIT {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private AccountService accountService;

  @MockBean
  private UserDetailsService userDetailsService;

  @MockBean
  private JwtUtils jwtUtils;

  @MockBean
  private AuthenticationManager authenticationManager;

  private AccountDTO sampleAccount;
  private List<AccountDTO> accountList;
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

    // Create sample account
    sampleAccount = AccountDTO.builder()
        .id(1L)
        .accountNumber("ACC-123456789")
        .balance(new BigDecimal("1000.00"))
        .availableLimit(new BigDecimal("1000.00"))
        .type(Account.AccountType.CHECKING)
        .status(Account.AccountStatus.ACTIVE)
        .createdAt(now)
        .ownerId(1L)
        .ownerName("Test Client")
        .ownerType("CLIENT")
        .build();

    // Create second account
    AccountDTO secondAccount = AccountDTO.builder()
        .id(2L)
        .accountNumber("ACC-987654321")
        .balance(new BigDecimal("2000.00"))
        .availableLimit(new BigDecimal("2000.00"))
        .type(Account.AccountType.SAVINGS)
        .status(Account.AccountStatus.ACTIVE)
        .createdAt(now)
        .ownerId(1L)
        .ownerName("Test Client")
        .ownerType("CLIENT")
        .build();

    accountList = Arrays.asList(sampleAccount, secondAccount);
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void getAllAccounts_ShouldReturnAccounts() throws Exception {
    // Arrange
    when(accountService.getAllAccounts()).thenReturn(accountList);

    // Act & Assert
    mockMvc.perform(get("/accounts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].id").value(1L))
        .andExpect(jsonPath("$[0].accountNumber").value("ACC-123456789"))
        .andExpect(jsonPath("$[0].ownerType").value("CLIENT"))
        .andExpect(jsonPath("$[1].id").value(2L))
        .andExpect(jsonPath("$[1].accountNumber").value("ACC-987654321"))
        .andExpect(jsonPath("$[1].ownerType").value("CLIENT"));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void getAccountById_ShouldReturnAccount() throws Exception {
    // Arrange
    when(accountService.getAccountById(1L)).thenReturn(sampleAccount);

    // Act & Assert
    mockMvc.perform(get("/accounts/{id}", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.accountNumber").value("ACC-123456789"))
        .andExpect(jsonPath("$.balance").value(1000.00))
        .andExpect(jsonPath("$.availableLimit").value(1000.00))
        .andExpect(jsonPath("$.type").value("CHECKING"))
        .andExpect(jsonPath("$.ownerName").value("Test Client"));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void getAccountByNumber_ShouldReturnAccount() throws Exception {
    // Arrange
    when(accountService.getAccountByNumber("ACC-123456789")).thenReturn(sampleAccount);

    // Act & Assert
    mockMvc.perform(get("/accounts/number/{accountNumber}", "ACC-123456789"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.accountNumber").value("ACC-123456789"))
        .andExpect(jsonPath("$.balance").value(1000.00))
        .andExpect(jsonPath("$.ownerId").value(1L));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void getAccountsByClient_ShouldReturnAccounts() throws Exception {
    // Arrange
    when(accountService.getAccountsByClient(1L)).thenReturn(accountList);

    // Act & Assert
    mockMvc.perform(get("/accounts/client/{clientId}", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].ownerId").value(1L))
        .andExpect(jsonPath("$[0].ownerType").value("CLIENT"))
        .andExpect(jsonPath("$[1].ownerId").value(1L))
        .andExpect(jsonPath("$[1].ownerType").value("CLIENT"));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void createClientAccount_ShouldCreateAndReturnAccount() throws Exception {
    // Arrange
    CreateAccountDTO createAccountDTO = new CreateAccountDTO();
    createAccountDTO.setOwnerId(1L);
    createAccountDTO.setType(Account.AccountType.CHECKING);
    createAccountDTO.setInitialDeposit(new BigDecimal("1000.00"));

    when(accountService.createClientAccount(any(CreateAccountDTO.class))).thenReturn(sampleAccount);

    // Act & Assert
    mockMvc.perform(post("/accounts/client")
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createAccountDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.accountNumber").value("ACC-123456789"))
        .andExpect(jsonPath("$.balance").value(1000.00))
        .andExpect(jsonPath("$.ownerType").value("CLIENT"));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void createMerchantAccount_ShouldCreateAndReturnAccount() throws Exception {
    // Arrange
    CreateAccountDTO createAccountDTO = new CreateAccountDTO();
    createAccountDTO.setOwnerId(2L);
    createAccountDTO.setType(AccountType.BUSINESS);
    createAccountDTO.setInitialDeposit(new BigDecimal("5000.00"));

    AccountDTO merchantAccount = AccountDTO.builder()
        .id(3L)
        .accountNumber("MERCH-123456")
        .balance(new BigDecimal("5000.00"))
        .availableLimit(new BigDecimal("5000.00"))
        .type(AccountType.BUSINESS)
        .status(Account.AccountStatus.ACTIVE)
        .createdAt(now)
        .ownerId(2L)
        .ownerName("Test Merchant")
        .ownerType("MERCHANT")
        .build();

    when(accountService.createMerchantAccount(any(CreateAccountDTO.class))).thenReturn(merchantAccount);

    // Act & Assert
    mockMvc.perform(post("/accounts/merchant")
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createAccountDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(3L))
        .andExpect(jsonPath("$.accountNumber").value("MERCH-123456"))
        .andExpect(jsonPath("$.balance").value(5000.00))
        .andExpect(jsonPath("$.ownerType").value("MERCHANT"));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void deposit_ShouldUpdateAndReturnAccount() throws Exception {
    // Arrange
    AccountDTO updatedAccount = AccountDTO.builder()
        .id(1L)
        .accountNumber("ACC-123456789")
        .balance(new BigDecimal("1500.00"))
        .availableLimit(new BigDecimal("1500.00"))
        .type(Account.AccountType.CHECKING)
        .status(Account.AccountStatus.ACTIVE)
        .createdAt(now)
        .ownerId(1L)
        .ownerName("Test Client")
        .ownerType("CLIENT")
        .build();

    when(accountService.deposit(anyString(), any(BigDecimal.class))).thenReturn(updatedAccount);

    // Act & Assert
    mockMvc.perform(post("/accounts/{accountNumber}/deposit", "ACC-123456789")
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .param("amount", "500.00"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.balance").value(1500.00))
        .andExpect(jsonPath("$.availableLimit").value(1500.00));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void withdraw_ShouldUpdateAndReturnAccount() throws Exception {
    // Arrange
    AccountDTO updatedAccount = AccountDTO.builder()
        .id(1L)
        .accountNumber("ACC-123456789")
        .balance(new BigDecimal("500.00"))
        .availableLimit(new BigDecimal("500.00"))
        .type(Account.AccountType.CHECKING)
        .status(Account.AccountStatus.ACTIVE)
        .createdAt(now)
        .ownerId(1L)
        .ownerName("Test Client")
        .ownerType("CLIENT")
        .build();

    when(accountService.withdraw(anyString(), any(BigDecimal.class))).thenReturn(updatedAccount);

    // Act & Assert
    mockMvc.perform(post("/accounts/{accountNumber}/withdraw", "ACC-123456789")
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .param("amount", "500.00"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.balance").value(500.00))
        .andExpect(jsonPath("$.availableLimit").value(500.00));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void updateAccountStatus_ShouldUpdateAndReturnAccount() throws Exception {
    // Arrange
    AccountDTO updatedAccount = AccountDTO.builder()
        .id(1L)
        .accountNumber("ACC-123456789")
        .balance(new BigDecimal("1000.00"))
        .availableLimit(new BigDecimal("1000.00"))
        .type(Account.AccountType.CHECKING)
        .status(Account.AccountStatus.BLOCKED)
        .createdAt(now)
        .ownerId(1L)
        .ownerName("Test Client")
        .ownerType("CLIENT")
        .build();

    when(accountService.updateAccountStatus(anyLong(), eq(Account.AccountStatus.BLOCKED)))
        .thenReturn(updatedAccount);

    // Act & Assert
    mockMvc.perform(put("/accounts/{id}/status", 1L)
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .param("status", "BLOCKED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("BLOCKED"));
  }
}
