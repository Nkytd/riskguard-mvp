package com.riskguard.common.utils;

import java.time.Clock;
import java.time.LocalDateTime;

public final class ClockUtils {

    private static final Clock CLOCK = Clock.systemDefaultZone();

    private ClockUtils() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(CLOCK);
    }
}
