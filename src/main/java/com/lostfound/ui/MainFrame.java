package com.lostfound.ui;

import com.lostfound.model.User;
import com.lostfound.service.NotificationService;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private final JTabbedPane tabs = new JTabbedPane();
    private final NotificationService notifications;
    private final User user;
    private int notificationsTab = -1;

    public MainFrame(ConfigurableApplicationContext ctx, User user) {
        super("Smart Lost & Found Portal");
        this.user = user;
        this.notifications = ctx.getBean(NotificationService.class);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1120, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // top banner with the signed-in user and a logout button
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(Ui.BRAND);
        banner.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        JLabel title = new JLabel("Smart Lost & Found Portal");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        JLabel who = new JLabel(user.getFullName() + (user.isAdmin() ? "  [Admin]" : "") + "   ");
        who.setForeground(Color.WHITE);
        JButton logout = Ui.button("Log out", () -> {
            dispose();
            new LoginFrame(ctx).setVisible(true);
        });
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        right.add(who);
        right.add(logout);
        banner.add(title, BorderLayout.WEST);
        banner.add(right, BorderLayout.EAST);
        add(banner, BorderLayout.NORTH);

        tabs.addTab("Browse & Search", new BrowsePanel(ctx, user));
        tabs.addTab("Report an Item", new ReportPanel(ctx, user, () -> tabs.setSelectedIndex(2)));
        tabs.addTab("My Items", new MyItemsPanel(ctx, user));
        tabs.addTab("Claims", new ClaimsPanel(ctx, user));
        tabs.addTab("Notifications", new NotificationsPanel(ctx, user, this::updateBadge));
        notificationsTab = tabs.getTabCount() - 1;
        if (user.isAdmin()) tabs.addTab("Admin", new AdminPanel(ctx, user));
        add(tabs, BorderLayout.CENTER);

        tabs.addChangeListener(e -> {
            Component c = tabs.getSelectedComponent();
            if (c instanceof Refreshable r) r.refresh();
            updateBadge();
        });
        ((Refreshable) tabs.getComponentAt(0)).refresh();
        updateBadge();
    }

    private void updateBadge() {
        long n = notifications.unreadCount(user);
        tabs.setTitleAt(notificationsTab, n > 0 ? "Notifications (" + n + ")" : "Notifications");
    }
}
