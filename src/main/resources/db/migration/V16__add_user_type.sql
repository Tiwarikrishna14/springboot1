ALTER TABLE users ADD COLUMN user_type VARCHAR(32);
ALTER TABLE users ADD CONSTRAINT chk_users_user_type CHECK (user_type IS NULL OR user_type IN ('EMPLOYEE', 'CUSTOMER'));
