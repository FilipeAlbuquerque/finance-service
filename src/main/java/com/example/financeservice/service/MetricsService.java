package com.example.financeservice.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.math.BigDecimal;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Serviço centralizado para registro de métricas do Finance Service. Fornece métodos para registrar
 * diversas métricas de negócio e técnicas.
 */
@Service
@Slf4j
public class MetricsService {

  private final MeterRegistry registry;

  // Métricas de conta
  private final Counter accountCreationCounter;
  private final Counter accountStatusUpdateCounter;

  // Métricas de cliente
  private final Counter clientCreationCounter;
  private final Counter clientUpdateCounter;
  private final Counter clientDeletionCounter;

  // Métricas de transações
  private final Counter transactionCounter;
  private final DistributionSummary transactionAmounts;
  private final Counter successfulTransactionCounter;
  private final Counter failedTransactionCounter;

  // Métricas de operações financeiras
  private final DistributionSummary depositAmounts;
  private final DistributionSummary withdrawalAmounts;
  private final DistributionSummary transferAmounts;

  // Métricas de performance
  private final Timer serviceExecutionTimer;
  private final Timer repositoryExecutionTimer;
  private final Counter exceptionCounter;

  // Counters para volumes financeiros diários
  private final Counter depositVolumeCounter;
  private final Counter withdrawalVolumeCounter;
  private final Counter transferVolumeCounter;

  public MetricsService(MeterRegistry registry) {
    this.registry = registry;

    // Inicialização das métricas de conta
    this.accountCreationCounter = Counter.builder("finance.accounts.created")
        .description("Número total de contas criadas")
        .register(registry);

    this.accountStatusUpdateCounter = Counter.builder("finance.accounts.status_updated")
        .description("Número total de atualizações de status de contas")
        .register(registry);

    // Inicialização das métricas de cliente
    this.clientCreationCounter = Counter.builder("finance.clients.created")
        .description("Número total de clientes criados")
        .register(registry);

    this.clientUpdateCounter = Counter.builder("finance.clients.updated")
        .description("Número total de atualizações de clientes")
        .register(registry);

    this.clientDeletionCounter = Counter.builder("finance.clients.deleted")
        .description("Número total de exclusões de clientes")
        .register(registry);

    // Inicialização das métricas de transações
    this.transactionCounter = Counter.builder("finance.transactions.count")
        .description("Número total de transações processadas")
        .register(registry);

    this.transactionAmounts = DistributionSummary.builder("finance.transactions.amount")
        .description("Distribuição dos valores das transações")
        .baseUnit("BRL")
        .publishPercentiles(0.5, 0.75, 0.95, 0.99)
        .register(registry);

    this.successfulTransactionCounter = Counter.builder("finance.transactions.result")
        .tag("status", "success")
        .description("Número de transações bem-sucedidas")
        .register(registry);

    this.failedTransactionCounter = Counter.builder("finance.transactions.result")
        .tag("status", "failed")
        .description("Número de transações que falharam")
        .register(registry);

    // Inicialização das métricas de operações financeiras
    this.depositAmounts = DistributionSummary.builder("finance.operations.deposit.amount")
        .description("Distribuição dos valores de depósitos")
        .baseUnit("BRL")
        .publishPercentiles(0.5, 0.75, 0.95, 0.99)
        .register(registry);

    this.withdrawalAmounts = DistributionSummary.builder("finance.operations.withdrawal.amount")
        .description("Distribuição dos valores de saques")
        .baseUnit("BRL")
        .publishPercentiles(0.5, 0.75, 0.95, 0.99)
        .register(registry);

    this.transferAmounts = DistributionSummary.builder("finance.operations.transfer.amount")
        .description("Distribuição dos valores de transferências")
        .baseUnit("BRL")
        .publishPercentiles(0.5, 0.75, 0.95, 0.99)
        .register(registry);

    // Inicialização das métricas de performance
    this.serviceExecutionTimer = Timer.builder("finance.performance.service")
        .description("Tempo de execução de métodos de serviço")
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(registry);

    this.repositoryExecutionTimer = Timer.builder("finance.performance.repository")
        .description("Tempo de execução de métodos de repositório")
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(registry);

    this.exceptionCounter = Counter.builder("finance.exceptions")
        .description("Contador de exceções por tipo")
        .register(registry);

    // Counters para volume financeiro
    this.depositVolumeCounter = Counter.builder("finance.daily_volume")
        .tag("type", "DEPOSIT")
        .description("Volume financeiro diário de depósitos")
        .register(registry);

    this.withdrawalVolumeCounter = Counter.builder("finance.daily_volume")
        .tag("type", "WITHDRAWAL")
        .description("Volume financeiro diário de saques")
        .register(registry);

    this.transferVolumeCounter = Counter.builder("finance.daily_volume")
        .tag("type", "TRANSFER")
        .description("Volume financeiro diário de transferências")
        .register(registry);

    log.info("MetricsService inicializado com sucesso");
  }

