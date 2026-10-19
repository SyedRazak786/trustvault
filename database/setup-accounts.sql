-- Run as the database admin. Replace the password before use.
CREATE USER IF NOT EXISTS 'trustvault_app'@'localhost' IDENTIFIED BY '7842233786';
GRANT SELECT, INSERT, UPDATE, DELETE ON trustvault.users TO 'trustvault_app'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON trustvault.documents TO 'trustvault_app'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON trustvault.grants TO 'trustvault_app'@'localhost';
-- audit_log: the application may only add and read rows.
GRANT SELECT, INSERT ON trustvault.audit_log TO 'trustvault_app'@'localhost';
FLUSH PRIVILEGES;
