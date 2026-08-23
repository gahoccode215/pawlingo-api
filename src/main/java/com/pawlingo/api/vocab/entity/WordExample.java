package com.pawlingo.api.vocab.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "word_examples")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WordExample {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "word_id", nullable = false)
    private UUID wordId;

    @Column(nullable = false, length = 500)
    private String sentence;

    @Column(length = 500)
    private String translation;

    @Column(length = 200)
    private String source;

    @Column(name = "order_index", nullable = false)
    @Builder.Default
    private int orderIndex = 0;
}
