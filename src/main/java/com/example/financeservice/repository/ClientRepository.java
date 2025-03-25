package com.example.financeservice.repository;

import com.example.financeservice.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
  Optional<Client> findByEmail(String email);
  Optional<Client> findByDocumentNumber(String documentNumber);
  boolean existsByEmail(String email);
  boolean existsByDocumentNumber(String documentNumber);
}
