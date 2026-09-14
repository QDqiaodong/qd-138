package com.example.scriptkill.dto.response;

import com.example.scriptkill.entity.Prop;
import lombok.Data;

/**
 * 扫码解析结果：归一化后的道具编号 + 档案是否已存在
 */
@Data
public class ScanParseResponse {

    /** 条码原始内容 */
    private String rawCode;

    /** 解析归一化后的道具编号 */
    private String propCode;

    /** 该编号是否已建档 */
    private boolean exists;

    /** 已建档时返回档案信息，未建档为 null */
    private Prop prop;
}
