package org.itmo.testing.lab3.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.itmo.testing.lab3.model.Session;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class UserStatusService {

    public static final String INACTIVE = "Inactive";
    public static final String ACTIVE = "Active";
    public static final String HIGHLY_ACTIVE = "Highly active";

    private final UserAnalyticsService userAnalyticsService;

    public String getUserStatus(String userId) {
        long totalActivityTime = userAnalyticsService.getTotalActivityTime(userId);

        if (totalActivityTime < 60) {
            return INACTIVE;
        } else if (totalActivityTime < 120) {
            return ACTIVE;
        } else {
            return HIGHLY_ACTIVE;
        }
    }

    public Optional<String> getUserLastSessionDate(String userId) {
        List<Session> userSessions = userAnalyticsService.getUserSessions(userId);
        if (CollectionUtils.isEmpty(userSessions)) {
            return Optional.empty();
        }
        Session lastSession = userSessions.stream()
                                          .sorted(Comparator.comparing(Session::logoutTime))
                                          .toList()
                                          .getLast();
        return Optional.of(lastSession.logoutTime()
                                      .toLocalDate()
                                      .toString());
    }
}
