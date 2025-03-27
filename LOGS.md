# 📌 Logging Best Practices with Lombok @Slf4j
Introduction
This guide presents best practices for using Lombok's @Slf4j annotation for logging in Java/Spring applications.
Benefits of @Slf4j

Reduced Boilerplate Code: Eliminates the need to manually declare loggers in each class
Consistency: Ensures the logger name (log) is the same throughout the application
Error Reduction: Avoids issues like referencing the wrong class in LoggerFactory.getLogger()
Compatibility: Works with all major logging frameworks (Logback, Log4j2, etc.)

## 🚀 Correct Usage
- Add the annotation to the class
   java import lombok.extern.slf4j.Slf4j;
```
@Slf4j
public class MyClass {
// The 'log' field is automatically available
}
```

***Example***:

- Use the logger directly @Slf4j
```
   public class ExampleService {

   public void doSomething() {
   log.debug("Starting process");
   // code...
   log.info("Process completed successfully");
   }
}
```
- Log Levels and When to Use Them
### TRACE

- Use for deep debugging and traceability

***Example***:

```
log.trace("Method entry with value x={}", value);
```
### DEBUG

- Information useful for debugging
- Execution details, important variable values

***Example***:

```
log.debug("Processing item {} of {}", current, total);
```

### INFO

- Normal and significant system events
- Actions that should be recorded in production

***Example***: 
```
log.info("Transaction {} completed for client {}", txId, clientId);
```

### WARN

- Potentially problematic situations
- Unexpected conditions that don't cause immediate errors

***Example***:
```
log.warn("Low balance in account {}: {}", accountId, balance);
```

### ERROR

Errors that impact a specific operation
Caught exceptions that affect functionality

***Example***:

``` 
log.error("Failed to process payment", exception);
```

### FATAL (not directly available in SLF4J)

- Severe errors that compromise the application
- Usually replaced by ERROR level in SLF4J

***Example***:
```
log.error("Critical database system failure", exception);
```

### **Recommended Practices**

- ***Placeholder Formatting***


***Use placeholders {} instead of string concatenation:***


   // CORRECT
```
   log.info("Client {} performed {} transactions", clientId, numTransactions);
```
// INCORRECT (poor performance)
```
log.info("Client " + clientId + " performed " + numTransactions + " transactions");
```
- ***Exception Logging***

Always pass the exception as the last argument:
``` java
   try {
   // code that may throw an exception
   } catch (Exception e) {
   log.error("Error processing transaction {}", transactionId, e);
   }
```

- ***Check Log Level Before Building Complex Messages***

For messages that require heavy processing:
```
   if (log.isDebugEnabled()) {
   log.debug("Complex details: {}", generateComplexDetails());
   }
```

- ***Structure Logs for Easy Filtering***

Use consistent prefixes to facilitate searching:
```
log.info("API Request: Endpoint {} called with parameters {}", endpoint, params);
```

```
log.info("API Response: Returning {} results in {}ms", count, time);
```
- ***Avoid Logging Sensitive Data***


Never log sensitive information such as passwords, tokens, or personal data:

// INCORRECT
``` logging
log.debug("Authenticating user with password {}", password);
```
// CORRECT
```
log.debug("Authenticating user {}", username);
```

- ***MDC (Mapped Diagnostic Context) for Correlation***

Use MDC to include contextual information in all logs:
```
   MDC.put("requestId", requestId);
   try {
   // operation code
   } finally {
   MDC.remove("requestId");
   }
```

- ***Configuration in application.properties***

``` properties
# Log level for different packages
logging.level.com.example.app=INFO
logging.level.com.example.app.controller=INFO
logging.level.com.example.app.service=DEBUG
```

### Console format
``` properties
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{requestId}] %-5level %logger{36} - %msg%n
```

### File configuration
``` properties
logging.file.name=logs/application.log
logging.logback.rollingpolicy.max-file-size=10MB
logging.logback.rollingpolicy.max-history=7
```
### Integration with Observability Tools
**Standardizing logs with @Slf4j facilitates integration with tools such as**:

- **Splunk**: Configure logback.xml to send logs in JSON format
- **ELK Stack**: Use specific appenders for Elasticsearch
- **Datadog**: Add the Datadog agent for automatic collection
- **Grafana/Loki**: Configure sending to Loki endpoints

### 🛠️ Patterns Adopted in Our Application

- **Controllers** - "API Request/Response" prefix
```
info("API Request: Fetching client with ID: {}", id);
```
```
log.info("API Response: Retrieved client with ID: {}, email: {}", id, client.getEmail());
```

- **Services** - "Service" prefix
```
log.debug("Service: Getting client with ID: {}", id);
```
```
log.info("Service: Client created successfully with ID: {}", id);
```

- **Errors** - Include exception and relevant context
```
log.error("API Error: Failed to create client with email: {}", email, exception);
```

- **Financial Transactions** - Record values before and after
```
info("Service: Transfer completed. Source: {} ({} -> {}), Destination: {} ({} -> {})",
sourceAccount, oldSourceBalance, newSourceBalance, destAccount, oldDestBalance, newDestBalance);
```

## ✅ Conclusion

- Using Lombok's @Slf4j significantly simplifies logging implementation in Java applications, reducing boilerplate code and promoting consistency. By following the best practices in this guide, you'll have more useful, efficient, and easy-to-analyze logs, whether in development or production environments.