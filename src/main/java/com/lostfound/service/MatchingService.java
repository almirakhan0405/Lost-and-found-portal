package com.lostfound.service;

import com.lostfound.model.*;
import com.lostfound.repo.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * The "smart" part: scores how likely a LOST report and a FOUND report describe the same object.
 *
 *   category            25 points  (same category)
 *   title similarity    35 points  (fuzzy word match, tolerates typos)
 *   description         15 points  (shared words in title + description)
 *   location            10 points  (shared words, e.g. "library")
 *   date proximity      15 points  (full marks within 2 days, fades to 0 at 21 days)
 *   -----------------------------
 *   total              100         (shown as a percentage; >= THRESHOLD counts as a match)
 */
@Service
@Transactional(readOnly = true)
public class MatchingService {

    public static final int THRESHOLD = 45;

    public record Match(Item item, int score, List<String> reasons) { }

    private final ItemRepository items;

    public MatchingService(ItemRepository items) {
        this.items = items;
    }

    /** All open items of the opposite type that are likely to be the same object, best first. */
    public List<Match> findMatches(Item source) {
        ItemType opposite = source.getType() == ItemType.LOST ? ItemType.FOUND : ItemType.LOST;
        List<Match> result = new ArrayList<>();
        for (Item candidate : items.findByTypeAndStatus(opposite, ItemStatus.OPEN)) {
            if (Objects.equals(candidate.getReporter().getId(), source.getReporter().getId())) continue;
            Match m = score(source, candidate);
            if (m.score() >= THRESHOLD) result.add(m);
        }
        result.sort(Comparator.comparingInt(Match::score).reversed());
        return result;
    }

    public Match score(Item a, Item b) {
        List<String> reasons = new ArrayList<>();
        int total = 0;

        if (a.getCategory() == b.getCategory()) {
            total += 25;
            reasons.add("same category");
        }

        int title = (int) Math.round(35 * TextUtil.dice(a.getTitle(), b.getTitle()));
        total += title;
        if (title >= 18) reasons.add("similar title");

        String textA = nz(a.getTitle()) + " " + nz(a.getDescription());
        String textB = nz(b.getTitle()) + " " + nz(b.getDescription());
        int desc = (int) Math.round(15 * TextUtil.overlap(textA, textB));
        total += desc;
        if (desc >= 8) reasons.add("matching description");

        int loc = (int) Math.round(10 * TextUtil.overlap(a.getLocation(), b.getLocation()));
        total += loc;
        if (loc >= 5) reasons.add("nearby location");

        if (a.getEventDate() != null && b.getEventDate() != null) {
            long days = Math.abs(ChronoUnit.DAYS.between(a.getEventDate(), b.getEventDate()));
            int date;
            if (days <= 2) date = 15;
            else if (days >= 21) date = 0;
            else date = (int) Math.round(15.0 * (21 - days) / 19.0);
            total += date;
            if (date >= 8) reasons.add("close dates");
        }

        return new Match(b, Math.min(100, total), reasons);
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
