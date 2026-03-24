package cc.desuka.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SavedViewRequest(
        @NotBlank(message = "{savedView.name.notBlank}") @Size(max = 100) String name,
        @NotNull @Valid SavedViewData data) {}
