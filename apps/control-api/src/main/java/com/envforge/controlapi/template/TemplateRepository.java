package com.envforge.controlapi.template;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateRepository
    extends JpaRepository<TemplateEntity, UUID> {

    List<TemplateEntity>
        findAllByActiveTrueOrderByDisplayNameAsc();
}