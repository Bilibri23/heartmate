CREATE TABLE users
(
    id                UUID                        NOT NULL,
    email             VARCHAR(255)                NOT NULL,
    phone             VARCHAR(255)                NOT NULL,
    password_hash     VARCHAR(255)                NOT NULL,
    first_name        VARCHAR(255)                NOT NULL,
    last_name         VARCHAR(255)                NOT NULL,
    gender            VARCHAR(255),
    date_of_birth     date,
    role              VARCHAR(255),
    account_status    VARCHAR(255),
    email_verified    BOOLEAN,
    phone_verified    BOOLEAN,
    profile_completed BOOLEAN,
    last_active       TIMESTAMP WITHOUT TIME ZONE,
    created_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

ALTER TABLE users
    ADD CONSTRAINT uc_users_phone UNIQUE (phone);