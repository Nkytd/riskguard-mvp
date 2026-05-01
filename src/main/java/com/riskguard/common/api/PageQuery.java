package com.riskguard.common.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PageQuery(
        @Min(1) Long pageNo,
        @Min(1) @Max(200) Long pageSize
) {

    public long normalizedPageNo() {
        return pageNo == null ? 1 : pageNo;
    }

    public long normalizedPageSize() {
        return pageSize == null ? 20 : pageSize;
    }
}
