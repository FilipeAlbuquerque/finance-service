-- Inicialização do schema para financedb
-- Este script será executado automaticamente quando o container PostgreSQL iniciar

-- Criação de tabelas iniciais

-- Tabela de clientes
CREATE TABLE clients (
                         id BIGSERIAL PRIMARY KEY,
                         name VARCHAR(100) NOT NULL,
                         email VARCHAR(255) NOT NULL UNIQUE,
                         document_number VARCHAR(255) NOT NULL UNIQUE,
                         phone VARCHAR(255) NOT NULL,
                         address VARCHAR(255),
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de comerciantes
CREATE TABLE merchants (
                           id BIGSERIAL PRIMARY KEY,
                           business_name VARCHAR(100) NOT NULL,
                           trading_name VARCHAR(255),
                           email VARCHAR(255) NOT NULL UNIQUE,
                           cnpj VARCHAR(255) NOT NULL UNIQUE,
                           phone VARCHAR(255) NOT NULL,
                           address VARCHAR(255),
                           merchant_category_code VARCHAR(255),
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de contas
CREATE TABLE accounts (
                          id BIGSERIAL PRIMARY KEY,
                          account_number VARCHAR(255) NOT NULL UNIQUE,
                          type VARCHAR(50) NOT NULL,
                          balance DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
                          available_limit DECIMAL(19, 2),
                          status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          client_id BIGINT REFERENCES clients(id),
                          merchant_id BIGINT REFERENCES merchants(id),
                          CONSTRAINT account_owner_check CHECK (
                              (client_id IS NOT NULL AND merchant_id IS NULL) OR
                              (client_id IS NULL AND merchant_id IS NOT NULL)
                              )
);

-- Tabela de transações
CREATE TABLE transactions (
                              id BIGSERIAL PRIMARY KEY,
                              transaction_id VARCHAR(255) NOT NULL UNIQUE,
                              amount DECIMAL(19, 2) NOT NULL,
                              type VARCHAR(50) NOT NULL,
                              description TEXT,
                              status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              processed_at TIMESTAMP,
                              source_account_id BIGINT REFERENCES accounts(id),
                              destination_account_id BIGINT REFERENCES accounts(id)
);

-- Índices para melhorar performance
CREATE INDEX idx_clients_email ON clients(email);
CREATE INDEX idx_clients_document_number ON clients(document_number);
CREATE INDEX idx_merchants_cnpj ON merchants(cnpj);
CREATE INDEX idx_accounts_account_number ON accounts(account_number);
CREATE INDEX idx_accounts_client_id ON accounts(client_id);
CREATE INDEX idx_accounts_merchant_id ON accounts(merchant_id);
CREATE INDEX idx_transactions_transaction_id ON transactions(transaction_id);
CREATE INDEX idx_transactions_source_account_id ON transactions(source_account_id);
CREATE INDEX idx_transactions_destination_account_id ON transactions(destination_account_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);