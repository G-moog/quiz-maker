package com.kingmonkey.quizmaker.controller;

import com.kingmonkey.quizmaker.config.JwtUtil;
import com.kingmonkey.quizmaker.entity.Question;
import com.kingmonkey.quizmaker.entity.QuestionSet;
import com.kingmonkey.quizmaker.entity.User;
import com.kingmonkey.quizmaker.repository.QuestionRepository;
import com.kingmonkey.quizmaker.repository.QuestionSetRepository;
import com.kingmonkey.quizmaker.repository.UserRepository;
import com.kingmonkey.quizmaker.service.ExcelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final ExcelService excelService;
    private final QuestionRepository questionRepository;
    private final QuestionSetRepository questionSetRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @PostMapping("/set/{setId}")
    public ResponseEntity<Map<String, String>> importToExistingSet(
            @PathVariable Long setId,
            @RequestParam("file") MultipartFile file) throws IOException {
        List<Question> questions = excelService.parseExcel(file, setId);
        questionRepository.saveAll(questions);
        return ResponseEntity.ok(Map.of("message", questions.size() + "개의 문항이 업로드되었습니다."));
    }

    @PostMapping("/new")
    public ResponseEntity<Map<String, Object>> importToNewSet(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "subject", required = false) String subject) throws IOException {
        User user = userRepository.findByUsername(getUsername(authHeader))
                .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다."));

        QuestionSet set = new QuestionSet();
        set.setTitle(title);
        set.setSubject(subject);
        set.setUser(user);
        questionSetRepository.save(set);

        List<Question> questions = excelService.parseExcel(file, set.getId());
        questionRepository.saveAll(questions);

        return ResponseEntity.ok(Map.of(
                "message", "문제집이 생성되었습니다.",
                "setId", set.getId()
        ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleException(RuntimeException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    private String getUsername(String authHeader) {
        return jwtUtil.extractUsername(authHeader.substring(7));
    }
}
