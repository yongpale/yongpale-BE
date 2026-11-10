-- MariaDB: 기존 DB에 Refresh Token / 로그아웃 기능용 테이블을 추가한다.
-- users 테이블이 먼저 있어야 한다. 기존 회원·가구·지역 데이터는 변경하지 않는다.
CREATE TABLE IF NOT EXISTS auth_sessions (
    session_id varchar(36) NOT NULL,
    user_id bigint NOT NULL,
    created_at datetime(6) NOT NULL,
    expires_at datetime(6) NOT NULL,
    revoked_at datetime(6) NULL,
    PRIMARY KEY (session_id),
    INDEX idx_auth_sessions_user (user_id),
    CONSTRAINT fk_auth_sessions_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS auth_refresh_tokens (
    token_hash varchar(64) NOT NULL,
    session_id varchar(36) NOT NULL,
    created_at datetime(6) NOT NULL,
    consumed_at datetime(6) NULL,
    PRIMARY KEY (token_hash),
    INDEX idx_refresh_tokens_session (session_id),
    CONSTRAINT fk_refresh_tokens_session FOREIGN KEY (session_id) REFERENCES auth_sessions (session_id)
) ENGINE=InnoDB;
