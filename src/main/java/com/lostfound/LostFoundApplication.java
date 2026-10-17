package com.lostfound;

import com.lostfound.ui.LoginFrame;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.SwingUtilities;
import java.awt.AWTError;
import java.awt.HeadlessException;

/**
 * Starts the Spring Boot backend (database, services, REST API + web page on port 8080)
 * and then opens the Swing desktop window.
 */
@SpringBootApplication
public class LostFoundApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(LostFoundApplication.class)
                .headless(false)          // we need AWT/Swing windows
                .run(args);

        // Hosting online? Set APP_DESKTOP=false (done in the Dockerfile) to skip the Swing window.
        if (!Boolean.parseBoolean(ctx.getEnvironment().getProperty("app.desktop", "true"))) {
            System.out.println("Desktop window disabled - web/API only.");
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                new LoginFrame(ctx).setVisible(true);
            } catch (HeadlessException | AWTError e) {
                System.out.println("No display available - running web/API only at http://localhost:8080");
            }
        });
    }
}
