package org.itmo.testing.lab3.service;

import org.itmo.testing.lab3.model.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.itmo.testing.lab3.service.UserStatusService.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserStatusServiceTest {

    private static final String USER_ID = "userId";

    private UserAnalyticsService userAnalyticsService;
    private UserStatusService userStatusService;

    @BeforeAll
    void setUp() {
        userAnalyticsService = mock(UserAnalyticsService.class);
        userStatusService = new UserStatusService(userAnalyticsService);
    }

    @AfterEach
    void tearDown() {
        reset(userAnalyticsService);
    }

    private void testGetUserStatus(String expectedStatus, long totalActivityTime) {
        when(userAnalyticsService.getTotalActivityTime(USER_ID)).thenReturn(totalActivityTime);
        String status = userStatusService.getUserStatus(USER_ID);
        assertEquals(expectedStatus, status);

        verify(userAnalyticsService).getTotalActivityTime(USER_ID);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 59})
    void testGetUserStatus_Inactive(long totalActivityTime) {
        testGetUserStatus(INACTIVE, totalActivityTime);
    }

    @ParameterizedTest
    @ValueSource(longs = {60, 90, 119})
    void testGetUserStatus_Active(long totalActivityTime) {
        testGetUserStatus(ACTIVE, totalActivityTime);
    }

    @ParameterizedTest
    @ValueSource(longs = {120, 240})
    void testGetUserStatus_Highly_Active(long totalActivityTime) {
        testGetUserStatus(HIGHLY_ACTIVE, totalActivityTime);
    }

    private Stream<Arguments> getSingleUserSession() {
        LocalDateTime loginTime = LocalDateTime.of(2025, 3, 1, 20, 20);
        LocalDateTime logoutTime = LocalDateTime.of(2025, 3, 2, 15, 45);
        Session session = new Session(loginTime, logoutTime);
        return Stream.of(Arguments.of(Collections.singletonList(session)));
    }

    private Stream<Arguments> getUserSessions() {
        LocalDateTime loginTime = LocalDateTime.of(2025, 3, 1, 20, 20);
        LocalDateTime logoutTime = LocalDateTime.of(2025, 3, 2, 15, 45);
        Session session1 = new Session(loginTime, logoutTime);

        loginTime = LocalDateTime.of(2025, 3, 5, 17, 10);
        logoutTime = LocalDateTime.of(2025, 3, 5, 18, 5);
        Session session2 = new Session(loginTime, logoutTime);

        loginTime = LocalDateTime.of(2024, 5, 5, 9, 30);
        logoutTime = LocalDateTime.of(2024, 5, 5, 10, 25);
        Session session3 = new Session(loginTime, logoutTime);

        return Stream.of(Arguments.of(List.of(session1, session2, session3)));
    }

    private void testGetUserLastSessionDate(LocalDate expected, List<Session> userSessions) {
        when(userAnalyticsService.getUserSessions(USER_ID)).thenReturn(userSessions);

        Optional<String> result = userStatusService.getUserLastSessionDate(USER_ID);
        if (expected == null) {
            assertTrue(result.isEmpty());
        } else {
            String actual = result.orElseThrow();
            assertEquals(expected.toString(), actual);
        }

        verify(userAnalyticsService).getUserSessions(USER_ID);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testGetUserLastSessionDate_NoSessions(List<Session> userSessions) {
        testGetUserLastSessionDate(null, userSessions);
    }

    @ParameterizedTest
    @MethodSource("getSingleUserSession")
    void testGetUserLastSessionDate_OneSession(List<Session> userSessions) {
        LocalDate expected = LocalDate.of(2025, 3, 2);
        testGetUserLastSessionDate(expected, userSessions);
    }

    @ParameterizedTest
    @MethodSource("getUserSessions")
    void testGetUserLastSessionDate_SomeSessions(List<Session> userSessions) {
        LocalDate expected = LocalDate.of(2025, 3, 5);
        testGetUserLastSessionDate(expected, userSessions);
    }
}
