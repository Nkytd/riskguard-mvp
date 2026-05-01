package com.riskguard.risklist.controller;

import com.riskguard.common.api.ApiResponse;
import com.riskguard.common.api.PageResponse;
import com.riskguard.risklist.dto.RiskListCreateRequest;
import com.riskguard.risklist.dto.RiskListResponse;
import com.riskguard.risklist.dto.RiskListUpdateRequest;
import com.riskguard.risklist.service.RiskListService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/risk-lists")
public class RiskListController {

    private final RiskListService riskListService;

    public RiskListController(RiskListService riskListService) {
        this.riskListService = riskListService;
    }

    @PostMapping
    public ApiResponse<RiskListResponse> create(@Valid @RequestBody RiskListCreateRequest request) {
        return ApiResponse.success(riskListService.createRiskList(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<RiskListResponse> update(@PathVariable Long id, @Valid @RequestBody RiskListUpdateRequest request) {
        return ApiResponse.success(riskListService.updateRiskList(id, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<RiskListResponse> get(@PathVariable Long id) {
        return ApiResponse.success(riskListService.getRiskList(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<RiskListResponse>> page(
            @RequestParam(defaultValue = "1") @Min(1) long pageNo,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize,
            @RequestParam(required = false) String listType,
            @RequestParam(required = false) String objectType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(riskListService.pageRiskLists(pageNo, pageSize, listType, objectType, status, keyword));
    }

    @PostMapping("/{id}/enable")
    public ApiResponse<RiskListResponse> enable(@PathVariable Long id) {
        return ApiResponse.success(riskListService.enableRiskList(id));
    }

    @PostMapping("/{id}/disable")
    public ApiResponse<RiskListResponse> disable(@PathVariable Long id) {
        return ApiResponse.success(riskListService.disableRiskList(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        riskListService.deleteRiskList(id);
        return ApiResponse.success();
    }
}
