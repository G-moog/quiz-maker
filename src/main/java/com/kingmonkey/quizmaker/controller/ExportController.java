package com.kingmonkey.quizmaker.controller;

import com.kingmonkey.quizmaker.config.JwtUtil;
import com.kingmonkey.quizmaker.service.ExcelService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<byte[]> exportSet(@PathVariable Long setId) throws IOException {
        return toResponse(excelService.createSetWorkbook(setId), "questions_" + setId + ".xlsx");
    }

    @GetMapping("/all")
    public ResponseEntity<byte[]> exportAll(
            @RequestHeader("Authorization") String authHeader) throws IOException {
        return toResponse(excelService.createAllWorkbook(getUsername(authHeader)), "all_questions.xlsx");
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> exportTemplate() throws IOException {
        return toResponse(excelService.createTemplateWorkbook(), "upload_template.xlsx");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleException(RuntimeException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
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

    private String getUsername(String authHeader) {
        return jwtUtil.extractUsername(authHeader.substring(7));
    }
}
