-- TrustVault schema. Run as the database admin account.
CREATE DATABASE IF NOT EXISTS trustvault CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE trustvault;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  role ENUM('ADMIN','OWNER','VIEWER') NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS documents (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  owner_id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  category VARCHAR(100),
  storage_key CHAR(36) NOT NULL UNIQUE,
  content_type VARCHAR(100) NOT NULL,
  size_bytes BIGINT NOT NULL,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS grants (
  document_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  permission ENUM('READ','EDIT') NOT NULL,
  granted_by BIGINT NOT NULL,
  PRIMARY KEY (document_id, user_id),
  FOREIGN KEY (document_id) REFERENCES documents(id),
  FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  prev_hash CHAR(64) NOT NULL,
  hash CHAR(64) NOT NULL,
  actor_id BIGINT,
  action VARCHAR(40) NOT NULL,
  document_id BIGINT,
  outcome ENUM('ALLOWED','DENIED') NOT NULL,
  detail VARCHAR(500),
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

DROP TRIGGER IF EXISTS audit_no_update;
DROP TRIGGER IF EXISTS audit_no_delete;
DELIMITER //
CREATE TRIGGER audit_no_update BEFORE UPDATE ON audit_log
FOR EACH ROW BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'audit_log is append-only';
END//
CREATE TRIGGER audit_no_delete BEFORE DELETE ON audit_log
FOR EACH ROW BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'audit_log is append-only';
END//
DELIMITER ;
