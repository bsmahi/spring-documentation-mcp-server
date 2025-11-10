-- Migration to create settings table for system configuration
-- This table should only have one row (singleton pattern)

CREATE TABLE settings (
    id BIGSERIAL PRIMARY KEY,
    enterprise_subscription_enabled BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default settings row
INSERT INTO settings (enterprise_subscription_enabled) VALUES (false);

-- Add a constraint to ensure only one row exists in the settings table
CREATE UNIQUE INDEX idx_settings_singleton ON settings ((id IS NOT NULL));
