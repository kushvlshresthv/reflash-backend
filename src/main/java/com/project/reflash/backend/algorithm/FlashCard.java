package com.project.reflash.backend.algorithm;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FlashCard {

    /** Unique card id (epoch milliseconds). */
    private final long id;

    /** The note this card was generated from. */
    private final Note note;

    /** Creation timestamp in epoch seconds. */
    private final long crt;

    // ── scheduling state ──────────────────────────────────────────────────

    /** Learning state of the card (NEW / LEARNING / REVIEW / RELEARNING). */
    private CardType type;

    /** Which queue this card currently sits in. */
    private CardQueue queue;

    /**
     * The current interval.
     * Negative = seconds (used during learning).
     * Positive = days    (used during review).
     */
    private int ivl;

    /**
     * Ease factor in permille (e.g. 2500 = ×2.5).
     * Starts at 0 for new cards; typically set to 2500 when the card graduates.
     */
    private int factor;

    /** Total number of reviews performed on this card. */
    private int reps;

    /**
     * Number of "lapses" — each time a review-stage card is answered
     * incorrectly it counts as one lapse.
     */
    private int lapses;

    /**
     * Encodes remaining learning steps.
     * Format: a * 1000 + b
     *   a = reps remaining today
     *   b = reps remaining until graduation
     */
    private int left;

    /**
     * When the card is due.
     *   NEW  → note id (used for ordering new cards).
     *   LRN  → epoch timestamp in seconds.
     *   REV  → day offset from the collection's creation date.
     */
    private long due;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Creates a brand-new card from the given note.
     *
     * All scheduling fields start in the "new" state,
     *   type  = NEW,  queue = NEW,  ivl/factor/reps/lapses/left = 0,
     *   due   = note.id  (so new cards are shown in creation order).
     *
     * @param note the {@link Note} this card represents.
     */
    public FlashCard(Note note) {
        this.id     = SchedulingAlgoUtils.intId();
        this.note   = note;
        this.crt    = SchedulingAlgoUtils.intTime(1);   // current epoch seconds
        this.type   = CardType.NEW;
        this.queue  = CardQueue.NEW;
        this.ivl    = 0;
        this.factor = 0;
        this.reps   = 0;
        this.lapses = 0;
        this.left   = 0;
        // For new cards, "due" is set to the note id.
        // This means new cards are presented in the order their notes were created.
        this.due    = note.getId();
    }
}
