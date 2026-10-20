-- 2026-10-20 질문 메모 표(question_notes)
--
-- 이 백엔드는 spring.jpa.hibernate.ddl-auto=validate 라서 표를 스스로 만들지 않는다.
-- 새 백엔드(QuestionNote 엔티티)를 배포하기 "전에" 운영 DB 에서 이 파일을 먼저 실행해야 한다.
-- 표 없이 백엔드가 먼저 올라가면 Hibernate 검사가 실패해 서버가 뜨지 않는다(앱 전체가 멈춤).
-- 칸 이름·타입은 entity/QuestionNote.java 와 똑같이 맞췄다.
--
-- ══ 1. 실행 전 확인 — 반드시 실행 ══════════════════════════════════
-- 운영 표는 Hibernate 가 만든 것이 아니라 사람이 만든 것이다(첫 커밋부터 ddl-auto=validate).
-- 그래서 users.id 가 bigint 인지 bigint unsigned 인지 알 수 없다(int 라면 지금도 서버가 뜨지 않으므로 둘 중 하나).
--
-- SELECT DATABASE();                                   -- 앱이 쓰는 스키마인지
-- SELECT COLUMN_TYPE FROM information_schema.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'id';
--     → "bigint"          : 아래 CREATE TABLE 을 그대로 실행
--     → "bigint unsigned" : 아래 user_id 줄을  user_id BIGINT UNSIGNED NOT NULL,  로 바꿔 실행
--                           (그대로 실행하면 ERROR 3780 "… are incompatible" 로 표가 만들어지지 않는다)
-- SELECT ENGINE FROM information_schema.TABLES
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users';
--     → "InnoDB" 이어야 외래키를 걸 수 있다
--
-- 참고(필요하면): SHOW TABLES;                          -- users, question_sets, questions 가 보여야 함
--                 SHOW TABLES LIKE 'question_notes';   -- 비어 있어야 함(이미 있으면 아래 CREATE 는 건너뜀)

CREATE TABLE IF NOT EXISTS question_notes (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,                -- 메모 주인(users.id). users.id 가 bigint unsigned 이면 BIGINT UNSIGNED 로
    set_id      BIGINT       NULL,                    -- 문제집 번호(외래키 없음 — 문제집을 지우면 앱이 NULL 로 비움)
    question_id BIGINT       NULL,                    -- 문항 번호(외래키 없음 — 문항을 지우면 앱이 NULL 로 비움)
    set_title   VARCHAR(100) NULL,                    -- 저장할 때 복사해 둔 문제집 제목
    blank_no    INT          NULL,                    -- 빈칸 번호(1부터)
    context     TEXT         NULL,                    -- 빈칸 앞뒤 글(앱에서 1000자까지)
    content     TEXT         NOT NULL,                -- 메모 본문(앱에서 2000자까지)
    resolved_at DATETIME(6)  NULL,                    -- 해결 표시 시각(NULL 이면 미해결). 시각 칸 셋은 모두 UTC
    created_at  DATETIME(6)  NOT NULL,                -- UTC(한국 시각 - 9시간). API 는 끝에 Z 를 붙여 보낸다
    updated_at  DATETIME(6)  NOT NULL,                -- UTC
    PRIMARY KEY (id),
    KEY idx_question_notes_user_created (user_id, created_at),
    KEY idx_question_notes_set_question (set_id, question_id),
    CONSTRAINT fk_question_notes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ══ 2. 실행 뒤 확인 — 반드시 실행 ══════════════════════════════════
-- SHOW CREATE TABLE question_notes;                    -- 위와 같은 칸 11개, 색인 2개, 외래키 1개
-- SELECT COUNT(*) FROM question_notes;                 -- 0
--
-- !! CREATE TABLE 이 오류로 끝났거나 SHOW CREATE TABLE 이 표를 보여 주지 않으면(ERROR 1146 …doesn't exist)
-- !! 백엔드를 push 하지 않는다. 그대로 push 하면 서버가 뜨지 않아 앱 전체(로그인·문제 풀기)가 멈춘다.
-- !! 오류 문구를 보고 1 의 users.id 타입부터 다시 확인한다.
--
-- ── 되돌리기(메모가 모두 지워진다) ─────────────────────────────
-- 반드시 백엔드를 메모 기능 이전 버전으로 먼저 되돌린 뒤에 실행한다.
-- (새 백엔드가 떠 있는 채로 표를 지우면 다음 재시작 때 서버가 뜨지 않는다)
-- DROP TABLE IF EXISTS question_notes;
