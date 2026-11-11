-- MariaDB: 필요 서류 체크리스트에서 회원이 "준비 완료"로 체크한 서류를 저장하는 테이블을 추가한다.
-- 개발용 JPA_DDL_AUTO=update 환경에서는 JPA가 같은 구조로 자동 추가한다. ddl-auto=validate를 쓰는 DB는 이 SQL을 먼저 적용한다.
-- 행이 있으면 체크, 없으면 미체크. document_code는 required-documents.json의 서류 코드 또는 ORIGINAL_1, ORIGINAL_2 …
CREATE TABLE IF NOT EXISTS policy_document_checks (
    check_id bigint NOT NULL AUTO_INCREMENT,
    user_id bigint NOT NULL,
    policy_id bigint NOT NULL,
    document_code varchar(40) NOT NULL,
    created_at datetime(6) NOT NULL DEFAULT now(),
    PRIMARY KEY (check_id),
    UNIQUE KEY uk_policy_document_checks (user_id, policy_id, document_code),
    INDEX idx_policy_document_checks_policy (policy_id),
    CONSTRAINT fk_policy_document_checks_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_policy_document_checks_policy FOREIGN KEY (policy_id) REFERENCES policies (policy_id)
) ENGINE=InnoDB;
