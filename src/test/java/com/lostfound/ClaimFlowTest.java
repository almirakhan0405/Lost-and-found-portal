package com.lostfound;

import com.lostfound.model.*;
import com.lostfound.service.AuthService;
import com.lostfound.service.ClaimService;
import com.lostfound.service.ItemService;
import com.lostfound.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** End-to-end check of the main workflow against an in-memory H2 database. */
@SpringBootTest
class ClaimFlowTest {

    @Autowired AuthService auth;
    @Autowired ItemService items;
    @Autowired ClaimService claims;
    @Autowired NotificationService notifications;

    private User newUser(String prefix) {
        String name = prefix + System.nanoTime();
        return auth.register(name, "Test " + prefix, name + "@test.edu", "secret123");
    }

    private Item newFound(User reporter, String title) {
        Item i = new Item();
        i.setReporter(reporter);
        i.setType(ItemType.FOUND);
        i.setTitle(title);
        i.setCategory(Category.KEYS);
        i.setLocation("Main gate");
        i.setEventDate(LocalDate.now());
        i.setDescription("Bunch of keys");
        i.setVerificationQuestion("How many keys?");
        i.setVerificationAnswer("3");
        items.report(i);
        return items.forReporter(reporter).get(0);
    }

    @Test
    void loginChecksThePassword() {
        User u = newUser("login");
        assertEquals(u.getId(), auth.login(u.getUsername(), "secret123").getId());
        assertThrows(IllegalArgumentException.class, () -> auth.login(u.getUsername(), "wrong-password"));
    }

    @Test
    void approvedClaimMarksItemReturned() {
        User finder = newUser("finder");
        User owner = newUser("owner");
        Item item = newFound(finder, "Keys with red keychain");

        claims.submit(item.getId(), owner, "These are my keys", "3");
        assertEquals(ItemStatus.CLAIMED, items.get(item.getId()).getStatus());

        Claim pending = claims.onMyItems(finder).get(0);
        claims.approve(pending.getId(), finder);

        assertEquals(ItemStatus.RETURNED, items.get(item.getId()).getStatus());
        assertEquals(ClaimStatus.APPROVED, claims.byClaimant(owner).get(0).getStatus());
        assertTrue(notifications.unreadCount(owner) > 0);
    }

    @Test
    void rejectedClaimReopensTheItem() {
        User finder = newUser("finder");
        User other = newUser("other");
        Item item = newFound(finder, "Silver keys");

        claims.submit(item.getId(), other, "Mine", "7");
        claims.reject(claims.onMyItems(finder).get(0).getId(), finder);

        assertEquals(ItemStatus.OPEN, items.get(item.getId()).getStatus());
    }

    @Test
    void reporterCannotClaimOwnItemAndStrangersCannotReview() {
        User finder = newUser("finder");
        User stranger = newUser("stranger");
        User claimant = newUser("claimant");
        Item item = newFound(finder, "Door keys");

        assertThrows(IllegalArgumentException.class, () -> claims.submit(item.getId(), finder, "mine", null));

        claims.submit(item.getId(), claimant, "mine", "3");
        Long claimId = claims.onMyItems(finder).get(0).getId();
        assertThrows(IllegalArgumentException.class, () -> claims.approve(claimId, stranger));
    }
}
