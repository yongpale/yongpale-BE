-- MariaDB: 챗봇 답변 피드백("도움됨 / 아님") 컬럼을 추가한다.
-- 개발용 JPA_DDL_AUTO=update 환경에서는 JPA가 같은 타입으로 자동 추가한다. ddl-auto=validate를 쓰는 DB는 이 SQL을 먼저 적용한다.
-- 값: HELPFUL(도움됨) / NOT_HELPFUL(아님) / NULL(피드백 없음). 기존 메시지는 NULL로 남는다.
ALTER TABLE chat_messages
    ADD COLUMN IF NOT EXISTS feedback enum('HELPFUL', 'NOT_HELPFUL') NULL;
