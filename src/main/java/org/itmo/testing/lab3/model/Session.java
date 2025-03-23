package org.itmo.testing.lab3.model;

import java.time.LocalDateTime;

public record Session(LocalDateTime loginTime, LocalDateTime logoutTime) {}
