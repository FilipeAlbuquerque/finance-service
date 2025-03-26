package com.example.financeservice.service.account;


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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

  private final AccountRepository accountRepository;
  private final ClientRepository clientRepository;
  private final MerchantRepository merchantRepository;

  @Transactional(readOnly = true)
  public List<AccountDTO> getAllAccounts() {
    return accountRepository.findAll().stream()
        .map(this::convertToDTO)
        .toList();
  }

  @Transactional(readOnly = true)
  public AccountDTO getAccountById(Long id) {
    Account account = accountRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
    return convertToDTO(account);
  }

  @Transactional(readOnly = true)
  public AccountDTO getAccountByNumber(String accountNumber) {
    Account account = accountRepository.findByAccountNumber(accountNumber)
        .orElseThrow(
            () -> new ResourceNotFoundException("Account not found with number: " + accountNumber));
    return convertToDTO(account);
  }

  @Transactional(readOnly = true)
  public List<AccountDTO> getAccountsByClient(Long clientId) {
    Client client = clientRepository.findById(clientId)
        .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + clientId));

    return accountRepository.findByClient(client).stream()
        .map(this::convertToDTO)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<AccountDTO> getAccountsByMerchant(Long merchantId) {
    Merchant merchant = merchantRepository.findById(merchantId)
        .orElseThrow(
            () -> new ResourceNotFoundException("Merchant not found with id: " + merchantId));

    return accountRepository.findByMerchant(merchant).stream()
        .map(this::convertToDTO)
        .collect(Collectors.toList());
  }

  @Transactional
  public AccountDTO createClientAccount(CreateAccountDTO createAccountDTO) {
    Client client = clientRepository.findById(createAccountDTO.getOwnerId())
        .orElseThrow(() -> new ResourceNotFoundException(
            "Client not found with id: " + createAccountDTO.getOwnerId()));

    Account account = new Account();
    account.setType(createAccountDTO.getType());
    account.setBalance(BigDecimal.ZERO);
    account.setClient(client);

    if (createAccountDTO.getInitialDeposit() != null
        && createAccountDTO.getInitialDeposit().compareTo(BigDecimal.ZERO) > 0) {
      account.setBalance(createAccountDTO.getInitialDeposit());
    }

    if (createAccountDTO.getAvailableLimit() != null) {
      account.setAvailableLimit(createAccountDTO.getAvailableLimit());
    }

    Account savedAccount = accountRepository.save(account);
    return convertToDTO(savedAccount);
  }

  @Transactional
  public AccountDTO createMerchantAccount(CreateAccountDTO createAccountDTO) {
    Merchant merchant = merchantRepository.findById(createAccountDTO.getOwnerId())
        .orElseThrow(() -> new ResourceNotFoundException(
            "Merchant not found with id: " + createAccountDTO.getOwnerId()));

    Account account = new Account();
    account.setType(createAccountDTO.getType());
    account.setBalance(BigDecimal.ZERO);
    account.setMerchant(merchant);

    if (createAccountDTO.getInitialDeposit() != null
        && createAccountDTO.getInitialDeposit().compareTo(BigDecimal.ZERO) > 0) {
      account.setBalance(createAccountDTO.getInitialDeposit());
    }

    if (createAccountDTO.getAvailableLimit() != null) {
      account.setAvailableLimit(createAccountDTO.getAvailableLimit());
    }

    Account savedAccount = accountRepository.save(account);
    return convertToDTO(savedAccount);
  }

  @Transactional
  public AccountDTO deposit(String accountNumber, BigDecimal amount) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Deposit amount must be positive");
    }

    Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
        .orElseThrow(
            () -> new ResourceNotFoundException("Account not found with number: " + accountNumber));

    account.setBalance(account.getBalance().add(amount));
    Account updatedAccount = accountRepository.save(account);

    return convertToDTO(updatedAccount);
  }

  @Transactional
  public AccountDTO withdraw(String accountNumber, BigDecimal amount) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Withdrawal amount must be positive");
    }

    Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
        .orElseThrow(
            () -> new ResourceNotFoundException("Account not found with number: " + accountNumber));

    if (account.getBalance().compareTo(amount) < 0) {
      throw new InsufficientFundsException("Insufficient funds for withdrawal");
    }

    account.setBalance(account.getBalance().subtract(amount));
    Account updatedAccount = accountRepository.save(account);

    return convertToDTO(updatedAccount);
  }

  @Transactional
  public AccountDTO updateAccountStatus(Long id, Account.AccountStatus status) {
    Account account = accountRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));

    account.setStatus(status);
    Account updatedAccount = accountRepository.save(account);

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