package com.envforge.controlapi.template;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateService {

    private final TemplateRepository templateRepository;

    public TemplateService(
        TemplateRepository templateRepository
    ) {
        this.templateRepository = templateRepository;
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> findActiveTemplates() {
        return templateRepository
            .findAllByActiveTrueOrderByDisplayNameAsc()
            .stream()
            .map(TemplateResponse::from)
            .toList();
    }
}