package com.project.reflash.backend.algorithm;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Deck — a collection of {@link FlashCard}s that are studied together.
 *
 * In standard Anki the "Collection" object holds all cards directly
 * (decks only act as filters/groupings).  In our application the hierarchy is:
 *
 *   StudyClass  →  Deck(s)  →  FlashCard(s)
 *
 * Each Deck therefore acts like a mini-collection: it owns its cards and
 * has its own {@link Scheduler} instance so that scheduling is per-deck.
 */

@Getter
@Setter
public class Deck {

    /** Unique deck id (epoch milliseconds). */
    private final long id;

    /** Human-readable name of this deck (e.g. "Biology Chapter 5"). */
    private String name;

    /**
     * Back-reference to the parent StudyClass (Anki's "Collection").
     * The scheduler needs this to access the collection creation timestamp (crt)
     * for day-offset calculations. Set when the deck is added to a StudyClass
     * via {@link StudyClass#addDeck(Deck)}.
     */
    private StudyClass studyClass;

    /**
     * In-memory list of all cards that belong to this deck.
     */
    private final List<FlashCard> cards;

    /**
     * The scheduler that handles "what card to show next" and "how to
     * reschedule a card after a review" for this deck.
     * Each deck gets its own scheduler instance.
     */
    private final Scheduler sched;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Creates a new, empty deck.
     *
     * @param name a human-readable name for the deck.
     */
    public Deck(String name) {
        this.id    = SchedulingAlgoUtils.intId();
        this.name  = name;
        this.cards = new ArrayList<>();
        // The scheduler needs a back-reference to this deck so it can
        // access the card list, creation time, etc.
        this.sched = new Scheduler(this);
    }

    /**
     * Creates a new FlashCard from the given {Note} and
     * adds it to this deck.
     */
    public void addNote(Note note) {
        cards.add(new FlashCard(note));
    }
}
