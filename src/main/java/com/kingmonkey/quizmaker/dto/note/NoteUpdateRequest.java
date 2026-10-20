package com.kingmonkey.quizmaker.dto.note;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// PUT /api/notes/{id} — 보낸 칸만 고친다. resolved true 면 해결 표시, false 면 해결 취소.
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NoteUpdateRequest {
    private String content;
    private Boolean resolved;
}
