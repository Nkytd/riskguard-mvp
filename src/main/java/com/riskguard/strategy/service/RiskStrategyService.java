package com.riskguard.strategy.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riskguard.common.api.ErrorCode;
import com.riskguard.common.api.PageResponse;
import com.riskguard.common.cache.RiskCacheService;
import com.riskguard.common.enums.EventType;
import com.riskguard.common.enums.PublishStatus;
import com.riskguard.common.exception.BusinessException;
import com.riskguard.common.utils.EnumUtils;
import com.riskguard.rule.entity.RiskRule;
import com.riskguard.rule.entity.RiskRuleVersion;
import com.riskguard.rule.mapper.RiskRuleMapper;
import com.riskguard.rule.service.RiskRuleService;
import com.riskguard.strategy.dto.StrategyPublishRequest;
import com.riskguard.strategy.dto.StrategyRollbackRequest;
import com.riskguard.strategy.dto.StrategyRuleBindRequest;
import com.riskguard.strategy.dto.StrategyRuleOrderRequest;
import com.riskguard.strategy.dto.StrategyRuleResponse;
import com.riskguard.strategy.dto.StrategyRuleSnapshot;
import com.riskguard.strategy.dto.StrategyVersionResponse;
import com.riskguard.strategy.dto.RiskStrategyCreateRequest;
import com.riskguard.strategy.dto.RiskStrategyResponse;
import com.riskguard.strategy.dto.RiskStrategyUpdateRequest;
import com.riskguard.strategy.entity.RiskStrategy;
import com.riskguard.strategy.entity.RiskStrategyRule;
import com.riskguard.strategy.entity.RiskStrategyVersion;
import com.riskguard.strategy.mapper.RiskStrategyMapper;
import com.riskguard.strategy.mapper.RiskStrategyRuleMapper;
import com.riskguard.strategy.mapper.RiskStrategyVersionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RiskStrategyService extends ServiceImpl<RiskStrategyMapper, RiskStrategy> {

    private final RiskStrategyRuleMapper strategyRuleMapper;
    private final RiskStrategyVersionMapper strategyVersionMapper;
    private final RiskRuleMapper riskRuleMapper;
    private final RiskRuleService riskRuleService;
    private final ObjectMapper objectMapper;
    private final RiskCacheService riskCacheService;

    public RiskStrategyService(RiskStrategyRuleMapper strategyRuleMapper,
                               RiskStrategyVersionMapper strategyVersionMapper,
                               RiskRuleMapper riskRuleMapper,
                               RiskRuleService riskRuleService,
                               ObjectMapper objectMapper,
                               RiskCacheService riskCacheService) {
        this.strategyRuleMapper = strategyRuleMapper;
        this.strategyVersionMapper = strategyVersionMapper;
        this.riskRuleMapper = riskRuleMapper;
        this.riskRuleService = riskRuleService;
        this.objectMapper = objectMapper;
        this.riskCacheService = riskCacheService;
    }

    @Transactional
    public RiskStrategyResponse createStrategy(RiskStrategyCreateRequest request) {
        RiskStrategy strategy = new RiskStrategy();
        strategy.setStrategyCode(request.strategyCode().trim());
        applyEditableFields(strategy, request.strategyName(), request.eventType(), request.grayRatio(), request.description());
        strategy.setStatus(PublishStatus.DRAFT.name());
        strategy.setVersion(0);
        save(strategy);
        return RiskStrategyResponse.from(strategy);
    }

    @Transactional
    public RiskStrategyResponse updateStrategy(Long id, RiskStrategyUpdateRequest request) {
        RiskStrategy strategy = requireStrategy(id);
        applyEditableFields(strategy, request.strategyName(), request.eventType(), request.grayRatio(), request.description());
        updateById(strategy);
        return RiskStrategyResponse.from(requireStrategy(id));
    }

    public RiskStrategyResponse getStrategy(Long id) {
        return RiskStrategyResponse.from(requireStrategy(id));
    }

    public PageResponse<RiskStrategyResponse> pageStrategies(long pageNo, long pageSize, String eventType, String status, String keyword) {
        var query = Wrappers.<RiskStrategy>lambdaQuery()
                .eq(StringUtils.hasText(eventType), RiskStrategy::getEventType, normalizeEventType(eventType))
                .eq(StringUtils.hasText(status), RiskStrategy::getStatus, normalizeStatus(status))
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(RiskStrategy::getStrategyCode, keyword)
                        .or()
                        .like(RiskStrategy::getStrategyName, keyword))
                .orderByDesc(RiskStrategy::getUpdatedAt)
                .orderByDesc(RiskStrategy::getId);

        Page<RiskStrategy> page = page(Page.of(pageNo, pageSize), query);
        return PageResponse.of(page.getRecords().stream().map(RiskStrategyResponse::from).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Transactional
    public RiskStrategyResponse enableStrategy(Long id) {
        return updateStatus(id, PublishStatus.ENABLED);
    }

    @Transactional
    public RiskStrategyResponse disableStrategy(Long id) {
        return updateStatus(id, PublishStatus.DISABLED);
    }

    @Transactional
    public StrategyRuleResponse bindRule(Long strategyId, StrategyRuleBindRequest request) {
        RiskStrategy strategy = requireStrategy(strategyId);
        RiskRule rule = riskRuleService.requirePublishedRule(request.ruleId());
        if (!strategy.getEventType().equals(rule.getEventType())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Rule eventType must match strategy eventType");
        }

        Integer ruleVersion = request.ruleVersion() == null ? rule.getVersion() : request.ruleVersion();
        riskRuleService.requireRuleVersion(rule.getId(), ruleVersion);

        RiskStrategyRule relation = strategyRuleMapper.selectOne(Wrappers.<RiskStrategyRule>lambdaQuery()
                .eq(RiskStrategyRule::getStrategyId, strategyId)
                .eq(RiskStrategyRule::getRuleId, rule.getId()));
        if (relation == null) {
            relation = new RiskStrategyRule();
            relation.setStrategyId(strategyId);
            relation.setRuleId(rule.getId());
        }
        relation.setRuleVersion(ruleVersion);
        relation.setExecuteOrder(request.executeOrder() == null ? 100 : request.executeOrder());
        relation.setEnabled(request.enabled() == null || request.enabled());

        if (relation.getId() == null) {
            strategyRuleMapper.insert(relation);
        } else {
            strategyRuleMapper.updateById(relation);
        }
        riskCacheService.evictActiveStrategy(strategy.getEventType());
        return StrategyRuleResponse.from(relation, rule);
    }

    @Transactional
    public void removeRule(Long strategyId, Long ruleId) {
        RiskStrategy strategy = requireStrategy(strategyId);
        strategyRuleMapper.delete(Wrappers.<RiskStrategyRule>lambdaQuery()
                .eq(RiskStrategyRule::getStrategyId, strategyId)
                .eq(RiskStrategyRule::getRuleId, ruleId));
        riskCacheService.evictActiveStrategy(strategy.getEventType());
    }

    @Transactional
    public List<StrategyRuleResponse> updateRuleOrder(Long strategyId, StrategyRuleOrderRequest request) {
        RiskStrategy strategy = requireStrategy(strategyId);
        Map<Long, RiskStrategyRule> relations = strategyRuleMapper.selectList(Wrappers.<RiskStrategyRule>lambdaQuery()
                        .eq(RiskStrategyRule::getStrategyId, strategyId))
                .stream()
                .collect(Collectors.toMap(RiskStrategyRule::getRuleId, Function.identity()));

        for (StrategyRuleOrderRequest.Item item : request.items()) {
            RiskStrategyRule relation = relations.get(item.ruleId());
            if (relation == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "Strategy rule relation not found: ruleId=" + item.ruleId());
            }
            relation.setExecuteOrder(item.executeOrder());
            strategyRuleMapper.updateById(relation);
        }
        riskCacheService.evictActiveStrategy(strategy.getEventType());
        return listRules(strategyId);
    }

    public List<StrategyRuleResponse> listRules(Long strategyId) {
        requireStrategy(strategyId);
        List<RiskStrategyRule> relations = strategyRuleMapper.selectList(Wrappers.<RiskStrategyRule>lambdaQuery()
                .eq(RiskStrategyRule::getStrategyId, strategyId)
                .orderByAsc(RiskStrategyRule::getExecuteOrder)
                .orderByAsc(RiskStrategyRule::getId));
        Map<Long, RiskRule> rules = riskRuleMapper.selectBatchIds(relations.stream().map(RiskStrategyRule::getRuleId).toList())
                .stream()
                .collect(Collectors.toMap(RiskRule::getId, Function.identity()));
        return relations.stream()
                .map(relation -> StrategyRuleResponse.from(relation, rules.get(relation.getRuleId())))
                .toList();
    }

    @Transactional
    public StrategyVersionResponse publishStrategy(Long id, StrategyPublishRequest request) {
        RiskStrategy strategy = requireStrategy(id);
        List<RiskStrategyRule> relations = strategyRuleMapper.selectList(Wrappers.<RiskStrategyRule>lambdaQuery()
                .eq(RiskStrategyRule::getStrategyId, id)
                .eq(RiskStrategyRule::getEnabled, true)
                .orderByAsc(RiskStrategyRule::getExecuteOrder)
                .orderByAsc(RiskStrategyRule::getId));
        if (relations.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Strategy must bind at least one enabled rule before publishing");
        }

        List<StrategyRuleSnapshot> snapshots = buildRuleSnapshots(strategy, relations);
        String snapshotJson = toJson(snapshots);
        int nextVersion = strategy.getVersion() == null ? 1 : strategy.getVersion() + 1;

        RiskStrategyVersion version = new RiskStrategyVersion();
        version.setStrategyId(strategy.getId());
        version.setVersion(nextVersion);
        version.setRuleSnapshot(snapshotJson);
        version.setGrayRatio(strategy.getGrayRatio());
        version.setStatus(PublishStatus.ENABLED.name());
        version.setPublishNote(request == null ? null : request.publishNote());
        strategyVersionMapper.insert(version);

        strategy.setVersion(nextVersion);
        strategy.setStatus(PublishStatus.ENABLED.name());
        updateById(strategy);

        StrategyVersionResponse response = StrategyVersionResponse.from(version);
        riskCacheService.putStrategyVersion(strategy.getId(), nextVersion, response);
        riskCacheService.putActiveStrategy(strategy.getEventType(), response);
        return response;
    }

    @Transactional
    public StrategyVersionResponse rollbackStrategy(Long id, StrategyRollbackRequest request) {
        RiskStrategy strategy = requireStrategy(id);
        RiskStrategyVersion version = requireStrategyVersion(id, request.version());
        strategy.setVersion(version.getVersion());
        strategy.setStatus(PublishStatus.ENABLED.name());
        updateById(strategy);

        StrategyVersionResponse response = StrategyVersionResponse.from(version);
        riskCacheService.putStrategyVersion(strategy.getId(), version.getVersion(), response);
        riskCacheService.putActiveStrategy(strategy.getEventType(), response);
        return response;
    }

    public List<StrategyVersionResponse> listVersions(Long id) {
        requireStrategy(id);
        return strategyVersionMapper.selectList(Wrappers.<RiskStrategyVersion>lambdaQuery()
                        .eq(RiskStrategyVersion::getStrategyId, id)
                        .orderByDesc(RiskStrategyVersion::getVersion))
                .stream()
                .map(StrategyVersionResponse::from)
                .toList();
    }

    private RiskStrategyResponse updateStatus(Long id, PublishStatus status) {
        RiskStrategy strategy = requireStrategy(id);
        strategy.setStatus(status.name());
        updateById(strategy);
        return RiskStrategyResponse.from(requireStrategy(id));
    }

    private RiskStrategy requireStrategy(Long id) {
        RiskStrategy strategy = getById(id);
        if (strategy == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Risk strategy not found: " + id);
        }
        return strategy;
    }

    private RiskStrategyVersion requireStrategyVersion(Long strategyId, Integer version) {
        RiskStrategyVersion strategyVersion = strategyVersionMapper.selectOne(Wrappers.<RiskStrategyVersion>lambdaQuery()
                .eq(RiskStrategyVersion::getStrategyId, strategyId)
                .eq(RiskStrategyVersion::getVersion, version));
        if (strategyVersion == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Strategy version not found: strategyId=" + strategyId + ", version=" + version);
        }
        return strategyVersion;
    }

    private List<StrategyRuleSnapshot> buildRuleSnapshots(RiskStrategy strategy, List<RiskStrategyRule> relations) {
        return relations.stream()
                .map(relation -> {
                    RiskRule rule = riskRuleMapper.selectById(relation.getRuleId());
                    if (rule == null || !strategy.getEventType().equals(rule.getEventType())) {
                        throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Rule is invalid for strategy: " + relation.getRuleId());
                    }
                    RiskRuleVersion version = riskRuleService.requireRuleVersion(relation.getRuleId(), relation.getRuleVersion());
                    return new StrategyRuleSnapshot(
                            rule.getId(),
                            rule.getRuleCode(),
                            rule.getRuleName(),
                            rule.getEventType(),
                            version.getVersion(),
                            version.getExpressionText(),
                            version.getScore(),
                            version.getAction(),
                            version.getPriority(),
                            relation.getExecuteOrder()
                    );
                })
                .toList();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to serialize strategy snapshot");
        }
    }

    private void applyEditableFields(RiskStrategy strategy, String strategyName, String eventType,
                                     Integer grayRatio, String description) {
        strategy.setStrategyName(strategyName.trim());
        strategy.setEventType(EnumUtils.requireName(EventType.class, eventType, "eventType"));
        strategy.setGrayRatio(grayRatio == null ? 100 : grayRatio);
        strategy.setDescription(description);
    }

    private String normalizeEventType(String eventType) {
        return StringUtils.hasText(eventType) ? EnumUtils.requireName(EventType.class, eventType, "eventType") : null;
    }

    private String normalizeStatus(String status) {
        return StringUtils.hasText(status) ? EnumUtils.requireName(PublishStatus.class, status, "status") : null;
    }
}
