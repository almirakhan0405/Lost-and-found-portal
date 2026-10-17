package com.lostfound.ui;

import com.lostfound.model.*;
import com.lostfound.service.ItemService;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** The signed-in user's own reports. */
class MyItemsPanel extends JPanel implements Refreshable {

    private final ItemService items;
    private final User user;
    private final DefaultTableModel model = Ui.model("ID", "Type", "Title", "Category", "Location", "Date", "Status");
    private final JTable table = Ui.table(model);

    MyItemsPanel(ConfigurableApplicationContext ctx, User user) {
        this.items = ctx.getBean(ItemService.class);
        this.user = user;
        setLayout(new BorderLayout());
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(Ui.buttonBar(
                Ui.button("Find smart matches", this::matches),
                Ui.button("Mark as returned", () -> setStatus(ItemStatus.RETURNED)),
                Ui.button("Re-open", () -> setStatus(ItemStatus.OPEN)),
                Ui.button("Delete", this::delete)), BorderLayout.SOUTH);
    }

    @Override
    public void refresh() {
        model.setRowCount(0);
        for (Item i : items.forReporter(user)) {
            model.addRow(new Object[]{i.getId(), i.getType().name(), i.getTitle(), i.getCategory().getLabel(),
                    i.getLocation(), String.valueOf(i.getEventDate()), i.getStatus().name()});
        }
    }

    private void matches() {
        Long id = Ui.selectedId(table, this);
        if (id != null) MatchesDialog.show(this, items.get(id), items.matchesFor(id));
    }

    private void setStatus(ItemStatus status) {
        Long id = Ui.selectedId(table, this);
        if (id == null) return;
        try {
            items.setStatus(id, status, user);
            refresh();
        } catch (IllegalArgumentException ex) {
            Ui.error(this, ex.getMessage());
        }
    }

    private void delete() {
        Long id = Ui.selectedId(table, this);
        if (id == null || !Ui.confirm(this, "Delete this report and all claims on it?")) return;
        try {
            items.delete(id, user);
            refresh();
        } catch (IllegalArgumentException ex) {
            Ui.error(this, ex.getMessage());
        }
    }
}
