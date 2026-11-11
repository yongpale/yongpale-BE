-- MariaDB: 챗봇 메시지에 대화 주인(user_id)과 답변 피드백(feedback) 컬럼을 추가한다.
-- 개발용 JPA_DDL_AUTO=update 환경에서는 JPA가 같은 타입으로 자동 추가한다. ddl-auto=validate를 쓰는 DB는 이 SQL을 먼저 적용한다.
--
-- user_id: 메시지를 보낸 회원. 본인 대화인지 확인할 때 쓴다. 가구 정보가 없는 회원도 대화할 수 있어서 profile_id로는 확인할 수 없다.
--   NOT NULL이라 chat_messages에 기존 행이 있으면 실패한다. 챗봇 API 이전에는 메시지가 저장되지 않으므로 보통 비어 있다.
-- feedback: HELPFUL(도움됨) / NOT_HELPFUL(아님) / NULL(피드백 없음).
ALTER TABLE chat_messages
    ADD COLUMN IF NOT EXISTS user_id bigint NOT NULL AFTER chat_session_id,
    ADD COLUMN IF NOT EXISTS feedback enum('HELPFUL', 'NOT_HELPFUL') NULL;

ALTER TABLE chat_messages
    ADD INDEX IF NOT EXISTS idx_chat_messages_user (user_id),
    ADD CONSTRAINT fk_chat_messages_user FOREIGN KEY (user_id) REFERENCES users (user_id);
