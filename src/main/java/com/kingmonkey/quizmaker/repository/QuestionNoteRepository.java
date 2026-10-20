package com.kingmonkey.quizmaker.repository;

import com.kingmonkey.quizmaker.entity.QuestionNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionNoteRepository extends JpaRepository<QuestionNote, Long> {

    List<QuestionNote> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    List<QuestionNote> findByUserIdAndSetIdOrderByCreatedAtDescIdDesc(Long userId, Long setId);

    // 문제집을 지울 때: 메모는 남기고 문제집·문항 번호만 비운다(set_title 은 그대로)
    @Modifying
    @Query("update QuestionNote n set n.setId = null, n.questionId = null where n.setId = :setId")
    int detachSet(@Param("setId") Long setId);

    // 문항을 지울 때: 메모는 남기고 문항 번호만 비운다
    @Modifying
    @Query("update QuestionNote n set n.questionId = null where n.setId = :setId and n.questionId = :questionId")
    int detachQuestion(@Param("setId") Long setId, @Param("questionId") Long questionId);
}
