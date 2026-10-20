package com.kingmonkey.quizmaker.dto.note;

import com.kingmonkey.quizmaker.entity.QuestionNote;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NoteResponse {
    private Long id;
    private Long setId;
    private Long questionId;
    private String setTitle;
    private Integer blankNo;
    private String context;
    private String content;
    private boolean resolved;
    // 시각은 시간대를 붙여 보낸다(예: "2026-10-20T13:40:00.123Z"). 앱·도구가 기기 시각으로 바꿔 보여 준다.
    private Instant resolvedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static NoteResponse from(QuestionNote note) {
        return new NoteResponse(
                note.getId(),
                note.getSetId(),
                note.getQuestionId(),
                note.getSetTitle(),
                note.getBlankNo(),
                note.getContext(),
                note.getContent(),
                note.getResolvedAt() != null,
                utc(note.getResolvedAt()),
                utc(note.getCreatedAt()),
                utc(note.getUpdatedAt())
        );
    }

    // 메모 시각 칸은 UTC 로 저장한다(QuestionNote.currentTime)
    private static Instant utc(LocalDateTime time) {
        return time == null ? null : time.toInstant(ZoneOffset.UTC);
    }
}
