-- 서버가 켜질 때마다 실행된다(application.properties 의 spring.sql.init 설정).
-- Hibernate 검사(ddl-auto=validate)보다 먼저 돌아서, 검사 전에 question_notes 표가 있게 만든다.
-- 표가 없으면 만들고 이미 있으면 아무것도 하지 않는다(IF NOT EXISTS).
--
-- 왜: 2026-10-20 운영 DB 에 표를 먼저 만들지 않은 채 백엔드가 배포되어
-- missing table [question_notes] 로 서버가 뜨지 않았다(로그인까지 모두 멈춤).
-- 그 뒤로 이 표는 서버가 스스로 만든다. 칸 이름과 타입은 entity/QuestionNote.java 와 같다.
--
-- 외래키(user_id 에서 users.id 로)는 일부러 걸지 않는다.
-- 운영 users.id 가 bigint unsigned 라면 외래키가 타입 불일치(ERROR 3780)로 실패해 표가 안 만들어지고
-- 서버가 다시 뜨지 않는다. 앱에는 사용자를 지우는 기능이 없고, 메모는 로그인한 사용자 번호로만 저장하고 읽는다.
-- 사람이 db/20261020_question_notes.sql 로 외래키까지 걸어 먼저 만들었다면 여기서는 그대로 둔다.
--
-- 주의: 켜질 때마다 도는 파일이다. 있는 표를 바꾸거나 지우는 문장(ALTER, DROP, DELETE)은 넣지 않는다.
-- 문장 끝은 세미콜론으로 나눈다. 설명 줄에는 세미콜론과 따옴표를 쓰지 않는다.

CREATE TABLE IF NOT EXISTS question_notes (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    set_id      BIGINT       NULL,
    question_id BIGINT       NULL,
    set_title   VARCHAR(100) NULL,
    blank_no    INT          NULL,
    context     TEXT         NULL,
    content     TEXT         NOT NULL,
    resolved_at DATETIME(6)  NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_question_notes_user_created (user_id, created_at),
    KEY idx_question_notes_set_question (set_id, question_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
