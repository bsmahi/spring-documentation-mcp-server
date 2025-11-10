-- Migration to update user roles from USER/READONLY to VIEWER
-- and update the check constraint

-- Drop the old constraint
ALTER TABLE users DROP CONSTRAINT IF EXISTS check_user_role;

-- Add the new constraint with updated role values
ALTER TABLE users ADD CONSTRAINT check_user_role CHECK (role IN ('ADMIN', 'VIEWER'));

-- Update any existing roles to the new values
-- USER -> VIEWER
-- READONLY -> VIEWER
UPDATE users SET role = 'VIEWER' WHERE role = 'USER';
UPDATE users SET role = 'VIEWER' WHERE role = 'READONLY';
