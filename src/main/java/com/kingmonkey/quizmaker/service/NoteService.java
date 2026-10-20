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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Predicate;

// 문제를 풀다 떠오른 질문 메모. 메모는 쓴 계정만 보고 고칠 수 있다.
@Service
@RequiredArgsConstructor
@Transactional
public class NoteService {

    static final int MAX_CONTENT_LENGTH = 2000;
    static final int MAX_CONTEXT_LENGTH = 1000;

    private final QuestionNoteRepository questionNoteRepository;
    private final QuestionSetRepository questionSetRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    // status: open(미해결) | resolved(해결) | all(기본), setId: 그 문제집 메모만. 최신순.
    @Transactional(readOnly = true)
    public List<NoteResponse> getNotes(String username, String status, Long setId) {
        Predicate<QuestionNote> statusFilter = statusFilter(status);
        User user = findUser(username);

        List<QuestionNote> notes = setId == null
                ? questionNoteRepository.findByUserIdOrderByCreatedAtDescIdDesc(user.getId())
                : questionNoteRepository.findByUserIdAndSetIdOrderByCreatedAtDescIdDesc(user.getId(), setId);

        return notes.stream()
                .filter(statusFilter)
                .map(NoteResponse::from)
                .toList();
    }

    public NoteResponse createNote(String username, NoteRequest request) {
        String content = checkContent(request.getContent());
        String context = checkContext(request.getContext());
        Integer blankNo = request.getBlankNo();
        if (blankNo != null && blankNo < 1) {
            throw new RuntimeException("빈칸 번호는 1 이상이어야 합니다.");
        }

        User user = findUser(username);
        QuestionSet set = findTargetSet(request.getSetId(), request.getQuestionId(), username);

        QuestionNote note = new QuestionNote();
        note.setUser(user);
        if (set != null) {
            note.setSetId(set.getId());
            note.setQuestionId(request.getQuestionId());
            note.setSetTitle(set.getTitle());
        }
        note.setBlankNo(blankNo);
        note.setContext(context);
        note.setContent(content);

        return NoteResponse.from(questionNoteRepository.save(note));
    }

    public NoteResponse updateNote(Long noteId, String username, NoteUpdateRequest request) {
        QuestionNote note = findNote(noteId);
        checkOwner(note, username, "수정 권한이 없습니다.");

        if (request.getContent() == null && request.getResolved() == null) {
            throw new RuntimeException("고칠 내용(content 또는 resolved)이 없습니다.");
        }
        String content = request.getContent() == null ? null : checkContent(request.getContent());

        LocalDateTime now = QuestionNote.currentTime();
        if (content != null) {
            note.setContent(content);
        }
        if (Boolean.TRUE.equals(request.getResolved())) {
            // 이미 해결 표시한 메모는 처음 해결한 시각을 그대로 둔다
            if (note.getResolvedAt() == null) {
                note.setResolvedAt(now);
            }
        } else if (Boolean.FALSE.equals(request.getResolved())) {
            note.setResolvedAt(null);
        }
        note.setUpdatedAt(now);

        return NoteResponse.from(questionNoteRepository.save(note));
    }

    public void deleteNote(Long noteId, String username) {
        QuestionNote note = findNote(noteId);
        checkOwner(note, username, "삭제 권한이 없습니다.");

        questionNoteRepository.delete(note);
    }

    // 메모를 붙일 문제집. 문항을 고르면 그 문항의 문제집(setId 를 함께 보내면 서로 맞아야 한다).
    private QuestionSet findTargetSet(Long setId, Long questionId, String username) {
        if (setId == null && questionId == null) {
            return null;
        }

        QuestionSet set;
        if (questionId != null) {
            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new RuntimeException("존재하지 않는 문항입니다."));
            set = question.getQuestionSet();
        } else {
            set = questionSetRepository.findById(setId)
                    .orElseThrow(() -> new RuntimeException("존재하지 않는 문제집입니다."));
        }

        if (!set.getUser().getUsername().equals(username)) {
            throw new RuntimeException("이 문제집에 메모를 남길 권한이 없습니다.");
        }
        if (setId != null && !setId.equals(set.getId())) {
            throw new RuntimeException("고른 문항이 그 문제집의 문항이 아닙니다.");
        }
        return set;
    }

    private Predicate<QuestionNote> statusFilter(String status) {
        String value = status == null || status.isBlank() ? "all" : status.trim();
        return switch (value) {
            case "all" -> note -> true;
            case "open" -> note -> note.getResolvedAt() == null;
            case "resolved" -> note -> note.getResolvedAt() != null;
            default -> throw new RuntimeException("status 는 open, resolved, all 중 하나여야 합니다.");
        };
    }

    private String checkContent(String content) {
        if (content == null || content.isBlank()) {
            throw new RuntimeException("메모 내용을 입력해 주세요.");
        }
        String trimmed = content.strip();
        if (trimmed.length() > MAX_CONTENT_LENGTH) {
            throw new RuntimeException("메모는 " + MAX_CONTENT_LENGTH + "자까지 쓸 수 있습니다. (지금 "
                    + trimmed.length() + "자)");
        }
        return trimmed;
    }

    // 앞뒤 글은 선택. 비어 있으면 저장하지 않는다.
    private String checkContext(String context) {
        if (context == null || context.isBlank()) {
            return null;
        }
        String trimmed = context.strip();
        if (trimmed.length() > MAX_CONTEXT_LENGTH) {
            throw new RuntimeException("앞뒤 글(context)은 " + MAX_CONTEXT_LENGTH + "자까지 보낼 수 있습니다. (지금 "
                    + trimmed.length() + "자)");
        }
        return trimmed;
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다."));
    }

    private QuestionNote findNote(Long noteId) {
        return questionNoteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 메모입니다."));
    }

    // 메모를 쓴 계정만 고치고 지울 수 있다
    private void checkOwner(QuestionNote note, String username, String deniedMessage) {
        if (!note.getUser().getUsername().equals(username)) {
            throw new RuntimeException(deniedMessage);
        }
    }
}
