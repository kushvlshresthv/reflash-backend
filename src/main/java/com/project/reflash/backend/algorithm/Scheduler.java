package com.project.reflash.backend.algorithm;

import lombok.Getter;

/**
 * The Scheduler — responsible for deciding *which* card to show next and
 * *how* to reschedule a card after the user answers.
 *
 * Each {@link Deck} owns one Scheduler instance, and the scheduler holds
 * a back-reference to its deck so it can access the card list and
 * related metadata (e.g. the creation timestamp of the parent
 * {@link StudyClass} for day-offset calculations).
 */
@Getter
public class Scheduler {

    /** The deck this scheduler operates on. */
    private final Deck deck;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Creates a scheduler bound to the given deck.
     *
     * @param deck the deck whose cards this scheduler will manage.
     */
    public Scheduler(Deck deck) {
        this.deck = deck;
    }

    // -----------------------------------------------------------------------
    // Getter
    // -----------------------------------------------------------------------

    public Deck getDeck() {
        return deck;
    }
}
