package com.lostfound;

import com.lostfound.model.*;
import com.lostfound.repo.ItemRepository;
import com.lostfound.service.MatchingService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MatchingServiceTest {

    private final ItemRepository repo = mock(ItemRepository.class);
    private final MatchingService service = new MatchingService(repo);

    private static User user(long id, String name) {
        User u = new User();
        u.setUsername(name);
        u.setFullName(name);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private static Item item(User reporter, ItemType type, String title, Category cat, String location, int daysAgo, String desc) {
        Item i = new Item();
        i.setReporter(reporter);
        i.setType(type);
        i.setTitle(title);
        i.setCategory(cat);
        i.setLocation(location);
        i.setEventDate(LocalDate.now().minusDays(daysAgo));
        i.setDescription(desc);
        return i;
    }

    @Test
    void sameObjectDescribedDifferentlyIsAStrongMatch() {
        User riya = user(1, "riya"), arjun = user(2, "arjun");
        Item lost = item(riya, ItemType.LOST, "Black Dell laptop bag", Category.BAGS_WALLETS,
                "Library 2nd floor", 3, "Black laptop bag with a Dell laptop and a blue charger inside.");
        Item found = item(arjun, ItemType.FOUND, "Dell laptop bag (black)", Category.BAGS_WALLETS,
                "Central library reading hall", 2, "Found a black bag with a laptop and charger.");
        when(repo.findByTypeAndStatus(ItemType.FOUND, ItemStatus.OPEN)).thenReturn(List.of(found));

        List<MatchingService.Match> matches = service.findMatches(lost);

        assertEquals(1, matches.size());
        assertTrue(matches.get(0).score() >= 80, "expected a strong match but got " + matches.get(0).score());
    }

    @Test
    void unrelatedItemsAreNotReportedAsMatches() {
        User riya = user(1, "riya"), arjun = user(2, "arjun");
        Item lost = item(riya, ItemType.LOST, "Brown leather wallet", Category.BAGS_WALLETS, "Cafeteria", 2, "Brown wallet with cards");
        Item found = item(arjun, ItemType.FOUND, "iPhone with cracked screen", Category.ELECTRONICS, "Gym", 2, "Black phone");
        when(repo.findByTypeAndStatus(ItemType.FOUND, ItemStatus.OPEN)).thenReturn(List.of(found));

        assertTrue(service.findMatches(lost).isEmpty());
    }

    @Test
    void ownReportsAreNeverMatchedAgainstEachOther() {
        User riya = user(1, "riya");
        Item lost = item(riya, ItemType.LOST, "Blue umbrella", Category.OTHER, "Bus stop", 1, "Blue umbrella");
        Item found = item(riya, ItemType.FOUND, "Blue umbrella", Category.OTHER, "Bus stop", 1, "Blue umbrella");
        when(repo.findByTypeAndStatus(ItemType.FOUND, ItemStatus.OPEN)).thenReturn(List.of(found));

        assertTrue(service.findMatches(lost).isEmpty());
    }
}
