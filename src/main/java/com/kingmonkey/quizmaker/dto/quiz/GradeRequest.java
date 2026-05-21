package com.kingmonkey.quizmaker.dto.quiz;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GradeRequest {
    /** 객관식: 사용자가 선택한 보기 인덱스 (0-based) */
    private Integer userAnswerIndex;
    /** 주관식 / OX: 사용자가 입력한 답 */
    private String userAnswer;
}
