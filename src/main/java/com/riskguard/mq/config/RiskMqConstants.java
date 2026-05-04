package com.riskguard.mq.config;

public final class RiskMqConstants {

    public static final String EVENT_EXCHANGE = "riskguard.events";
    public static final String PROFILE_UPDATE_QUEUE = "riskguard.profile.update";
    public static final String DASHBOARD_AGGREGATE_QUEUE = "riskguard.dashboard.aggregate";

    public static final String ROUTING_DECISION_CREATED = "risk.decision.created";
    public static final String ROUTING_CASE_CREATED = "risk.case.created";

    private RiskMqConstants() {
    }
}
