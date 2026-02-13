package com.project.reflash.backend.algorithm;

import lombok.Getter;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

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

    // ── deck configuration constants (simplified) ─────────────────────────
    // In full Anki, these live in deckConf["new"]["perDay"] and
    // deckConf["rev"]["perDay"]. We hardcode sensible defaults.

    /** Maximum number of *new* cards to introduce per day. */
    private static final int NEW_CARDS_PER_DAY = 20;

    /** Maximum number of *review* cards to show per day. */
    private static final int REVIEW_CARDS_PER_DAY = 200;

    // ── new-card spread setting ───────────────────────────────────────────
    // In Anki, colConf['newSpread'] controls how new cards are mixed in
    // with review cards.  The constant below means "distribute new cards
    // evenly among reviews" (as opposed to showing them all at the start
    // or end of a session).

    /** Value of newSpread that means "interleave new cards among reviews". */
    private static final int NEW_CARDS_DISTRIBUTE = 1;

    /**
     * Current newSpread setting.
     * 0 = show new cards at the end,
     * 1 = distribute (interleave) new cards among reviews.
     * We default to DISTRIBUTE so the user gets a mixed session.
     */
    private int newSpread = NEW_CARDS_DISTRIBUTE;

    // ── card queues ───────────────────────────────────────────────────────
    // These three lists are the in-memory queues from which getCard()
    // draws the next card.  They start empty and are filled lazily
    // by the _fill*() methods the first time a card is requested.

    /** Queue of new cards (queue == NEW), sorted by due, limited to perDay. */
    private List<FlashCard> newQueue;

    /** Queue of learning cards (queue == LEARNING) that are due soon. */
    private List<FlashCard> lrnQueue;

    /** Queue of review cards (queue == REVIEW) that are due today, shuffled. */
    private List<FlashCard> revQueue;

    /**
     * Determines how often a new card is inserted between review cards.
     *
     * Example: if newCardModulus == 6, then every 6th card shown is a new card.
     * A value of 0 means "do not distribute" (new cards shown at the end).
     *
     * Calculated by {@link #updateNewCardRatio()} based on the sizes of
     * newQueue and revQueue.
     */
    private int newCardModulus = 0;

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
     */
    public void reset() {
        this.today     = daysSinceCreation();
        this.dayCutoff = computeDayCutoff();

        // Reset the three queues in the same order Anki does.
        resetLrn();
        resetRev();
        resetNew();
    }

    // =====================================================================
    //  NEW CARDS
    // =====================================================================

    /**
     * Clears the new-card queue and recalculates the new-card ratio.
     *
     * In Anki:
     *   def _resetNew(self):
     *       self._newQueue = []
     *       self._updateNewCardRatio()
     */
    private void resetNew() {
        this.newQueue = new ArrayList<>();
        updateNewCardRatio();
    }

    /**
     * Fills the new-card queue if it is empty.
     *
     * Logic (mirrors Anki):
     *   1. If the queue already has cards, return true immediately (no work needed).
     *   2. Otherwise, find all cards in the deck whose queue == NEW.
     *   3. Sort them by {@code due} (which equals the note id for new cards,
     *      so they appear in creation order).
     *   4. Trim to the daily limit: min(queueLimit, NEW_CARDS_PER_DAY).
     *   5. Return true if there are cards to study, false otherwise.
     *
     * @return true if the new-card queue is non-empty after filling.
     */
    boolean fillNew() {
        // Already have cards? Nothing to do.
        if (!newQueue.isEmpty()) {
            return true;
        }

        // Daily limit for new cards
        int limit = Math.min(queueLimit, NEW_CARDS_PER_DAY);

        // Filter: only cards sitting in the NEW queue (queue == 0).
        // Sort:   by due (= note id → creation order).
        // Limit:  take at most `limit` cards.
        newQueue = deck.getCards().stream()
                .filter(card -> card.getQueue() == CardQueue.NEW)
                .sorted(Comparator.comparingLong(FlashCard::getDue))
                .limit(limit)
                .collect(Collectors.toList());

        return !newQueue.isEmpty();
    }

    /**
     * Determines how often a new card should appear among review cards.
     *
     * When {@code newSpread == NEW_CARDS_DISTRIBUTE}:
     *   ratio = (newCount + revCount) / newCount
     *   If there are review cards, enforce ratio ≥ 2 so that at least
     *   one review card appears between every two new cards.
     *
     * Example: 10 new + 50 review → ratio = 60/10 = 6
     *          → every 6th card shown will be a new card.
     *
     * If newSpread is anything else (e.g. 0 = "show at end"), the modulus
     * is set to 0 which disables interleaving.
     */
    private void updateNewCardRatio() {
        if (newSpread == NEW_CARDS_DISTRIBUTE) {
            if (!newQueue.isEmpty()) {
                int newCount = newQueue.size();
                int revCount = revQueue != null ? revQueue.size() : 0;

                newCardModulus = (newCount + revCount) / newCount;

                // If there are review cards, make sure we don't show two
                // new cards in a row — enforce a minimum modulus of 2.
                if (revCount > 0) {
                    newCardModulus = Math.max(2, newCardModulus);
                }
                return;
            }
        }
        // Default: do not distribute new cards (show them at the end).
        newCardModulus = 0;
    }

    // =====================================================================
    //  LEARNING CARDS
    // =====================================================================

    /**
     * Clears the learning queue and force-updates the learn-ahead cutoff.
     *
     */
    private void resetLrn() {
        updateLrnCutoff(true);
        this.lrnQueue = new ArrayList<>();
    }

    /**
     * Fills the learning queue if it is empty.
     *
     * Logic :
     *   1. If the queue already has cards, return true.
     *   2. Compute a cutoff = now + collapseTime (learn-ahead window).
     *   3. Find all cards whose queue == LEARNING **and** due < cutoff.
     *   4. Sort by id (≈ creation timestamp, so older learning cards first).
     *   5. Trim to reportLimit.
     *
     * @return true if the learning queue is non-empty after filling.
     */
    boolean fillLrn() {
        if (!lrnQueue.isEmpty()) {
            return true;
        }

        // How far into the future we're willing to look for learning cards.
        long cutoff = SchedulingAlgoUtils.intTime() + COLLAPSE_TIME;

        // Filter: queue == LEARNING *and* due timestamp hasn't passed the cutoff.
        // Sort:   by card.id (= creation timestamp → FIFO order).
        // Limit:  reportLimit.
        lrnQueue = deck.getCards().stream()
                .filter(card -> card.getQueue() == CardQueue.LEARNING
                        && card.getDue() < cutoff)
                .sorted(Comparator.comparingLong(FlashCard::getId))
                .limit(reportLimit)
                .collect(Collectors.toList());

        return !lrnQueue.isEmpty();
    }

    // =====================================================================
    //  REVIEW CARDS
    // =====================================================================

    /**
     * Clears the review queue.
     */
    private void resetRev() {
        this.revQueue = new ArrayList<>();
    }

    /**
     * Fills the review queue if it is empty.
     *
     * Logic:
     *   1. If the queue already has cards, return true.
     *   2. Find all cards whose queue == REVIEW **and** due <= today.
     *      (due for review cards is a day-offset relative to the collection's
     *       creation time, so we compare against {@code this.today}.)
     *   3. Sort by due date.
     *   4. Trim to daily limit: min(queueLimit, REVIEW_CARDS_PER_DAY).
     *   5. Shuffle the result using a deterministic seed (= today)
     *      so that the order is randomised but reproducible within the
     *      same day.
     *
     * @return true if the review queue is non-empty after filling.
     */
    boolean fillRev() {
        if (!revQueue.isEmpty()) {
            return true;
        }

        int limit = Math.min(queueLimit, REVIEW_CARDS_PER_DAY);

        // Filter: queue == REVIEW and due day has arrived (due <= today).
        // Sort:   by due (so oldest-due cards are picked first).
        // Limit:  daily cap.
        revQueue = deck.getCards().stream()
                .filter(card -> card.getQueue() == CardQueue.REVIEW
                        && card.getDue() <= today)
                .sorted(Comparator.comparingLong(FlashCard::getDue))
                .limit(limit)
                .collect(Collectors.toList());

        if (!revQueue.isEmpty()) {
            // Shuffle with a seed = today so the order is random but
            // consistent within the same day (restarting the app doesn't
            // re-shuffle).
            Random rng = new Random(today);
            Collections.shuffle(revQueue, rng);
            return true;
        }

        return false;
    }


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
