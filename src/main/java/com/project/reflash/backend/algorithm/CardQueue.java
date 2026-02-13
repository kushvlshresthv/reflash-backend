package com.project.reflash.backend.algorithm;

/**
 * Represents the scheduling-queue a card belongs to.
 *
 * We store this as an integer in the database.  The queue determines
 * *when* a card is eligible to be shown, while {@link CardType} tracks
 * *what stage* the card is at.
 *
 * <pre>
 *  -1 = SUSPENDED – The card is suspended (e.g. it became a leech).
 *                    Manual suspension is not supported in our simplified version,
 *                    so this state is only reached automatically.
 *   0 = NEW       – The card has never been shown (waiting in the new-card queue).
 *   1 = LEARNING  – The card is in the learning / relearning queue
 *                    (short intervals, shown multiple times per session).
 *   2 = REVIEW    – The card is in the review queue (long intervals, once per day).
 * </pre>
 */
public enum CardQueue {

    SUSPENDED(-1),
    NEW(0),
    LEARNING(1),
    REVIEW(2);

    private final int value;

    CardQueue(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
