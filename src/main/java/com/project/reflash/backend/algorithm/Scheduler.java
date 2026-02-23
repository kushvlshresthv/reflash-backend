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
 * Each {@link Deck} owns one Scheduler instance. The scheduler holds a
 * back-reference to its deck so it can access the card list and, through
 * the deck's parent {@link StudyClass}, the collection creation timestamp
 * required for day-offset calculations.
 *
 */

//NOTE: Scheduler will be primarily used by the frontend, therefore, for every request, a Scheduler is initialzed
@Getter
public class Scheduler {

    // ── references ────────────────────────────────────────────────────────

    /** The deck this scheduler operates on */
    //NOTE: this variable is only used to retrieve the cards, which is essentially done by the backend.
            //Therefore, this variable might not be necessary for implementing the schedular for the frontend
    private final Deck deck;

    // ── limits ────────────────────────────────────────────────────────────


    /**
     * Upper limit for the number of learning cards that can be fetched
     * in one study session.
     */
    //NOTE: fillLrn() uses this method to show the maximum number of learning cards in a session
    private int reportLimit = 1000;

    // ── daily state ───────────────────────────────────────────────────────

    /**
     * The number of cards already reviewed *today*.
     * Reset to 0 each day (or when reset() is called).
     */

    //NOTE: incremented when getCard() function returns a card
    //NOTE: reps is also a variable inside FlashCard class and it tracks the number of reviews done on this card, incremented when answerCard() is called.
    //NOTE: this variable is used to track whether a new card is to be shown or review card is to be shown in spread = DISTRIBUTE setting
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
    //NOTE: stores the number of full days that have elapsed since the collection was created
    private int today;

    /**
     * Epoch-second timestamp of the start of the *next* day (midnight).
     *
     * When the current time crosses this boundary the scheduler knows
     * a new day has begun and it should recalculate "today", refill the
     * new/review queues, etc.
     */
    //NOTE: stores the seconds for the midnight. When the current time crosses this boundry the scheduler knows a new day has bug and it should recalculated 'today', refill the new/review queues, etc
            //NOTE: used in the checkDay() function
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
    //NOTE: stores the current time in seconds and is updated by updateLrnCutoff()
            //NOTE: it is initialized in the constructor with reset() -> resetLrn() call
            //NOTE: it is updated when checkDay() decides to reset()
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

    // ── learning step configuration ───────────────────────────────────────
    // In full Anki, these live in deckConf["new"]["delays"] and
    // deckConf["laps"]["delays"]. We hardcode Anki's defaults.

    /**
     * Learning steps for NEW cards, in minutes.
     *
     * Default Anki config: [1, 10] means:
     *   Step 1: show again in 1 minute
     *   Step 2: show again in 10 minutes
     *   After completing both steps → card graduates to the review queue.
     *
     * In Anki this is deckConf["new"]["delays"].
     */
    private static final int[] NEW_STEPS = {1, 10};

    /**
     * Learning steps for LAPSED (relearning) cards, in minutes.
     *
     * Default Anki config: [10] means:
     *   Step 1: show again in 10 minutes
     *   After completing the step → card returns to the review queue.
     *
     * In Anki this is deckConf["laps"]["delays"].
     */
    private static final int[] LAPSE_STEPS = {10};

    /**
     * Minimum interval (in days) for a card after a lapse.
     *
     * When a review card is answered "Again", its interval gets reduced.
     * This constant sets a floor so the interval never drops below 1 day.
     *
     * In Anki this is deckConf["lapse"]["minInt"].
     */
    private static final int LAPSE_MIN_IVL = 1;

    /**
     * Multiplier applied to the current interval after a lapse.
     *
     * E.g. 0 means the interval resets to LAPSE_MIN_IVL.
     * A value of 0.5 would halve the interval.
     *
     * In Anki this is deckConf["lapse"]["mult"].
     * Default Anki value is 0 (= full reset).
     */
    private static final double LAPSE_MULT = 0;

    /**
     * Number of lapses before a card is considered a "leech".
     *
     * A leech is a card that keeps being forgotten despite many reviews.
     * When a card reaches this many lapses, it is automatically suspended
     * (queue = SUSPENDED) and tagged with "leech" so the user can review
     * the note and decide what to do with it.
     *
     * In Anki this is deckConf["lapse"]["leechFails"].  Default = 8.
     */
    private static final int LEECH_FAILS = 8;

    /**
     * The initial ease factor assigned to a card when it graduates from
     * NEW → REVIEW for the first time, in permille.
     *
     * 2500 means the interval will be multiplied by 2.5 the next time
     * the user presses "Good".
     */
    private static final int INITIAL_FACTOR = 2500;

    /**
     * The interval (in days) assigned to a card when it graduates from
     * learning after completing all steps ("Good" on the last step).
     */
    //NOTE: when graduated by completing all steps, set the graduating interval to GRADUATING_IVL
    private static final int GRADUATING_IVL = 1;

