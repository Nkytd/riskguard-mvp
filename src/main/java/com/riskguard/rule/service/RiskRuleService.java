package com.riskguard.rule.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.googlecode.aviator.AviatorEvaluator;
import com.riskguard.common.api.ErrorCode;
import com.riskguard.common.api.PageResponse;
import com.riskguard.common.cache.RiskCacheService;
import com.riskguard.common.enums.EventType;
import com.riskguard.common.enums.PublishStatus;
import com.riskguard.common.enums.RuleAction;
import com.riskguard.common.exception.BusinessException;
import com.riskguard.common.utils.EnumUtils;
import com.riskguard.rule.dto.ExpressionValidateResponse;
import com.riskguard.rule.dto.RiskRuleCreateRequest;
import com.riskguard.rule.dto.RiskRuleResponse;
import com.riskguard.rule.dto.RiskRuleUpdateRequest;
import com.riskguard.rule.dto.RulePublishRequest;
import com.riskguard.rule.dto.RuleVersionResponse;
import com.riskguard.rule.entity.RiskRule;
import com.riskguard.rule.entity.RiskRuleVersion;
import com.riskguard.rule.mapper.RiskRuleMapper;
import com.riskguard.rule.mapper.RiskRuleVersionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class RiskRuleService extends ServiceImpl<RiskRuleMapper, RiskRule> {

    private final RiskRuleVersionMapper ruleVersionMapper;
    private final RiskCacheService riskCacheService;

    public RiskRuleService(RiskRuleVersionMapper ruleVersionMapper, RiskCacheService riskCacheService) {
        this.ruleVersionMapper = ruleVersionMapper;
        this.riskCacheService = riskCacheService;
    }

    @Transactional
    public RiskRuleResponse createRule(RiskRuleCreateRequest request) {
        RiskRule rule = new RiskRule();
        rule.setRuleCode(request.ruleCode().trim());
        applyEditableFields(rule, request.ruleName(), request.eventType(), request.expression(),
                request.score(), request.action(), request.priority(), request.description());
        rule.setStatus(PublishStatus.DRAFT.name());
        rule.setVersion(0);
        save(rule);
        return RiskRuleResponse.from(rule);
    }

    @Transactional
    public RiskRuleResponse updateRule(Long id, RiskRuleUpdateRequest request) {
        RiskRule rule = requireRule(id);
        applyEditableFields(rule, request.ruleName(), request.eventType(), request.expression(),
                request.score(), request.action(), request.priority(), request.description());
        updateById(rule);
        return RiskRuleResponse.from(requireRule(id));
    }

    public RiskRuleResponse getRule(Long id) {
        return RiskRuleResponse.from(requireRule(id));
    }

    public PageResponse<RiskRuleResponse> pageRules(long pageNo, long pageSize, String eventType, String status, String keyword) {
        var query = Wrappers.<RiskRule>lambdaQuery()
                .eq(StringUtils.hasText(eventType), RiskRule::getEventType, normalizeEventType(eventType))
                .eq(StringUtils.hasText(status), RiskRule::getStatus, normalizeStatus(status))
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(RiskRule::getRuleCode, keyword)
                        .or()
                        .like(RiskRule::getRuleName, keyword))
                .orderByDesc(RiskRule::getUpdatedAt)
                .orderByDesc(RiskRule::getId);

        Page<RiskRule> page = page(Page.of(pageNo, pageSize), query);
        return PageResponse.of(page.getRecords().stream().map(RiskRuleResponse::from).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Transactional
    public RiskRuleResponse enableRule(Long id) {
        return updateStatus(id, PublishStatus.ENABLED);
    }

    @Transactional
    public RiskRuleResponse disableRule(Long id) {
        return updateStatus(id, PublishStatus.DISABLED);
    }

    @Transactional
    public RuleVersionResponse publishRule(Long id, RulePublishRequest request) {
        RiskRule rule = requireRule(id);
        validateExpressionOrThrow(rule.getExpressionText());

        int nextVersion = rule.getVersion() == null ? 1 : rule.getVersion() + 1;
        RiskRuleVersion version = new RiskRuleVersion();
        version.setRuleId(rule.getId());
        version.setVersion(nextVersion);
        version.setExpressionText(rule.getExpressionText());
        version.setScore(rule.getScore());
        version.setAction(rule.getAction());
        version.setPriority(rule.getPriority());
        version.setStatus(PublishStatus.ENABLED.name());
        version.setPublishNote(request == null ? null : request.publishNote());
        ruleVersionMapper.insert(version);

        rule.setVersion(nextVersion);
        rule.setStatus(PublishStatus.ENABLED.name());
        updateById(rule);

        riskCacheService.putRuleVersion(rule.getId(), nextVersion, RuleVersionResponse.from(version));
        return RuleVersionResponse.from(version);
    }

    public List<RuleVersionResponse> listRuleVersions(Long id) {
        requireRule(id);
        return ruleVersionMapper.selectList(Wrappers.<RiskRuleVersion>lambdaQuery()
                        .eq(RiskRuleVersion::getRuleId, id)
                        .orderByDesc(RiskRuleVersion::getVersion))
                .stream()
                .map(RuleVersionResponse::from)
                .toList();
    }

    public ExpressionValidateResponse validateExpression(String expression) {
        try {
            validateExpressionOrThrow(expression);
            return new ExpressionValidateResponse(true, "Expression is valid");
        } catch (BusinessException ex) {
            return new ExpressionValidateResponse(false, ex.getMessage());
        }
    }

    public RiskRule requirePublishedRule(Long id) {
        RiskRule rule = requireRule(id);
        if (!PublishStatus.ENABLED.name().equals(rule.getStatus()) || rule.getVersion() == null || rule.getVersion() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Risk rule must be published before binding: " + id);
        }
        return rule;
    }

    public RiskRuleVersion requireRuleVersion(Long ruleId, Integer version) {
        RiskRuleVersion ruleVersion = ruleVersionMapper.selectOne(Wrappers.<RiskRuleVersion>lambdaQuery()
                .eq(RiskRuleVersion::getRuleId, ruleId)
                .eq(RiskRuleVersion::getVersion, version));
        if (ruleVersion == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Rule version not found: ruleId=" + ruleId + ", version=" + version);
        }
        return ruleVersion;
    }

    private RiskRuleResponse updateStatus(Long id, PublishStatus status) {
        RiskRule rule = requireRule(id);
        rule.setStatus(status.name());
        updateById(rule);
        return RiskRuleResponse.from(requireRule(id));
    }

    private RiskRule requireRule(Long id) {
        RiskRule rule = getById(id);
        if (rule == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Risk rule not found: " + id);
        }
        return rule;
    }

    private void validateExpressionOrThrow(String expression) {
        if (!StringUtils.hasText(expression)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "expression must not be blank");
        }
        try {
            AviatorEvaluator.compile(expression, true);
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Invalid expression: " + ex.getMessage());
        }
    }

    private void applyEditableFields(RiskRule rule, String ruleName, String eventType, String expression,
                                     Integer score, String action, Integer priority, String description) {
        rule.setRuleName(ruleName.trim());
        rule.setEventType(EnumUtils.requireName(EventType.class, eventType, "eventType"));
        rule.setExpressionText(expression.trim());
        rule.setScore(score);
        rule.setAction(EnumUtils.requireName(RuleAction.class, action, "action"));
        rule.setPriority(priority == null ? 100 : priority);
        rule.setDescription(description);
    }

    private String normalizeEventType(String eventType) {
        return StringUtils.hasText(eventType) ? EnumUtils.requireName(EventType.class, eventType, "eventType") : null;
    }

    private String normalizeStatus(String status) {
        return StringUtils.hasText(status) ? EnumUtils.requireName(PublishStatus.class, status, "status") : null;
    }
}
