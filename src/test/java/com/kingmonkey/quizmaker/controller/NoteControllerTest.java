package com.kingmonkey.quizmaker.controller;

import com.kingmonkey.quizmaker.config.JwtUtil;
import com.kingmonkey.quizmaker.dto.note.NoteResponse;
import com.kingmonkey.quizmaker.service.NoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

// 질문 메모 API 의 400 안내가 한국어인지, 시각이 시간대(Z)를 붙여 나가는지. DB·보안 설정 없이 컨트롤러만 띄운다.
@ExtendWith(MockitoExtension.class)
class NoteControllerTest {

    @Mock NoteService noteService;

    MockMvc mvc;
    String bearer;

    @BeforeEach
    void setUp() {
        JwtUtil jwtUtil = new JwtUtil("note-controller-test-secret-0123456789abcdef", 60_000);
        bearer = "Bearer " + jwtUtil.generateToken("owner", "USER");
        // 글자 응답은 실제 서버(Spring Boot)처럼 UTF-8 로. 기본 변환기는 ISO-8859-1 이라 한글이 '?' 로 깨진다.
        mvc = MockMvcBuilders.standaloneSetup(new NoteController(noteService, jwtUtil))
                .setMessageConverters(new StringHttpMessageConverter(StandardCharsets.UTF_8),
                        new JacksonJsonHttpMessageConverter())
                .build();
    }

    @Test
    void 숫자가_아닌_setId_는_한국어_400() throws Exception {
        MvcResult res = send(get("/api/notes").param("setId", "abc"));

        assertBadRequest(res, "요청 값 setId 의 형식이 올바르지 않습니다. 숫자여야 합니다.");
        verifyNoInteractions(noteService);
    }

    @Test
    void 숫자가_아닌_메모_번호는_한국어_400() throws Exception {
        MvcResult res = send(put("/api/notes/abc").contentType(MediaType.APPLICATION_JSON).content("{\"resolved\":true}"));

        assertBadRequest(res, "요청 값 noteId 의 형식이 올바르지 않습니다. 숫자여야 합니다.");
        verifyNoInteractions(noteService);
    }

    @Test
    void 깨진_JSON_틀린_종류_빈_본문은_한국어_400() throws Exception {
        String expected = "요청 본문(JSON)의 형식이 올바르지 않습니다. 칸 이름과 값의 종류(숫자·글자·참거짓)를 확인해 주세요.";
        for (String body : List.of(
                "{\"content\": \"질문",                          // 깨진 JSON
                "{\"content\":\"질문\",\"blankNo\":\"x\"}",       // 숫자 칸에 글자
                "{\"content\":\"질문\",\"blankNo\":99999999999}", // int 를 넘는 수
                "{\"content\":\"질문\",\"setId\":\"abc\"}",
                "")) {                                           // 본문 없음
            MvcResult res = send(post("/api/notes").contentType(MediaType.APPLICATION_JSON).content(body));

            assertBadRequest(res, expected);
        }
        MvcResult res = send(put("/api/notes/5").contentType(MediaType.APPLICATION_JSON).content("{\"resolved\":\"글쎄\"}"));
        assertBadRequest(res, expected);
        verifyNoInteractions(noteService);
    }

    @Test
    void 업무_오류는_서비스_문구_그대로_400() throws Exception {
        when(noteService.createNote(anyString(), any())).thenThrow(new RuntimeException("메모 내용을 입력해 주세요."));

        MvcResult res = send(post("/api/notes").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"  \"}"));

        assertBadRequest(res, "메모 내용을 입력해 주세요.");
    }

    @Test
    void 시각은_Z_를_붙인_UTC_로_나감() throws Exception {
        NoteResponse note = new NoteResponse(7L, 10L, 100L, "광합성 문제집", 3, "맥락", "질문", true,
                Instant.parse("2026-10-20T23:30:00.120Z"), Instant.parse("2026-10-20T23:00:00.123Z"),
                Instant.parse("2026-10-20T23:30:00Z"));
        when(noteService.getNotes("owner", "all", null)).thenReturn(List.of(note));

        MvcResult res = send(get("/api/notes"));

        assertThat(res.getResponse().getStatus()).isEqualTo(200);
        String json = res.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(json)
                .contains("\"createdAt\":\"2026-10-20T23:00:00.123Z\"")
                .contains("\"resolvedAt\":\"2026-10-20T23:30:00.120Z\"")
                .contains("\"updatedAt\":\"2026-10-20T23:30:00Z\"")
                .contains("\"resolved\":true");
    }

    private MvcResult send(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header("Authorization", bearer)).andReturn();
    }

    private static void assertBadRequest(MvcResult res, String message) throws Exception {
        assertThat(res.getResponse().getStatus()).isEqualTo(400);
        String body = res.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).isEqualTo(message);
    }
}
