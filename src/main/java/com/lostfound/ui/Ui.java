package com.lostfound.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Small helpers shared by all Swing panels. */
final class Ui {

    static final Color BRAND = new Color(0x2F5BEA);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private Ui() { }

    /** Read-only table model; column 0 is always the numeric id. */
    static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
            @Override public Class<?> getColumnClass(int col) { return col == 0 ? Long.class : String.class; }
        };
    }

    static JTable table(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setRowHeight(26);
        t.setAutoCreateRowSorter(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.getTableHeader().setReorderingAllowed(false);
        t.setFillsViewportHeight(true);
        return t;
    }

    /** Id in column 0 of the selected row, or null (after telling the user) if nothing is selected. */
    static Long selectedId(JTable t, Component parent) {
        int row = t.getSelectedRow();
        if (row < 0) {
            info(parent, "Please select a row first.");
            return null;
        }
        return (Long) t.getModel().getValueAt(t.convertRowIndexToModel(row), 0);
    }

    static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Problem", JOptionPane.ERROR_MESSAGE);
    }

    static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Smart Lost & Found", JOptionPane.INFORMATION_MESSAGE);
    }

    static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Please confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    static JButton button(String text, Runnable action) {
        JButton b = new JButton(text);
        b.addActionListener(e -> action.run());
        return b;
    }

    static JPanel buttonBar(JComponent... parts) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        for (JComponent c : parts) p.add(c);
        return p;
    }

    /** Combo box with an "All" entry first, followed by the given values. */
    static JComboBox<Object> combo(String allLabel, Object[] values) {
        JComboBox<Object> c = new JComboBox<>();
        c.addItem(allLabel);
        for (Object v : values) c.addItem(v);
        return c;
    }

    /** The selected enum value, or null when "All" is selected. */
    static <T> T selected(JComboBox<Object> combo, Class<T> type) {
        Object o = combo.getSelectedItem();
        return type.isInstance(o) ? type.cast(o) : null;
    }

    static String when(LocalDateTime t) {
        return t == null ? "" : t.format(DATE_TIME);
    }

    static String nz(String s) {
        return s == null ? "" : s;
    }

    static JScrollPane scroll(Component c, int w, int h) {
        JScrollPane sp = new JScrollPane(c);
        sp.setPreferredSize(new Dimension(w, h));
        return sp;
    }
}
