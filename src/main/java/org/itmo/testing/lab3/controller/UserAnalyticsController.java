package org.itmo.testing.lab3.controller;

import io.javalin.Javalin;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.itmo.testing.lab3.service.UserAnalyticsService;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserAnalyticsController {

    public static Javalin createApp() {
        UserAnalyticsService service = new UserAnalyticsService();
        Javalin app = Javalin.create();

        app.post("/register", ctx -> {
            String userId = ctx.queryParam("userId");
            String userName = ctx.queryParam("userName");
            if (userId == null || userName == null) {
                ctx.status(400)
                   .result("Missing parameters");
                return;
            }
            boolean success = false;
            try {
                service.registerUser(userId, userName);
                success = true;
            } catch (Exception ignored) {
                ctx.status(400);
            }
            ctx.result("User registered: " + success);
        });

        app.post("/recordSession", ctx -> {
            String userId = ctx.queryParam("userId");
            String loginTime = ctx.queryParam("loginTime");
            String logoutTime = ctx.queryParam("logoutTime");
            if (userId == null || loginTime == null || logoutTime == null) {
                ctx.status(400)
                   .result("Missing parameters");
                return;
            }
            try {
                LocalDateTime login = LocalDateTime.parse(loginTime);
                LocalDateTime logout = LocalDateTime.parse(logoutTime);
                service.recordSession(userId, login, logout);
                ctx.result("Session recorded");
            } catch (Exception e) {
                ctx.status(400)
                   .result("Invalid data: " + e.getMessage());
            }
        });

        app.get("/totalActivity", ctx -> {
            String userId = ctx.queryParam("userId");
            if (userId == null) {
                ctx.status(400)
                   .result("Missing userId");
                return;
            }
            try {
                long minutes = service.getTotalActivityTime(userId);
                ctx.result("Total activity: " + minutes + " minutes");
            } catch (Exception e) {
                ctx.status(400)
                   .result(e.getMessage());
            }
        });

        app.get("/inactiveUsers", ctx -> {
            String daysParam = ctx.queryParam("days");
            if (daysParam == null) {
                ctx.status(400)
                   .result("Missing days parameter");
                return;
            }
            try {
                int days = Integer.parseInt(daysParam);
                List<String> inactiveUsers = service.findInactiveUsers(days);
                ctx.json(inactiveUsers);
            } catch (NumberFormatException e) {
                ctx.status(400)
                   .result("Invalid number format for days");
            }
        });

        app.get("/monthlyActivity", ctx -> {
            String userId = ctx.queryParam("userId");
            String monthParam = ctx.queryParam("month");
            if (userId == null || monthParam == null) {
                ctx.status(400)
                   .result("Missing parameters");
                return;
            }
            try {
                YearMonth month = YearMonth.parse(monthParam);
                Map<String, Long> activity = service.getMonthlyActivityMetric(userId, month);
                ctx.json(activity);
            } catch (Exception e) {
                ctx.status(400)
                   .result("Invalid data: " + e.getMessage());
            }
        });
        return app;
    }

    public static void main(String[] args) {
        createApp().start();
    }
}

