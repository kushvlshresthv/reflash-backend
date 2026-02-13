package com.project.reflash.backend.algorithm;

/**
 * Represents the learning-state of a card — exactly the four states Anki defines.
 *
 * In Anki's database the type is stored as an integer (0-3).
 * We use an enum for readability and safety, but keep the original int value
 * accessible via {@link #getValue()}.
 *
 * <pre>
 *  0 = NEW        – The card has never been shown to the user.
 *  1 = LEARNING   – The card is currently being learnt for the first time.
 *  2 = REVIEW     – The card has graduated and is in the long-term review cycle.
 *  3 = RELEARNING – The card was forgotten during review and is being re-learnt.
 * </pre>
 */
public enum CardType {

    NEW(0),
    LEARNING(1),
    REVIEW(2),
    RELEARNING(3);

    private final int value;

    CardType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
