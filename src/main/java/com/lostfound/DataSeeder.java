package com.lostfound;

import com.lostfound.model.*;
import com.lostfound.repo.UserRepository;
import com.lostfound.service.AuthService;
import com.lostfound.service.ItemService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** Creates demo accounts and sample reports the first time the app starts. */
@Component
@ConditionalOnProperty(name = "app.seed", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private final UserRepository users;
    private final AuthService auth;
    private final ItemService items;

    public DataSeeder(UserRepository users, AuthService auth, ItemService items) {
        this.users = users;
        this.auth = auth;
        this.items = items;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) return;

        User admin = auth.register("admin", "Portal Admin", "admin@campus.edu", "admin123", Role.ADMIN);
        User riya = auth.register("student1", "Riya Sharma", "riya@campus.edu", "pass123");
        User arjun = auth.register("student2", "Arjun Mehta", "arjun@campus.edu", "pass123");

        LocalDate today = LocalDate.now();

        add(riya, ItemType.LOST, "Black Dell laptop bag", Category.BAGS_WALLETS, "Library 2nd floor",
                today.minusDays(3), "Black laptop bag with a Dell 15 inch laptop and a blue charger inside.", null, null);
        add(arjun, ItemType.FOUND, "Dell laptop bag (black)", Category.BAGS_WALLETS, "Central library reading hall",
                today.minusDays(2), "Found a black bag with a laptop and charger near the reading hall.",
                "What colour is the charger?", "blue");
        add(arjun, ItemType.LOST, "Silver wrist watch", Category.JEWELLERY, "Canteen",
                today.minusDays(5), "Silver analog watch with a brown leather strap.", null, null);
        add(riya, ItemType.FOUND, "Set of keys with red keychain", Category.KEYS, "Parking lot B",
                today.minusDays(1), "Three keys on a red keychain shaped like a heart.",
                "How many keys are on the ring?", "3");
        add(riya, ItemType.LOST, "Student ID card", Category.DOCUMENTS_IDS, "Computer lab",
                today.minusDays(4), "ID card of Arjun Mehta, Computer Science.", null, null);
        add(admin, ItemType.FOUND, "Blue water bottle", Category.OTHER, "Sports ground",
                today.minusDays(6), "Steel water bottle, dark blue, small dent near the bottom.", null, null);
    }

    private void add(User reporter, ItemType type, String title, Category cat, String loc, LocalDate date,
                     String desc, String question, String answer) {
        Item i = new Item();
        i.setReporter(reporter);
        i.setType(type);
        i.setTitle(title);
        i.setCategory(cat);
        i.setLocation(loc);
        i.setEventDate(date);
        i.setDescription(desc);
        i.setVerificationQuestion(question);
        i.setVerificationAnswer(answer);
        items.report(i);
    }
}
