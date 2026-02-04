package com.project.reflash.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class AIFlashCardGenerationRequestDto {
    @NotEmpty(message="text is required")
    String text;

    String prompt;

    @NotNull(message="count is required")
    @Max(value = 200, message="card count should be less than 100")
    @Min(value = 1, message="card count should be greater than 1")
    Integer count;
}
