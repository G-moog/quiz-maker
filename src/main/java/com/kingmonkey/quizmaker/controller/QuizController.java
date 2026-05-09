package com.kingmonkey.quizmaker.controller;

import com.kingmonkey.quizmaker.config.JwtUtil;
import com.kingmonkey.quizmaker.dto.quiz.QuestionRequest;
import com.kingmonkey.quizmaker.dto.quiz.QuestionResponse;
import com.kingmonkey.quizmaker.dto.quiz.QuestionSetRequest;
import com.kingmonkey.quizmaker.dto.quiz.QuestionSetResponse;
import com.kingmonkey.quizmaker.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;
    private final JwtUtil jwtUtil;

    @GetMapping("/sets")
    public ResponseEntity<List<QuestionSetResponse>> getSets(
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(quizService.getSets(getUsername(authHeader)));
    }

    @PostMapping("/sets")
    public ResponseEntity<QuestionSetResponse> createSet(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody QuestionSetRequest request) {
        return ResponseEntity.ok(quizService.createSet(getUsername(authHeader), request));
    }

    @DeleteMapping("/sets/{setId}")
    public ResponseEntity<Void> deleteSet(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long setId) {
        quizService.deleteSet(setId, getUsername(authHeader));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/sets/{setId}/questions")
    public ResponseEntity<List<QuestionResponse>> getQuestions(@PathVariable Long setId) {
        return ResponseEntity.ok(quizService.getQuestions(setId));
    }

    @PostMapping("/sets/{setId}/questions")
    public ResponseEntity<QuestionResponse> addQuestion(
            @PathVariable Long setId,
            @RequestBody QuestionRequest request) {
        return ResponseEntity.ok(quizService.addQuestion(setId, request));
    }

    @PutMapping("/questions/{questionId}")
    public ResponseEntity<QuestionResponse> updateQuestion(
            @PathVariable Long questionId,
            @RequestBody QuestionRequest request) {
        return ResponseEntity.ok(quizService.updateQuestion(questionId, request));
    }

    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long questionId) {
        quizService.deleteQuestion(questionId);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    private String getUsername(String authHeader) {
        return jwtUtil.extractUsername(authHeader.substring(7));
    }
}
