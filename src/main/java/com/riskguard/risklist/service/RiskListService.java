package com.riskguard.risklist.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.riskguard.common.api.ErrorCode;
import com.riskguard.common.api.PageResponse;
import com.riskguard.common.enums.ListEffectType;
import com.riskguard.common.enums.ListType;
import com.riskguard.common.enums.ObjectType;
import com.riskguard.common.enums.PublishStatus;
import com.riskguard.common.enums.RiskLevel;
import com.riskguard.common.exception.BusinessException;
import com.riskguard.common.utils.EnumUtils;
import com.riskguard.risklist.dto.RiskListCreateRequest;
import com.riskguard.risklist.dto.RiskListResponse;
import com.riskguard.risklist.dto.RiskListUpdateRequest;
import com.riskguard.risklist.entity.RiskList;
import com.riskguard.risklist.mapper.RiskListMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RiskListService extends ServiceImpl<RiskListMapper, RiskList> {

    @Transactional
    public RiskListResponse createRiskList(RiskListCreateRequest request) {
        RiskList riskList = new RiskList();
        riskList.setListType(EnumUtils.requireName(ListType.class, request.listType(), "listType"));
        riskList.setObjectType(EnumUtils.requireName(ObjectType.class, request.objectType(), "objectType"));
        riskList.setObjectValue(request.objectValue().trim());
        applyEditableFields(riskList, request.riskLevel(), request.effectType(), request.scoreDelta(),
                request.reason(), request.startTime(), request.endTime());
        riskList.setStatus(PublishStatus.ENABLED.name());
        save(riskList);
        return RiskListResponse.from(riskList);
    }

    @Transactional
    public RiskListResponse updateRiskList(Long id, RiskListUpdateRequest request) {
        RiskList riskList = requireRiskList(id);
        applyEditableFields(riskList, request.riskLevel(), request.effectType(), request.scoreDelta(),
                request.reason(), request.startTime(), request.endTime());
        updateById(riskList);
        return RiskListResponse.from(requireRiskList(id));
    }

    public RiskListResponse getRiskList(Long id) {
        return RiskListResponse.from(requireRiskList(id));
    }

    public PageResponse<RiskListResponse> pageRiskLists(long pageNo, long pageSize, String listType,
                                                        String objectType, String status, String keyword) {
        var query = Wrappers.<RiskList>lambdaQuery()
                .eq(StringUtils.hasText(listType), RiskList::getListType, normalizeListType(listType))
                .eq(StringUtils.hasText(objectType), RiskList::getObjectType, normalizeObjectType(objectType))
                .eq(StringUtils.hasText(status), RiskList::getStatus, normalizeStatus(status))
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(RiskList::getObjectValue, keyword)
                        .or()
                        .like(RiskList::getReason, keyword))
                .orderByDesc(RiskList::getUpdatedAt)
                .orderByDesc(RiskList::getId);

        Page<RiskList> page = page(Page.of(pageNo, pageSize), query);
        return PageResponse.of(page.getRecords().stream().map(RiskListResponse::from).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Transactional
    public RiskListResponse enableRiskList(Long id) {
        return updateStatus(id, PublishStatus.ENABLED);
    }

    @Transactional
    public RiskListResponse disableRiskList(Long id) {
        return updateStatus(id, PublishStatus.DISABLED);
    }

    @Transactional
    public void deleteRiskList(Long id) {
        requireRiskList(id);
        removeById(id);
    }

    private RiskListResponse updateStatus(Long id, PublishStatus status) {
        RiskList riskList = requireRiskList(id);
        riskList.setStatus(status.name());
        updateById(riskList);
        return RiskListResponse.from(requireRiskList(id));
    }

    private RiskList requireRiskList(Long id) {
        RiskList riskList = getById(id);
        if (riskList == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Risk list item not found: " + id);
        }
        return riskList;
    }

    private void applyEditableFields(RiskList riskList, String riskLevel, String effectType, Integer scoreDelta,
                                     String reason, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        riskList.setRiskLevel(normalizeRiskLevel(riskLevel));
        riskList.setEffectType(EnumUtils.requireName(ListEffectType.class, effectType, "effectType"));
        riskList.setScoreDelta(scoreDelta == null ? 0 : scoreDelta);
        riskList.setReason(reason);
        riskList.setStartTime(startTime);
        riskList.setEndTime(endTime);
    }

    private String normalizeListType(String listType) {
        return StringUtils.hasText(listType) ? EnumUtils.requireName(ListType.class, listType, "listType") : null;
    }

    private String normalizeObjectType(String objectType) {
        return StringUtils.hasText(objectType) ? EnumUtils.requireName(ObjectType.class, objectType, "objectType") : null;
    }

    private String normalizeStatus(String status) {
        return StringUtils.hasText(status) ? EnumUtils.requireName(PublishStatus.class, status, "status") : null;
    }

    private String normalizeRiskLevel(String riskLevel) {
        return StringUtils.hasText(riskLevel) ? EnumUtils.requireName(RiskLevel.class, riskLevel, "riskLevel") : null;
    }
}
