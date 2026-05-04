package com.riskguard.dashboard.controller;

import com.riskguard.common.api.ApiResponse;
import com.riskguard.dashboard.dto.CaseStatisticsResponse;
import com.riskguard.dashboard.dto.DashboardDistributionItem;
import com.riskguard.dashboard.dto.DashboardOverviewResponse;
import com.riskguard.dashboard.dto.RiskTrendItem;
import com.riskguard.dashboard.dto.RuleHitRankItem;
import com.riskguard.dashboard.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public ApiResponse<DashboardOverviewResponse> overview() {
        return ApiResponse.success(dashboardService.overview());
    }

    @GetMapping("/decision-distribution")
    public ApiResponse<List<DashboardDistributionItem>> decisionDistribution() {
        return ApiResponse.success(dashboardService.decisionDistribution());
    }

    @GetMapping("/rule-hit-rank")
    public ApiResponse<List<RuleHitRankItem>> ruleHitRank() {
        return ApiResponse.success(dashboardService.ruleHitRank());
    }

    @GetMapping("/risk-trend")
    public ApiResponse<List<RiskTrendItem>> riskTrend() {
        return ApiResponse.success(dashboardService.riskTrend());
    }

    @GetMapping("/case-statistics")
    public ApiResponse<CaseStatisticsResponse> caseStatistics() {
        return ApiResponse.success(dashboardService.caseStatistics());
    }
}
