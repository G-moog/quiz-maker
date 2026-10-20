package com.kingmonkey.quizmaker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

// 문제를 풀다 떠오른 질문 메모. 표는 db/20261020_question_notes.sql 로 운영 DB 에 먼저 만든다(ddl-auto=validate).
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

    // 시각 칸은 DATETIME(6)(마이크로초). 더 잘게 넣으면 MySQL 이 반올림해 저장해서
    // 응답으로 돌려준 값과 다음에 읽은 값이 어긋나므로 미리 마이크로초로 자른다.
    public static LocalDateTime currentTime() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }
}
