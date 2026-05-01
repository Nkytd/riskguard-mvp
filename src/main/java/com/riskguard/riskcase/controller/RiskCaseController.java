package com.riskguard.riskcase.controller;

import com.riskguard.common.api.ApiResponse;
import com.riskguard.common.api.PageResponse;
import com.riskguard.riskcase.dto.CaseActionRequest;
import com.riskguard.riskcase.dto.CaseOperationResponse;
import com.riskguard.riskcase.dto.RiskCaseResponse;
import com.riskguard.riskcase.service.RiskCaseService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/cases")
public class RiskCaseController {

    private final RiskCaseService riskCaseService;

    public RiskCaseController(RiskCaseService riskCaseService) {
        this.riskCaseService = riskCaseService;
    }

    @GetMapping
    public ApiResponse<PageResponse<RiskCaseResponse>> page(
            @RequestParam(defaultValue = "1") @Min(1) long pageNo,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String userId) {
        return ApiResponse.success(riskCaseService.pageCases(pageNo, pageSize, status, userId));
    }

    @GetMapping("/{caseNo}")
    public ApiResponse<RiskCaseResponse> get(@PathVariable String caseNo) {
        return ApiResponse.success(riskCaseService.getCase(caseNo));
    }

    @PostMapping("/{caseNo}/claim")
    public ApiResponse<RiskCaseResponse> claim(@PathVariable String caseNo, @RequestBody(required = false) CaseActionRequest request) {
        return ApiResponse.success(riskCaseService.claim(caseNo, request));
    }

    @PostMapping("/{caseNo}/approve")
    public ApiResponse<RiskCaseResponse> approve(@PathVariable String caseNo, @RequestBody(required = false) CaseActionRequest request) {
        return ApiResponse.success(riskCaseService.approve(caseNo, request));
    }

    @PostMapping("/{caseNo}/reject")
    public ApiResponse<RiskCaseResponse> reject(@PathVariable String caseNo, @RequestBody(required = false) CaseActionRequest request) {
        return ApiResponse.success(riskCaseService.reject(caseNo, request));
    }

    @PostMapping("/{caseNo}/close")
    public ApiResponse<RiskCaseResponse> close(@PathVariable String caseNo, @RequestBody(required = false) CaseActionRequest request) {
        return ApiResponse.success(riskCaseService.close(caseNo, request));
    }

    @PostMapping("/{caseNo}/mark-false-positive")
    public ApiResponse<RiskCaseResponse> markFalsePositive(@PathVariable String caseNo, @RequestBody(required = false) CaseActionRequest request) {
        return ApiResponse.success(riskCaseService.markFalsePositive(caseNo, request));
    }

    @GetMapping("/{caseNo}/operations")
    public ApiResponse<List<CaseOperationResponse>> operations(@PathVariable String caseNo) {
        return ApiResponse.success(riskCaseService.listOperations(caseNo));
    }
}
