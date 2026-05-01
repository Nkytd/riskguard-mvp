package com.riskguard.strategy.controller;

import com.riskguard.common.api.ApiResponse;
import com.riskguard.common.api.PageResponse;
import com.riskguard.strategy.dto.RiskStrategyCreateRequest;
import com.riskguard.strategy.dto.RiskStrategyResponse;
import com.riskguard.strategy.dto.RiskStrategyUpdateRequest;
import com.riskguard.strategy.dto.StrategyPublishRequest;
import com.riskguard.strategy.dto.StrategyRollbackRequest;
import com.riskguard.strategy.dto.StrategyRuleBindRequest;
import com.riskguard.strategy.dto.StrategyRuleOrderRequest;
import com.riskguard.strategy.dto.StrategyRuleResponse;
import com.riskguard.strategy.dto.StrategyVersionResponse;
import com.riskguard.strategy.service.RiskStrategyService;
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

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/strategies")
public class RiskStrategyController {

    private final RiskStrategyService riskStrategyService;

    public RiskStrategyController(RiskStrategyService riskStrategyService) {
        this.riskStrategyService = riskStrategyService;
    }

    @PostMapping
    public ApiResponse<RiskStrategyResponse> create(@Valid @RequestBody RiskStrategyCreateRequest request) {
        return ApiResponse.success(riskStrategyService.createStrategy(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<RiskStrategyResponse> update(@PathVariable Long id, @Valid @RequestBody RiskStrategyUpdateRequest request) {
        return ApiResponse.success(riskStrategyService.updateStrategy(id, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<RiskStrategyResponse> get(@PathVariable Long id) {
        return ApiResponse.success(riskStrategyService.getStrategy(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<RiskStrategyResponse>> page(
            @RequestParam(defaultValue = "1") @Min(1) long pageNo,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(riskStrategyService.pageStrategies(pageNo, pageSize, eventType, status, keyword));
    }

    @PostMapping("/{id}/enable")
    public ApiResponse<RiskStrategyResponse> enable(@PathVariable Long id) {
        return ApiResponse.success(riskStrategyService.enableStrategy(id));
    }

    @PostMapping("/{id}/disable")
    public ApiResponse<RiskStrategyResponse> disable(@PathVariable Long id) {
        return ApiResponse.success(riskStrategyService.disableStrategy(id));
    }

    @PostMapping("/{id}/rules")
    public ApiResponse<StrategyRuleResponse> bindRule(@PathVariable Long id, @Valid @RequestBody StrategyRuleBindRequest request) {
        return ApiResponse.success(riskStrategyService.bindRule(id, request));
    }

    @GetMapping("/{id}/rules")
    public ApiResponse<List<StrategyRuleResponse>> listRules(@PathVariable Long id) {
        return ApiResponse.success(riskStrategyService.listRules(id));
    }

    @DeleteMapping("/{id}/rules/{ruleId}")
    public ApiResponse<Void> removeRule(@PathVariable Long id, @PathVariable Long ruleId) {
        riskStrategyService.removeRule(id, ruleId);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/rules/order")
    public ApiResponse<List<StrategyRuleResponse>> updateRuleOrder(
            @PathVariable Long id,
            @Valid @RequestBody StrategyRuleOrderRequest request) {
        return ApiResponse.success(riskStrategyService.updateRuleOrder(id, request));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<StrategyVersionResponse> publish(@PathVariable Long id, @RequestBody(required = false) StrategyPublishRequest request) {
        return ApiResponse.success(riskStrategyService.publishStrategy(id, request));
    }

    @PostMapping("/{id}/rollback")
    public ApiResponse<StrategyVersionResponse> rollback(@PathVariable Long id, @Valid @RequestBody StrategyRollbackRequest request) {
        return ApiResponse.success(riskStrategyService.rollbackStrategy(id, request));
    }

    @GetMapping("/{id}/versions")
    public ApiResponse<List<StrategyVersionResponse>> versions(@PathVariable Long id) {
        return ApiResponse.success(riskStrategyService.listVersions(id));
    }
}
