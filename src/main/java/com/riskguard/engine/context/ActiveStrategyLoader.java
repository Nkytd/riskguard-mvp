package com.riskguard.engine.context;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riskguard.common.api.ErrorCode;
import com.riskguard.common.cache.RiskCacheService;
import com.riskguard.common.enums.PublishStatus;
import com.riskguard.common.exception.BusinessException;
import com.riskguard.engine.model.ActiveStrategySnapshot;
import com.riskguard.strategy.dto.StrategyRuleSnapshot;
import com.riskguard.strategy.dto.StrategyVersionResponse;
import com.riskguard.strategy.entity.RiskStrategy;
import com.riskguard.strategy.entity.RiskStrategyVersion;
import com.riskguard.strategy.mapper.RiskStrategyMapper;
import com.riskguard.strategy.mapper.RiskStrategyVersionMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ActiveStrategyLoader {

    private final RiskStrategyMapper strategyMapper;
    private final RiskStrategyVersionMapper strategyVersionMapper;
    private final RiskCacheService riskCacheService;
    private final ObjectMapper objectMapper;

    public ActiveStrategyLoader(RiskStrategyMapper strategyMapper,
                                RiskStrategyVersionMapper strategyVersionMapper,
                                RiskCacheService riskCacheService,
                                ObjectMapper objectMapper) {
        this.strategyMapper = strategyMapper;
        this.strategyVersionMapper = strategyVersionMapper;
        this.riskCacheService = riskCacheService;
        this.objectMapper = objectMapper;
    }

    public ActiveStrategySnapshot load(String eventType) {
        return riskCacheService.getActiveStrategy(eventType, StrategyVersionResponse.class)
                .map(this::toSnapshot)
                .orElseGet(() -> loadFromDatabase(eventType));
    }

    private ActiveStrategySnapshot loadFromDatabase(String eventType) {
        List<RiskStrategy> strategies = strategyMapper.selectList(Wrappers.<RiskStrategy>lambdaQuery()
                .eq(RiskStrategy::getEventType, eventType)
                .eq(RiskStrategy::getStatus, PublishStatus.ENABLED.name())
                .gt(RiskStrategy::getVersion, 0)
                .orderByDesc(RiskStrategy::getUpdatedAt)
                .orderByDesc(RiskStrategy::getId));
        if (strategies.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "No active strategy found for eventType: " + eventType);
        }

        RiskStrategy strategy = strategies.get(0);
        RiskStrategyVersion version = strategyVersionMapper.selectOne(Wrappers.<RiskStrategyVersion>lambdaQuery()
                .eq(RiskStrategyVersion::getStrategyId, strategy.getId())
                .eq(RiskStrategyVersion::getVersion, strategy.getVersion()));
        if (version == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Active strategy version not found: " + strategy.getId());
        }

        StrategyVersionResponse response = StrategyVersionResponse.from(version);
        riskCacheService.putActiveStrategy(strategy.getEventType(), response);
        return toSnapshot(response);
    }

    private ActiveStrategySnapshot toSnapshot(StrategyVersionResponse response) {
        try {
            List<StrategyRuleSnapshot> rules = objectMapper.readValue(response.ruleSnapshot(), new TypeReference<>() {
            });
            return new ActiveStrategySnapshot(response.strategyId(), response.version(), response.grayRatio(), rules);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to parse strategy rule snapshot");
        }
    }
}
