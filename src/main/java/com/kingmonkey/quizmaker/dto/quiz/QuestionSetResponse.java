package com.kingmonkey.quizmaker.dto.quiz;

import com.kingmonkey.quizmaker.entity.QuestionSet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionSetResponse {
    private Long id;
    private String title;
    private String subject;
    private LocalDateTime createdAt;
    private int questionCount;

    public static QuestionSetResponse from(QuestionSet set) {
        return new QuestionSetResponse(
                set.getId(),
                set.getTitle(),
                set.getSubject(),
                set.getCreatedAt(),
                set.getQuestions().size()
        );
    }
}
