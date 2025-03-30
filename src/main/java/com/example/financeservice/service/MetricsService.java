package com.example.financeservice.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.math.BigDecimal;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Centralized service for Finance Service metrics registration. Provides methods to record
 * various business and technical metrics.
 */
@Service
@Slf4j
public class MetricsService {

  private static final String OPERATION_TYPE_DEPOSIT = "DEPOSIT";
  private static final String OPERATION_TYPE_WITHDRAWAL = "WITHDRAWAL";
  private static final String OPERATION_TYPE_TRANSFER = "TRANSFER";
  private static final String STATUS_TAG = "status";
  private static final String SUCCESS_TAG_VALUE = "success";
  private static final String FAILED_TAG_VALUE = "failed";
  private static final String FINANCE_DAILY_VOLUME = "finance.daily_volume";

  @Getter
  private final MeterRegistry registry;

  // Account metrics
  private final Counter accountCreationCounter;
  private final Counter accountStatusUpdateCounter;

  // Client metrics
  private final Counter clientCreationCounter;
  private final Counter clientUpdateCounter;
  private final Counter clientDeletionCounter;

  // Transaction metrics
  private final Counter transactionCounter;
  private final DistributionSummary transactionAmounts;
  private final Counter successfulTransactionCounter;
  private final Counter failedTransactionCounter;

  // Financial operation metrics
  private final DistributionSummary depositAmounts;
  private final DistributionSummary withdrawalAmounts;
  private final DistributionSummary transferAmounts;

  // Daily financial volume counters
  private final Counter depositVolumeCounter;
  private final Counter withdrawalVolumeCounter;
  private final Counter transferVolumeCounter;

  public MetricsService(MeterRegistry registry) {
    this.registry = registry;

    // Initialize account metrics
    this.accountCreationCounter = Counter.builder("finance.accounts.created")
        .description("Total number of created accounts")
        .register(registry);

    this.accountStatusUpdateCounter = Counter.builder("finance.accounts.status_updated")
        .description("Total number of account status updates")
        .register(registry);

    // Initialize client metrics
    this.clientCreationCounter = Counter.builder("finance.clients.created")
        .description("Total number of created clients")
        .register(registry);

    this.clientUpdateCounter = Counter.builder("finance.clients.updated")
        .description("Total number of client updates")
        .register(registry);

    this.clientDeletionCounter = Counter.builder("finance.clients.deleted")
        .description("Total number of client deletions")
        .register(registry);

    // Initialize transaction metrics
    this.transactionCounter = Counter.builder("finance.transactions.count")
        .description("Total number of processed transactions")
        .register(registry);

    this.transactionAmounts = DistributionSummary.builder("finance.transactions.amount")
        .description("Distribution of transaction amounts")
        .baseUnit("BRL")
        .publishPercentiles(0.5, 0.75, 0.95, 0.99)
        .register(registry);

    this.successfulTransactionCounter = Counter.builder("finance.transactions.result")
        .tag(STATUS_TAG, SUCCESS_TAG_VALUE)
        .description("Number of successful transactions")
        .register(registry);

    this.failedTransactionCounter = Counter.builder("finance.transactions.result")
        .tag(STATUS_TAG, FAILED_TAG_VALUE)
        .description("Number of failed transactions")
        .register(registry);

    // Initialize financial operation metrics
    this.depositAmounts = DistributionSummary.builder("finance.operations.deposit.amount")
        .description("Distribution of deposit amounts")
        .baseUnit("BRL")
        .publishPercentiles(0.5, 0.75, 0.95, 0.99)
        .register(registry);

    this.withdrawalAmounts = DistributionSummary.builder("finance.operations.withdrawal.amount")
        .description("Distribution of withdrawal amounts")
        .baseUnit("BRL")
        .publishPercentiles(0.5, 0.75, 0.95, 0.99)
        .register(registry);

    this.transferAmounts = DistributionSummary.builder("finance.operations.transfer.amount")
        .description("Distribution of transfer amounts")
        .baseUnit("BRL")
        .publishPercentiles(0.5, 0.75, 0.95, 0.99)
        .register(registry);

    // Initialize financial volume counters
    this.depositVolumeCounter = Counter.builder(FINANCE_DAILY_VOLUME)
        .tag("type", OPERATION_TYPE_DEPOSIT)
        .description("Daily financial volume of deposits")
        .register(registry);

    this.withdrawalVolumeCounter = Counter.builder(FINANCE_DAILY_VOLUME)
        .tag("type", OPERATION_TYPE_WITHDRAWAL)
        .description("Daily financial volume of withdrawals")
        .register(registry);

    this.transferVolumeCounter = Counter.builder(FINANCE_DAILY_VOLUME)
        .tag("type", OPERATION_TYPE_TRANSFER)
        .description("Daily financial volume of transfers")
        .register(registry);

    log.info("MetricsService successfully initialized");
  }

