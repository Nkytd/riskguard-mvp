package com.riskguard.common.utils;

import com.riskguard.common.api.ErrorCode;
import com.riskguard.common.exception.BusinessException;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class EnumUtils {

    private EnumUtils() {
    }

    public static <E extends Enum<E>> String requireName(Class<E> enumType, String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, fieldName + " must not be blank");
        }
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase()).name();
        } catch (IllegalArgumentException ex) {
            String supported = Arrays.stream(enumType.getEnumConstants())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, fieldName + " must be one of: " + supported);
        }
    }
}
