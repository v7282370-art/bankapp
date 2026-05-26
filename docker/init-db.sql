-- Схема банковского приложения

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    phone VARCHAR(20),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    account_number VARCHAR(20) UNIQUE NOT NULL,
    account_type VARCHAR(20) NOT NULL, -- CHECKING, SAVINGS, CREDIT
    currency VARCHAR(3) DEFAULT 'RUB',
    balance DECIMAL(15,2) DEFAULT 0.00,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS cards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID REFERENCES accounts(id),
    card_number VARCHAR(16) UNIQUE NOT NULL,
    card_holder VARCHAR(100),
    expiry_date DATE,
    card_type VARCHAR(20), -- DEBIT, CREDIT
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    from_account_id UUID REFERENCES accounts(id),
    to_account_id UUID REFERENCES accounts(id),
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'RUB',
    type VARCHAR(30) NOT NULL, -- TRANSFER, DEPOSIT, WITHDRAWAL
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, COMPLETED, FAILED
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    completed_at TIMESTAMP
);

-- Тестовые данные
INSERT INTO users (username, email, password_hash, first_name, last_name, phone) VALUES
('ivan.petrov', 'ivan@test.com', 'hash1', 'Иван', 'Петров', '+79001234567'),
('maria.sidorova', 'maria@test.com', 'hash2', 'Мария', 'Сидорова', '+79007654321'),
('test.user', 'test@test.com', 'hash3', 'Тест', 'Юзер', '+79001111111');

INSERT INTO accounts (user_id, account_number, account_type, balance)
SELECT id, '40817810000000000001', 'CHECKING', 50000.00 FROM users WHERE username = 'ivan.petrov';

INSERT INTO accounts (user_id, account_number, account_type, balance)
SELECT id, '40817810000000000002', 'SAVINGS', 150000.00 FROM users WHERE username = 'maria.sidorova';
