package com.kingmonkey.quizmaker.dto.quiz;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionSetRequest {
    private String title;
    private String subject;
}
