package com.riskguard.rule.dto;

import jakarta.validation.constraints.NotBlank;

public record ExpressionValidateRequest(
        @NotBlank String expression
) {
}
