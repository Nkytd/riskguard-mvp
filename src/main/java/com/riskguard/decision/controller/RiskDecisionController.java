package com.riskguard.decision.controller;

import com.riskguard.common.api.ApiResponse;
import com.riskguard.common.api.PageResponse;
import com.riskguard.decision.dto.DecisionHitRuleResponse;
import com.riskguard.decision.dto.DecisionLogResponse;
import com.riskguard.decision.dto.RiskDecisionRequest;
import com.riskguard.decision.dto.RiskDecisionResponse;
import com.riskguard.decision.service.DecisionPersistenceService;
import com.riskguard.decision.service.RiskDecisionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/risk/decisions")
public class RiskDecisionController {

    private final RiskDecisionService riskDecisionService;
    private final DecisionPersistenceService decisionPersistenceService;

    public RiskDecisionController(RiskDecisionService riskDecisionService,
                                  DecisionPersistenceService decisionPersistenceService) {
        this.riskDecisionService = riskDecisionService;
        this.decisionPersistenceService = decisionPersistenceService;
    }

    @PostMapping
    public ApiResponse<RiskDecisionResponse> decide(@Valid @RequestBody RiskDecisionRequest request) {
        return ApiResponse.success(riskDecisionService.decide(request));
    }

    @PostMapping("/simulate")
    public ApiResponse<RiskDecisionResponse> simulate(@Valid @RequestBody RiskDecisionRequest request) {
        return ApiResponse.success(riskDecisionService.simulate(request));
    }

    @GetMapping("/{decisionNo}")
    public ApiResponse<DecisionLogResponse> getDecisionLog(@PathVariable String decisionNo) {
        return ApiResponse.success(decisionPersistenceService.getDecisionLog(decisionNo));
    }

    @GetMapping("/{decisionNo}/hit-rules")
    public ApiResponse<List<DecisionHitRuleResponse>> hitRules(@PathVariable String decisionNo) {
        return ApiResponse.success(decisionPersistenceService.listHitRules(decisionNo));
    }

    @GetMapping
    public ApiResponse<PageResponse<DecisionLogResponse>> page(
            @RequestParam(defaultValue = "1") @Min(1) long pageNo,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long pageSize,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String decision,
            @RequestParam(required = false) String userId) {
        return ApiResponse.success(decisionPersistenceService.pageDecisionLogs(pageNo, pageSize, eventType, decision, userId));
    }
}
