package com.project.reflash.backend.entity;

import com.project.reflash.backend.algorithm.SchedulingAlgoUtils;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "decks")
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class Deck {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="name")
    String name;

    @ManyToOne
    @JoinColumn(name="course_id", referencedColumnName="id")
    Course course;

    @OneToMany(mappedBy="deck")
    List<FlashCard> flashcards;

    public Deck(String name) {
        this.name  = name;
        this.flashcards = new ArrayList<>();
    }
}
