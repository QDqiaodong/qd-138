package com.example.scriptkill.service.impl;

import com.example.scriptkill.dto.response.CharacterReviewResponse;
import com.example.scriptkill.entity.CharacterRole;
import com.example.scriptkill.entity.Prop;
import com.example.scriptkill.entity.PropChangeRecord;
import com.example.scriptkill.entity.ScriptTheme;
import com.example.scriptkill.repository.CharacterRoleRepository;
import com.example.scriptkill.repository.PropChangeRecordRepository;
import com.example.scriptkill.repository.PropRepository;
import com.example.scriptkill.repository.ScriptThemeRepository;
import com.example.scriptkill.service.CharacterReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CharacterReviewServiceImpl implements CharacterReviewService {

    /** 角色已被删除（记录中角色ID已置空）时归入的汇总分组 */
    private static final long UNKNOWN_ROLE_KEY = 0L;

    @Autowired
    private PropChangeRecordRepository changeRecordRepository;

    @Autowired
    private PropRepository propRepository;

    @Autowired
    private CharacterRoleRepository characterRoleRepository;

    @Autowired
    private ScriptThemeRepository scriptThemeRepository;

    @Override
    public CharacterReviewResponse getCharacterReview(LocalDate date) {
        LocalDate reviewDate = date != null ? date : LocalDate.now();
        LocalDateTime start = reviewDate.atStartOfDay();
        LocalDateTime end = reviewDate.plusDays(1).atStartOfDay();
        List<PropChangeRecord> records =
                changeRecordRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(start, end);

        // 当天明细：逐条补齐道具/人物/剧本名称，未写原因的标记为未确认变更
        List<CharacterReviewResponse.ReviewRecord> details = new ArrayList<>();
        for (PropChangeRecord record : records) {
            Prop prop = propRepository.findById(record.getPropId()).orElse(null);
            CharacterRole role = record.getCharacterRoleId() != null
                    ? characterRoleRepository.findById(record.getCharacterRoleId()).orElse(null)
                    : null;
            ScriptTheme theme = record.getScriptThemeId() != null
                    ? scriptThemeRepository.findById(record.getScriptThemeId()).orElse(null)
                    : null;

            CharacterReviewResponse.ReviewRecord item = new CharacterReviewResponse.ReviewRecord();
            item.setId(record.getId());
            item.setPropId(record.getPropId());
            item.setPropCode(prop != null ? prop.getPropCode() : "");
            item.setPropName(prop != null ? prop.getPropName() : "");
            item.setRoleId(record.getCharacterRoleId());
            item.setRoleName(role != null ? role.getRoleName() : "未知人物");
            item.setThemeName(theme != null ? theme.getThemeName() : "");
            item.setChangeType(record.getChangeType());
            item.setBeforeValue(record.getBeforeValue());
            item.setAfterValue(record.getAfterValue());
            item.setChangeReason(record.getChangeReason());
            item.setUnconfirmed(isBlankReason(record.getChangeReason()));
            item.setOperator(record.getOperator());
            item.setCreatedAt(record.getCreatedAt());
            details.add(item);
        }

        // 回看名单直接从当天明细汇总，保证名单计数与明细条数始终一致
        Map<Long, CharacterReviewResponse.RoleSummary> summaryMap = new LinkedHashMap<>();
        int unconfirmedTotal = 0;
        for (CharacterReviewResponse.ReviewRecord item : details) {
            Long key = item.getRoleId() != null ? item.getRoleId() : UNKNOWN_ROLE_KEY;
            CharacterReviewResponse.RoleSummary summary = summaryMap.computeIfAbsent(key, k -> {
                CharacterReviewResponse.RoleSummary s = new CharacterReviewResponse.RoleSummary();
                s.setRoleId(item.getRoleId());
                s.setRoleName(item.getRoleName());
                s.setThemeName(item.getThemeName());
                return s;
            });
            if ("绑定".equals(item.getChangeType())) {
                summary.setBindCount(summary.getBindCount() + 1);
            } else if ("解绑".equals(item.getChangeType())) {
                summary.setUnbindCount(summary.getUnbindCount() + 1);
            } else if ("更换".equals(item.getChangeType())) {
                summary.setChangeCount(summary.getChangeCount() + 1);
            }
            summary.setTotalCount(summary.getTotalCount() + 1);
            if (item.isUnconfirmed()) {
                summary.setUnconfirmedCount(summary.getUnconfirmedCount() + 1);
                unconfirmedTotal++;
            }
        }

        // 名单按当天变更数降序，变更多的人物排在前面优先核对
        List<CharacterReviewResponse.RoleSummary> summary = new ArrayList<>(summaryMap.values());
        summary.sort((a, b) -> Integer.compare(b.getTotalCount(), a.getTotalCount()));

        CharacterReviewResponse response = new CharacterReviewResponse();
        response.setDate(reviewDate.toString());
        response.setRoleCount(summary.size());
        response.setTotalCount(details.size());
        response.setUnconfirmedCount(unconfirmedTotal);
        response.setSummary(summary);
        response.setDetails(details);
        return response;
    }

    private boolean isBlankReason(String reason) {
        return reason == null || reason.trim().isEmpty();
    }
}
