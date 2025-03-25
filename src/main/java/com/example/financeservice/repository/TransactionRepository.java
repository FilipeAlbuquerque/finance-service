package com.example.financeservice.repository;

import com.example.financeservice.model.Account;
import com.example.financeservice.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

  Optional<Transaction> findByTransactionId(String transactionId);

  List<Transaction> findBySourceAccount(Account account);

  List<Transaction> findByDestinationAccount(Account account);

  @Query("SELECT t FROM Transaction t WHERE t.sourceAccount = :account OR t.destinationAccount = :account")
  List<Transaction> findByAccount(@Param("account") Account account);

  @Query("SELECT t FROM Transaction t WHERE t.sourceAccount = :account OR t.destinationAccount = :account")
  Page<Transaction> findByAccountPaginated(@Param("account") Account account, Pageable pageable);

  @Query("SELECT t FROM Transaction t WHERE (t.sourceAccount = :account OR t.destinationAccount = :account) AND t.createdAt BETWEEN :startDate AND :endDate")
  List<Transaction> findByAccountAndDateRange(
      @Param("account") Account account,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);
}
