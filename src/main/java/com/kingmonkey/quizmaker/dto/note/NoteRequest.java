package com.kingmonkey.quizmaker.dto.note;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// POST /api/notes — content 만 필수. setTitle 은 서버가 문제집에서 채운다.
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NoteRequest {
    private Long setId;
    private Long questionId;
    private Integer blankNo;
    private String context;
    private String content;
}