  // Methods for recording account events
  public void recordAccountCreated(String type, String ownerType) {
    accountCreationCounter.increment();
    Counter.builder("finance.accounts.created.detailed")
        .tag("type", type)
        .tag("owner", ownerType)
        .register(registry)
        .increment();
    log.debug("Metric: Account created of type {} for {}", type, ownerType);
  }

  public void recordAccountStatusUpdate(String previousStatus, String newStatus) {
    accountStatusUpdateCounter.increment();
    Counter.builder("finance.accounts.status_transition")
        .tag("from", previousStatus)
        .tag("to", newStatus)
        .register(registry)
        .increment();
    log.debug("Metric: Account status updated from {} to {}", previousStatus, newStatus);
  }

  // Methods for recording client events
  public void recordClientCreated() {
    clientCreationCounter.increment();
    log.debug("Metric: Client created");
  }

  public void recordClientUpdated() {
    clientUpdateCounter.increment();
    log.debug("Metric: Client updated");
  }

  public void recordClientDeleted() {
    clientDeletionCounter.increment();
    log.debug("Metric: Client deleted");
  }

  // Methods for recording transaction events
  public void recordTransactionProcessed(String type, BigDecimal amount, boolean success) {
    transactionCounter.increment();
    transactionAmounts.record(amount.doubleValue());

    if (success) {
      successfulTransactionCounter.increment();

      // Record specific metrics by transaction type
      switch (type) {
        case OPERATION_TYPE_DEPOSIT:
          depositAmounts.record(amount.doubleValue());
          break;
        case OPERATION_TYPE_WITHDRAWAL:
          withdrawalAmounts.record(amount.doubleValue());
          break;
        case OPERATION_TYPE_TRANSFER:
          transferAmounts.record(amount.doubleValue());
          break;
        default:
          log.warn("Unknown transaction type: {}", type);
          break;
      }
    } else {
      failedTransactionCounter.increment();
    }

    // Specific counter by transaction type
    Counter.builder("finance.transactions.by_type")
        .tag("type", type)
        .register(registry)
        .increment();

    log.debug("Metric: Transaction processed - type: {}, amount: {}, success: {}",
        type, amount, success);
  }

  // Methods for recording performance metrics
  public <T> T recordServiceExecutionTime(String service, String method, Supplier<T> execution) {
    return Timer.builder("finance.performance.service")
        .tag("service", service)
        .tag("method", method)
        .register(registry)
        .record(execution);
  }

  public <T> T recordRepositoryExecutionTime(String repository, String method,
      Supplier<T> execution) {
    return Timer.builder("finance.performance.repository")
        .tag("repository", repository)
        .tag("method", method)
        .register(registry)
        .record(execution);
  }

  public void recordExceptionOccurred(String exceptionType, String operationType) {
    Counter.builder("finance.exceptions")
        .tag("type", exceptionType)
        .tag("operation", operationType)
        .register(registry)
        .increment();
    log.debug("Metric: Exception occurred - type: {}, operation: {}", exceptionType, operationType);
  }

  // Methods for manual timer (for more complex cases)
  public Timer.Sample startTimer() {
    return Timer.start(registry);
  }

  public void stopTimer(Timer.Sample sample, String name, String... tags) {
    if (tags.length % 2 != 0) {
      throw new IllegalArgumentException("Tags must be key-value pairs (even number of arguments)");
    }

    Timer.Builder builder = Timer.builder(name);
    for (int i = 0; i < tags.length; i += 2) {
      builder.tag(tags[i], tags[i + 1]);
    }

    sample.stop(builder.register(registry));
  }

  // Method for recording daily financial volumes
  public void recordDailyFinancialVolume(String operationType, BigDecimal amount) {
    double value = amount.doubleValue();

    switch (operationType) {
      case OPERATION_TYPE_DEPOSIT:
        depositVolumeCounter.increment(value);
        break;
      case OPERATION_TYPE_WITHDRAWAL:
        withdrawalVolumeCounter.increment(value);
        break;
      case OPERATION_TYPE_TRANSFER:
        transferVolumeCounter.increment(value);
        break;
      default:
        log.warn("Unknown financial operation type: {}", operationType);
        break;
    }

    log.debug("Metric: Daily financial volume updated - type: {}, amount: {}",
        operationType, amount);
  }

  public void recordSuccessfulLogin(String username) {
    Counter.builder("finance.authentication.login")
        .tag(STATUS_TAG, SUCCESS_TAG_VALUE)
        .register(registry)
        .increment();
    log.debug("Metric: Successful login for user {}", username);
  }

  public void recordFailedLogin(String username) {
    Counter.builder("finance.authentication.login")
        .tag(STATUS_TAG, FAILED_TAG_VALUE)
        .register(registry)
        .increment();
    log.debug("Metric: Failed login for user {}", username);
  }
}
