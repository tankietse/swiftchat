CREATE TABLE users (
  id UUID PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255),
  activated BOOLEAN DEFAULT FALSE,
  activation_key VARCHAR(20),
  reset_key VARCHAR(20),
  created_at TIMESTAMP,
  last_login_at TIMESTAMP
);

CREATE TABLE oauth2_accounts (
  id UUID PRIMARY KEY,
  user_id UUID REFERENCES users(id),
  provider VARCHAR(50) NOT NULL,
  provider_id VARCHAR(255) NOT NULL,
  created_at TIMESTAMP,
  UNIQUE(provider, provider_id)
);

CREATE TABLE roles (
  id UUID PRIMARY KEY,
  name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE user_roles (
  user_id UUID REFERENCES users(id),
  role_id UUID REFERENCES roles(id),
  PRIMARY KEY (user_id, role_id)
);

CREATE TABLE refresh_tokens (
  id UUID PRIMARY KEY,
  token VARCHAR(255) UNIQUE NOT NULL,
  user_id UUID REFERENCES users(id) NOT NULL,
  expiry_date TIMESTAMP NOT NULL,
  revoked BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP
);

-- Insert default roles
INSERT INTO roles (id, name) VALUES 
  ('11111111-1111-1111-1111-111111111111', 'ROLE_USER'),
  ('22222222-2222-2222-2222-222222222222', 'ROLE_ADMIN'),
  ('33333333-3333-3333-3333-333333333333', 'ROLE_MODERATOR');
