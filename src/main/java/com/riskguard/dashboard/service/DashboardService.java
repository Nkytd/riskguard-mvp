package com.riskguard.dashboard.service;

import com.riskguard.common.enums.CaseStatus;
import com.riskguard.common.enums.RiskDecision;
import com.riskguard.common.enums.RiskLevel;
import com.riskguard.common.utils.ClockUtils;
import com.riskguard.dashboard.dto.CaseStatisticsResponse;
import com.riskguard.dashboard.dto.DashboardCountRow;
import com.riskguard.dashboard.dto.DashboardDistributionItem;
import com.riskguard.dashboard.dto.DashboardOverviewResponse;
import com.riskguard.dashboard.dto.RiskTrendItem;
import com.riskguard.dashboard.dto.RiskTrendRow;
import com.riskguard.dashboard.dto.RuleHitRankItem;
import com.riskguard.dashboard.dto.RuleHitRankRow;
import com.riskguard.dashboard.mapper.DashboardMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final DateTimeFormatter HOUR_BUCKET_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00:00");

    private final DashboardMapper dashboardMapper;

    public DashboardService(DashboardMapper dashboardMapper) {
        this.dashboardMapper = dashboardMapper;
    }

    public DashboardOverviewResponse overview() {
        LocalDateTime startTime = LocalDate.now().atStartOfDay();
        LocalDateTime endTime = startTime.plusDays(1);
        long total = dashboardMapper.countDecisions(startTime, endTime);
        Map<String, Long> decisionCounts = toCountMap(dashboardMapper.countDecisionsByDecision(startTime, endTime));
        Map<String, Long> riskLevelCounts = toCountMap(dashboardMapper.countDecisionsByRiskLevel(startTime, endTime));
        double averageCostMs = round(dashboardMapper.averageCostMs(startTime, endTime));
        long highRiskCount = countOf(riskLevelCounts, RiskLevel.HIGH.name());
        long criticalRiskCount = countOf(riskLevelCounts, RiskLevel.CRITICAL.name());

        return new DashboardOverviewResponse(
                startTime,
                endTime,
                total,
                countOf(decisionCounts, RiskDecision.PASS.name()),
                countOf(decisionCounts, RiskDecision.VERIFY.name()),
                countOf(decisionCounts, RiskDecision.REVIEW.name()),
                countOf(decisionCounts, RiskDecision.REJECT.name()),
                averageCostMs,
                highRiskCount,
                criticalRiskCount,
                highRiskCount + criticalRiskCount
        );
    }

    public List<DashboardDistributionItem> decisionDistribution() {
        Map<String, Long> counts = toCountMap(dashboardMapper.countAllDecisionsByDecision());
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return Arrays.stream(RiskDecision.values())
                .map(decision -> toDistributionItem(decision.name(), countOf(counts, decision.name()), total))
                .toList();
    }

    public List<RuleHitRankItem> ruleHitRank() {
        return dashboardMapper.topHitRules(10).stream()
                .map(this::toRuleHitRankItem)
                .toList();
    }

    public List<RiskTrendItem> riskTrend() {
        LocalDateTime endTime = ClockUtils.now().truncatedTo(ChronoUnit.HOURS).plusHours(1);
        LocalDateTime startTime = endTime.minusHours(24);
        Map<String, RiskTrendRow> rows = dashboardMapper.riskTrend(startTime, endTime).stream()
                .collect(Collectors.toMap(RiskTrendRow::getTimeBucket, Function.identity()));

        return java.util.stream.Stream.iterate(startTime, bucket -> bucket.isBefore(endTime), bucket -> bucket.plusHours(1))
                .map(bucket -> {
                    String timeBucket = bucket.format(HOUR_BUCKET_FORMATTER);
                    RiskTrendRow row = rows.get(timeBucket);
                    return row == null ? emptyTrendItem(timeBucket) : toRiskTrendItem(row);
                })
                .toList();
    }

    public CaseStatisticsResponse caseStatistics() {
        Map<String, Long> counts = toCountMap(dashboardMapper.countCasesByStatus());
        long pendingCount = countOf(counts, CaseStatus.PENDING.name());
        long processingCount = countOf(counts, CaseStatus.PROCESSING.name());
        long approvedCount = countOf(counts, CaseStatus.APPROVED.name());
        long rejectedCount = countOf(counts, CaseStatus.REJECTED.name());
        long closedCount = countOf(counts, CaseStatus.CLOSED.name());
        long total = pendingCount + processingCount + approvedCount + rejectedCount + closedCount;
        long audited = approvedCount + rejectedCount;

        return new CaseStatisticsResponse(
                total,
                pendingCount,
                processingCount,
                approvedCount,
                rejectedCount,
                closedCount,
                ratio(approvedCount, audited),
                ratio(rejectedCount, audited)
        );
    }

    private DashboardDistributionItem toDistributionItem(String name, long count, long total) {
        return new DashboardDistributionItem(name, count, ratio(count, total));
    }

    private RuleHitRankItem toRuleHitRankItem(RuleHitRankRow row) {
        return new RuleHitRankItem(
                row.getRuleCode(),
                row.getRuleName(),
                safeLong(row.getHitCount()),
                safeLong(row.getTotalScoreDelta()),
                row.getLastHitAt()
        );
    }

    private RiskTrendItem toRiskTrendItem(RiskTrendRow row) {
        return new RiskTrendItem(
                row.getTimeBucket(),
                safeLong(row.getDecisionCount()),
                safeLong(row.getHighRiskCount()),
                safeLong(row.getCriticalRiskCount()),
                safeLong(row.getReviewCount()),
                safeLong(row.getRejectCount())
        );
    }

    private RiskTrendItem emptyTrendItem(String timeBucket) {
        return new RiskTrendItem(timeBucket, 0, 0, 0, 0, 0);
    }

    private Map<String, Long> toCountMap(List<DashboardCountRow> rows) {
        return rows.stream()
                .collect(Collectors.toMap(DashboardCountRow::getName, row -> safeLong(row.getCount()), Long::sum));
    }

    private long countOf(Map<String, Long> counts, String name) {
        return counts.getOrDefault(name, 0L);
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private double ratio(long value, long total) {
        if (total <= 0) {
            return 0D;
        }
        return BigDecimal.valueOf(value)
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private double round(BigDecimal value) {
        if (value == null) {
            return 0D;
        }
        return value.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
