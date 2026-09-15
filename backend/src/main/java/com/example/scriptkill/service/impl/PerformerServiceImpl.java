package com.example.scriptkill.service.impl;

import com.example.scriptkill.dto.request.PerformerCreateRequest;
import com.example.scriptkill.entity.Performer;
import com.example.scriptkill.exception.BusinessException;
import com.example.scriptkill.repository.PerformerRepository;
import com.example.scriptkill.repository.SessionAssignmentRepository;
import com.example.scriptkill.service.PerformerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PerformerServiceImpl implements PerformerService {

    @Autowired
    private PerformerRepository performerRepository;

    @Autowired
    private SessionAssignmentRepository sessionAssignmentRepository;

    @Override
    public List<Performer> listAll() {
        return performerRepository.findAll();
    }

    @Override
    @Transactional
    public Performer create(PerformerCreateRequest request) {
        String name = request.getPerformerName() != null ? request.getPerformerName().trim() : "";
        if (name.isEmpty()) {
            throw new BusinessException("请填写演职人员姓名");
        }
        performerRepository.findByPerformerName(name).ifPresent(existing -> {
            throw new BusinessException("演职人员「" + name + "」已在名册中，请勿重复添加");
        });
        Performer performer = new Performer();
        performer.setPerformerName(name);
        try {
            return performerRepository.saveAndFlush(performer);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("演职人员「" + name + "」已在名册中，请勿重复添加");
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Performer performer = performerRepository.findById(id)
                .orElseThrow(() -> new BusinessException("演职人员不存在，请刷新后重试"));
        // 有排班在身的人员不许从名册删除，避免历史场次的排班断链
        if (sessionAssignmentRepository.existsByPerformerId(id)) {
            throw new BusinessException("「" + performer.getPerformerName() + "」已有场次排班，请先在相关场次撤下后再删除");
        }
        performerRepository.deleteById(id);
    }
}
