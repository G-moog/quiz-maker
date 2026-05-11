package com.kingmonkey.quizmaker.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingmonkey.quizmaker.entity.Question;
import com.kingmonkey.quizmaker.entity.QuestionSet;
import com.kingmonkey.quizmaker.repository.QuestionRepository;
import com.kingmonkey.quizmaker.repository.QuestionSetRepository;
import com.kingmonkey.quizmaker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelService {

    private static final String[] HEADERS = {"번호", "유형", "문제", "보기1", "보기2", "보기3", "보기4", "정답", "해설"};

    private final QuestionRepository questionRepository;
    private final QuestionSetRepository questionSetRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public Workbook createSetWorkbook(Long setId) {
        QuestionSet set = questionSetRepository.findById(setId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 문제집입니다."));
        List<Question> questions = questionRepository.findByQuestionSetIdOrderByOrderIndexAsc(setId);

        Workbook workbook = new XSSFWorkbook();
        String sheetName = set.getTitle().length() > 31 ? set.getTitle().substring(0, 31) : set.getTitle();
        Sheet sheet = workbook.createSheet(sheetName);
        writeHeader(workbook, sheet);
        writeQuestions(sheet, questions);
        autoSize(sheet);
        return workbook;
    }

    public Workbook createAllWorkbook(String username) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다."));
        List<QuestionSet> sets = questionSetRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        Workbook workbook = new XSSFWorkbook();
        for (QuestionSet set : sets) {
            String sheetName = set.getTitle().length() > 31 ? set.getTitle().substring(0, 31) : set.getTitle();
            Sheet sheet = workbook.createSheet(sheetName);
            writeHeader(workbook, sheet);
            List<Question> questions = questionRepository.findByQuestionSetIdOrderByOrderIndexAsc(set.getId());
            writeQuestions(sheet, questions);
            autoSize(sheet);
        }
        return workbook;
    }

    public Workbook createTemplateWorkbook() {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("업로드양식");

        // 안내 행 (1행)
        Row guideRow = sheet.createRow(0);
        Cell guideCell = guideRow.createCell(0);
        guideCell.setCellValue("※ 유형: 객관식/주관식/OX | 정답: 객관식=1~4숫자, 주관식=텍스트, OX=O또는X | 보기는 객관식만 입력");
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));
        CellStyle guideStyle = workbook.createCellStyle();
        guideStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        guideStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        guideCell.setCellStyle(guideStyle);

        // 헤더 행 (2행)
        writeHeader(workbook, sheet);

        // 예시 데이터 (3~5행)
        writeExampleRow(sheet, 2, "1", "객관식", "대한민국의 수도는?", "서울", "부산", "대구", "인천", "1", "수도는 서울");
        writeExampleRow(sheet, 3, "2", "주관식", "세계에서 가장 높은 산은?", "", "", "", "", "에베레스트", "히말라야 산맥에 위치");
        writeExampleRow(sheet, 4, "3", "OX", "지구는 태양 주위를 공전한다", "", "", "", "", "O", "공전 주기는 약 365일");

        autoSize(sheet);
        return workbook;
    }

    public List<Question> parseExcel(MultipartFile file, Long setId) throws IOException {
        QuestionSet set = questionSetRepository.findById(setId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 문제집입니다."));

        List<Question> result = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int orderIndex = 0;

            // 0행(안내), 1행(헤더) 건너뜀 → 2행부터 데이터
            for (int i = 2; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String type = cellStr(row, 1);
                    String text = cellStr(row, 2);
                    if (type.isBlank() && text.isBlank()) continue;

                    Question q = new Question();
                    q.setQuestionSet(set);
                    q.setOrderIndex(orderIndex++);
                    q.setText(text);

                    switch (type) {
                        case "객관식" -> {
                            q.setType("multiple_choice");
                            List<String> opts = List.of(
                                    cellStr(row, 3), cellStr(row, 4),
                                    cellStr(row, 5), cellStr(row, 6)
                            );
                            q.setOptions(objectMapper.writeValueAsString(opts));
                            String ansStr = cellStr(row, 7);
                            q.setAnswerIndex(ansStr.isBlank() ? 0 : Integer.parseInt(ansStr.trim()) - 1);
                        }
                        case "주관식" -> {
                            q.setType("short_answer");
                            q.setAnswer(cellStr(row, 7));
                        }
                        case "OX" -> {
                            q.setType("ox");
                            q.setAnswer(cellStr(row, 7));
                        }
                        default -> { continue; }
                    }

                    q.setExplanation(cellStr(row, 8));
                    result.add(q);
                } catch (Exception ignored) {
                    // 파싱 실패한 행은 건너뜀
                }
            }
        }
        return result;
    }

    // ── 내부 헬퍼 ──────────────────────────────────────────

    private void writeHeader(Workbook workbook, Sheet sheet) {
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setBold(true);
        headerStyle.setFont(font);

        // 헤더는 항상 인덱스 1번 행(시트에서 두 번째 행)에 위치
        // createTemplateWorkbook에서는 0번이 안내행이므로 1번에 헤더 작성
        // createSetWorkbook에서는 0번에 바로 헤더 작성
        int headerRowIndex = sheet.getLastRowNum() < 0 ? 0 : sheet.getLastRowNum() + 1;
        Row headerRow = sheet.createRow(headerRowIndex);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void writeQuestions(Sheet sheet, List<Question> questions) {
        int rowNum = sheet.getLastRowNum() + 1;
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(i + 1);
            row.createCell(1).setCellValue(typeLabel(q.getType()));
            row.createCell(2).setCellValue(q.getText());

            if ("multiple_choice".equals(q.getType())) {
                List<String> opts = parseOptions(q.getOptions());
                for (int j = 0; j < opts.size() && j < 4; j++) {
                    row.createCell(3 + j).setCellValue(opts.get(j));
                }
                row.createCell(7).setCellValue(q.getAnswerIndex() != null ? q.getAnswerIndex() + 1 : "");
            } else {
                row.createCell(7).setCellValue(q.getAnswer() != null ? q.getAnswer() : "");
            }
            row.createCell(8).setCellValue(q.getExplanation() != null ? q.getExplanation() : "");
        }
    }

    private void writeExampleRow(Sheet sheet, int rowIndex, String... values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }

    private void autoSize(Sheet sheet) {
        for (int i = 0; i < HEADERS.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private String typeLabel(String type) {
        return switch (type) {
            case "multiple_choice" -> "객관식";
            case "short_answer" -> "주관식";
            case "ox" -> "OX";
            default -> type;
        };
    }

    private String cellStr(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                yield v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private List<String> parseOptions(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of("", "", "", "");
        }
    }
}
