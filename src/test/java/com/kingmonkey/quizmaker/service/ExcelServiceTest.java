package com.kingmonkey.quizmaker.service;

import com.kingmonkey.quizmaker.entity.QuestionSet;
import com.kingmonkey.quizmaker.entity.User;
import com.kingmonkey.quizmaker.repository.QuestionRepository;
import com.kingmonkey.quizmaker.repository.QuestionSetRepository;
import com.kingmonkey.quizmaker.repository.UserRepository;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 엑셀 내려받기·올리기는 문제집을 만든 계정만 할 수 있다
@ExtendWith(MockitoExtension.class)
class ExcelServiceTest {

    @Mock QuestionRepository questionRepository;
    @Mock QuestionSetRepository questionSetRepository;
    @Mock UserRepository userRepository;
    @Mock ObjectMapper objectMapper;
    @InjectMocks ExcelService excelService;

    QuestionSet set;

    @BeforeEach
    void setUp() {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("owner");

        set = new QuestionSet();
        set.setId(10L);
        set.setTitle("문제집");
        set.setUser(owner);
    }

    @Test
    void createSetWorkbook_다른_계정은_거부() {
        when(questionSetRepository.findById(10L)).thenReturn(Optional.of(set));

        assertThatThrownBy(() -> excelService.createSetWorkbook(10L, "other"))
                .hasMessage("조회 권한이 없습니다.");
        verify(questionRepository, never()).findByQuestionSetIdOrderByOrderIndexAsc(any());
    }

    @Test
    void createSetWorkbook_주인은_내려받기() throws Exception {
        when(questionSetRepository.findById(10L)).thenReturn(Optional.of(set));
        when(questionRepository.findByQuestionSetIdOrderByOrderIndexAsc(10L)).thenReturn(List.of());

        try (Workbook workbook = excelService.createSetWorkbook(10L, "owner")) {
            assertThat(workbook.getSheetName(0)).isEqualTo("문제집");
        }
    }

    @Test
    void parseExcel_다른_계정은_거부() throws Exception {
        when(questionSetRepository.findById(10L)).thenReturn(Optional.of(set));

        assertThatThrownBy(() -> excelService.parseExcel(templateFile(), 10L, "other"))
                .hasMessage("수정 권한이 없습니다.");
    }

    @Test
    void parseExcel_주인은_올리기() throws Exception {
        when(questionSetRepository.findById(10L)).thenReturn(Optional.of(set));

        // 양식의 예시 4행 중 객관식은 objectMapper(가짜)가 null 을 돌려줘도 그대로 들어간다
        assertThat(excelService.parseExcel(templateFile(), 10L, "owner")).hasSize(4);
    }

    @Test
    void parseExcel_없는_문제집() {
        when(questionSetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> excelService.parseExcel(templateFile(), 99L, "owner"))
                .hasMessage("존재하지 않는 문제집입니다.");
    }

    private MockMultipartFile templateFile() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Workbook workbook = excelService.createTemplateWorkbook()) {
            workbook.write(out);
        }
        return new MockMultipartFile("file", "upload_template.xlsx", null, out.toByteArray());
    }
}
