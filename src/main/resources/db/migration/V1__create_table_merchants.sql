CREATE TABLE merchants (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    api_key VARCHAR(255) NOT NULL UNIQUE,
    secret VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP,
    updated_by VARCHAR(50),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE 
);