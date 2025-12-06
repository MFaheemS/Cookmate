-- Add FCM token column to Users table
ALTER TABLE Users ADD COLUMN fcm_token VARCHAR(255) DEFAULT NULL;

-- Add index for faster lookups
CREATE INDEX idx_fcm_token ON Users(fcm_token);

