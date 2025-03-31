package com.example.financeservice.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetricsServiceTest {

  private SimpleMeterRegistry registry;
  private MetricsService metricsService;

  @BeforeEach
  void setUp() {
    // Usar um registry real para todos os testes
    registry = new SimpleMeterRegistry();
    metricsService = new MetricsService(registry);
  }

  @Test
  void recordAccountCreated_ShouldIncrementCounters() {
    // Act
    metricsService.recordAccountCreated("CHECKING", "CLIENT");

    // Assert
    assertEquals(1, registry.get("finance.accounts.created").counter().count());
    assertEquals(1, registry.get("finance.accounts.created.detailed")
        .tag("type", "CHECKING")
        .tag("owner", "CLIENT")
        .counter().count());
  }

  @Test
  void recordAccountStatusUpdate_ShouldIncrementCounters() {
    // Act
    metricsService.recordAccountStatusUpdate("ACTIVE", "BLOCKED");

    // Assert
    assertEquals(1, registry.get("finance.accounts.status_updated").counter().count());
    assertEquals(1, registry.get("finance.accounts.status_transition")
        .tag("from", "ACTIVE")
        .tag("to", "BLOCKED")
        .counter().count());
  }

  @Test
  void recordClientOperations_ShouldIncrementAppropriateCounters() {
    // Act
    metricsService.recordClientCreated();
    metricsService.recordClientUpdated();
    metricsService.recordClientDeleted();

    // Assert
    assertEquals(1, registry.get("finance.clients.created").counter().count());
    assertEquals(1, registry.get("finance.clients.updated").counter().count());
    assertEquals(1, registry.get("finance.clients.deleted").counter().count());
  }

  @Test
  void recordTransactionProcessed_WithSuccessfulDeposit_ShouldIncrementAndRecordMetrics() {
    // Arrange
    BigDecimal amount = new BigDecimal("150.00");

    // Act
    metricsService.recordTransactionProcessed("DEPOSIT", amount, true);

    // Assert
    assertEquals(1, registry.get("finance.transactions.count").counter().count());
    assertEquals(1, registry.get("finance.transactions.result")
        .tag("status", "success")
        .counter().count());
    assertEquals(150.0, registry.get("finance.transactions.amount").summary().totalAmount());
    assertEquals(150.0, registry.get("finance.operations.deposit.amount").summary().totalAmount());
    assertEquals(1, registry.get("finance.transactions.by_type")
        .tag("type", "DEPOSIT")
        .counter().count());
  }

  @Test
  void recordTransactionProcessed_WithFailedTransaction_ShouldIncrementFailureMetrics() {
    // Arrange
    BigDecimal amount = new BigDecimal("200.00");

    // Act
    metricsService.recordTransactionProcessed("WITHDRAWAL", amount, false);

    // Assert
    assertEquals(1, registry.get("finance.transactions.count").counter().count());
    assertEquals(1, registry.get("finance.transactions.result")
        .tag("status", "failed")
        .counter().count());
    assertEquals(200.0, registry.get("finance.transactions.amount").summary().totalAmount());
    // Não deve registrar no withdrawalAmounts, pois a transação falhou
    assertEquals(0.0, registry.get("finance.operations.withdrawal.amount").summary().totalAmount());
    assertEquals(1, registry.get("finance.transactions.by_type")
        .tag("type", "WITHDRAWAL")
        .counter().count());
  }

  @Test
  void recordRepositoryExecutionTime_ShouldExecuteSupplierAndRecordTime() {
    // Arrange
    String result = "test result";

    // Act
    String actual = metricsService.recordRepositoryExecutionTime(
        "TestRepository", "findById", () -> {
          // Não precisamos de Thread.sleep aqui, pois o timer ainda vai registrar
          // algum tempo de execução, mesmo que mínimo
          return result;
        });

    // Assert
    assertEquals(result, actual);

    Timer timer = registry.get("finance.performance.repository")
        .tag("repository", "TestRepository")
        .tag("method", "findById")
        .timer();

    assertTrue(timer.count() > 0);
    // Verificamos apenas que algum tempo foi registrado, sem verificar um valor específico
    assertTrue(timer.totalTime(TimeUnit.NANOSECONDS) > 0);
  }

  @Test
  void recordServiceExecutionTime_ShouldExecuteSupplierAndRecordTime() {
    // Arrange
    Integer result = 42;

    // Act
    Integer actual = metricsService.recordServiceExecutionTime(
        "TestService", "calculate", () -> {
          // Não precisamos de Thread.sleep aqui
          return result;
        });

    // Assert
    assertEquals(result, actual);

    Timer timer = registry.get("finance.performance.service")
        .tag("service", "TestService")
        .tag("method", "calculate")
        .timer();

    assertTrue(timer.count() > 0);
    // Verificamos apenas que algum tempo foi registrado
    assertTrue(timer.totalTime(TimeUnit.NANOSECONDS) > 0);
  }

  @Test
  void recordExceptionOccurred_ShouldIncrementExceptionCounter() {
    // Act
    metricsService.recordExceptionOccurred("IllegalArgumentException", "validateInput");

    // Assert
    assertEquals(1, registry.get("finance.exceptions")
        .tag("type", "IllegalArgumentException")
        .tag("operation", "validateInput")
        .counter().count());
  }

  @Test
  void startTimerAndStopTimer_ShouldCreateAndStopTimer() {
    // Act
    Timer.Sample sample = metricsService.startTimer();
    // Removemos o Thread.sleep e simplesmente paramos o timer
    metricsService.stopTimer(sample, "finance.operations.test", "operation", "test");

    // Assert
    Timer timer = registry.get("finance.operations.test")
        .tag("operation", "test")
        .timer();

    assertTrue(timer.count() > 0);
    // Verificamos apenas que algum tempo foi registrado, sem verificar um valor específico
    assertTrue(timer.totalTime(TimeUnit.NANOSECONDS) > 0);
  }

  @Test
  void stopTimer_WithOddNumberOfTags_ShouldThrowException() {
    // Arrange
    Timer.Sample sample = metricsService.startTimer();

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> metricsService.stopTimer(sample, "finance.operations.test", "single_tag"));

    assertEquals("Tags must be key-value pairs (even number of arguments)", exception.getMessage());
  }

  @Test
  void recordDailyFinancialVolume_ShouldIncrementAppropriateCounter() {
    // Act
    metricsService.recordDailyFinancialVolume("DEPOSIT", new BigDecimal("100.00"));
    metricsService.recordDailyFinancialVolume("WITHDRAWAL", new BigDecimal("50.00"));
    metricsService.recordDailyFinancialVolume("TRANSFER", new BigDecimal("75.00"));

    // Assert
    assertEquals(100.0, registry.get("finance.daily_volume")
        .tag("type", "DEPOSIT")
        .counter().count());
    assertEquals(50.0, registry.get("finance.daily_volume")
        .tag("type", "WITHDRAWAL")
        .counter().count());
    assertEquals(75.0, registry.get("finance.daily_volume")
        .tag("type", "TRANSFER")
        .counter().count());
  }

  @Test
  void recordLoginMetrics_ShouldIncrementLoginCounters() {
    // Act
    metricsService.recordSuccessfulLogin("testuser");
    metricsService.recordFailedLogin("baduser");

    // Assert
    assertEquals(1, registry.get("finance.authentication.login")
        .tag("status", "success")
        .counter().count());
    assertEquals(1, registry.get("finance.authentication.login")
        .tag("status", "failed")
        .counter().count());
  }

  @Test
  void getRegistry_ShouldReturnTheRegistry() {
    // Act
    MeterRegistry result = metricsService.getRegistry();

    // Assert
    assertEquals(registry, result);
  }
}
