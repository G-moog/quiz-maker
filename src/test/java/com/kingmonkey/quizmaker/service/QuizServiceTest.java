package com.kingmonkey.quizmaker.service;

import com.kingmonkey.quizmaker.dto.quiz.GradeRequest;
import com.kingmonkey.quizmaker.dto.quiz.QuestionRequest;
import com.kingmonkey.quizmaker.entity.Question;
import com.kingmonkey.quizmaker.entity.QuestionSet;
import com.kingmonkey.quizmaker.entity.User;
import com.kingmonkey.quizmaker.repository.QuestionRepository;
import com.kingmonkey.quizmaker.repository.QuestionSetRepository;
import com.kingmonkey.quizmaker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 문항 API 는 문제집을 만든 계정만 쓸 수 있다
@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock QuestionSetRepository questionSetRepository;
    @Mock QuestionRepository questionRepository;
    @Mock UserRepository userRepository;
    @InjectMocks QuizService quizService;

    QuestionSet set;
    Question question;

    @BeforeEach
    void setUp() {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("owner");

        set = new QuestionSet();
        set.setId(10L);
        set.setTitle("문제집");
        set.setUser(owner);

        question = new Question();
        question.setId(100L);
        question.setType("short_answer");
        question.setAnswer("정답");
        question.setQuestionSet(set);
    }

    @Test
    void getQuestions_다른_계정은_거부() {
        when(questionSetRepository.findById(10L)).thenReturn(Optional.of(set));

        assertThatThrownBy(() -> quizService.getQuestions(10L, "other"))
                .hasMessage("조회 권한이 없습니다.");
        verify(questionRepository, never()).findByQuestionSetIdOrderByOrderIndexAsc(any());
    }

    @Test
    void getQuestions_주인은_조회() {
        when(questionSetRepository.findById(10L)).thenReturn(Optional.of(set));
        when(questionRepository.findByQuestionSetIdOrderByOrderIndexAsc(10L)).thenReturn(List.of(question));

        assertThat(quizService.getQuestions(10L, "owner")).hasSize(1);
    }

    @Test
    void getQuestions_없는_문제집() {
        when(questionSetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quizService.getQuestions(99L, "owner"))
                .hasMessage("존재하지 않는 세트입니다.");
    }

    @Test
    void addQuestion_다른_계정은_거부() {
        when(questionSetRepository.findById(10L)).thenReturn(Optional.of(set));

        assertThatThrownBy(() -> quizService.addQuestion(10L, "other", new QuestionRequest()))
                .hasMessage("수정 권한이 없습니다.");
        verify(questionRepository, never()).save(any());
    }

    @Test
    void addQuestion_주인은_추가() {
        when(questionSetRepository.findById(10L)).thenReturn(Optional.of(set));
        when(questionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        quizService.addQuestion(10L, "owner", new QuestionRequest());

        verify(questionRepository).save(any());
    }

    @Test
    void updateQuestion_다른_계정은_거부() {
        when(questionRepository.findById(100L)).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> quizService.updateQuestion(100L, "other", new QuestionRequest()))
                .hasMessage("수정 권한이 없습니다.");
        verify(questionRepository, never()).save(any());
        assertThat(question.getAnswer()).isEqualTo("정답");
    }

    @Test
    void deleteQuestion_다른_계정은_거부() {
        when(questionRepository.findById(100L)).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> quizService.deleteQuestion(100L, "other"))
                .hasMessage("삭제 권한이 없습니다.");
        verify(questionRepository, never()).delete(any());
    }

    @Test
    void deleteQuestion_주인은_삭제() {
        when(questionRepository.findById(100L)).thenReturn(Optional.of(question));

        quizService.deleteQuestion(100L, "owner");

        verify(questionRepository).delete(question);
    }

    @Test
    void gradeQuestion_다른_계정은_거부() {
        when(questionRepository.findById(100L)).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> quizService.gradeQuestion(100L, "other", new GradeRequest()))
                .hasMessage("조회 권한이 없습니다.");
    }

    @Test
    void gradeQuestion_주인은_채점() {
        when(questionRepository.findById(100L)).thenReturn(Optional.of(question));
        GradeRequest request = new GradeRequest();
        ReflectionTestUtils.setField(request, "userAnswer", "정답");

        assertThat(quizService.gradeQuestion(100L, "owner", request).getCorrect()).isTrue();
    }
}
