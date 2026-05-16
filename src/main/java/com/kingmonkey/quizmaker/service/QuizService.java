package com.kingmonkey.quizmaker.service;

import com.kingmonkey.quizmaker.dto.quiz.QuestionRequest;
import com.kingmonkey.quizmaker.dto.quiz.QuestionResponse;
import com.kingmonkey.quizmaker.dto.quiz.QuestionSetRequest;
import com.kingmonkey.quizmaker.dto.quiz.QuestionSetResponse;
import com.kingmonkey.quizmaker.entity.Question;
import com.kingmonkey.quizmaker.entity.QuestionSet;
import com.kingmonkey.quizmaker.entity.User;
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

        if (!set.getUser().getUsername().equals(username)) {
            throw new RuntimeException("수정 권한이 없습니다.");
        }

        set.setTitle(request.getTitle());
        set.setSubject(request.getSubject());

        return QuestionSetResponse.from(questionSetRepository.save(set));
    }

    public void deleteSet(Long setId, String username) {
        QuestionSet set = findSet(setId);

        if (!set.getUser().getUsername().equals(username)) {
            throw new RuntimeException("삭제 권한이 없습니다.");
        }

        questionSetRepository.delete(set);
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestions(Long setId) {
        return questionRepository.findByQuestionSetIdOrderByOrderIndexAsc(setId)
                .stream()
                .map(QuestionResponse::from)
                .toList();
    }

    public QuestionResponse addQuestion(Long setId, QuestionRequest request) {
        QuestionSet set = findSet(setId);

        Question question = new Question();
        applyRequest(question, request);
        question.setQuestionSet(set);

        return QuestionResponse.from(questionRepository.save(question));
    }

    public QuestionResponse updateQuestion(Long questionId, QuestionRequest request) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 문항입니다."));

        applyRequest(question, request);

        return QuestionResponse.from(questionRepository.save(question));
    }

    public void deleteQuestion(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 문항입니다."));
        questionRepository.delete(question);
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다."));
    }

    private QuestionSet findSet(Long setId) {
        return questionSetRepository.findById(setId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 세트입니다."));
    }

    private void applyRequest(Question question, QuestionRequest request) {
        question.setType(request.getType());
        question.setText(request.getText());
        question.setOptions(request.getOptions());
        question.setAnswerIndex(request.getAnswerIndex());
        question.setAnswer(request.getAnswer());
        question.setExplanation(request.getExplanation());
        question.setOrderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : 0);
    }
}
