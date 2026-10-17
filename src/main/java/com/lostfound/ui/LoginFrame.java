package com.lostfound.ui;

import com.lostfound.model.User;
import com.lostfound.service.AuthService;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    private final ConfigurableApplicationContext ctx;
    private final AuthService auth;
    private final JTextField username = new JTextField(18);
    private final JPasswordField password = new JPasswordField(18);

    public LoginFrame(ConfigurableApplicationContext ctx) {
        super("Smart Lost & Found Portal - Sign in");
        this.ctx = ctx;
        this.auth = ctx.getBean(AuthService.class);
        installLookAndFeel();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel banner = new JPanel(new GridLayout(2, 1));
        banner.setBackground(Ui.BRAND);
        banner.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        JLabel title = new JLabel("Smart Lost & Found Portal");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        JLabel sub = new JLabel("Report it. Match it. Return it.");
        sub.setForeground(new Color(0xDDE5FF));
        banner.add(title);
        banner.add(sub);
        add(banner, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 8, 24));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = 0; g.gridy = 0; form.add(new JLabel("Username"), g);
        g.gridx = 1; form.add(username, g);
        g.gridx = 0; g.gridy = 1; form.add(new JLabel("Password"), g);
        g.gridx = 1; form.add(password, g);
        add(form, BorderLayout.CENTER);

        JButton login = Ui.button("Sign in", this::doLogin);
        JButton register = Ui.button("Create account", this::doRegister);
        getRootPane().setDefaultButton(login);

        JLabel hint = new JLabel("<html><center>Demo accounts: admin / admin123 &nbsp;·&nbsp; student1 / pass123 &nbsp;·&nbsp; student2 / pass123</center></html>");
        hint.setForeground(Color.GRAY);
        hint.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        south.setBorder(BorderFactory.createEmptyBorder(0, 24, 16, 24));
        south.add(Ui.buttonBar(login, register), BorderLayout.NORTH);
        south.add(hint, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }

    static void installLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;
                }
            }
        } catch (Exception ignored) {
            // fall back to the default look and feel
        }
    }

    private void doLogin() {
        try {
            User u = auth.login(username.getText(), new String(password.getPassword()));
            new MainFrame(ctx, u).setVisible(true);
            dispose();
        } catch (IllegalArgumentException ex) {
            Ui.error(this, ex.getMessage());
            password.setText("");
        }
    }

    private void doRegister() {
        JTextField user = new JTextField(16), name = new JTextField(16), email = new JTextField(16);
        JPasswordField pass = new JPasswordField(16), confirm = new JPasswordField(16);
        JPanel p = new JPanel(new GridLayout(0, 2, 6, 8));
        p.add(new JLabel("Username")); p.add(user);
        p.add(new JLabel("Full name")); p.add(name);
        p.add(new JLabel("Email")); p.add(email);
        p.add(new JLabel("Password")); p.add(pass);
        p.add(new JLabel("Confirm password")); p.add(confirm);

        if (JOptionPane.showConfirmDialog(this, p, "Create account", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;

        String pw = new String(pass.getPassword());
        if (!pw.equals(new String(confirm.getPassword()))) {
            Ui.error(this, "Passwords do not match.");
            return;
        }
        try {
            auth.register(user.getText(), name.getText(), email.getText(), pw);
            username.setText(user.getText().trim());
            password.setText("");
            Ui.info(this, "Account created. You can sign in now.");
        } catch (IllegalArgumentException ex) {
            Ui.error(this, ex.getMessage());
        }
    }
}
