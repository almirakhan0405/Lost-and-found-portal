package com.lostfound.service;

import com.lostfound.model.*;
import com.lostfound.repo.ClaimRepository;
import com.lostfound.repo.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/** The claim workflow: submit -> reporter/admin approves or rejects -> item becomes RETURNED. */
@Service
@Transactional
public class ClaimService {

    private final ClaimRepository claims;
    private final ItemRepository items;
    private final NotificationService notifications;

    public ClaimService(ClaimRepository claims, ItemRepository items, NotificationService notifications) {
        this.claims = claims;
        this.items = items;
        this.notifications = notifications;
    }

    public Claim submit(Long itemId, User claimant, String message, String answer) {
        Item item = items.findById(itemId).orElseThrow(() -> new NoSuchElementException("Item not found"));
        if (Objects.equals(item.getReporter().getId(), claimant.getId()))
            throw new IllegalArgumentException("You cannot claim your own item.");
        if (item.getStatus() == ItemStatus.RETURNED)
            throw new IllegalArgumentException("This item has already been returned.");
        if (claims.existsByItemAndClaimantAndStatus(item, claimant, ClaimStatus.PENDING))
            throw new IllegalArgumentException("You already have a pending claim on this item.");
        if (message == null || message.isBlank())
            throw new IllegalArgumentException("Please add a short message for the reporter.");

        Claim c = new Claim();
        c.setItem(item);
        c.setClaimant(claimant);
        c.setMessage(message.trim());
        c.setAnswerGiven(answer == null || answer.isBlank() ? null : answer.trim());
        claims.save(c);

        item.setStatus(ItemStatus.CLAIMED);
        items.save(item);

        notifications.notify(item.getReporter(), claimant.getFullName() + " made a claim on your item \""
                + item.getTitle() + "\". Review it in the Claims tab.");
        return c;
    }

    public void approve(Long claimId, User actor) {
        Claim c = loadForReview(claimId, actor);
        c.setStatus(ClaimStatus.APPROVED);
        Item item = c.getItem();
        item.setStatus(ItemStatus.RETURNED);
        items.save(item);
        // all other pending claims on the same item are closed
        for (Claim other : claims.findByItemAndStatus(item, ClaimStatus.PENDING)) {
            if (!other.getId().equals(c.getId())) {
                other.setStatus(ClaimStatus.REJECTED);
                notifications.notify(other.getClaimant(), "Your claim on \"" + item.getTitle() + "\" was closed: the item has been returned.");
            }
        }
        notifications.notify(c.getClaimant(), "Good news! Your claim on \"" + item.getTitle() + "\" was APPROVED. Contact "
                + item.getReporter().getFullName() + " at " + item.getReporter().getEmail() + ".");
    }

    public void reject(Long claimId, User actor) {
        Claim c = loadForReview(claimId, actor);
        c.setStatus(ClaimStatus.REJECTED);
        Item item = c.getItem();
        if (claims.findByItemAndStatus(item, ClaimStatus.PENDING).stream().allMatch(x -> x.getId().equals(c.getId()))) {
            item.setStatus(ItemStatus.OPEN);       // nobody else is waiting, reopen it
            items.save(item);
        }
        notifications.notify(c.getClaimant(), "Your claim on \"" + item.getTitle() + "\" was not approved.");
    }

    private Claim loadForReview(Long claimId, User actor) {
        Claim c = claims.findById(claimId).orElseThrow(() -> new NoSuchElementException("Claim not found"));
        boolean owner = Objects.equals(c.getItem().getReporter().getId(), actor.getId());
        if (!owner && !actor.isAdmin()) throw new IllegalArgumentException("Only the item's reporter or an admin can review this claim.");
        if (c.getStatus() != ClaimStatus.PENDING) throw new IllegalArgumentException("This claim has already been reviewed.");
        return c;
    }

    @Transactional(readOnly = true)
    public List<Claim> onMyItems(User reporter) {
        return claims.findByItem_ReporterOrderByCreatedAtDesc(reporter);
    }

    @Transactional(readOnly = true)
    public List<Claim> all() {
        List<Claim> list = new java.util.ArrayList<>(claims.findAll());
        list.sort(java.util.Comparator.comparing(Claim::getCreatedAt).reversed());
        return list;
    }

    @Transactional(readOnly = true)
    public List<Claim> byClaimant(User claimant) {
        return claims.findByClaimantOrderByCreatedAtDesc(claimant);
    }
}
