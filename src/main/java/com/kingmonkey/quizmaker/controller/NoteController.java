package com.kingmonkey.quizmaker.controller;

import com.kingmonkey.quizmaker.config.JwtUtil;
import com.kingmonkey.quizmaker.dto.note.NoteRequest;
import com.kingmonkey.quizmaker.dto.note.NoteResponse;
import com.kingmonkey.quizmaker.dto.note.NoteUpdateRequest;
import com.kingmonkey.quizmaker.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    private String getUsername(String authHeader) {
        return jwtUtil.extractUsername(authHeader.substring(7));
    }
}
