package com.envforge.controlapi.template;

import java.util.UUID;

import com.envforge.controlapi.environment.EnvironmentTemplate;

public record TemplateResponse(
    UUID id,
    EnvironmentTemplate code,
    String displayName,
    String imageRepository,
    String defaultImageVersion
) {
    public static TemplateResponse from(
        TemplateEntity template
    ) {
        return new TemplateResponse(
            template.getId(),
            template.getCode(),
            template.getDisplayName(),
            template.getImageRepository(),
            template.getDefaultImageVersion()
        );
    }
}