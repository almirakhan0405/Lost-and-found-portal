package com.lostfound.ui;

import com.lostfound.model.Item;
import com.lostfound.service.MatchingService.Match;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;
import java.util.List;

/** Shows the smart-match results for an item. */
final class MatchesDialog {

    private MatchesDialog() { }

    static void show(Component parent, Item source, List<Match> matches) {
        if (matches.isEmpty()) {
            Ui.info(parent, "No likely matches yet for \"" + source.getTitle()
                    + "\".\nYou will get a notification as soon as a matching report is posted.");
            return;
        }
        DefaultTableModel m = Ui.model("ID", "Match", "Type", "Title", "Location", "Date", "Reported by", "Why it matches");
        for (Match x : matches) {
            Item i = x.item();
            m.addRow(new Object[]{i.getId(), x.score() + "%", i.getType().name(), i.getTitle(), i.getLocation(),
                    String.valueOf(i.getEventDate()), i.getReporter().getFullName(), String.join(", ", x.reasons())});
        }
        JTable t = Ui.table(m);
        t.getColumnModel().getColumn(0).setMaxWidth(50);
        t.getColumnModel().getColumn(1).setMaxWidth(70);
        JOptionPane.showMessageDialog(parent, Ui.scroll(t, 860, 220),
                "Likely matches for \"" + source.getTitle() + "\"", JOptionPane.PLAIN_MESSAGE);
    }
}
