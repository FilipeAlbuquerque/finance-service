-- Dados iniciais para ambiente de desenvolvimento
-- Este script será executado após o 01-schema.sql

-- Inserir alguns clientes de testeSELECT * FROM clients;
INSERT INTO clients (name, email, document_number, phone, address)
VALUES
    ('João Silva', 'joao@example.com', '123.456.789-00', '(11) 98765-4321', 'Rua A, 123, São Paulo - SP'),
    ('Maria Oliveira', 'maria@example.com', '987.654.321-00', '(11) 91234-5678', 'Av. B, 456, Rio de Janeiro - RJ');

-- Inserir alguns comerciantes de teste
INSERT INTO merchants (business_name, trading_name, email, cnpj, phone, address, merchant_category_code)
VALUES
    ('Mercado ABC Ltda', 'Mercado ABC', 'contato@mercadoabc.com', '12.345.678/0001-90', '(11) 3333-4444', 'Rua do Comércio, 789, São Paulo - SP', '5411'),
    ('Restaurante XYZ Ltda', 'Restaurante XYZ', 'contato@restaurantexyz.com', '98.765.432/0001-10', '(11) 4444-3333', 'Av. Paulista, 1000, São Paulo - SP', '5812');

-- Criar contas para os clientes e comerciantes
INSERT INTO accounts (account_number, type, balance, available_limit, status, client_id)
VALUES
    ('1001-01', 'CHECKING', 1000.00, 2000.00, 'ACTIVE', 1),
    ('1002-01', 'SAVINGS', 2500.00, NULL, 'ACTIVE', 2);

INSERT INTO accounts (account_number, type, balance, available_limit, status, merchant_id)
VALUES
    ('2001-01', 'MERCHANT', 5000.00, NULL, 'ACTIVE', 1),
    ('2002-01', 'MERCHANT', 7500.00, NULL, 'ACTIVE', 2);

-- Inserir algumas transações de exemplo
INSERT INTO transactions (transaction_id, amount, type, description, status, processed_at, source_account_id, destination_account_id)
VALUES
    ('TRX-001', 100.00, 'PAYMENT', 'Pagamento Mercado ABC', 'COMPLETED', CURRENT_TIMESTAMP, 1, 3),
    ('TRX-002', 75.50, 'PAYMENT', 'Pagamento Restaurante XYZ', 'COMPLETED', CURRENT_TIMESTAMP, 2, 4),
    ('TRX-003', 500.00, 'TRANSFER', 'Transferência entre contas', 'COMPLETED', CURRENT_TIMESTAMP, 1, 2);