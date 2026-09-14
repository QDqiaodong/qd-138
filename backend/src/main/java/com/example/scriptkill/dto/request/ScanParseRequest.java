package com.example.scriptkill.dto.request;

import lombok.Data;

/**
 * 扫码解析请求：内容为扫码枪/摄像头读到的条码原始值
 */
@Data
public class ScanParseRequest {

    /** 条码原始内容（可能包含控制字符、AIM标识符或URL包装） */
    private String rawCode;
}
