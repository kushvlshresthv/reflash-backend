package com.project.reflash.backend.algorithm;

public final class SchedulingAlgoUtils {

    private SchedulingAlgoUtils() {
    }

    /**
     * Returns the current time as an integer.
     *
     * @param scale multiply by this before truncating.
     *              Pass 1 for seconds, 1000 for milliseconds.
     * @return current epoch time multiplied by {@code scale}, truncated to a long.
     */
    public static long intTime(int scale) {
        return System.currentTimeMillis() / 1000 * scale;
    }


    /**
     * Returns a unique integer identifier based on the current time in milliseconds.
     *
     * calls intTime(1000) and then busy-waits
     * until the millisecond changes so that two successive calls never collide.
     * We replicate the same behaviour here.
     *
     * @return a unique long identifier (epoch milliseconds at the moment of the call).
     *
     * NOTE: this method does not guarantee uniqueness if two separate threads call the intID() at the same millisecond
     * This method only guarantees uniqueness for consecutive call of intID() in the same thread
     *  long a = intId();
     *  long b = intId();
     *  the first call does not return until the clock ticks to the next millisecond and only then the second call is made.
     */

    public static long intId() {
        long t = intTime(1000);

        // Busy-wait until the clock ticks to the next millisecond.
        // This guarantees that two back-to-back calls produce different IDs.
        while (intTime(1000) == t) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return t;
    }
}
