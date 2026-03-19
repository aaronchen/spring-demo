-- V2: Seed a default admin user for first login
-- Password: 'password' (BCrypt encoded)
-- Change this password after first login via the profile page

INSERT INTO users (name, email, password, role, enabled)
VALUES ('Admin', 'admin@example.com', '$2b$10$eKasj4ieP4KxeTTOYcMBh.9MHNSWIKjRc5lcTWr7Um8K5m/CJIux2', 'ADMIN', true);
