package com.kingmonkey.quizmaker.service;

import com.kingmonkey.quizmaker.dto.quiz.GradeRequest;
import com.kingmonkey.quizmaker.dto.quiz.GradeResponse;
import com.kingmonkey.quizmaker.dto.quiz.QuestionRequest;
import com.kingmonkey.quizmaker.dto.quiz.QuestionResponse;
import com.kingmonkey.quizmaker.dto.quiz.QuestionSetRequest;
import com.kingmonkey.quizmaker.dto.quiz.QuestionSetResponse;
import com.kingmonkey.quizmaker.entity.Question;
import com.kingmonkey.quizmaker.entity.QuestionSet;
import com.kingmonkey.quizmaker.entity.User;
import com.kingmonkey.quizmaker.repository.QuestionNoteRepository;
import com.kingmonkey.quizmaker.repository.QuestionRepository;
import com.kingmonkey.quizmaker.repository.QuestionSetRepository;
import com.kingmonkey.quizmaker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizService {

    private final QuestionSetRepository questionSetRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final QuestionNoteRepository questionNoteRepository;

    @Transactional(readOnly = true)
    public List<QuestionSetResponse> getSets(String username) {
        User user = findUser(username);
        return questionSetRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(QuestionSetResponse::from)
                .toList();
    }

    public QuestionSetResponse createSet(String username, QuestionSetRequest request) {
        User user = findUser(username);

        QuestionSet set = new QuestionSet();
        set.setTitle(request.getTitle());
        set.setSubject(request.getSubject());
        set.setUser(user);

        return QuestionSetResponse.from(questionSetRepository.save(set));
    }

    public QuestionSetResponse updateSet(Long setId, String username, QuestionSetRequest request) {
        QuestionSet set = findSet(setId);
        checkOwner(set, username, "수정 권한이 없습니다.");

        set.setTitle(request.getTitle());
        set.setSubject(request.getSubject());

        return QuestionSetResponse.from(questionSetRepository.save(set));
    }

    public void deleteSet(Long setId, String username) {
        QuestionSet set = findSet(setId);
        checkOwner(set, username, "삭제 권한이 없습니다.");

        // 질문 메모는 지우지 않는다 — 문제집·문항 번호만 비우고 문제집 제목(set_title)은 남긴다
        questionNoteRepository.detachSet(setId);
        questionSetRepository.delete(set);
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestions(Long setId, String username) {
        checkOwner(findSet(setId), username, "조회 권한이 없습니다.");

        return questionRepository.findByQuestionSetIdOrderByOrderIndexAsc(setId)
                .stream()
                .map(QuestionResponse::from)
                .toList();
    }

    public QuestionResponse addQuestion(Long setId, String username, QuestionRequest request) {
        QuestionSet set = findSet(setId);
        checkOwner(set, username, "수정 권한이 없습니다.");

        Question question = new Question();
        applyRequest(question, request);
        question.setQuestionSet(set);

        return QuestionResponse.from(questionRepository.save(question));
    }

    public QuestionResponse updateQuestion(Long questionId, String username, QuestionRequest request) {
        Question question = findQuestion(questionId);
        checkOwner(question.getQuestionSet(), username, "수정 권한이 없습니다.");

        applyRequest(question, request);

        return QuestionResponse.from(questionRepository.save(question));
    }

    public void deleteQuestion(Long questionId, String username) {
        Question question = findQuestion(questionId);
        checkOwner(question.getQuestionSet(), username, "삭제 권한이 없습니다.");

        // 질문 메모는 지우지 않는다 — 문항 번호만 비운다
        questionNoteRepository.detachQuestion(question.getQuestionSet().getId(), questionId);
        questionRepository.delete(question);
    }

    @Transactional(readOnly = true)
    public GradeResponse gradeQuestion(Long questionId, String username, GradeRequest request) {
        Question question = findQuestion(questionId);
        checkOwner(question.getQuestionSet(), username, "조회 권한이 없습니다.");

        String type = question.getType();

        // fill 유형: 채점 없이 정답 배열만 반환
        if ("fill".equals(type)) {
            return GradeResponse.builder()
                    .type("fill")
                    .answers(question.getAnswers())
                    .build();
        }

        // 객관식
        if ("multiple_choice".equals(type)) {
            boolean correct = question.getAnswerIndex() != null
                    && question.getAnswerIndex().equals(request.getUserAnswerIndex());
            return GradeResponse.builder()
                    .type(type)
                    .correct(correct)
                    .correctAnswerIndex(question.getAnswerIndex())
                    .explanation(question.getExplanation())
                    .build();
        }

        // 주관식 / OX
        String correctAnswer = question.getAnswer();
        boolean correct = correctAnswer != null
                && correctAnswer.trim().equalsIgnoreCase(
                        request.getUserAnswer() == null ? "" : request.getUserAnswer().trim());
        return GradeResponse.builder()
                .type(type)
                .correct(correct)
                .correctAnswer(correctAnswer)
                .explanation(question.getExplanation())
                .build();
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다."));
    }

    private QuestionSet findSet(Long setId) {
        return questionSetRepository.findById(setId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 세트입니다."));
    }

    private Question findQuestion(Long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 문항입니다."));
    }

    // 문제집을 만든 계정만 문제집과 그 문항을 보고 고칠 수 있다
    private void checkOwner(QuestionSet set, String username, String deniedMessage) {
        if (!set.getUser().getUsername().equals(username)) {
            throw new RuntimeException(deniedMessage);
        }
    }

    private void applyRequest(Question question, QuestionRequest request) {
        question.setType(request.getType());
        question.setText(request.getText());
        question.setOptions(request.getOptions());
        question.setAnswerIndex(request.getAnswerIndex());
        question.setAnswer(request.getAnswer());
        question.setAnswers(request.getAnswers());
        question.setExplanation(request.getExplanation());
        question.setQuestionImageUrl(request.getQuestionImageUrl());
        question.setAnswerImageUrl(request.getAnswerImageUrl());
        question.setOrderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : 0);
    }
}
