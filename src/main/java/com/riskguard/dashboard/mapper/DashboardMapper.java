package com.riskguard.dashboard.mapper;

import com.riskguard.dashboard.dto.DashboardCountRow;
import com.riskguard.dashboard.dto.RiskTrendRow;
import com.riskguard.dashboard.dto.RuleHitRankRow;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface DashboardMapper {

    @Select("""
            SELECT COUNT(*)
            FROM risk_decision_log
            WHERE created_at >= #{startTime}
              AND created_at < #{endTime}
            """)
    long countDecisions(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Select("""
            SELECT decision AS name, COUNT(*) AS count
            FROM risk_decision_log
            WHERE created_at >= #{startTime}
              AND created_at < #{endTime}
            GROUP BY decision
            """)
    List<DashboardCountRow> countDecisionsByDecision(@Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime);

    @Select("""
            SELECT risk_level AS name, COUNT(*) AS count
            FROM risk_decision_log
            WHERE created_at >= #{startTime}
              AND created_at < #{endTime}
            GROUP BY risk_level
            """)
    List<DashboardCountRow> countDecisionsByRiskLevel(@Param("startTime") LocalDateTime startTime,
                                                      @Param("endTime") LocalDateTime endTime);

    @Select("""
            SELECT COALESCE(AVG(cost_ms), 0)
            FROM risk_decision_log
            WHERE created_at >= #{startTime}
              AND created_at < #{endTime}
            """)
    BigDecimal averageCostMs(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Select("""
            SELECT decision AS name, COUNT(*) AS count
            FROM risk_decision_log
            GROUP BY decision
            """)
    List<DashboardCountRow> countAllDecisionsByDecision();

    @Select("""
            SELECT
                rule_code AS ruleCode,
                MAX(rule_name) AS ruleName,
                COUNT(*) AS hitCount,
                COALESCE(SUM(score_delta), 0) AS totalScoreDelta,
                MAX(created_at) AS lastHitAt
            FROM risk_decision_hit_rule
            GROUP BY rule_code
            ORDER BY hitCount DESC, lastHitAt DESC
            LIMIT #{limit}
            """)
    List<RuleHitRankRow> topHitRules(@Param("limit") int limit);

    @Select("""
            SELECT
                DATE_FORMAT(created_at, '%Y-%m-%d %H:00:00') AS timeBucket,
                COUNT(*) AS decisionCount,
                SUM(CASE WHEN risk_level = 'HIGH' THEN 1 ELSE 0 END) AS highRiskCount,
                SUM(CASE WHEN risk_level = 'CRITICAL' THEN 1 ELSE 0 END) AS criticalRiskCount,
                SUM(CASE WHEN decision = 'REVIEW' THEN 1 ELSE 0 END) AS reviewCount,
                SUM(CASE WHEN decision = 'REJECT' THEN 1 ELSE 0 END) AS rejectCount
            FROM risk_decision_log
            WHERE created_at >= #{startTime}
              AND created_at < #{endTime}
            GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d %H:00:00')
            ORDER BY timeBucket ASC
            """)
    List<RiskTrendRow> riskTrend(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Select("""
            SELECT status AS name, COUNT(*) AS count
            FROM risk_case
            GROUP BY status
            """)
    List<DashboardCountRow> countCasesByStatus();
}
