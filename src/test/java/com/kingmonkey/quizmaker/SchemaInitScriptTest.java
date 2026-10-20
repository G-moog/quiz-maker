package com.kingmonkey.quizmaker;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

// 서버가 켜질 때마다 도는 db/init SQL 이 지켜야 할 약속.
// 2026-10-20 표 없이 배포돼 서버가 뜨지 않은 일 뒤로 question_notes 는 서버가 스스로 만든다.
class SchemaInitScriptTest {

    private static final String SCRIPT = "db/init/question_notes.sql";

    @Test
    void 시작할_때_db_init_SQL_을_실행하도록_설정돼_있다() throws IOException {
        Properties props = new Properties();
        try (InputStream in = resource("application.properties")) {
            props.load(in);
        }
        assertThat(props.getProperty("spring.sql.init.mode")).isEqualTo("always");
        assertThat(props.getProperty("spring.sql.init.schema-locations")).isEqualTo("classpath:" + SCRIPT);
        // 미뤄 실행하면(defer) Hibernate 검사가 먼저 돌아 표가 없다고 서버가 다시 멈춘다
        assertThat(props.getProperty("spring.jpa.defer-datasource-initialization")).isNotEqualTo("true");
    }

    @Test
    void 표가_있으면_건드리지_않고_없을_때만_만든다() throws IOException {
        List<String> statements = statements();
        assertThat(statements).hasSize(1);
        assertThat(statements.get(0)).startsWith("CREATE TABLE IF NOT EXISTS question_notes (");
    }

    @Test
    void 외래키와_표를_바꾸는_문장이_없다() throws IOException {
        String sql = String.join("\n", statements()).toUpperCase();
        // users.id 가 bigint unsigned 면 외래키 때문에 표가 안 만들어져 서버가 다시 멈춘다
        assertThat(sql).doesNotContain("FOREIGN KEY").doesNotContain("REFERENCES");
        // 켜질 때마다 도는 파일이라 있는 표·데이터를 바꾸는 문장은 두지 않는다
        assertThat(sql).doesNotContain("ALTER ").doesNotContain("DROP ").doesNotContain("DELETE ")
                .doesNotContain("TRUNCATE ").doesNotContain("UPDATE ").doesNotContain("INSERT ");
    }

    @Test
    void 엔티티의_칸이_모두_있다() throws IOException {
        String sql = statements().get(0);
        for (String column : List.of("id", "user_id", "set_id", "question_id", "set_title", "blank_no",
                "context", "content", "resolved_at", "created_at", "updated_at")) {
            assertThat(sql).containsPattern("(?m)^\\s*" + column + "\\s");
        }
    }

    // 설명 줄(--)을 빼고 세미콜론으로 나눈 문장들
    private static List<String> statements() throws IOException {
        String text;
        try (InputStream in = resource(SCRIPT)) {
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String code = text.lines()
                .filter(line -> !line.trim().startsWith("--"))
                .collect(Collectors.joining("\n"));
        return Arrays.stream(code.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static InputStream resource(String path) {
        InputStream in = SchemaInitScriptTest.class.getClassLoader().getResourceAsStream(path);
        assertThat(in).as(path).isNotNull();
        return in;
    }
}
