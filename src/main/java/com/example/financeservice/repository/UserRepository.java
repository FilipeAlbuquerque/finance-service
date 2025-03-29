package com.example.financeservice.repository;

import com.example.financeservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByUsername(String username);

  Optional<User> findByEmail(String email);

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);

  @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r = :role")
  long countByRole(String role);

  Optional<User> findByPasswordResetToken(String token);
}
