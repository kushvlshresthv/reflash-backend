package com.project.reflash.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "notes")
@AllArgsConstructor
@Builder
public class Note {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(mappedBy="note")
    private FlashCard flashCard;

    @Column(name="front")
    private String front;

    @Column(name="back")
    private String back;

    @Column(name="additional_context")
    private String additionalContext;

    @ElementCollection
    @CollectionTable(
        name = "note_tags",
        joinColumns = @JoinColumn(name = "note_id")
    )
    @Column(name = "tag")
    private List<String> tags;

    public Note() {
        this.tags = new ArrayList<>();
    }

    public void addTag(String tag) {
        if (!tags.contains(tag)) {
            tags.add(tag);
        }
    }
}
