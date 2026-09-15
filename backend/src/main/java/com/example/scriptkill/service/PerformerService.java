package com.example.scriptkill.service;

import com.example.scriptkill.dto.request.PerformerCreateRequest;
import com.example.scriptkill.entity.Performer;

import java.util.List;

public interface PerformerService {

    List<Performer> listAll();

    Performer create(PerformerCreateRequest request);

    void delete(Long id);
}
