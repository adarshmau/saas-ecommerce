CREATE TABLE tenants(
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE ,
    subdomain VARCHAR(50) NOT NULL UNIQUE ,
    email VARCHAR(150) NOT NULL UNIQUE ,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP

);
INSERT INTO tenants (id, name, subdomain, email, status)
VALUES
    ('tenant-1', 'Store One', 'store1', 'store1@example.com', 'ACTIVE'),
    ('tenant-2', 'Store Two', 'store2', 'store2@example.com', 'ACTIVE');