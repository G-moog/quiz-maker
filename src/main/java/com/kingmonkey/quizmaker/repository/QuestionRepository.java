package com.kingmonkey.quizmaker.repository;

import com.kingmonkey.quizmaker.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByQuestionSetIdOrderByOrderIndexAsc(Long setId);
}
