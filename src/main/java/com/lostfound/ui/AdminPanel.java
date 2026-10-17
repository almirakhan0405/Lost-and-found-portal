package com.lostfound.ui;

import com.lostfound.model.*;
import com.lostfound.service.AuthService;
import com.lostfound.service.ClaimService;
import com.lostfound.service.ItemService;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

/** Admin dashboard: statistics, all users, all items (delete), all claims (approve / reject). */
class AdminPanel extends JPanel implements Refreshable {

    private final ItemService items;
    private final ClaimService claims;
    private final AuthService auth;
    private final User admin;

    private final JLabel stats = new JLabel(" ");
    private final DefaultTableModel users = Ui.model("ID", "Username", "Full name", "Email", "Role");
    private final DefaultTableModel allItems = Ui.model("ID", "Type", "Title", "Location", "Status", "Reported by");
    private final JTable itemsTable = Ui.table(allItems);
    private final DefaultTableModel allClaims = Ui.model("ID", "Item", "Claimant", "Message", "Status", "Date");
    private final JTable claimsTable = Ui.table(allClaims);

    AdminPanel(ConfigurableApplicationContext ctx, User admin) {
        this.items = ctx.getBean(ItemService.class);
        this.claims = ctx.getBean(ClaimService.class);
        this.auth = ctx.getBean(AuthService.class);
        this.admin = admin;
        setLayout(new BorderLayout());

        stats.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        stats.setFont(stats.getFont().deriveFont(Font.BOLD, 14f));
        add(stats, BorderLayout.NORTH);

        JTabbedPane inner = new JTabbedPane();

        inner.addTab("Users", new JScrollPane(Ui.table(users)));

        JPanel itemsPanel = new JPanel(new BorderLayout());
        itemsPanel.add(new JScrollPane(itemsTable), BorderLayout.CENTER);
        itemsPanel.add(Ui.buttonBar(Ui.button("Delete selected item", this::deleteItem)), BorderLayout.SOUTH);
        inner.addTab("All items", itemsPanel);

        JPanel claimsPanel = new JPanel(new BorderLayout());
        claimsPanel.add(new JScrollPane(claimsTable), BorderLayout.CENTER);
        claimsPanel.add(Ui.buttonBar(
                Ui.button("Approve", () -> review(true)),
                Ui.button("Reject", () -> review(false))), BorderLayout.SOUTH);
        inner.addTab("All claims", claimsPanel);

        add(inner, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Map<String, Long> s = items.stats();
        stats.setText("Total reports: " + s.get("total") + "   |   Lost: " + s.get("lost") + "   Found: " + s.get("found")
                + "   |   Open: " + s.get("open") + "   Claimed: " + s.get("claimed") + "   Returned: " + s.get("returned"));

        users.setRowCount(0);
        for (User u : auth.allUsers())
            users.addRow(new Object[]{u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getRole().name()});

        allItems.setRowCount(0);
        for (Item i : items.all())
            allItems.addRow(new Object[]{i.getId(), i.getType().name(), i.getTitle(), i.getLocation(),
                    i.getStatus().name(), i.getReporter().getFullName()});

        allClaims.setRowCount(0);
        for (Claim c : claims.all())
            allClaims.addRow(new Object[]{c.getId(), c.getItem().getTitle(), c.getClaimant().getFullName(),
                    c.getMessage(), c.getStatus().name(), Ui.when(c.getCreatedAt())});
    }

    private void deleteItem() {
        Long id = Ui.selectedId(itemsTable, this);
        if (id == null || !Ui.confirm(this, "Delete this item and its claims?")) return;
        try {
            items.delete(id, admin);
            refresh();
        } catch (IllegalArgumentException ex) {
            Ui.error(this, ex.getMessage());
        }
    }

    private void review(boolean approve) {
        Long id = Ui.selectedId(claimsTable, this);
        if (id == null) return;
        try {
            if (approve) claims.approve(id, admin); else claims.reject(id, admin);
            refresh();
        } catch (IllegalArgumentException | java.util.NoSuchElementException ex) {
            Ui.error(this, ex.getMessage());
        }
    }
}
