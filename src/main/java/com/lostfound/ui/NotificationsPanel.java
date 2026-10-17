package com.lostfound.ui;

import com.lostfound.model.Notification;
import com.lostfound.model.User;
import com.lostfound.service.NotificationService;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

class NotificationsPanel extends JPanel implements Refreshable {

    private final NotificationService service;
    private final User user;
    private final Runnable onChange;
    private final DefaultTableModel model = Ui.model("ID", "", "When", "Message");
    private final JTable table = Ui.table(model);

    NotificationsPanel(ConfigurableApplicationContext ctx, User user, Runnable onChange) {
        this.service = ctx.getBean(NotificationService.class);
        this.user = user;
        this.onChange = onChange;
        setLayout(new BorderLayout());
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(1).setMaxWidth(60);
        table.getColumnModel().getColumn(2).setMaxWidth(170);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(Ui.buttonBar(Ui.button("Mark all as read", () -> {
            service.markAllRead(user);
            refresh();
            onChange.run();
        })), BorderLayout.SOUTH);
    }

    @Override
    public void refresh() {
        model.setRowCount(0);
        for (Notification n : service.forUser(user)) {
            model.addRow(new Object[]{n.getId(), n.isSeen() ? "" : "NEW", Ui.when(n.getCreatedAt()), n.getMessage()});
        }
    }
}
