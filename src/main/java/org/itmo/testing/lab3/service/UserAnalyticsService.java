package org.itmo.testing.lab3.service;

import org.itmo.testing.lab3.model.Session;
import org.itmo.testing.lab3.model.User;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserAnalyticsService {

    private final Map<String, User> users = new HashMap<>();
    private final Map<String, List<Session>> userSessions = new HashMap<>();

    public boolean registerUser(String userId, String userName) {
        if (users.containsKey(userId)) {
            throw new IllegalArgumentException("User already exists");
        }
        users.put(userId, new User(userId, userName));
        return true;
    }

    public void recordSession(String userId, LocalDateTime loginTime, LocalDateTime logoutTime) {
        if (!users.containsKey(userId)) {
            throw new IllegalArgumentException("User not found");
        }
        Session session = new Session(loginTime, logoutTime);
        userSessions.computeIfAbsent(userId, k -> new ArrayList<>())
                    .add(session);
    }

    public long getTotalActivityTime(String userId) {
        if (!userSessions.containsKey(userId)) {
            throw new IllegalArgumentException("No sessions found for user");
        }
        return userSessions.get(userId)
                           .stream()
                           .mapToLong(session -> ChronoUnit.MINUTES.between(session.loginTime(), session.logoutTime()))
                           .sum();
    }

    public List<String> findInactiveUsers(int days) {
        List<String> inactiveUsers = new ArrayList<>();
        for (Map.Entry<String, List<Session>> entry : userSessions.entrySet()) {
            String userId = entry.getKey();
            List<Session> sessions = entry.getValue();
            if (sessions.isEmpty()) {
                continue;
            }
            LocalDateTime lastSessionTime = sessions.getLast()
                                                    .logoutTime();
            long daysInactive = ChronoUnit.DAYS.between(lastSessionTime, LocalDateTime.now());
            if (daysInactive > days) {
                inactiveUsers.add(userId);
            }
        }
        return inactiveUsers;
    }

    public Map<String, Long> getMonthlyActivityMetric(String userId, YearMonth month) {
        if (!userSessions.containsKey(userId)) {
            throw new IllegalArgumentException("No sessions found for user");
        }
        Map<String, Long> activityByDay = new HashMap<>();
        userSessions.get(userId)
                    .stream()
                    .filter(session -> isSessionInMonth(session, month))
                    .forEach(session -> {
                        String dayKey = session.loginTime()
                                               .toLocalDate()
                                               .toString();
                        long minutes = ChronoUnit.MINUTES.between(session.loginTime(), session.logoutTime());
                        activityByDay.put(dayKey, activityByDay.getOrDefault(dayKey, 0L) + minutes);
                    });
        return activityByDay;
    }

    private boolean isSessionInMonth(Session session, YearMonth month) {
        LocalDateTime start = session.loginTime();
        return start.getYear() == month.getYear() && start.getMonth() == month.getMonth();
    }

    public User getUser(String userId) {
        return users.get(userId);
    }

    public List<Session> getUserSessions(String userId) {
        return userSessions.get(userId);
    }
}
