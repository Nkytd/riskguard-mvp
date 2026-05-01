package com.riskguard.common.trace;

import org.slf4j.MDC;

public final class TraceIdHolder {

    public static final String TRACE_ID = "traceId";

    private TraceIdHolder() {
    }

    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    public static void clear() {
        MDC.remove(TRACE_ID);
    }
}
