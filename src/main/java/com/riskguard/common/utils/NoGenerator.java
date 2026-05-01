package com.riskguard.common.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class NoGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private NoGenerator() {
    }

    public static String decisionNo() {
        return "D" + suffix();
    }

    public static String eventNo() {
        return "E" + suffix();
    }

    public static String caseNo() {
        return "C" + suffix();
    }

    public static String simulateNo() {
        return "S" + suffix();
    }

    private static String suffix() {
        return LocalDateTime.now().format(FORMATTER) + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
