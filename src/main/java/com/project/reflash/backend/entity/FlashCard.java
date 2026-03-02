package com.project.reflash.backend.entity;

import com.project.reflash.backend.algorithm.CardQueue;
import com.project.reflash.backend.algorithm.CardType;
import com.project.reflash.backend.algorithm.SchedulingAlgoUtils;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Table(name = "flashcards")
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class FlashCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(orphanRemoval = true)
    @JoinColumn(name = "note_id")
    private Note note;

    @Column(name="crt")
    private Long crt;

    @Column(name="type")
    @Enumerated(EnumType.STRING)
    private CardType type;

    @Column(name="queue")
    @Enumerated(EnumType.STRING)
    private CardQueue queue;

    @Column(name="ivl")
    private Integer ivl;

    @Column(name="factor")
    private Integer factor;

    @Column(name="reps")
    private Integer reps;

    @Column(name="lapses")
    private Integer lapses;

    @Column(name="left")
    private Integer left;

    @Column(name="due")
    private Long due;

    @ManyToOne
    @JoinColumn(name="deck_id", referencedColumnName="id")
    private Deck deck ;

    public FlashCard(Note note) {
        this.note   = note;
        this.crt    = SchedulingAlgoUtils.intTime(1);   // current epoch seconds
        this.type   = CardType.NEW;
        this.queue  = CardQueue.NEW;
        //NOTE: interval is perhaps for the review card to calculate the time values using the ease factor.
        this.ivl    = 0;
        this.factor = 0;
        this.reps   = 0;
        this.lapses = 0;
        this.left   = 0;
        // For new cards, "due" is set to the created date.
        // This means new cards are presented in the order their notes were created.
        this.due    =  this.crt;
    }
}
