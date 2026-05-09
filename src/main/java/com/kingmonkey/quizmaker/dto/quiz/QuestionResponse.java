package com.kingmonkey.quizmaker.dto.quiz;

import com.kingmonkey.quizmaker.entity.Question;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {
    private Long id;
    private String type;
    private String text;
    private String options;
    private Integer answerIndex;
    private String answer;
    private String explanation;
    private Integer orderIndex;

    public static QuestionResponse from(Question question) {
        return new QuestionResponse(
                question.getId(),
                question.getType(),
                question.getText(),
                question.getOptions(),
                question.getAnswerIndex(),
                question.getAnswer(),
                question.getExplanation(),
                question.getOrderIndex()
        );
    }
}
