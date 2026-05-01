package com.riskguard.common.api;

import java.util.List;

public record PageResponse<T>(
        List<T> records,
        long total,
        long pageNo,
        long pageSize
) {

    public static <T> PageResponse<T> of(List<T> records, long total, long pageNo, long pageSize) {
        return new PageResponse<>(records, total, pageNo, pageSize);
    }
}
