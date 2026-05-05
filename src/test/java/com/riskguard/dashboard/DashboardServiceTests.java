package com.riskguard.dashboard;

import com.riskguard.dashboard.dto.DashboardCountRow;
import com.riskguard.dashboard.mapper.DashboardMapper;
import com.riskguard.dashboard.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTests {

    @Mock
    private DashboardMapper dashboardMapper;

    @Test
    void overviewFillsDecisionAndRiskLevelCounts() {
        DashboardService service = new DashboardService(dashboardMapper);
        when(dashboardMapper.countDecisions(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(12L);
        when(dashboardMapper.countDecisionsByDecision(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(countRow("PASS", 5), countRow("REJECT", 2)));
        when(dashboardMapper.countDecisionsByRiskLevel(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(countRow("HIGH", 3), countRow("CRITICAL", 1)));
        when(dashboardMapper.averageCostMs(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("12.345"));

        var overview = service.overview();

        assertThat(overview.todayDecisionCount()).isEqualTo(12);
        assertThat(overview.passCount()).isEqualTo(5);
        assertThat(overview.verifyCount()).isZero();
        assertThat(overview.reviewCount()).isZero();
        assertThat(overview.rejectCount()).isEqualTo(2);
        assertThat(overview.averageCostMs()).isEqualTo(12.35);
        assertThat(overview.highRiskCount()).isEqualTo(3);
        assertThat(overview.criticalRiskCount()).isEqualTo(1);
        assertThat(overview.highAndCriticalRiskCount()).isEqualTo(4);
    }

    @Test
    void decisionDistributionIncludesZeroCategoriesAndRatios() {
        DashboardService service = new DashboardService(dashboardMapper);
        when(dashboardMapper.countAllDecisionsByDecision())
                .thenReturn(List.of(countRow("PASS", 3), countRow("REJECT", 1)));

        var distribution = service.decisionDistribution();

        assertThat(distribution).hasSize(4);
        assertThat(distribution)
                .extracting("name")
                .containsExactly("PASS", "VERIFY", "REVIEW", "REJECT");
        assertThat(distribution.get(0).count()).isEqualTo(3);
        assertThat(distribution.get(0).ratio()).isEqualTo(0.75);
        assertThat(distribution.get(1).count()).isZero();
        assertThat(distribution.get(3).ratio()).isEqualTo(0.25);
    }

    @Test
    void riskTrendReturnsTwentyFourHourlyBuckets() {
        DashboardService service = new DashboardService(dashboardMapper);
        when(dashboardMapper.riskTrend(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());

        var trend = service.riskTrend();

        assertThat(trend).hasSize(24);
        assertThat(trend).allSatisfy(item -> {
            assertThat(item.decisionCount()).isZero();
            assertThat(item.highRiskCount()).isZero();
            assertThat(item.criticalRiskCount()).isZero();
            assertThat(item.reviewCount()).isZero();
            assertThat(item.rejectCount()).isZero();
        });
    }

    @Test
    void caseStatisticsCalculatesAuditRates() {
        DashboardService service = new DashboardService(dashboardMapper);
        when(dashboardMapper.countCasesByStatus()).thenReturn(List.of(
                countRow("PENDING", 2),
                countRow("PROCESSING", 1),
                countRow("APPROVED", 3),
                countRow("REJECTED", 1),
                countRow("CLOSED", 4)
        ));

        var statistics = service.caseStatistics();

        assertThat(statistics.totalCount()).isEqualTo(11);
        assertThat(statistics.pendingCount()).isEqualTo(2);
        assertThat(statistics.processingCount()).isEqualTo(1);
        assertThat(statistics.approvedCount()).isEqualTo(3);
        assertThat(statistics.rejectedCount()).isEqualTo(1);
        assertThat(statistics.closedCount()).isEqualTo(4);
        assertThat(statistics.approvalRate()).isEqualTo(0.75);
        assertThat(statistics.rejectionRate()).isEqualTo(0.25);
    }

    private DashboardCountRow countRow(String name, long count) {
        DashboardCountRow row = new DashboardCountRow();
        row.setName(name);
        row.setCount(count);
        return row;
    }
}
