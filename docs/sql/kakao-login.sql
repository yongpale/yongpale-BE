-- 기존 일반 로그인용 users 테이블을 카카오 로그인용으로 변경하는 MariaDB SQL.
-- 신규 로컬 DB는 JPA_DDL_AUTO=update로 Entity에 맞게 생성할 수 있다.
-- MariaDB DDL은 암묵적으로 커밋되므로 운영 적용 전 별도 검토한다.
ALTER TABLE users ADD COLUMN IF NOT EXISTS kakao_id bigint;
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_kakao_id ON users (kakao_id);
ALTER TABLE users
    MODIFY COLUMN email varchar(255) NULL,
    MODIFY COLUMN password_hash varchar(255) NULL,
    MODIFY COLUMN terms_agreed_at datetime(6) NULL,
    MODIFY COLUMN privacy_agreed_at datetime(6) NULL;
