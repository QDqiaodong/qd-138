package com.example.scriptkill.service;

import com.example.scriptkill.dto.response.RolePropsResponse;
import com.example.scriptkill.dto.response.ScanParseResponse;
import com.example.scriptkill.dto.response.ScriptThemeDetailResponse;
import com.example.scriptkill.entity.Prop;

import java.util.List;
import java.util.Map;

public interface PropService {
    List<Prop> getAllProps();
    Prop getPropById(Long id);
    Prop getPropByCode(String code);
    ScanParseResponse parseScannedCode(String rawCode);
    Prop createProp(Prop prop);
    Prop updateProp(Long id, Prop prop);
    void deleteProp(Long id);
    List<Prop> getPropsByEra(String era);
    List<Prop> getPropsByType(String type);
    Map<String, List<String>> getEraStyleTemplate();
    void cacheEraStyleTemplate();
}