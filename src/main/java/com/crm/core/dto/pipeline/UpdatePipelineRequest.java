package com.crm.core.dto.pipeline;

import com.crm.core.entity.enums.LostReasonMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdatePipelineRequest(
        @NotBlank String name,
        @NotNull LostReasonMode lostReasonMode
) {}