  // Métodos para registrar eventos de conta
  public void recordAccountCreated(String type, String ownerType) {
    accountCreationCounter.increment();
    Counter.builder("finance.accounts.created.detailed")
        .tag("type", type)
        .tag("owner", ownerType)
        .register(registry)
        .increment();
    log.debug("Métrica: Conta criada do tipo {} para {}", type, ownerType);
  }

  public void recordAccountStatusUpdate(String previousStatus, String newStatus) {
    accountStatusUpdateCounter.increment();
    Counter.builder("finance.accounts.status_transition")
        .tag("from", previousStatus)
        .tag("to", newStatus)
        .register(registry)
        .increment();
    log.debug("Métrica: Status da conta atualizado de {} para {}", previousStatus, newStatus);
  }

  // Métodos para registrar eventos de cliente
  public void recordClientCreated() {
    clientCreationCounter.increment();
    log.debug("Métrica: Cliente criado");
  }

  public void recordClientUpdated() {
    clientUpdateCounter.increment();
    log.debug("Métrica: Cliente atualizado");
  }

  public void recordClientDeleted() {
    clientDeletionCounter.increment();
    log.debug("Métrica: Cliente excluído");
  }

  // Métodos para registrar eventos de transação
  public void recordTransactionProcessed(String type, BigDecimal amount, boolean success) {
    transactionCounter.increment();
    transactionAmounts.record(amount.doubleValue());

    if (success) {
      successfulTransactionCounter.increment();

      // Registrar métricas específicas por tipo de transação
      switch (type) {
        case "DEPOSIT":
          depositAmounts.record(amount.doubleValue());
          break;
        case "WITHDRAWAL":
          withdrawalAmounts.record(amount.doubleValue());
          break;
        case "TRANSFER":
          transferAmounts.record(amount.doubleValue());
          break;
      }
    } else {
      failedTransactionCounter.increment();
    }

    // Contador específico por tipo de transação
    Counter.builder("finance.transactions.by_type")
        .tag("type", type)
        .register(registry)
        .increment();

    log.debug("Métrica: Transação processada - tipo: {}, valor: {}, sucesso: {}",
        type, amount, success);
  }

  // Métodos para registrar métricas de performance
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
    log.debug("Métrica: Exceção ocorrida - tipo: {}, operação: {}", exceptionType, operationType);
  }

  // Métodos para timer manual (para casos mais complexos)
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

  // Metodo para registrar volumes financeiros diários
  public void recordDailyFinancialVolume(String operationType, BigDecimal amount) {
    double value = amount.doubleValue();

    switch (operationType) {
      case "DEPOSIT":
        depositVolumeCounter.increment(value);
        break;
      case "WITHDRAWAL":
        withdrawalVolumeCounter.increment(value);
        break;
      case "TRANSFER":
        transferVolumeCounter.increment(value);
        break;
    }

    log.debug("Métrica: Volume financeiro diário atualizado - tipo: {}, valor: {}",
        operationType, amount);
  }

  public void recordSuccessfulLogin(String username) {
    Counter.builder("finance.authentication.login")
        .tag("status", "success")
        .register(registry)
        .increment();
    log.debug("Métrica: Login bem-sucedido para o usuário {}", username);
  }

  public void recordFailedLogin(String username) {
    Counter.builder("finance.authentication.login")
        .tag("status", "failed")
        .register(registry)
        .increment();
    log.debug("Métrica: Falha de login para o usuário {}", username);
  }

  public MeterRegistry getRegistry() {
    return this.registry;
  }
}
