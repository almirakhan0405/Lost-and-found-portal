package com.lostfound.service;

import com.lostfound.model.*;
import com.lostfound.repo.ClaimRepository;
import com.lostfound.repo.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class ItemService {

    private final ItemRepository items;
    private final ClaimRepository claims;
    private final MatchingService matching;
    private final NotificationService notifications;

    public ItemService(ItemRepository items, ClaimRepository claims,
                       MatchingService matching, NotificationService notifications) {
        this.items = items;
        this.claims = claims;
        this.matching = matching;
        this.notifications = notifications;
    }

    /** Saves a new report and immediately looks for matches, notifying both owners. Returns the matches. */
    public List<MatchingService.Match> report(Item item) {
        if (item.getTitle() == null || item.getTitle().isBlank()) throw new IllegalArgumentException("Title is required.");
        if (item.getType() == null) throw new IllegalArgumentException("Choose Lost or Found.");
        if (item.getLocation() == null || item.getLocation().isBlank()) throw new IllegalArgumentException("Location is required.");
        if (item.getEventDate() == null) item.setEventDate(LocalDate.now());
        if (item.getEventDate().isAfter(LocalDate.now())) throw new IllegalArgumentException("Date cannot be in the future.");

        item.setTitle(item.getTitle().trim());
        item.setStatus(ItemStatus.OPEN);
        item.setCreatedAt(LocalDateTime.now());
        if (item.getType() == ItemType.LOST) {            // question/answer only make sense for found items
            item.setVerificationQuestion(null);
            item.setVerificationAnswer(null);
        }
        Item saved = items.save(item);

        List<MatchingService.Match> found = matching.findMatches(saved);
        for (MatchingService.Match m : found) {
            notifications.notify(saved.getReporter(), "Possible match (" + m.score() + "%): \""
                    + m.item().getTitle() + "\" [" + m.item().getType() + "] - open Browse to view it.");
            notifications.notify(m.item().getReporter(), "A new " + saved.getType() + " report \""
                    + saved.getTitle() + "\" may match your item \"" + m.item().getTitle() + "\" (" + m.score() + "%).");
        }
        return found;
    }

    @Transactional(readOnly = true)
    public Item get(Long id) {
        return items.findById(id).orElseThrow(() -> new NoSuchElementException("Item " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<MatchingService.Match> matchesFor(Long itemId) {
        return matching.findMatches(get(itemId));
    }

    @Transactional(readOnly = true)
    public List<Item> forReporter(User user) {
        return items.findByReporterOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<Item> all() {
        List<Item> list = new ArrayList<>(items.findAll());
        list.sort(Comparator.comparing(Item::getCreatedAt).reversed());
        return list;
    }

    /**
     * Filtered, typo-tolerant search. Every word typed must (fuzzily) appear in the item's
     * title, description, location or category; results are ranked by how well they match.
     */
    @Transactional(readOnly = true)
    public List<Item> search(String query, ItemType type, Category category, ItemStatus status) {
        List<String> words = TextUtil.tokens(query);
        record Scored(Item item, int score) { }
        List<Scored> hits = new ArrayList<>();

        for (Item it : items.findAll()) {
            if (type != null && it.getType() != type) continue;
            if (category != null && it.getCategory() != category) continue;
            if (status != null && it.getStatus() != status) continue;

            int score = 0;
            if (!words.isEmpty()) {
                List<String> hay = TextUtil.tokens(it.getTitle() + " " + it.getDescription() + " "
                        + it.getLocation() + " " + it.getCategory().getLabel());
                for (String w : words) {
                    int best = 0;
                    for (String h : hay) {
                        if (h.equals(w)) best = Math.max(best, 3);
                        else if (h.startsWith(w)) best = Math.max(best, 2);
                        else if (TextUtil.fuzzyEquals(w, h)) best = Math.max(best, 1);
                    }
                    if (best == 0) { score = -1; break; }    // every word must match something
                    score += best;
                }
                if (score < 0) continue;
            }
            hits.add(new Scored(it, score));
        }
        hits.sort(Comparator.comparingInt(Scored::score).reversed()
                .thenComparing(s -> s.item().getCreatedAt(), Comparator.reverseOrder()));
        return hits.stream().map(Scored::item).toList();
    }

    public void delete(Long itemId, User actor) {
        Item it = get(itemId);
        if (!actor.isAdmin() && !Objects.equals(it.getReporter().getId(), actor.getId()))
            throw new IllegalArgumentException("You can only delete your own items.");
        claims.deleteByItem(it);
        items.delete(it);
    }

    /** Owner (or admin) can mark an item as resolved / reopen it. */
    public void setStatus(Long itemId, ItemStatus status, User actor) {
        Item it = get(itemId);
        if (!actor.isAdmin() && !Objects.equals(it.getReporter().getId(), actor.getId()))
            throw new IllegalArgumentException("You can only change your own items.");
        it.setStatus(status);
        items.save(it);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> stats() {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("total", items.count());
        m.put("lost", items.countByType(ItemType.LOST));
        m.put("found", items.countByType(ItemType.FOUND));
        m.put("open", items.countByStatus(ItemStatus.OPEN));
        m.put("claimed", items.countByStatus(ItemStatus.CLAIMED));
        m.put("returned", items.countByStatus(ItemStatus.RETURNED));
        return m;
    }
}