    /**
     * The interval (in days) assigned to a card when it graduates early
     * by pressing "Easy" during learning.
     */
    //NOTE: when easy button is clicked, set the graduating interval to EASY_IVL
    private static final int EASY_IVL = 4;

    // ── new-card spread setting ───────────────────────────────────────────
    // In Anki, colConf['newSpread'] controls how new cards are mixed in
    // with review cards.  The constant below means "distribute new cards
    // evenly among reviews" (as opposed to showing them all at the start
    // or end of a session).

    /**
     * newSpread constants — control when new cards appear in a session.
     *   0 = NEW_CARDS_DISTRIBUTE → interleave new cards among reviews (default).
     *   1 = NEW_CARDS_LAST       → show new cards after all reviews.
     *   2 = NEW_CARDS_FIRST      → show new cards before any reviews.
     */
    private static final int NEW_CARDS_DISTRIBUTE = 0;
    private static final int NEW_CARDS_LAST       = 1;
    private static final int NEW_CARDS_FIRST      = 2;

    /**
     * Current newSpread setting.
     * 0 = distribute (interleave) new cards among reviews (default).
     * 1 = show new cards at the end.
     * 2 = show new cards at the start.
     */
    //NOTE: allow the users to customize this setting
    private int newSpread = NEW_CARDS_DISTRIBUTE;

    // ── card queues ───────────────────────────────────────────────────────
    // These three lists are the in-memory queues from which getCard()
    // draws the next card.  They start empty and are filled lazily
    // by the _fill*() methods the first time a card is requested.

    //NOTE: these queues are returned by the backend to the frontend
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
     * @param deck the deck whose cards this scheduler will manage.
     */

    //NOTE: in the frontend, initialize the scheduler by requesting the cards from the backend
    //NOTE: the constructor initializes the queues as we as 'today' and 'dayCutoff' variables
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


    //TODO: the day cut off is computed in UTC, but this depends on the user and the value is supplied by the service layer, will fix later, let it be like this right now
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
     * Recalculates {@code today}, {@code dayCutoff}, and resets all queues.
     *
     */
    public void reset() {
        updateCutoff();

        // Reset the three queues in the same order Anki does.
        resetLrn();
        //NOTE: resetRev() must be called before resetNew() because resetNew() calls updateNewCardRation() to calculate the cardModulus
        resetRev();
        resetNew();
    }

    /**
     * Refreshes the day-related fields: {@code today} and {@code dayCutoff}.
     *
     * Called every time the queues are reset (= once per day).
     * When a new day begins the day counter advances and the cutoff
     * moves to the next midnight.
     *
     */
    private void updateCutoff() {
        this.today     = daysSinceCreation();
        this.dayCutoff = computeDayCutoff();
    }

    /**
     * Checks whether the current day has rolled over past {@code dayCutoff}.
     *
     * If the current time exceeds dayCutoff it means a new day has begun,
     * so we call {@link #reset()} to refresh the day counter and reinitialise
     * all queues — other cards may now be due.
     *
     * This is called at the top of {@link #getCard()} every time a card is
     * requested, so the transition is seamless even during a long study session.
     *
     */
    private void checkDay() {
        if (SchedulingAlgoUtils.intTime() > this.dayCutoff) {
            reset();
        }
    }

    // =====================================================================
    //  NEW CARDS
    // =====================================================================

