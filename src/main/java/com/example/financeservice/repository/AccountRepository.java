package com.example.financeservice.repository;

import com.example.financeservice.model.Account;
import com.example.financeservice.model.Client;
import com.example.financeservice.model.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
  Optional<Account> findByAccountNumber(String accountNumber);

  List<Account> findByClient(Client client);

  List<Account> findByMerchant(Merchant merchant);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT a FROM Account a WHERE a.id = :id")
  Optional<Account> findByIdWithLock(Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
  Optional<Account> findByAccountNumberWithLock(String accountNumber);
}
