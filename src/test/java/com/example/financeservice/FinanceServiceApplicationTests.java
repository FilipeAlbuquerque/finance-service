package com.example.financeservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;


@DataJpaTest
@ActiveProfiles("test")
class FinanceServiceApplicationTests {

  @Test
  void contextLoads() {
    // This just verifies that the test context loads successfully
    System.out.println("Context loaded successfully.");
  }
}