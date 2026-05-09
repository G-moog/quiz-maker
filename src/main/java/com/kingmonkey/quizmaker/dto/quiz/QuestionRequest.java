package com.kingmonkey.quizmaker.dto.quiz;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionRequest {
    private String type;
    private String text;
    private String options;
    private Integer answerIndex;
    private String answer;
    private String explanation;
    private Integer orderIndex;
}
