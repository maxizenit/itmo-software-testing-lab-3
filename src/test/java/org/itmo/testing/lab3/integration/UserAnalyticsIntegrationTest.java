package org.itmo.testing.lab3.integration;

import io.javalin.Javalin;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.itmo.testing.lab3.controller.UserAnalyticsController;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserAnalyticsIntegrationTest {

    private Javalin app;

    @BeforeAll
    void setUp() {
        app = UserAnalyticsController.createApp();
        int port = 7000;
        app.start(port);
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @AfterAll
    void tearDown() {
        app.stop();
    }

    private Response recordSession(String userId, String loginTime, String logoutTime) {
        RequestSpecification reqSpec = given();
        if (userId != null) {
            reqSpec = reqSpec.queryParam("userId", userId);
        }
        if (loginTime != null) {
            reqSpec = reqSpec.queryParam("loginTime", loginTime.toString());
        }
        if (logoutTime != null) {
            reqSpec = reqSpec.queryParam("logoutTime", logoutTime.toString());
        }
        return reqSpec.when()
                      .post("/recordSession");
    }

    private Response registerUser(String userId, String userName) {
        RequestSpecification reqSpec = given();
        if (userId != null) {
            reqSpec = reqSpec.queryParam("userId", userId);
        }
        if (userName != null) {
            reqSpec = reqSpec.queryParam("userName", userName);
        }
        return reqSpec.when()
                      .post("/register");
    }

    private Response getTotalActivity(String userId) {
        RequestSpecification reqSpec = given();
        if (userId != null) {
            reqSpec = reqSpec.queryParam("userId", userId);
        }
        return reqSpec.when()
                      .get("/totalActivity");
    }

    private Response getInactiveUsers(String days) {
        RequestSpecification reqSpec = given();
        if (days != null) {
            reqSpec = reqSpec.queryParam("days", days);
        }
        return reqSpec.when()
                      .get("/inactiveUsers");
    }

    private Response getMonthlyActivity(String userId, String month) {
        RequestSpecification reqSpec = given();
        if (userId != null) {
            reqSpec = reqSpec.queryParam("userId", userId);
        }
        if (month != null) {
            reqSpec = reqSpec.queryParam("month", month);
        }
        return reqSpec.when()
                      .get("/monthlyActivity");
    }

    @Test
    @Order(1)
    @DisplayName("Тест регистрации пользователя")
    void testUserRegistration() {
        registerUser("user1", "Alice").then()
                                      .statusCode(200)
                                      .body(equalTo("User registered: true"));
    }

    @Test
    @Order(2)
    @DisplayName("Тест регистрации пользователя с уже существующим userId")
    void testUserRegistration_UserAlreadyExists() {
        registerUser("user1", "Alice").then()
                                      .statusCode(400)
                                      .body(equalTo("User registered: false"));
    }

    @Test
    @Order(3)
    @DisplayName("Тест регистрации пользователя с отсутствующими параметрами")
    void testUserRegistration_MissingParameters() {
        registerUser(null, "Alice").then()
                                   .statusCode(400)
                                   .body(equalTo("Missing parameters"));
        registerUser("user1", null).then()
                                   .statusCode(400)
                                   .body(equalTo("Missing parameters"));
    }

    @Test
    @Order(4)
    @DisplayName("Тест записи сессии")
    void testRecordSession() {
        LocalDateTime now = LocalDateTime.now();
        recordSession("user1", now.minusHours(1)
                                  .toString(), now.toString()).then()
                                                              .statusCode(200)
                                                              .body(equalTo("Session recorded"));
    }

    @Test
    @Order(5)
    @DisplayName("Тест записи сессии с отсутствующими параметрами")
    void testRecordSession_MissingParameters() {
        LocalDateTime logoutTime = LocalDateTime.now();
        LocalDateTime loginTime = logoutTime.minusHours(1);

        recordSession(null, loginTime.toString(), logoutTime.toString()).then()
                                                                        .statusCode(400)
                                                                        .body(equalTo("Missing parameters"));
        recordSession("user1", null, logoutTime.toString()).then()
                                                           .statusCode(400)
                                                           .body(equalTo("Missing parameters"));
        recordSession("user1", loginTime.toString(), null).then()
                                                          .statusCode(400)
                                                          .body(equalTo("Missing parameters"));
    }

    @Test
    @Order(6)
    @DisplayName("Тест записи сессии с некорректными данными")
    void testRecordSession_InvalidData() {
        LocalDateTime logoutTime = LocalDateTime.now();
        LocalDateTime loginTime = logoutTime.minusHours(1);

        recordSession("user2", loginTime.toString(), logoutTime.toString()).then()
                                                                           .statusCode(400)
                                                                           .body(startsWith("Invalid data"));
        recordSession("user1", "abc", logoutTime.toString()).then()
                                                            .statusCode(400)
                                                            .body(startsWith("Invalid data"));
        recordSession("user1", loginTime.toString(), "def").then()
                                                           .statusCode(400)
                                                           .body(startsWith("Invalid data"));
    }

    @Test
    @Order(7)
    @DisplayName("Тест получения общего времени активности")
    void testGetTotalActivity() {
        getTotalActivity("user1").then()
                                 .statusCode(200)
                                 .body(containsString("Total activity:"))
                                 .body(containsString("60 minutes"));

        LocalDateTime logoutTime = LocalDateTime.now();
        LocalDateTime loginTime = logoutTime.minusHours(2);
        recordSession("user1", loginTime.toString(), logoutTime.toString());

        getTotalActivity("user1").then()
                                 .statusCode(200)
                                 .body(containsString("Total activity:"))
                                 .body(containsString("180 minutes"));
    }

    @Test
    @Order(8)
    @DisplayName("Тест получения общего времени активности с отсутствующим userId")
    void testGetTotalActivity_MissingUserId() {
        getTotalActivity(null).then()
                              .statusCode(400)
                              .body(equalTo("Missing userId"));
    }

    @Test
    @Order(9)
    @DisplayName("Тест получения общего времени активности для пользователя без сессий")
    void testGetTotalActivity_NoSessions() {
        registerUser("userWithoutSessions", "John");
        getTotalActivity("userWithoutSessions").then()
                                               .statusCode(400)
                                               .body(equalTo("No sessions found for user"));
    }

    @Test
    @Order(10)
    @DisplayName("Тест получения неактивных пользователей")
    void testGetInactiveUsers() {
        getInactiveUsers("1").then()
                             .statusCode(200)
                             .body(anything());

        registerUser("user3", "Jane");

        LocalDateTime logoutTime = LocalDateTime.now()
                                                .minusDays(5);
        LocalDateTime loginTime = logoutTime.minusHours(1);
        recordSession("user3", loginTime.toString(), logoutTime.toString());

        getInactiveUsers("3").then()
                             .statusCode(200)
                             .body(containsString("user3"));
        getInactiveUsers("5").then()
                             .statusCode(200)
                             .body(anything());
    }

    @Test
    @Order(11)
    @DisplayName("Тест получения неактивных пользователей с отсутствующим параметром")
    void testGetInactiveUsers_MissingParameter() {
        getInactiveUsers(null).then()
                              .statusCode(400)
                              .body(equalTo("Missing days parameter"));
    }

    @Test
    @Order(12)
    @DisplayName("Тест получения неактивных пользователей с некорректным параметром")
    void testGetInactiveUsers_InvalidNumberFormat() {
        getInactiveUsers("abc").then()
                               .statusCode(400)
                               .body(equalTo("Invalid number format for days"));
    }

    @Test
    @Order(13)
    @DisplayName("Тест получения времени активности пользователя за месяц")
    void testGetMonthlyActivity() {
        registerUser("user4", "Mike");

        LocalDateTime logoutTime = LocalDateTime.of(2024, 3, 1, 10, 0);
        LocalDateTime loginTime = logoutTime.minusHours(2);
        recordSession("user4", loginTime.toString(), logoutTime.toString());

        loginTime = LocalDateTime.of(2024, 2, 29, 20, 10);
        logoutTime = loginTime.minusMinutes(30);
        recordSession("user4", loginTime.toString(), logoutTime.toString());

        logoutTime = LocalDateTime.of(2024, 3, 5, 15, 45);
        loginTime = logoutTime.minusMinutes(45);
        recordSession("user4", loginTime.toString(), logoutTime.toString());

        logoutTime = LocalDateTime.of(2024, 3, 5, 12, 45);
        loginTime = logoutTime.minusMinutes(45);
        recordSession("user4", loginTime.toString(), logoutTime.toString());

        getMonthlyActivity("user4", "2024-03").then()
                                              .statusCode(200)
                                              .body(containsString("\"2024-03-01\":120"))
                                              .body(containsString("\"2024-03-05\":90"));
    }

    @Test
    @Order(14)
    @DisplayName("Тест получения времени активности пользователя за месяц с отсутствующим параметром")
    void testGetMonthlyActivity_MissingParameters() {
        getMonthlyActivity(null, "2025-03").then()
                                           .statusCode(400)
                                           .body(equalTo("Missing parameters"));
        getMonthlyActivity("user1", null).then()
                                         .statusCode(400)
                                         .body(equalTo("Missing parameters"));
    }

    @Test
    @Order(15)
    @DisplayName("Тест получения времени активности пользователя за месяц для пользователя без сессий")
    void testGetMonthlyActivity_NoSessions() {
        getMonthlyActivity("userWithoutSessions", "2025-03").then()
                                                            .statusCode(400)
                                                            .body(equalTo("Invalid data: No sessions found for user"));
    }

    @Test
    @Order(16)
    @DisplayName("Тест получения времени активности пользователя с некорректным параметром")
    void testGetMonthlyActivity_InvalidData() {
        getMonthlyActivity("user1", "abc").then()
                                       .statusCode(400)
                                       .body(startsWith("Invalid data: "));
    }
}
