# 📌 Finance API

Finance API is a Spring Boot RESTful service for managing clients, accounts, and transfers. It supports secure authentication, database persistence, and transaction handling.

## 🚀 Features
- **Client Management**: Create and manage clients
- **Bank Accounts**: Open and track accounts
- **Transfers**: Transfer funds securely between accounts
- **Security**: JWT authentication and role-based access
- **Persistence**: Uses H2 (dev) and PostgreSQL/MySQL (prod)
- **Testing**: JUnit and Mockito for unit/integration tests

## 🛠️ Tech Stack
- **Java 21** + **Spring Boot**
- **Spring Data JPA** (H2, PostgreSQL, MySQL)
- **Spring Security** (JWT, Role-based Authorization)
- **JUnit 5, Mockito, WireMock**
- **Gradle** (Build Tool)

## 📦 Installation
Clone the repository and navigate to the project directory:

```sh
 git clone https://github.com/your-repo/finance-api.git
 cd finance-api
```

## 🔧 Configuration
Create a `.env` file or set environment variables for database connection.

For development, use the default H2 database:
```properties
spring.datasource.url=jdbc:h2:file:./data/finance-db
spring.datasource.username=sa
spring.datasource.password=
```

For PostgreSQL:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/finance_db
spring.datasource.username=your_user
spring.datasource.password=your_password
```

## ▶️ Running the Application
To run the application locally:
```sh
 ./gradlew bootRun
```
The API will be available at: `http://localhost:8080`

## 🔍 API Endpoints
| Method | Endpoint            | Description               |
|--------|--------------------|---------------------------|
| GET    | /clients           | Get all clients          |
| POST   | /clients           | Create a new client      |
| GET    | /accounts          | Get all accounts         |
| POST   | /transfers         | Create a transfer        |

## ✅ Running Tests
Run all tests using:
```sh
 ./gradlew test
```
