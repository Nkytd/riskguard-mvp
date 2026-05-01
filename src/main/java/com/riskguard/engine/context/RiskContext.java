package com.riskguard.engine.context;

import java.util.Map;

public record RiskContext(
        Map<String, Object> variables
) {
}
