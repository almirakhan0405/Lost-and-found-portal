package com.lostfound.ui;

import com.lostfound.model.*;
import com.lostfound.service.ClaimService;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Top: claims other people made on my items (approve / reject). Bottom: claims I made. */
class ClaimsPanel extends JPanel implements Refreshable {

    private final ClaimService claims;
    private final User user;

    private final DefaultTableModel incoming = Ui.model("ID", "Item", "Claimant", "Email", "Message",
            "Their answer", "Your answer", "Status", "Date");
    private final JTable incomingTable = Ui.table(incoming);
    private final DefaultTableModel mine = Ui.model("ID", "Item", "Item type", "Reporter", "My message", "Status", "Date");
    private final JTable mineTable = Ui.table(mine);

    ClaimsPanel(ConfigurableApplicationContext ctx, User user) {
        this.claims = ctx.getBean(ClaimService.class);
        this.user = user;
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createTitledBorder("Claims on my items"));
        incomingTable.getColumnModel().getColumn(0).setMaxWidth(50);
        top.add(new JScrollPane(incomingTable), BorderLayout.CENTER);
        top.add(Ui.buttonBar(
                Ui.button("Approve", () -> review(true)),
                Ui.button("Reject", () -> review(false))), BorderLayout.SOUTH);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createTitledBorder("My claims"));
        mineTable.getColumnModel().getColumn(0).setMaxWidth(50);
        bottom.add(new JScrollPane(mineTable), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
        split.setResizeWeight(0.55);
        add(split, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        incoming.setRowCount(0);
        for (Claim c : claims.onMyItems(user)) {
            incoming.addRow(new Object[]{c.getId(), c.getItem().getTitle(), c.getClaimant().getFullName(),
                    c.getClaimant().getEmail(), c.getMessage(), Ui.nz(c.getAnswerGiven()),
                    Ui.nz(c.getItem().getVerificationAnswer()), c.getStatus().name(), Ui.when(c.getCreatedAt())});
        }
        mine.setRowCount(0);
        for (Claim c : claims.byClaimant(user)) {
            mine.addRow(new Object[]{c.getId(), c.getItem().getTitle(), c.getItem().getType().name(),
                    c.getItem().getReporter().getFullName(), c.getMessage(), c.getStatus().name(), Ui.when(c.getCreatedAt())});
        }
    }

    private void review(boolean approve) {
        Long id = Ui.selectedId(incomingTable, this);
        if (id == null) return;
        try {
            if (approve) claims.approve(id, user); else claims.reject(id, user);
            refresh();
        } catch (IllegalArgumentException | java.util.NoSuchElementException ex) {
            Ui.error(this, ex.getMessage());
        }
    }
}
