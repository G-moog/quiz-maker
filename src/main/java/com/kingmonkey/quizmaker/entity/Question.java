package com.kingmonkey.quizmaker.entity;

import com.kingmonkey.quizmaker.converter.StringListConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20)
    private String type;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(columnDefinition = "JSON")
    private String options;

    private Integer answerIndex;

    @Column(length = 500)
    private String answer;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "JSON")
    private List<String> answers;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(length = 500)
    private String questionImageUrl;

    @Column(length = 500)
    private String answerImageUrl;

    @Column(nullable = false)
    private Integer orderIndex = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "set_id", nullable = false)
    private QuestionSet questionSet;
}
