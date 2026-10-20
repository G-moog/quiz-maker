package com.kingmonkey.quizmaker.service;

import com.kingmonkey.quizmaker.dto.note.NoteRequest;
import com.kingmonkey.quizmaker.dto.note.NoteResponse;
import com.kingmonkey.quizmaker.dto.note.NoteUpdateRequest;
import com.kingmonkey.quizmaker.entity.Question;
import com.kingmonkey.quizmaker.entity.QuestionNote;
import com.kingmonkey.quizmaker.entity.QuestionSet;
import com.kingmonkey.quizmaker.entity.User;
import com.kingmonkey.quizmaker.repository.QuestionNoteRepository;
import com.kingmonkey.quizmaker.repository.QuestionRepository;
import com.kingmonkey.quizmaker.repository.QuestionSetRepository;
import com.kingmonkey.quizmaker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 질문 메모는 쓴 계정만 보고 고칠 수 있다. 문제집을 고르면 내 문제집이어야 하고 제목은 서버가 채운다.
@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock QuestionNoteRepository questionNoteRepository;
    @Mock QuestionSetRepository questionSetRepository;
    @Mock QuestionRepository questionRepository;
    @Mock UserRepository userRepository;
    @InjectMocks NoteService noteService;

    User owner;
    User other;
    QuestionSet set;
    QuestionSet otherSet;
    Question question;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setUsername("owner");

        other = new User();
        other.setId(2L);
        other.setUsername("other");

        set = new QuestionSet();
        set.setId(10L);
        set.setTitle("광합성 문제집");
        set.setUser(owner);

        otherSet = new QuestionSet();
        otherSet.setId(20L);
        otherSet.setTitle("남의 문제집");
        otherSet.setUser(other);

        question = new Question();
        question.setId(100L);
        question.setType("fill");
        question.setQuestionSet(set);
    }

    // ── 만들기 ─────────────────────────────────────────────

    @Test
    void createNote_내_문제집_제목은_서버가_채움() {
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));
        when(questionSetRepository.findById(10L)).thenReturn(Optional.of(set));
        when(questionNoteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NoteResponse res = noteService.createNote("owner",
                new NoteRequest(10L, null, 3, "  엽록체에서 [ 3 ] 이 일어난다  ", "  왜 엽록체일까?\n"));

        QuestionNote saved = captureSaved();
        assertThat(saved.getUser()).isSameAs(owner);
        assertThat(saved.getSetId()).isEqualTo(10L);
        assertThat(saved.getSetTitle()).isEqualTo("광합성 문제집");
        assertThat(saved.getQuestionId()).isNull();
        assertThat(saved.getBlankNo()).isEqualTo(3);
        assertThat(saved.getContext()).isEqualTo("엽록체에서 [ 3 ] 이 일어난다");
        assertThat(saved.getContent()).isEqualTo("왜 엽록체일까?");
        assertThat(saved.getResolvedAt()).isNull();
        assertThat(res.getSetTitle()).isEqualTo("광합성 문제집");
        assertThat(res.isResolved()).isFalse();
    }

    @Test
    void createNote_문항을_고르면_그_문항의_문제집() {
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));
        when(questionRepository.findById(100L)).thenReturn(Optional.of(question));
        when(questionNoteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NoteResponse res = noteService.createNote("owner", new NoteRequest(10L, 100L, 1, null, "질문"));

        assertThat(res.getSetId()).isEqualTo(10L);
        assertThat(res.getQuestionId()).isEqualTo(100L);
        assertThat(res.getSetTitle()).isEqualTo("광합성 문제집");
    }

    @Test
    void createNote_문항만_보내도_문제집을_찾아_채움() {
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));
        when(questionRepository.findById(100L)).thenReturn(Optional.of(question));
        when(questionNoteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NoteResponse res = noteService.createNote("owner", new NoteRequest(null, 100L, null, null, "질문"));

        assertThat(res.getSetId()).isEqualTo(10L);
        assertThat(res.getSetTitle()).isEqualTo("광합성 문제집");
    }

    @Test
    void createNote_다른_문제집의_문항이면_거부() {
        QuestionSet mySecondSet = new QuestionSet();
        mySecondSet.setId(11L);
        mySecondSet.setTitle("두 번째");
        mySecondSet.setUser(owner);
        question.setQuestionSet(mySecondSet);
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));
        when(questionRepository.findById(100L)).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> noteService.createNote("owner", new NoteRequest(10L, 100L, null, null, "질문")))
                .hasMessage("고른 문항이 그 문제집의 문항이 아닙니다.");
        verify(questionNoteRepository, never()).save(any());
    }

    @Test
    void createNote_남의_문제집은_거부() {
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));
        when(questionSetRepository.findById(20L)).thenReturn(Optional.of(otherSet));

        assertThatThrownBy(() -> noteService.createNote("owner", new NoteRequest(20L, null, null, null, "질문")))
                .hasMessage("이 문제집에 메모를 남길 권한이 없습니다.");
        verify(questionNoteRepository, never()).save(any());
    }

    @Test
    void createNote_남의_문항도_거부() {
        when(userRepository.findByUsername("other")).thenReturn(Optional.of(other));
        when(questionRepository.findById(100L)).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> noteService.createNote("other", new NoteRequest(null, 100L, null, null, "질문")))
                .hasMessage("이 문제집에 메모를 남길 권한이 없습니다.");
        verify(questionNoteRepository, never()).save(any());
    }

    @Test
    void createNote_없는_문제집() {
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));
        when(questionSetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.createNote("owner", new NoteRequest(99L, null, null, null, "질문")))
                .hasMessage("존재하지 않는 문제집입니다.");
        verify(questionNoteRepository, never()).save(any());
    }

    @Test
    void createNote_없는_문항() {
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));
        when(questionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.createNote("owner", new NoteRequest(10L, 999L, null, null, "질문")))
                .hasMessage("존재하지 않는 문항입니다.");
        verify(questionNoteRepository, never()).save(any());
    }

    @Test
    void createNote_문제집_없이_그냥_메모() {
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));
        when(questionNoteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NoteResponse res = noteService.createNote("owner", new NoteRequest(null, null, null, "   ", "그냥 궁금한 것"));

        assertThat(res.getSetId()).isNull();
        assertThat(res.getSetTitle()).isNull();
        assertThat(res.getContext()).isNull();
        assertThat(res.getContent()).isEqualTo("그냥 궁금한 것");
        verify(questionSetRepository, never()).findById(anyLong());
    }

    @Test
    void createNote_빈_본문은_거부() {
        for (String blank : new String[]{null, "", "   \n\t "}) {
            assertThatThrownBy(() -> noteService.createNote("owner", new NoteRequest(null, null, null, null, blank)))
                    .hasMessage("메모 내용을 입력해 주세요.");
        }
        verify(questionNoteRepository, never()).save(any());
    }

    @Test
    void createNote_긴_본문은_거부() {
        String tooLong = "가".repeat(NoteService.MAX_CONTENT_LENGTH + 1);

        assertThatThrownBy(() -> noteService.createNote("owner", new NoteRequest(null, null, null, null, tooLong)))
                .hasMessage("메모는 2000자까지 쓸 수 있습니다. (지금 2001자)");
        verify(questionNoteRepository, never()).save(any());
    }

    @Test
    void createNote_본문_2000자는_허용_앞뒤_공백은_세지_않음() {
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));
        when(questionNoteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        String max = "  " + "가".repeat(NoteService.MAX_CONTENT_LENGTH) + "\n";

        NoteResponse res = noteService.createNote("owner", new NoteRequest(null, null, null, null, max));

        assertThat(res.getContent()).hasSize(NoteService.MAX_CONTENT_LENGTH);
    }

    @Test
    void createNote_긴_앞뒤_글은_거부() {
        String tooLong = "가".repeat(NoteService.MAX_CONTEXT_LENGTH + 1);

        assertThatThrownBy(() -> noteService.createNote("owner", new NoteRequest(null, null, null, tooLong, "질문")))
                .hasMessage("앞뒤 글(context)은 1000자까지 보낼 수 있습니다. (지금 1001자)");
        verify(questionNoteRepository, never()).save(any());
    }

    @Test
    void createNote_빈칸_번호는_1부터() {
        assertThatThrownBy(() -> noteService.createNote("owner", new NoteRequest(null, null, 0, null, "질문")))
                .hasMessage("빈칸 번호는 1 이상이어야 합니다.");
        verify(questionNoteRepository, never()).save(any());
    }

    // ── 목록 ───────────────────────────────────────────────

    @Test
    void getNotes_기본은_전부_최신순_그대로() {
        QuestionNote open = note(1L, owner, null);
        QuestionNote done = note(2L, owner, LocalDateTime.now());
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));
        when(questionNoteRepository.findByUserIdOrderByCreatedAtDescIdDesc(1L)).thenReturn(List.of(done, open));

        List<NoteResponse> all = noteService.getNotes("owner", "all", null);
        List<NoteResponse> noStatus = noteService.getNotes("owner", null, null);

        assertThat(all).extracting(NoteResponse::getId).containsExactly(2L, 1L);
        assertThat(noStatus).extracting(NoteResponse::getId).containsExactly(2L, 1L);
    }

    @Test
    void getNotes_open_은_미해결만_resolved_는_해결만() {
        QuestionNote open = note(1L, owner, null);
        QuestionNote done = note(2L, owner, LocalDateTime.now());
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));
        when(questionNoteRepository.findByUserIdOrderByCreatedAtDescIdDesc(1L)).thenReturn(List.of(done, open));

        assertThat(noteService.getNotes("owner", "open", null))
                .extracting(NoteResponse::getId).containsExactly(1L);
        assertThat(noteService.getNotes("owner", "resolved", null))
                .singleElement()
                .satisfies(r -> {
                    assertThat(r.getId()).isEqualTo(2L);
                    assertThat(r.isResolved()).isTrue();
                });
    }

    @Test
    void getNotes_setId_로_거르면_그_문제집만_조회() {
        QuestionNote inSet = note(1L, owner, null);
        inSet.setSetId(10L);
        QuestionNote doneInSet = note(2L, owner, LocalDateTime.now());
        doneInSet.setSetId(10L);
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));
        when(questionNoteRepository.findByUserIdAndSetIdOrderByCreatedAtDescIdDesc(1L, 10L))
                .thenReturn(List.of(doneInSet, inSet));

        assertThat(noteService.getNotes("owner", "open", 10L))
                .extracting(NoteResponse::getId).containsExactly(1L);
        verify(questionNoteRepository, never()).findByUserIdOrderByCreatedAtDescIdDesc(anyLong());
    }

    @Test
    void getNotes_남의_메모는_안_보임() {
        when(userRepository.findByUsername("other")).thenReturn(Optional.of(other));
        when(questionNoteRepository.findByUserIdOrderByCreatedAtDescIdDesc(2L)).thenReturn(List.of());

        assertThat(noteService.getNotes("other", "all", null)).isEmpty();
        verify(questionNoteRepository, never()).findByUserIdOrderByCreatedAtDescIdDesc(1L);
    }

    @Test
    void getNotes_모르는_status_는_거부() {
        assertThatThrownBy(() -> noteService.getNotes("owner", "done", null))
                .hasMessage("status 는 open, resolved, all 중 하나여야 합니다.");
        verify(questionNoteRepository, never()).findByUserIdOrderByCreatedAtDescIdDesc(anyLong());
    }

    // ── 고치기 ─────────────────────────────────────────────

    @Test
    void updateNote_해결_켜기() {
        QuestionNote n = note(5L, owner, null);
        when(questionNoteRepository.findById(5L)).thenReturn(Optional.of(n));
        when(questionNoteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NoteResponse res = noteService.updateNote(5L, "owner", new NoteUpdateRequest(null, true));

        assertThat(res.isResolved()).isTrue();
        assertThat(res.getResolvedAt()).isNotNull();
        assertThat(res.getUpdatedAt()).isEqualTo(res.getResolvedAt());
        assertThat(res.getContent()).isEqualTo("질문 5");
        // DATETIME(6) 에 반올림 없이 그대로 들어가고 응답이 "…T13:40:00.123Z" 꼴이 되도록 밀리초까지만
        assertThat(res.getResolvedAt().getNano() % 1_000_000).isZero();
        // 저장 값은 UTC — 응답 시각이 지금(Instant)과 거의 같아야 한다(서버 시간대만큼 어긋나지 않음)
        assertThat(Duration.between(res.getResolvedAt(), Instant.now()).abs()).isLessThan(Duration.ofMinutes(1));
        assertThat(n.getResolvedAt()).isEqualTo(LocalDateTime.ofInstant(res.getResolvedAt(), ZoneOffset.UTC));
    }

    @Test
    void currentTime_은_서버_시간대와_상관없이_UTC() {
        TimeZone before = TimeZone.getDefault();
        try {
            for (String zone : List.of("Asia/Seoul", "UTC", "America/Los_Angeles")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                LocalDateTime now = QuestionNote.currentTime();

                assertThat(Duration.between(now.toInstant(ZoneOffset.UTC), Instant.now()).abs())
                        .as("JVM 시간대 %s", zone)
                        .isLessThan(Duration.ofMinutes(1));
                assertThat(now.getNano() % 1_000_000).isZero();
            }
        } finally {
            TimeZone.setDefault(before);
        }
    }

    @Test
    void 응답_시각은_저장한_UTC_값에_Z_를_붙인_것() {
        QuestionNote n = note(5L, owner, LocalDateTime.of(2026, 10, 20, 23, 0, 0, 123_000_000));
        n.setUpdatedAt(LocalDateTime.of(2026, 10, 20, 23, 30));

        NoteResponse res = NoteResponse.from(n);

        // 저장 값은 UTC. 해결 시각 UTC 10/20 23:00 = 한국 시각 10/21 08:00 — 앱·도구가 기기 시각으로 바꿔 보여 준다
        assertThat(res.getCreatedAt()).isEqualTo(Instant.parse("2026-10-20T08:00:00Z"));
        assertThat(res.getResolvedAt()).isEqualTo(Instant.parse("2026-10-20T23:00:00.123Z"));
        assertThat(res.getResolvedAt().toString()).isEqualTo("2026-10-20T23:00:00.123Z");
        assertThat(res.getUpdatedAt()).isEqualTo(Instant.parse("2026-10-20T23:30:00Z"));
        assertThat(NoteResponse.from(note(6L, owner, null)).getResolvedAt()).isNull();
    }

    @Test
    void updateNote_이미_해결한_메모는_처음_해결_시각_유지() {
        LocalDateTime first = LocalDateTime.of(2026, 10, 1, 9, 0);
        QuestionNote n = note(5L, owner, first);
        when(questionNoteRepository.findById(5L)).thenReturn(Optional.of(n));
        when(questionNoteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NoteResponse res = noteService.updateNote(5L, "owner", new NoteUpdateRequest("고친 질문", true));

        assertThat(res.getResolvedAt()).isEqualTo(first.toInstant(ZoneOffset.UTC));
        assertThat(n.getResolvedAt()).isEqualTo(first);
        assertThat(res.getContent()).isEqualTo("고친 질문");
    }

    @Test
    void updateNote_해결_끄기() {
        QuestionNote n = note(5L, owner, LocalDateTime.now());
        when(questionNoteRepository.findById(5L)).thenReturn(Optional.of(n));
        when(questionNoteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NoteResponse res = noteService.updateNote(5L, "owner", new NoteUpdateRequest(null, false));

        assertThat(res.isResolved()).isFalse();
        assertThat(res.getResolvedAt()).isNull();
    }

    @Test
    void updateNote_본문_고치기() {
        QuestionNote n = note(5L, owner, null);
        when(questionNoteRepository.findById(5L)).thenReturn(Optional.of(n));
        when(questionNoteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NoteResponse res = noteService.updateNote(5L, "owner", new NoteUpdateRequest("  더 궁금한 것  ", null));

        assertThat(res.getContent()).isEqualTo("더 궁금한 것");
        assertThat(res.isResolved()).isFalse();
    }

    @Test
    void updateNote_빈_본문이나_긴_본문은_거부_그대로_둠() {
        QuestionNote n = note(5L, owner, null);
        when(questionNoteRepository.findById(5L)).thenReturn(Optional.of(n));
        String tooLong = "가".repeat(NoteService.MAX_CONTENT_LENGTH + 1);

        assertThatThrownBy(() -> noteService.updateNote(5L, "owner", new NoteUpdateRequest(" ", true)))
                .hasMessage("메모 내용을 입력해 주세요.");
        assertThatThrownBy(() -> noteService.updateNote(5L, "owner", new NoteUpdateRequest(tooLong, null)))
                .hasMessage("메모는 2000자까지 쓸 수 있습니다. (지금 2001자)");
        assertThat(n.getContent()).isEqualTo("질문 5");
        assertThat(n.getResolvedAt()).isNull();
        verify(questionNoteRepository, never()).save(any());
    }

    @Test
    void updateNote_고칠_것이_없으면_거부() {
        when(questionNoteRepository.findById(5L)).thenReturn(Optional.of(note(5L, owner, null)));

        assertThatThrownBy(() -> noteService.updateNote(5L, "owner", new NoteUpdateRequest(null, null)))
                .hasMessage("고칠 내용(content 또는 resolved)이 없습니다.");
        verify(questionNoteRepository, never()).save(any());
    }

    @Test
    void updateNote_남의_메모는_거부() {
        QuestionNote n = note(5L, owner, null);
        when(questionNoteRepository.findById(5L)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> noteService.updateNote(5L, "other", new NoteUpdateRequest("가로채기", true)))
                .hasMessage("수정 권한이 없습니다.");
        verify(questionNoteRepository, never()).save(any());
        assertThat(n.getContent()).isEqualTo("질문 5");
        assertThat(n.getResolvedAt()).isNull();
    }

    @Test
    void updateNote_없는_메모() {
        when(questionNoteRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.updateNote(404L, "owner", new NoteUpdateRequest(null, true)))
                .hasMessage("존재하지 않는 메모입니다.");
    }

    // ── 지우기 ─────────────────────────────────────────────

    @Test
    void deleteNote_주인은_삭제() {
        QuestionNote n = note(5L, owner, null);
        when(questionNoteRepository.findById(5L)).thenReturn(Optional.of(n));

        noteService.deleteNote(5L, "owner");

        verify(questionNoteRepository).delete(n);
    }

    @Test
    void deleteNote_남의_메모는_거부() {
        when(questionNoteRepository.findById(5L)).thenReturn(Optional.of(note(5L, owner, null)));

        assertThatThrownBy(() -> noteService.deleteNote(5L, "other"))
                .hasMessage("삭제 권한이 없습니다.");
        verify(questionNoteRepository, never()).delete(any());
    }

    private QuestionNote note(Long id, User user, LocalDateTime resolvedAt) {
        QuestionNote n = new QuestionNote();
        n.setId(id);
        n.setUser(user);
        n.setContent("질문 " + id);
        n.setResolvedAt(resolvedAt);
        n.setCreatedAt(LocalDateTime.of(2026, 10, 20, 8, 0));
        n.setUpdatedAt(n.getCreatedAt());
        return n;
    }

    private QuestionNote captureSaved() {
        ArgumentCaptor<QuestionNote> captor = ArgumentCaptor.forClass(QuestionNote.class);
        verify(questionNoteRepository).save(captor.capture());
        return captor.getValue();
    }
}
