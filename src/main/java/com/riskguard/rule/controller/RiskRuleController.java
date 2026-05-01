package com.riskguard.rule.controller;

import com.riskguard.common.api.ApiResponse;
import com.riskguard.common.api.PageResponse;
import com.riskguard.rule.dto.ExpressionValidateRequest;
import com.riskguard.rule.dto.ExpressionValidateResponse;
import com.riskguard.rule.dto.RiskRuleCreateRequest;
import com.riskguard.rule.dto.RiskRuleResponse;
import com.riskguard.rule.dto.RiskRuleUpdateRequest;
import com.riskguard.rule.dto.RulePublishRequest;
import com.riskguard.rule.dto.RuleVersionResponse;
import com.riskguard.rule.service.RiskRuleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/rules")
public class RiskRuleController {

    private final RiskRuleService riskRuleService;

    public RiskRuleController(RiskRuleService riskRuleService) {
        this.riskRuleService = riskRuleService;
    }

    @PostMapping
    public ApiResponse<RiskRuleResponse> create(@Valid @RequestBody RiskRuleCreateRequest request) {
        return ApiResponse.success(riskRuleService.createRule(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<RiskRuleResponse> update(@PathVariable Long id, @Valid @RequestBody RiskRuleUpdateRequest request) {
        return ApiResponse.success(riskRuleService.updateRule(id, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<RiskRuleResponse> get(@PathVariable Long id) {
        return ApiResponse.success(riskRuleService.getRule(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<RiskRuleResponse>> page(
            @RequestParam(defaultValue = "1") @Min(1) long pageNo,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(riskRuleService.pageRules(pageNo, pageSize, eventType, status, keyword));
    }

    @PostMapping("/{id}/enable")
    public ApiResponse<RiskRuleResponse> enable(@PathVariable Long id) {
        return ApiResponse.success(riskRuleService.enableRule(id));
    }

    @PostMapping("/{id}/disable")
    public ApiResponse<RiskRuleResponse> disable(@PathVariable Long id) {
        return ApiResponse.success(riskRuleService.disableRule(id));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<RuleVersionResponse> publish(@PathVariable Long id, @RequestBody(required = false) RulePublishRequest request) {
        return ApiResponse.success(riskRuleService.publishRule(id, request));
    }

    @GetMapping("/{id}/versions")
    public ApiResponse<List<RuleVersionResponse>> versions(@PathVariable Long id) {
        return ApiResponse.success(riskRuleService.listRuleVersions(id));
    }

    @PostMapping("/validate-expression")
    public ApiResponse<ExpressionValidateResponse> validateExpression(@Valid @RequestBody ExpressionValidateRequest request) {
        return ApiResponse.success(riskRuleService.validateExpression(request.expression()));
    }
}
