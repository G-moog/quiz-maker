package com.kingmonkey.quizmaker.repository;

import com.kingmonkey.quizmaker.entity.QuestionSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionSetRepository extends JpaRepository<QuestionSet, Long> {
    List<QuestionSet> findByUserIdOrderByCreatedAtDesc(Long userId);
}
