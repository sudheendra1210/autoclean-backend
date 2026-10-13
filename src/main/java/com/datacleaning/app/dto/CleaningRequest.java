package com.datacleaning.app.dto;
import jakarta.validation.constraints.NotBlank;
public record CleaningRequest(@NotBlank String session_id, @NotBlank String instruction) {}
