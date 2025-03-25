package com.example.financeservice.repository;

import com.example.financeservice.model.Merchant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Long> {
  Optional<Merchant> findByEmail(String email);
  Optional<Merchant> findByNif(String nif);
  boolean existsByEmail(String email);
  boolean existsByNif(String nif);
}