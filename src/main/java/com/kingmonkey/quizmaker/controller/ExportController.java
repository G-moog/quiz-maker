package com.kingmonkey.quizmaker.controller;

import com.kingmonkey.quizmaker.config.JwtUtil;
import com.kingmonkey.quizmaker.service.ExcelService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ExcelService excelService;
    private final JwtUtil jwtUtil;

    @GetMapping("/set/{setId}")
    public ResponseEntity<byte[]> exportSet(
            @PathVariable Long setId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String queryToken) throws IOException {
        if (!isAuthenticated(authHeader, queryToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return toResponse(excelService.createSetWorkbook(setId), "questions_" + setId + ".xlsx");
    }

    @GetMapping("/all")
    public ResponseEntity<byte[]> exportAll(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String queryToken) throws IOException {
        String token = resolveToken(authHeader, queryToken);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String username = jwtUtil.extractUsername(token);
        return toResponse(excelService.createAllWorkbook(username), "all_questions.xlsx");
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> exportTemplate(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String queryToken) throws IOException {
        if (!isAuthenticated(authHeader, queryToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return toResponse(excelService.createTemplateWorkbook(), "upload_template.xlsx");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleException(RuntimeException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
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

    private ResponseEntity<byte[]> toResponse(Workbook workbook, String filename) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(out.toByteArray());
    }
}
