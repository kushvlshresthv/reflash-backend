package com.project.reflash.backend.algorithm;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * The top-level container — equivalent to Anki's "Collection" object.
 *
 * In our application the hierarchy is:
 *
 *   StudyClass  →  Deck(s)  →  FlashCard(s)
 *
 * Key field:
 *   crt – Creation timestamp in epoch seconds (midnight of the creation day).
 *         The scheduler uses this as "day zero" to compute day-offsets for
 *         review cards.
 */

@Getter
@Setter
public class StudyClass {

    /** Unique id (epoch milliseconds). */
    private final long id;

    /** Human-readable name for this class (e.g. "Biology 101"). */
    private String name;

    /**
     * Creation date expressed as an epoch-second timestamp,
     * truncated to the **start of the day** (midnight).
     *
     * Anki does the same thing — it stores the creation date at midnight
     * so that "day X" calculations are always relative to the start of
     * a full day.
     */
    private final long crt;

    /** All decks that belong to this class. */
    private final List<Deck> decks;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Creates a new StudyClass.
     * The creation timestamp ({@code crt}) is set to midnight of *today*
     * in the system's default time-zone — exactly what Anki does.
     *
     * @param name a human-readable name for the class.
     */
    public StudyClass(String name) {
        this.id   = SchedulingAlgoUtils.intId();
        this.name = name;

        // Truncate "now" to the start of today (midnight) and convert to epoch seconds.
        // Anki's Python code does:
        //   d = datetime.datetime.today()
        //   d = datetime.datetime(d.year, d.month, d.day)
        //   self.crt = int(time.mktime(d.timetuple()))
        // The Java equivalent:
        this.crt = LocalDate.now()
                .atStartOfDay(ZoneId.of("UTC"))
                .toEpochSecond();

        this.decks = new ArrayList<>();
    }

    // -----------------------------------------------------------------------
    // Deck management
    // -----------------------------------------------------------------------

    /**
     * Adds an existing deck to this class.
     *
     * @param deck the deck to add.
     */
    public void addDeck(Deck deck) {
        // Set the back-reference so the deck (and its scheduler) can access
        // this StudyClass's creation timestamp and other metadata.
        deck.setStudyClass(this);
        decks.add(deck);
    }
}
