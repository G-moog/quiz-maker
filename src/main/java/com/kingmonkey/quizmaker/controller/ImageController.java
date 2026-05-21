package com.kingmonkey.quizmaker.controller;

import com.kingmonkey.quizmaker.config.JwtUtil;
import com.kingmonkey.quizmaker.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;
    private final JwtUtil jwtUtil;

    /**
     * POST /api/images/upload
     * 이미지를 Cloudinary에 업로드하고 URL을 반환합니다.
     * Authorization 헤더 또는 ?token= 쿼리 파라미터로 인증
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String queryToken) throws IOException {

        if (!isAuthenticated(authHeader, queryToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "인증이 필요합니다."));
        }

        String url = imageService.upload(file);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleValidation(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, String>> handleIo(IOException e) {
        return ResponseEntity.internalServerError().body(Map.of("error", "이미지 업로드에 실패했습니다."));
    }

    private boolean isAuthenticated(String authHeader, String queryToken) {
        String token = resolveToken(authHeader, queryToken);
        return token != null && jwtUtil.validateToken(token);
    }

    private String resolveToken(String authHeader, String queryToken) {
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        if (StringUtils.hasText(queryToken)) {
            return queryToken;
        }
        return null;
    }
}
