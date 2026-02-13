package com.project.reflash.backend.algorithm;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Note {

    /**
     * Unique identifier for this note, generated via {@link SchedulingAlgoUtils#intId()}.
     * It is based on the current epoch time in milliseconds so that every note
     * has a globally unique ID.
     */
    private final long id;

    /**
     * Free-form tags attached to this note (e.g. "biology", "chapter-3").
     * Tags are unique per note — adding a duplicate is silently ignored.
     */
    private final List<String> tags;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public Note() {
        this.id = SchedulingAlgoUtils.intId();
        this.tags = new ArrayList<>();
    }

    // -----------------------------------------------------------------------
    // Tag helpers
    // -----------------------------------------------------------------------

    /**
     * Adds a tag to this note if it is not already present.
     * Duplicates are silently ignored
     *
     * @param tag the tag string to add.
     */
    public void addTag(String tag) {
        if (!tags.contains(tag)) {
            tags.add(tag);
        }
    }
}
