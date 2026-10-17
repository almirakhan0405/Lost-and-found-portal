package com.lostfound.ui;

import com.lostfound.model.*;
import com.lostfound.service.ItemService;
import com.lostfound.service.MatchingService.Match;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import java.awt.*;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/** Form for reporting a lost or found item. */
class ReportPanel extends JPanel {

    private final ItemService items;
    private final User user;
    private final Runnable afterReport;

    private final JComboBox<ItemType> type = new JComboBox<>(ItemType.values());
    private final JTextField title = new JTextField(30);
    private final JComboBox<Category> category = new JComboBox<>(Category.values());
    private final JTextField location = new JTextField(30);
    private final JSpinner date = new JSpinner(new SpinnerDateModel(new Date(), null, new Date(), Calendar.DAY_OF_MONTH));
    private final JTextArea description = new JTextArea(5, 30);
    private final JTextField question = new JTextField(30);
    private final JTextField answer = new JTextField(30);

    ReportPanel(ConfigurableApplicationContext ctx, User user, Runnable afterReport) {
        this.items = ctx.getBean(ItemService.class);
        this.user = user;
        this.afterReport = afterReport;

        date.setEditor(new JSpinner.DateEditor(date, "yyyy-MM-dd"));
        description.setLineWrap(true);
        description.setWrapStyleWord(true);

        setLayout(new BorderLayout());
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 30, 10, 30));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.NORTHWEST;

        int row = 0;
        row = addRow(form, g, row, "I have...", type);
        row = addRow(form, g, row, "Item name *", title);
        row = addRow(form, g, row, "Category", category);
        row = addRow(form, g, row, "Location *", location);
        row = addRow(form, g, row, "Date lost / found", date);
        row = addRow(form, g, row, "Description", new JScrollPane(description));
        row = addRow(form, g, row, "Verification question", question);
        row = addRow(form, g, row, "Correct answer", answer);

        JLabel hint = new JLabel("<html><i>Finders only: ask something only the real owner would know "
                + "(e.g. \"What is the phone wallpaper?\"). The answer is kept private.</i></html>");
        hint.setForeground(Color.GRAY);
        g.gridx = 1; g.gridy = row++;
        form.add(hint, g);

        g.gridx = 1; g.gridy = row;
        form.add(Ui.buttonBar(Ui.button("Submit report", this::submit), Ui.button("Clear", this::clear)), g);

        type.addActionListener(e -> updateFoundFields());
        updateFoundFields();

        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT));
        wrap.add(form);
        add(new JScrollPane(wrap), BorderLayout.CENTER);
    }

    private static int addRow(JPanel p, GridBagConstraints g, int row, String label, JComponent field) {
        g.gridx = 0; g.gridy = row;
        p.add(new JLabel(label), g);
        g.gridx = 1;
        p.add(field, g);
        return row + 1;
    }

    private void updateFoundFields() {
        boolean found = type.getSelectedItem() == ItemType.FOUND;
        question.setEnabled(found);
        answer.setEnabled(found);
    }

    private void submit() {
        Item i = new Item();
        i.setReporter(user);
        i.setType((ItemType) type.getSelectedItem());
        i.setTitle(title.getText());
        i.setCategory((Category) category.getSelectedItem());
        i.setLocation(location.getText());
        i.setEventDate(((Date) date.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        i.setDescription(description.getText().trim());
        i.setVerificationQuestion(question.getText().trim());
        i.setVerificationAnswer(answer.getText().trim());
        try {
            List<Match> matches = items.report(i);
            clear();
            if (matches.isEmpty()) {
                Ui.info(this, "Your report was saved.\nNo matches yet - you will be notified when a likely match appears.");
            } else {
                Ui.info(this, "Your report was saved and " + matches.size() + " likely match(es) were found!");
                MatchesDialog.show(this, i, matches);
            }
            afterReport.run();
        } catch (IllegalArgumentException ex) {
            Ui.error(this, ex.getMessage());
        }
    }

    private void clear() {
        title.setText("");
        location.setText("");
        description.setText("");
        question.setText("");
        answer.setText("");
        category.setSelectedIndex(0);
        date.setValue(new Date());
    }
}
