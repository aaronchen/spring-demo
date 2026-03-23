package cc.desuka.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SavedViewRequest(
        @NotBlank(message = "{savedView.name.notBlank}") @Size(max = 100) String name,
        @NotBlank(message = "{savedView.filters.notBlank}") @Size(max = 2000) String filters) {}
