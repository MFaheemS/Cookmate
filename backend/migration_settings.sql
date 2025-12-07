-- Add privacy and notification settings columns to Users table
-- Run this migration on your MySQL database

ALTER TABLE Users
ADD COLUMN is_private TINYINT(1) DEFAULT 0;

-- Note: Only is_private needs to be on the server for access control.
-- The other notification settings are stored locally in the SQLite database.


