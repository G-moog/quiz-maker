package com.kingmonkey.quizmaker.dto.quiz;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GradeResponse {
    private String type;

    // fill 전용: 정답 배열만 반환 (채점 없음)
    private List<String> answers;

    // 객관식 / 주관식 / OX 공통
    private Boolean correct;
    private String explanation;

    // 객관식 전용
    private Integer correctAnswerIndex;

    // 주관식 / OX 전용
    private String correctAnswer;
}
