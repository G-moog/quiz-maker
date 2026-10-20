package com.kingmonkey.quizmaker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

// 문제를 풀다 떠오른 질문 메모. 표는 서버가 켜질 때 db/init/question_notes.sql 이 없을 때만 만든다
// (ddl-auto=validate 검사보다 먼저). 칸을 바꾸면 그 SQL 과 db/20261020_question_notes.sql 도 같이 고친다.
@Entity
@Table(name = "question_notes")
@Getter
@Setter
@NoArgsConstructor
public class QuestionNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 문제집·문항은 외래키 없이 번호만 둔다. 문제집·문항을 지우면 번호만 비우고 메모는 남긴다(QuizService).
    private Long setId;

    private Long questionId;

    // 저장할 때 문제집 제목을 복사해 둔다 — 문제집이 지워져도 무엇에 대한 메모인지 남게
    @Column(length = 100)
    private String setTitle;

    // 화면에 보이는 빈칸 번호(1부터)
    private Integer blankNo;

    // 그 빈칸이 있는 문장 등 앞뒤 글(1000자까지)
    @Column(columnDefinition = "TEXT")
    private String context;

    // 메모 본문(2000자까지)
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // 시각 칸 셋(resolvedAt·createdAt·updatedAt)은 모두 UTC 로 저장한다(currentTime).
    // 응답에서는 NoteResponse 가 끝에 Z 를 붙인 시각으로 바꿔 보내고, 앱·도구가 기기 시각(한국 시각)으로 바꿔 보여 준다.

    // 해결 표시 시각 — null 이면 아직 못 푼 질문
    private LocalDateTime resolvedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = currentTime();
        this.updatedAt = this.createdAt;
    }

    // 지금 시각(UTC). 서버(JVM)의 시간대 설정과 상관없이 늘 UTC 라서, Railway 시간대가 UTC 든
    // 나중에 Asia/Seoul 로 바뀌든 저장된 값의 기준이 섞이지 않는다.
    // 밀리초로 자른다: DATETIME(6) 에 반올림 없이 그대로 들어가 응답 값과 다시 읽은 값이 같고,
    // 응답 글자(…T13:40:00.123Z)가 자바스크립트 Date 의 표준 형식(소수 3자리)과 같아진다.
    public static LocalDateTime currentTime() {
        return LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS);
    }
}
