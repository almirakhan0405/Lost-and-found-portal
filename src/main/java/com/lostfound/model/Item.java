package com.lostfound.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    private Category category = Category.OTHER;

    private String location;

    /** Date the item was lost or found. */
    private LocalDate eventDate;

    @Enumerated(EnumType.STRING)
    private ItemType type;

    @Enumerated(EnumType.STRING)
    private ItemStatus status = ItemStatus.OPEN;

    /** Optional: finder asks a question only the real owner can answer ("What is the phone wallpaper?"). */
    private String verificationQuestion;
    /** Never exposed through the REST API. */
    private String verificationAnswer;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
    public ItemType getType() { return type; }
    public void setType(ItemType type) { this.type = type; }
    public ItemStatus getStatus() { return status; }
    public void setStatus(ItemStatus status) { this.status = status; }
    public String getVerificationQuestion() { return verificationQuestion; }
    public void setVerificationQuestion(String q) { this.verificationQuestion = q; }
    public String getVerificationAnswer() { return verificationAnswer; }
    public void setVerificationAnswer(String a) { this.verificationAnswer = a; }
    public User getReporter() { return reporter; }
    public void setReporter(User reporter) { this.reporter = reporter; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return "#" + id + " " + type + " " + title; }
}