    /**
     * Clears the new-card queue and recalculates the new-card ratio.
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
     *   4. Trim to the daily limit: NEW_CARDS_PER_DAY.
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
        int limit = NEW_CARDS_PER_DAY;

        // Filter: only cards sitting in the NEW queue (queue == 0).
        // Sort:   by due (= note id → creation order).
        // Limit:  take at most `limit` cards.
        newQueue = deck.getCards().stream()
                .filter(card -> card.getQueue() == CardQueue.NEW)
                //NOTE: sorted based on FlashCard::getId to sort based on the creation time
                .sorted(Comparator.comparingLong(FlashCard::getId))
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
     *          → every 6th card shown will be a new card
     *          NOTE: the 5 cards may be learning card or review card or mixed
     *          - see implementation of timeForNewCard where 'reps' is used
     *          - to decide whether to show a new card or not
     *
     * If newSpread is anything else (e.g. 0 = "show at end"), the modulus
     * is set to 0 which disables interleaving.
     */
    private void updateNewCardRatio() {
        if (newSpread == NEW_CARDS_DISTRIBUTE) {
            if (!newQueue.isEmpty()) {
                int newCount = newQueue.size();
                //NOTE: resetRev() is called before resetNew() which in turn calls this method. Therefore, the review queue has already been populated
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

        //ORIGINAL
        // Filter: queue == LEARNING *and* due timestamp hasn't passed the cutoff.
        // Sort:   by card.id (= creation timestamp → FIFO order).
        // Limit:  reportLimit.

        //NEW FIX: sort by due date
        lrnQueue = deck.getCards().stream()
                .filter(card -> card.getQueue() == CardQueue.LEARNING
                        && card.getDue() < cutoff)
                .sorted(Comparator.comparingLong(FlashCard::getDue))
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

        int limit = REVIEW_CARDS_PER_DAY)

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


    // =====================================================================
    //  CARD RETRIEVAL — public API
    // =====================================================================

    /**
     * Returns the next card to study, or {@code null} if the session is over.
     *
     * Before fetching a card we check whether a new day has started
     * (via {@link #checkDay()}).  If a card is returned, the
     * {@code reps} counter is incremented — this counter drives the
     * new-card distribution logic ({@link #timeForNewCard()}).
     *
     * Mirrors Anki's Scheduler.getCard().
     */
    public FlashCard getCard() {
        // If the day has rolled over, reset the queues so that newly-due
        // cards become available.
        checkDay();

        FlashCard card = getCardInternal();
        if (card != null) {
            // Increment the session counter.  This is used by
            // timeForNewCard() to decide when to interleave a new card.
            reps += 1;
            return card;
        }
        // No cards left — study session is complete.
        return null;
    }

    // =====================================================================
    //  CARD RETRIEVAL — internal logic
    // =====================================================================

    /**
     * Core card-selection logic.  Tries the queues in a carefully chosen
     * order that mirrors Anki's priority:
     *
     *   1. Learning cards that are due right now   (highest priority)
     *   2. New cards — IF it's "time" for one       (interleave / first)
     *   3. Review cards
     *   4. New cards — any remaining                (catch-all)
     *   5. Learning cards — with collapse           (look-ahead window)
     *
     * The first non-null result wins.
     *
     * Mirrors Anki's Scheduler._getCard().
     *
     * @return the next due card, or {@code null} if nothing is available.
     */
    private FlashCard getCardInternal() {

        // 1. Learning card due right now?
        FlashCard c = getLrnCard();
        if (c != null) return c;

        // 2. Is it time to show a new card (distribute / first)?
        if (timeForNewCard()) {
            c = getNewCard();
            if (c != null) return c;
        }

        // 3. Review card due today?
        c = getRevCard();
        if (c != null) return c;

        // 4. Any new cards left (covers NEW_CARDS_LAST and exhausted reviews)?
        c = getNewCard();
        if (c != null) return c;

        // 5. Collapse: look ahead for learning cards within the collapse window.
        //    This avoids ending the session when a learning card is almost due.
        c = getLrnCard();
        return c; // may be null → session over
    }

    // ── new cards ──────────────────────────────────────────────────────────

    /**
     * Pops and returns the next new card from the queue, or {@code null}.
     *
     * The queue is lazily filled by {@link #fillNew()} the first time
     * this method is called.
     *
     */

    //NOTE: it simply removes the last new card from the queue
    //NOTE: if the card isn't answered, how is it handled? the card has been removed from the frontend
    //NOTE: when the card is re-requested, perhaps it will be loaded again in our server-client architecture.
    private FlashCard getNewCard() {
        if (fillNew()) {
            // Pop the last element (most efficient for an ArrayList).
            return newQueue.remove(newQueue.size() - 1);
        }
        return null;
    }

    /**
     * Decides whether it is time to show a new card right now.
     *
     * The decision depends on the {@code newSpread} setting:
     *   - NEW_CARDS_LAST       → never (new cards come after reviews).
     *   - NEW_CARDS_FIRST      → always (new cards come before reviews).
     *   - NEW_CARDS_DISTRIBUTE → yes if  reps % newCardModulus == 0
     *                            (i.e. every N-th card is a new card).
     *
     * Mirrors Anki's Scheduler._timeForNewCard().
     *
     * @return true if a new card should be shown now.
     */
    //NOTE: based on the spread settings and newCardModulus, decides whether it is time for a new card or not
    private boolean timeForNewCard() {
        // No new cards available? Nothing to decide.
        if (newQueue.isEmpty() && !fillNew()) {
            return false;
        }

        if (newSpread == NEW_CARDS_LAST) {
            // New cards are shown only after all reviews are done.
            return false;
        } else if (newSpread == NEW_CARDS_FIRST) {
            // New cards are shown before any reviews.
            return true;
        } else {
            // NEW_CARDS_DISTRIBUTE:
            // Show a new card every `newCardModulus` reviews.
            // reps is 0-based at this point so the very first card (reps==0)
            // won't match; that's fine — a learning/review card goes first.
            return newCardModulus != 0
                    && reps > 0
                    && reps % newCardModulus == 0;
        }
    }

    // ── learning cards ────────────────────────────────────────────────────
    /**
     * Pops and returns the next learning card from the queue, or {@code null}.
     */
    private FlashCard getLrnCard() {
        if (fillLrn()) {
            return lrnQueue.remove(lrnQueue.size() - 1);
        }
        return null;
    }

    // ── review cards ──────────────────────────────────────────────────────

    /**
     * Pops and returns the next review card from the queue, or {@code null}.
     */
    private FlashCard getRevCard() {
        if (fillRev()) {
            return revQueue.remove(revQueue.size() - 1);
        }
        return null;
    }

    // =====================================================================
    //  ANSWER CARD — public API
    // =====================================================================

    /**
     * Updates the given card after the user has answered.
     *
     * This is the second core method of the scheduler (alongside getCard).
     * It dispatches to a specialised handler based on the card's current queue:
     *
     *   queue == NEW      → {@link #answerNewCard(FlashCard, int)}
     *   queue == LEARNING  → {@link #answerLrnCard(FlashCard, int)}
     *   queue == REVIEW    → {@link #answerRevCard(FlashCard, int)}
     *
     * Before dispatching, the card's {@code reps} counter is incremented in FlashCard object(not the scheduler which also has reps counter)
     * (total number of times this card has ever been reviewed).
     *
     * @param card the card that was reviewed.
     * @param ease the user's answer (1-based):
     *             1 = Again, 2 = Hard, 3 = Good, 4 = Easy.
     * @throws IllegalArgumentException if ease is not in [1, 4] or the
     *                                  card's queue is unexpected.
     */
    //NOTE: this should be implemented in the frontend and then update time should be sent to the backend
    public void answerCard(FlashCard card, int ease) {
        // Validate inputs — same assertions as Anki:
        //   assert 1 <= ease <= 4
        if (ease < 1 || ease > 4) {
            throw new IllegalArgumentException("ease must be between 1 and 4, got: " + ease);
        }

        // Increment the card's total review count.
        card.setReps(card.getReps() + 1);

        // Dispatch based on the card's current queue.
        if (card.getQueue() == CardQueue.NEW) {
            // Brand-new card being seen for the first time.
            answerNewCard(card, ease);

        } else if (card.getQueue() == CardQueue.LEARNING) {
            // Card is in the learning (or relearning) queue.
            answerLrnCard(card, ease);

        } else if (card.getQueue() == CardQueue.REVIEW) {
            // Card is in the review queue.
            answerRevCard(card, ease);

        } else {
            throw new IllegalStateException(
                    "Unexpected card queue: " + card.getQueue());
        }
    }

    // =====================================================================
    //  ANSWERING NEW CARDS
    // =====================================================================

    /**
     * Handles answering a card that is currently in the NEW queue.
     *
     * What happens:
     *   1. Move the card from the NEW queue to the LEARNING queue.
     *   2. Set the card type to LEARNING.
     *   3. Initialise the {@code left} field which encodes how many
     *      learning steps remain (both for today and until graduation).
     *
     * After this method, the card is in the learning pipeline and will
     * be handled by answerLrnCard on subsequent reviews.
     *
     * @param card the new card being answered.
     * @param ease the user's answer (1–4). Not used for new cards because
     *             the card always moves to LEARNING regardless of ease.
     */
    //NOTE: if the new card is answered, the Queue type is changed to LEARNING, but it is not moved to the learning queue.
    //NOTE: Anki simply updates the attribute queue to move a card to different queue. When the destination queue will be reset(ex: for tomorrow's session), the card will be automatically inserted into it)

    //TODO: why isn't the due date being updated based on the ease here?

    private void answerNewCard(FlashCard card, int ease) {
        // Move from the NEW queue → LEARNING queue.
        // Anki does: card.queue = 1; card.type = 1;
        card.setQueue(CardQueue.LEARNING);
        card.setType(CardType.LEARNING);

        // Initialise the learning-steps counter.
        // This tells the scheduler how many steps are left before
        // the card graduates to the REVIEW queue.
        //NOTE: this sets the left to the first step of the learning steps
        card.setLeft(startingLeft(card));

        //NOTE: after initializations, answer like a learning card ie if 'easy' was clicked, graduate the card and stuffs
        answerLrnCard(card, ease);
    }

    // =====================================================================
    //  LEARNING-STEP HELPERS
    // =====================================================================

    /**
     * Returns the learning-step delays for the given card.
     *
     * If the card is (re-)learning after a lapse (type == REVIEW or
     * type == RELEARNING) it uses {@link #LAPSE_STEPS}.
     * Otherwise it uses {@link #NEW_STEPS}.
     *
     * @param card the card.
     * @return the array of step delays in minutes.
     */
    int[] lrnConf(FlashCard card) {
        // If the card was previously a review card (lapse), use lapse steps.
        // Otherwise use new-card steps.
        if (card.getType() == CardType.REVIEW || card.getType() == CardType.RELEARNING) {
            return LAPSE_STEPS;
        }
        return NEW_STEPS;
    }

    /**
     * Computes the initial value of the {@code left} field for a card
     * that is entering the learning queue.
     *
     * The left field encodes two numbers as:  {@code  today * 1000 + total_left}
     *   - total = total number of learning steps (e.g. 2 for [1, 10])
     *   - today = how many of those steps can be completed before
     *             the day cutoff ({@link #leftToday}).
     *
     * Example with steps [1, 10] starting at 23:55:
     *   total = 2
     *   today = 1  (only the 1-min step fits before midnight)
     *   left  = 1 * 1000 + 2 = 1002
     *
     *
     * @param card the card entering the learning queue.
     * @return encoded left value.
     */
    private int startingLeft(FlashCard card) {
        int[] delays = lrnConf(card);
        // Total number of steps until graduation.
        int total = delays.length;
        // How many of those steps can be completed today.
        int today = leftToday(delays, total);
        // Encode as:  todaySteps * 1000 + totalSteps
        return today * 1000 + total;
    }

    /**
     * Calculates how many learning steps (out of {@code left}) can be
     * completed before the day cutoff.
     *
     * Starting from "now", we walk through the *last* {@code left} delays
     * (since earlier steps have already been completed) and check if adding
     * each delay (in minutes → seconds) still lands before {@link #dayCutoff}.
     *
     * Example:
     *   delays = [1, 10],  left = 2,  now = 23:55,  dayCutoff = 00:00
     *     step 0: now + 1 min  = 23:56  < 00:00 ✓  (ok = 1)
     *     step 1: now + 10 min = 00:06  > 00:00 ✗  (break)
     *   → returns 1  (only 1 step fits today)
     *
     * @param delays the full array of learning-step delays (in minutes).
     * @param left   how many steps remain (we use the *last* {@code left}
     *               entries of the delays array).
     * @return the number of steps completable before dayCutoff (≥ 1).
     */
    private int leftToday(int[] delays, int left) {
        long now = SchedulingAlgoUtils.intTime();

        // We only care about the last `left` delays.
        // E.g. if delays=[1,10] and left=2, offset=0 so we start from index 0.
        // If delays=[1,10] and left=1, offset=1 so we start from index 1 (the 10-min step).
        int offset = delays.length - left;

        int ok = 0;
        for (int i = 0; i < left; i++) {
            // Add the delay (convert minutes → seconds).
            now += delays[offset + i] * 60L;

            // If this step lands after the day cutoff, stop counting.
            if (now > dayCutoff) {
                break;
            }
            ok = i + 1;
        }

        // At least 1 step can always be done today (even if it overflows).
        return Math.max(ok, 1);
    }

    // =====================================================================
    //  ANSWERING LEARNING CARDS
    // =====================================================================

    /**
     * Handles answering a card that is currently in the LEARNING queue.
     *
     * Dispatches to one of four actions based on the ease button:
     *
     *   ease 4 (Easy)  → immediately graduate to review queue.
     *   ease 3 (Good)  → advance to the next step; if no steps remain,
     *                     graduate to review.
     *   ease 2 (Hard)  → repeat the current step (same delay again).
     *   ease 1 (Again) → go back to the first step.
     *
     * @param card the learning card being answered.
     * @param ease 1=Again, 2=Hard, 3=Good, 4=Easy.
     */
    //NOTE: when a learning card is answered:
    private void answerLrnCard(FlashCard card, int ease) {
        int[] conf = lrnConf(card);

        if (ease == 4) {
            // "Easy" — skip remaining steps and graduate immediately.
            rescheduleAsRev(card, conf, true);

        } else if (ease == 3) {
            // "Good" — check if the card has finished all its steps.
            // card.left % 1000 gives the total steps remaining.
            // If only 1 (or 0) step remains, the card graduates.
            int stepsLeft = card.getLeft() % 1000;
            if (stepsLeft - 1 <= 0) {
                // No more steps → graduate to review.
                rescheduleAsRev(card, conf, false);
            } else {
                // More steps remain → move to the next one.
                moveToNextStep(card, conf);
            }

        } else if (ease == 2) {
            // "Hard" — repeat the current step with the same delay.
            // NOTE: The current card step is repeated. This means the attribute `left` is unchanged. We still have the same number of steps before graduation.
            // NOTE: The difference is that the card will be scheduled in a delay slightly longer than the previous one. We average the last and next delays [Ex: 1m 10m 20m and we are at step 2 => repeat in 15m)
            repeatStep(card, conf);

        } else {
            // ease == 1, "Again" — back too the very first step.

            //NOTE: We restore the attribute 'left' as if the card were new

            //NOTE: We process lapses differently(the RELEARNING cards probably). By default we reset the attribute ivl to 1(next review in one day)(ivl is only applicable for review cards, no?)

            //NOTE: The card due date is determined by adding the next step to the current date. The card remains in the learning queue(1). (Since the left was set as if the card were new, we are back to the first step.)

            //NOTE: The delayForGrade() is a helper method to get the next step (to calculate the due date). This method extract the number of remaining steps from the attribute 'left' (Ex: 1002 => 2 remaining steps) and uses the setting delay to find the matching delay(Ex: 1m 10m 1d => next study in 10m)

            moveToFirstStep(card, conf);
        }
    }

    // ── Again (ease 1) ────────────────────────────────────────────────────

    /**
     * Moves the card back to the first learning step.
     *
     * @param card the card to reset.
     * @param conf the step delays array (minutes).
     */
    private void moveToFirstStep(FlashCard card, int[] conf) {
        // Reset the steps counter as if the card is freshly entering learning.
        card.setLeft(startingLeft(card));

        // If this is a relearning card (a review card that lapsed),
        // reduce its review interval to reflect the failure.
        if (card.getType() == CardType.RELEARNING) {
            updateRevIvlOnFail(card);
        }

        // Schedule the card for the first step's delay.
        rescheduleLrnCard(card, conf, null);
    }

    /**
     * After a lapse ("Again" on a relearning card), reduce the card's
     * review interval.
     *
     * @param card the lapsed card.
     */
    private void updateRevIvlOnFail(FlashCard card) {
        card.setIvl(lapseIvl(card));
    }

    /**
     * Computes the new interval for a card after a lapse.
     *
     * Formula:  max(1, minInt, ivl × mult)
     *
     * With the default settings (mult=0, minInt=1) this always returns 1,
     * meaning the card's interval resets to 1 day.
     *
     * @param card the lapsed card.
     * @return the new interval in days (≥ 1).
     */
    private int lapseIvl(FlashCard card) {
        int ivl = (int) (card.getIvl() * LAPSE_MULT);
        return Math.max(1, Math.max(LAPSE_MIN_IVL, ivl));
    }

    // ── Scheduling helpers ────────────────────────────────────────────────

    /**
     * Reschedules a learning card: sets its due date and keeps it in
     * the LEARNING queue.
     *
     * If {@code delay} is {@code null}, the delay is derived from the
     * card's current step using {@link #delayForGrade}.
     *
     * @param card  the card to reschedule.
     * @param conf  the step delays array (minutes).
     * @param delay override delay in seconds, or {@code null} to
     *              use the current step's delay.
     * @return the delay that was applied (in seconds).
     */
    private long rescheduleLrnCard(FlashCard card, int[] conf, Long delay) {
        if (delay == null) {
            delay = delayForGrade(conf, card.getLeft());
        }

        // Set due = now + delay (epoch seconds).
        card.setDue(SchedulingAlgoUtils.intTime() + delay);
        // Keep (or move) the card in the learning queue.
        card.setQueue(CardQueue.LEARNING);

        return delay;
    }

    /**
     * Returns the delay in seconds for the current learning step.
     *
     * Extracts the number of remaining steps from {@code left}
     * (the low 3 digits = total steps remaining) and looks up the
     * matching delay from the conf array.
     *
     * @param conf the step delays array (in minutes).
     * @param left the card's left field.
     * @return delay in seconds.
     */
    private long delayForGrade(int[] conf, int left) {
        // Extract total steps remaining from the low 3 digits.
        int stepsRemaining = left % 1000;

        //NOTE: stepsRemaining is initialized with conf.length meaning that at the last step, it is equal '1'. Therefore the following operation does not overflow. (see 'startLeft() implementation above)

        //NOTE: for the first step, stepsRemaining = conf.length and hence 0th index is accessed
        int delayMinutes = conf[conf.length - stepsRemaining];
        // Convert minutes → seconds.
        return delayMinutes * 60L;
    }

    /**
     * Advances the card to the next learning step ("Good" button, steps remaining).
     *
     * Decrements the total-steps counter (low 3 digits of {@code left}) and
     * recalculates how many of the remaining steps can fit before the day cutoff.
     *
     * @param card the card to advance.
     * @param conf the step delays array (minutes).
     */
    private void moveToNextStep(FlashCard card, int[] conf) {
        // Decrement the total number of remaining steps.
        int left = (card.getLeft() % 1000) - 1;

        // Recalculate how many of those remaining steps can be done today,
        // and re-encode the left field.
        card.setLeft(leftToday(conf, left) * 1000 + left);

        // Reschedule with the delay for the new current step.
        rescheduleLrnCard(card, conf, null);
    }

    private void repeatStep(FlashCard card, int[] conf) {
        // "Hard" — repeat the current step, but with a slightly longer delay.
        // Instead of using the exact same delay, we average the current step's
        // delay with the next step's delay so the wait is a bit longer.
        long delay = delayForRepeatingGrade(conf, card.getLeft());
        rescheduleLrnCard(card, conf, delay);
    }

    /**
     * Computes the delay for repeating the current step ("Hard" button).
     *
     * Takes the average of the current step's delay and the next step's delay.
     * This makes the user wait a bit longer than the current step but not as
     * long as the next step.
     *
     * Example:
     *   steps = [1, 10, 20],  currently at step 2 (10 min)
     *   delay1 = 10 min (current step)
     *   delay2 = 20 min (next step)
     *   avg = (10 + max(10, 20)) / 2 = (10 + 20) / 2 = 15 min
     *
     * If on the last step (no next step), delay2 will be the same step,
     * so the average equals the current delay.
     *
     * @param conf the step delays array (in minutes).
     * @param left the card's left field.
     * @return delay in seconds.
     */
    private long delayForRepeatingGrade(int[] conf, int left) {
        long delay1 = delayForGrade(conf, left);
        long delay2;
        int next = (left - 1) % 1000;

        //if this is the last step, delay2 = delay1, else delay2 = next delay option
        if (next == 0) {
            delay2 = delay1;
        } else {
            delay2 = delayForGrade(conf, left - 1);
        }
        // Average of current delay and the larger of the two.
        // This ensures the result is always >= delay1.
        return (delay1 + Math.max(delay1, delay2)) / 2;
    }

    /**
     * Graduates a learning card to the REVIEW queue.
     *
     * Called when:
     *   - The user presses "Easy" during learning (early = true).
     *   - The user presses "Good" on the last learning step (early = false).
     *
     * The logic differs depending on whether the card is a lapse
     * (previously a review card that was forgotten) or a genuinely new card.
     *
     * @param card  the card to graduate.
     * @param conf  the step delays array (minutes).
     * @param early true if the user pressed "Easy" (skip remaining steps).
     */
    private void rescheduleAsRev(FlashCard card, int[] conf, boolean early) {
        // Was this card in the review queue before it lapsed?
        // card.type tracks the *original* state: REVIEW means it was a review
        // card that failed and entered relearning.
        boolean lapse = (card.getType() == CardType.REVIEW);

        if (lapse) {
            rescheduleGraduatingLapse(card);
        } else {
            rescheduleNew(card, conf, early);
        }
    }

    /**
     * Graduates a lapsed card back to the REVIEW queue.
     *
     * The interval (ivl) was already reduced by {@link #updateRevIvlOnFail}
     * when the card first lapsed.  We simply set the due date to
     * today + that reduced interval.
     *
     * In Anki:
     *   def _rescheduleGraduatingLapse(self, card):
     *       card.due = self.today + card.ivl
     *       card.type = card.queue = 2
     *
     * @param card the lapsed card being graduated.
     */
    private void rescheduleGraduatingLapse(FlashCard card) {
        // due for review cards = day offset relative to collection creation.
        card.setDue(this.today + card.getIvl());
        card.setType(CardType.REVIEW);
        card.setQueue(CardQueue.REVIEW);
    }

    /**
     * Graduates a genuinely new card to the REVIEW queue for the first time.
     *
     * Initialises the three key SRS fields:
     *   - ivl    = graduating interval (1 day for "Good", 4 days for "Easy")
     *   - due    = today + ivl
     *   - factor = initial ease factor (2500 = ×2.5)
     *
     * @param card  the new card being graduated.
     * @param conf  the step delays array (minutes) — used indirectly.
     * @param early true if graduating via "Easy" button.
     */
    private void rescheduleNew(FlashCard card, int[] conf, boolean early) {
        card.setIvl(graduatingIvl(card, conf, early));
        card.setDue(this.today + card.getIvl());
        card.setFactor(INITIAL_FACTOR);
        card.setType(CardType.REVIEW);
        card.setQueue(CardQueue.REVIEW);
    }

    /**
     * Determines the initial interval when a card graduates to review.
     *
     * - If the card was already a review/relearning card (lapse), keep its
     *   current interval.
     * - If it's a new card graduating normally ("Good"), use GRADUATING_IVL
     *   (default 1 day).
     * - If it's a new card graduating early ("Easy"), use EASY_IVL
     *   (default 4 days).
     *
     * @param card  the card.
     * @param conf  the step delays array (not directly used here).
     * @param early true if graduating via "Easy".
     * @return interval in days.
     */
    private int graduatingIvl(FlashCard card, int[] conf, boolean early) {
        // Lapsed cards keep their existing (already-reduced) interval.
        if (card.getType() == CardType.REVIEW || card.getType() == CardType.RELEARNING) {
            return card.getIvl();
        }

        // New card graduating:
        if (!early) {
            // Completed all steps → use the normal graduating interval.
            return GRADUATING_IVL;  // default: 1 day
        } else {
            // Pressed "Easy" → use the early-graduation interval.
            return EASY_IVL;        // default: 4 days
        }
    }

    // =====================================================================
    //  ANSWERING REVIEW CARDS
    // =====================================================================

    /**
     * Handles answering a card that is currently in the REVIEW queue.
     *
     * Two paths:
     *   ease == 1 (Again) → the card has lapsed, handle via {@link #rescheduleLapse}.
     *   ease >= 2          → the card was recalled, reschedule with a new interval.
     *
     * @param card the review card being answered.
     * @param ease 1=Again, 2=Hard, 3=Good, 4=Easy.
     */
    private void answerRevCard(FlashCard card, int ease) {
        if (ease == 1) {
            rescheduleLapse(card);
        } else {
            rescheduleRev(card, ease);
        }
    }

    // ── Again (ease 1) — lapse ────────────────────────────────────────────

    /**
     * Handles a lapse — the user pressed "Again" on a review card.
     *
     * What happens:
     *   1. Increment the card's lapse counter.
     *   2. Reduce the ease factor by 200 (0.2), floored at 1300 (1.3).
     *      This follows the SM-2 recommendation: never let ease drop below 1.3.
     *   3. Check if the card has become a leech (too many lapses).
     *   4. If NOT suspended as a leech:
     *        - Keep type = REVIEW (so lrnConf returns LAPSE_STEPS).
     *        - Move to the first learning step via moveToFirstStep.
     *   5. If suspended as a leech:
     *        - No relearning steps; just reduce the interval.
     *
     * In Anki:
     *   def _rescheduleLapse(self, card):
     *       conf = self.col.deckConf["lapse"]
     *       card.lapses += 1
     *       card.factor = max(1300, card.factor - 200)
     *       suspended = self._checkLeech(card, conf)
     *       if not suspended:
     *           card.type = 2
     *           delay = self._moveToFirstStep(card, conf)
     *       else:
     *           self._updateRevIvlOnFail(card, conf)
     *           delay = 0
     *       return delay
     *
     * @param card the review card that lapsed.
     */
    private void rescheduleLapse(FlashCard card) {
        // 1. Increment lapse counter.
        card.setLapses(card.getLapses() + 1);

        // 2. Reduce ease factor by 200 (= 0.2 in real terms).
        //    Floor at 1300 (= 1.3×) as recommended by SM-2.
        card.setFactor(Math.max(1300, card.getFactor() - 200));

        // 3. Check if the card is now a leech.
        boolean suspended = checkLeech(card);

        if (!suspended) {
            // 4a. Not a leech → enter relearning.
            //     Keep type = REVIEW so that lrnConf() returns LAPSE_STEPS
            //     and rescheduleAsRev() knows this is a lapse (not a new card).

            //NOTE: here the card type is set to REVIEW seems to be correct
            //NOTE: in the answerLrnCard, for easy and again options, the card type of REVIEW is checked for special actions on RELEARNING cards
            card.setType(CardType.REVIEW);
            moveToFirstStep(card, LAPSE_STEPS);
        } else {
            // 4b. Suspended as a leech → no relearning steps.
            //     Just reduce the review interval for when the user
            //     eventually unsuspends the card.
            updateRevIvlOnFail(card);
        }
    }

    // ── Leech detection ───────────────────────────────────────────────────

    /**
     * Checks whether a card has become a leech (too many lapses).
     *
     * A leech is a card that the user keeps forgetting. When the lapse
     * count reaches {@link #LEECH_FAILS}, the card is:
     *   1. Tagged with "leech" on its note (so the user can find it).
     *   2. Suspended (queue = SUSPENDED = -1).
     *
     * A suspended card is invisible to all fill*() methods — it won't
     * appear in any queue until the user manually unsuspends it.
     *
     * @param card the card to check.
     * @return true if the card was suspended as a leech, false otherwise.
     */
    private boolean checkLeech(FlashCard card) {
        if (card.getLapses() >= LEECH_FAILS) {
            // Tag the note so the user can easily find leeches.
            card.getNote().addTag("leech");
            // Suspend the card — it will no longer appear in any queue.
            card.setQueue(CardQueue.SUSPENDED);
            return true;
        }
        return false;
    }

    // ── Hard / Good / Easy (ease 2-4) — reschedule review ─────────────────

    /**
     * Reschedules a review card after a successful recall (ease >= 2).
     *
     * This is where the SRS interval multiplication and ease factor
     * adjustment happens. Will be implemented in the next step.
     *
     * @param card the review card being rescheduled.
     * @param ease 2=Hard, 3=Good, 4=Easy.
     */
    private void rescheduleRev(FlashCard card, int ease) {
        // TODO: implement in the next step (interval calculation, ease adjustment, fuzz).
    }
}
