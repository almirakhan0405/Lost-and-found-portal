package com.lostfound.ui;

import com.lostfound.model.*;
import com.lostfound.service.ClaimService;
import com.lostfound.service.ItemService;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Search and filter every report, view details, make a claim, or see smart matches. */
class BrowsePanel extends JPanel implements Refreshable {

    private final ItemService items;
    private final ClaimService claims;
    private final User user;

    private final JTextField search = new JTextField(22);
    private final JComboBox<Object> type = Ui.combo("All types", ItemType.values());
    private final JComboBox<Object> category = Ui.combo("All categories", Category.values());
    private final JComboBox<Object> status = Ui.combo("Any status", ItemStatus.values());
    private final DefaultTableModel model =
            Ui.model("ID", "Type", "Title", "Category", "Location", "Date", "Status", "Reported by");
    private final JTable table = Ui.table(model);
    private final JLabel count = new JLabel(" ");

    BrowsePanel(ConfigurableApplicationContext ctx, User user) {
        this.items = ctx.getBean(ItemService.class);
        this.claims = ctx.getBean(ClaimService.class);
        this.user = user;
        setLayout(new BorderLayout());

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        filters.add(new JLabel("Search:"));
        filters.add(search);
        filters.add(type);
        filters.add(category);
        filters.add(status);
        filters.add(Ui.button("Clear", () -> {
            search.setText("");
            type.setSelectedIndex(0);
            category.setSelectedIndex(0);
            status.setSelectedIndex(0);
        }));
        filters.add(count);
        add(filters, BorderLayout.NORTH);

        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(1).setMaxWidth(70);
        add(new JScrollPane(table), BorderLayout.CENTER);

        add(Ui.buttonBar(
                Ui.button("View details", this::details),
                Ui.button("Claim / I found this", this::claim),
                Ui.button("Smart matches", this::matches)), BorderLayout.SOUTH);

        // live filtering: reload whenever a filter changes
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refresh(); }
            public void removeUpdate(DocumentEvent e) { refresh(); }
            public void changedUpdate(DocumentEvent e) { refresh(); }
        });
        type.addActionListener(e -> refresh());
        category.addActionListener(e -> refresh());
        status.addActionListener(e -> refresh());
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) details();
            }
        });
    }

    @Override
    public void refresh() {
        model.setRowCount(0);
        var found = items.search(search.getText(),
                Ui.selected(type, ItemType.class),
                Ui.selected(category, Category.class),
                Ui.selected(status, ItemStatus.class));
        for (Item i : found) {
            model.addRow(new Object[]{i.getId(), i.getType().name(), i.getTitle(), i.getCategory().getLabel(),
                    i.getLocation(), String.valueOf(i.getEventDate()), i.getStatus().name(), i.getReporter().getFullName()});
        }
        count.setText(found.size() + " item(s)");
    }

    private void details() {
        Long id = Ui.selectedId(table, this);
        if (id == null) return;
        Item i = items.get(id);
        String text = "Title:        " + i.getTitle()
                + "\nType:         " + i.getType()
                + "\nStatus:       " + i.getStatus()
                + "\nCategory:     " + i.getCategory().getLabel()
                + "\nLocation:     " + i.getLocation()
                + "\nDate:         " + i.getEventDate()
                + "\nReported by:  " + i.getReporter().getFullName()
                + "\nPosted:       " + Ui.when(i.getCreatedAt())
                + "\n\nDescription:\n" + Ui.nz(i.getDescription());
        JTextArea area = new JTextArea(text, 12, 44);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Item #" + i.getId(), JOptionPane.PLAIN_MESSAGE);
    }

    private void claim() {
        Long id = Ui.selectedId(table, this);
        if (id == null) return;
        Item i = items.get(id);
        if (i.getReporter().getId().equals(user.getId())) {
            Ui.error(this, "This is your own report - you cannot claim it.");
            return;
        }
        boolean found = i.getType() == ItemType.FOUND;
        boolean hasQuestion = found && i.getVerificationQuestion() != null && !i.getVerificationQuestion().isBlank();

        JTextField answer = new JTextField(28);
        JTextArea message = new JTextArea(4, 28);
        message.setLineWrap(true);
        message.setWrapStyleWord(true);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(new JLabel(found ? "Claim \"" + i.getTitle() + "\" - explain why it is yours:"
                : "Tell the owner about \"" + i.getTitle() + "\" - where is it now?"));
        p.add(Box.createVerticalStrut(6));
        p.add(new JScrollPane(message));
        if (hasQuestion) {
            p.add(Box.createVerticalStrut(10));
            p.add(new JLabel("Verification question: " + i.getVerificationQuestion()));
            p.add(answer);
        }
        int ok = JOptionPane.showConfirmDialog(this, p, found ? "Claim item" : "I found this item",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        try {
            claims.submit(id, user, message.getText(), answer.getText());
            Ui.info(this, "Your request was sent to " + i.getReporter().getFullName()
                    + ". You will be notified when it is reviewed.");
            refresh();
        } catch (IllegalArgumentException | java.util.NoSuchElementException ex) {
            Ui.error(this, ex.getMessage());
        }
    }

    private void matches() {
        Long id = Ui.selectedId(table, this);
        if (id == null) return;
        MatchesDialog.show(this, items.get(id), items.matchesFor(id));
    }
}
