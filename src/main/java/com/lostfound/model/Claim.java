package com.lostfound.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A claim is a request on an item:
 *  - on a FOUND item: "this is mine" (claimant may answer the verification question)
 *  - on a LOST item: "I found it" (claimant leaves a message)
 * The item's reporter (or an admin) approves or rejects it.
 */
@Entity
@Table(name = "claims")
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "item_id")
    private Item item;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "claimant_id")
    private User claimant;

    @Column(length = 500)
    private String message;

    private String answerGiven;

    @Enumerated(EnumType.STRING)
    private ClaimStatus status = ClaimStatus.PENDING;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
    public User getClaimant() { return claimant; }
    public void setClaimant(User claimant) { this.claimant = claimant; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getAnswerGiven() { return answerGiven; }
    public void setAnswerGiven(String answerGiven) { this.answerGiven = answerGiven; }
    public ClaimStatus getStatus() { return status; }
    public void setStatus(ClaimStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
