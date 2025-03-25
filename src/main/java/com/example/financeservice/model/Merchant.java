package com.example.financeservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "merchants")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Merchant {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "Business name is required")
  @Size(min = 3, max = 100, message = "Business name must be between 3 and 100 characters")
  private String businessName;

  @Column(name = "trading_name")
  private String tradingName;

  @NotBlank(message = "Email is required")
  @Email(message = "Email should be valid")
  @Column(unique = true)
  private String email;

  @NotBlank(message = "NIF is required")
  @Column(unique = true)
  private String nif;

  @NotBlank(message = "Phone is required")
  private String phone;

  private String address;

  @Column(name = "merchant_category_code")
  private String merchantCategoryCode;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "merchant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Account> accounts = new ArrayList<>();

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}