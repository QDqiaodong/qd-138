package com.example.scriptkill.service;

import com.example.scriptkill.dto.request.PerformerCreateRequest;
import com.example.scriptkill.entity.Performer;

import java.util.List;

public interface PerformerService {

    List<Performer> listAll();

    Performer create(PerformerCreateRequest request);

    /** 标停演并落停演原因原文；未开演场次的名单里此人随即标为待换 */
    Performer suspend(Long id, String reason);

    /** 恢复演出：清掉停演标记和原因，各未开演场次的待换随即消失 */
    Performer resume(Long id);

    void delete(Long id);
}
