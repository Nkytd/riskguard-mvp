package com.riskguard.rule.dto;

public record ExpressionValidateResponse(
        boolean valid,
        String message
) {
}
