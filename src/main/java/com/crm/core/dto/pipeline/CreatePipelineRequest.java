package com.crm.core.dto.pipeline;

import com.crm.core.entity.enums.LostReasonMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Новая воронка: этапы по порядку и (необязательно) причины проигрыша. */
public record CreatePipelineRequest(
        @NotBlank String name,
        LostReasonMode lostReasonMode,
        @NotEmpty List<@NotBlank String> stages,
        List<@NotBlank String> lostReasons
) {}
