package com.kingmonkey.quizmaker.dto.note;

import com.kingmonkey.quizmaker.entity.QuestionNote;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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
                note.getResolvedAt(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}
