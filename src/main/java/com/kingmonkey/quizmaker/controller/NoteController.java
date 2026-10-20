package com.kingmonkey.quizmaker.controller;

import com.kingmonkey.quizmaker.config.JwtUtil;
import com.kingmonkey.quizmaker.dto.note.NoteRequest;
import com.kingmonkey.quizmaker.dto.note.NoteResponse;
import com.kingmonkey.quizmaker.dto.note.NoteUpdateRequest;
import com.kingmonkey.quizmaker.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

// 질문 메모 API — 내 메모만 보고 고칠 수 있다
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<List<NoteResponse>> getNotes(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(required = false) Long setId) {
        return ResponseEntity.ok(noteService.getNotes(getUsername(authHeader), status, setId));
    }

    @PostMapping
    public ResponseEntity<NoteResponse> createNote(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody NoteRequest request) {
        return ResponseEntity.ok(noteService.createNote(getUsername(authHeader), request));
    }

    @PutMapping("/{noteId}")
    public ResponseEntity<NoteResponse> updateNote(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long noteId,
            @RequestBody NoteUpdateRequest request) {
        return ResponseEntity.ok(noteService.updateNote(noteId, getUsername(authHeader), request));
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> deleteNote(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long noteId) {
        noteService.deleteNote(noteId, getUsername(authHeader));
        return ResponseEntity.noContent().build();
    }

    // 숫자 자리에 글자가 온 주소(?setId=abc, /api/notes/abc). Spring 의 영어 내부 문구 대신 한국어로 알린다.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body("요청 값 " + e.getName() + " 의 형식이 올바르지 않습니다. 숫자여야 합니다.");
    }

    // 본문이 없거나 깨진 JSON, 숫자·참거짓 칸에 맞지 않는 값(blankNo: "x", 너무 큰 수 등)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleUnreadableBody(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body("요청 본문(JSON)의 형식이 올바르지 않습니다. 칸 이름과 값의 종류(숫자·글자·참거짓)를 확인해 주세요.");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    private String getUsername(String authHeader) {
        return jwtUtil.extractUsername(authHeader.substring(7));
    }
}
