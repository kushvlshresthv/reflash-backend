package com.project.reflash.backend.algorithm;

import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * The Scheduler — the core of the Anki algorithm.
 *
 * A scheduler supports two main operations:
 *   1. {@link #getCard()}      — returns the next card to review.
 *   2. {@link #answerCard}     — updates a card after the user answers
 *                                 (ease: 0=Again, 1=Hard, 2=Good, 3=Easy).
 *
 * These two methods will be implemented in a later step.  For now, this class
 * sets up all the state that those methods will need.
 *
 * Each {@link Deck} owns one Scheduler instance. The scheduler holds a
 * back-reference to its deck so it can access the card list and, through
 * the deck's parent {@link StudyClass}, the collection creation timestamp
 * required for day-offset calculations.
 */
@Getter
public class Scheduler {

    // ── references ────────────────────────────────────────────────────────

    /** The deck this scheduler operates on */
    private final Deck deck;

    // ── limits ────────────────────────────────────────────────────────────

    /**
     * Upper limit for the number of new + review cards that can be
     * fetched in one study session. Defaults to 50.
     */
    private int queueLimit = 50;

    /**
     * Upper limit for the number of learning cards that can be fetched
     * in one study session.
     */
    private int reportLimit = 1000;

    // ── daily state ───────────────────────────────────────────────────────

    /**
     * The number of cards already reviewed *today*.
     * Reset to 0 each day (or when reset() is called).
     */
    private int reps;

    /**
     * The number of full days that have elapsed since the collection
     * (StudyClass) was created.
     *
     * Used when looking up review cards — a review card is due when
     * card.due <= today.
     *
     * Anki calculates this as:
     *   (now - collection.crt) // 86400
     */
    private int today;

    /**
     * Epoch-second timestamp of the start of the *next* day (midnight).
     *
     * When the current time crosses this boundary the scheduler knows
     * a new day has begun and it should recalculate "today", refill the
     * new/review queues, etc.
     */
    private long dayCutoff;

    // ── learn-ahead ───────────────────────────────────────────────────────

    /**
     * Epoch-second timestamp that defines how far into the future the
     * scheduler will look for learning cards.
     *
     * If a learning card is due within this window it can be shown early
     * rather than making the user wait.  Updated via
     * {@link #updateLrnCutoff(boolean)}.
     */
    private long lrnCutoff = 0;

    /**
     * The "learn ahead limit" in seconds.
     * In Anki this lives in colConf['collapseTime'] and defaults to
     * 1200 s (= 20 minutes).  We keep it as a simple constant.
     */
    private static final int COLLAPSE_TIME = 1200;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Creates a scheduler bound to the given deck.
     *
     * Mirrors Anki's Scheduler.__init__(col):
     *   - stores the collection (here: deck) reference
     *   - sets queue limits
     *   - computes today / dayCutoff
     *   - calls reset() to initialise the queues
     *
     * @param deck the deck whose cards this scheduler will manage.
     */
    public Scheduler(Deck deck) {
        this.deck = deck;

        // reps starts at 0 — no cards reviewed yet today.
        this.reps = 0;

        // Compute the current day number and the cutoff timestamp.
        // These depend on the parent StudyClass's creation time (crt).
        // At construction time the deck may not yet be linked to a StudyClass,
        // so we guard against that and allow re-initialisation via reset().
        this.today     = daysSinceCreation();
        this.dayCutoff = computeDayCutoff();

        // Initialise the card queues (to be implemented later).
        reset();
    }

    // -----------------------------------------------------------------------
    // Day / time calculations
    // -----------------------------------------------------------------------

    /**
     * Returns how many full days have passed since the parent StudyClass
     * was created.
     *
     * We implement this:
     *   (currentEpochSeconds − studyClass.crt) / 86400   (integer division)
     *
     * If the deck has not been added to a StudyClass yet, we return 0.
     *
     * @return number of elapsed days (≥ 0).
     */
    private int daysSinceCreation() {
        StudyClass studyClass = deck.getStudyClass();
        if (studyClass == null) {
            // Inconsistent state perhaps no? Because the DECK must be associated with a Class
            // Deck not yet attached to a StudyClass; treat as day 0.
            return 0;
        }

        long nowSeconds = SchedulingAlgoUtils.intTime();    // current epoch seconds
        long crt = studyClass.getCrt();                     // collection creation (epoch s)

        // 86400 seconds = 1 day
        return (int) ((nowSeconds - crt) / 86400);
    }

    /**
     * Computes the epoch-second timestamp of the start of *tomorrow* (midnight).
     *
     * In plain English: take today's midnight; if that is in the past
     * (which it always is unless it's exactly midnight), add one day.
     * The result is the epoch-second timestamp of *tomorrow* at 00:00.
     *
     * @return epoch seconds of the next midnight boundary.
     */
    private long computeDayCutoff() {
        // Get today's date at midnight in the system's default time zone.
        ZonedDateTime midnight = LocalDate.now()
                .atStartOfDay(ZoneId.of("UTC"));

        // midnight is in the past (unless it is exactly 00:00:00),
        // so move it to tomorrow.
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        if (midnight.isBefore(now)) {
            midnight = midnight.plusDays(1);
        }

        return midnight.toEpochSecond();
    }

    // -----------------------------------------------------------------------
    // Learn-ahead cutoff
    // -----------------------------------------------------------------------

    /**
     * Recalculates the learn-ahead cutoff if enough time has passed
     * (or if forced).
     *
     * Logic:
     *   • Compute a candidate cutoff = now + collapseTime (20 min).
     *   • Only apply it if it differs from the current cutoff by more
     *     than 60 seconds, **or** if {@code force} is true.
     *   • This avoids recalculating too frequently while still keeping
     *     the window reasonably up-to-date.
     *
     * @param force if true, always update regardless of the 60-second
     *              debounce window.
     * @return true if the cutoff was actually updated, false otherwise.
     */
    public boolean updateLrnCutoff(boolean force) {
        long nextCutoff = SchedulingAlgoUtils.intTime() + COLLAPSE_TIME;

       /* Has the window shifted forwared by more than 60 seconds?
        * OR is it forced, if yes then update the lrnCutOff
        * The 60-second rule exists because every time we answer a card, the algorithm would recompute: lrnCutOff = now + COLLAPSE_TIME
        * Instead, we only update the learn-ahead boundry if it is moved by 60 seconds*/

        if (nextCutoff - this.lrnCutoff > 60 || force) {
            this.lrnCutoff = nextCutoff;
            return true;
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Reset
    // -----------------------------------------------------------------------

    /**
     * Resets the scheduler's daily state.
     *
     * Called at construction time and whenever a new day is detected.
     * Recalculates {@code today}, {@code dayCutoff}, and resets counters.
     *
     * Queue-filling logic will be added in a later step.
     */
    public void reset() {
        this.today     = daysSinceCreation();
        this.dayCutoff = computeDayCutoff();
        // lrnCutoff is force-updated so that any learning cards due within
        // the learn-ahead window are immediately visible.
        updateLrnCutoff(true);
        // TODO: fill new / learning / review queues (next implementation step)
    }

    // -----------------------------------------------------------------------
    // Main API (stubs — will be implemented in the next step)
    // -----------------------------------------------------------------------

    /**
     * Returns the next card to review, or {@code null} if no cards are due.
     * (To be implemented.)
     */
    public FlashCard getCard() {
        // TODO: implement in next step
        return null;
    }

    /**
     * Updates the given card after the user has answered.
     *
     * @param card the card that was reviewed.
     * @param ease the user's answer:
     *             0 = Again, 1 = Hard, 2 = Good, 3 = Easy.
     * (To be implemented.)
     */
    public void answerCard(FlashCard card, int ease) {
        // TODO: implement in next step
    }
}
