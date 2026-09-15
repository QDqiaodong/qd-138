package com.example.scriptkill.service;

import com.example.scriptkill.dto.response.CharacterReviewResponse;

import java.time.LocalDate;

public interface CharacterReviewService {
    /**
     * 人物回看：按人物和日期汇总当天道具变更，
     * 回看名单与当天明细同源统计，未填写原因的记为未确认变更。
     *
     * @param date 回看日期，为空时默认当天
     */
    CharacterReviewResponse getCharacterReview(LocalDate date);
}
