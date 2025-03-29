package com.example.financeservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@ActiveProfiles("test")
class FinanceServiceApplicationTests {

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  void contextLoads() {
    // This verifies that the test context loads successfully
    System.out.println("Context loaded successfully.");

    // Assert that the application context is not null
    assertNotNull(applicationContext, "Application context should not be null");
  }
}