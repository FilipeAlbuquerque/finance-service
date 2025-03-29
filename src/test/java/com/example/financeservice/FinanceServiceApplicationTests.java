package com.example.financeservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FinanceServiceApplicationTests {

  // Specify bean name to avoid ambiguity
  @MockBean(name = "userDetailsService")
  private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

  @MockBean
  private AuthenticationManager authenticationManager;

  @Test
  void contextLoads() {
    System.out.println("Context loaded successfully.");
  }
}
