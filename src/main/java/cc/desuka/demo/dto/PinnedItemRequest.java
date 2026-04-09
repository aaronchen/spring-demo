package cc.desuka.demo.dto;

import jakarta.validation.constraints.NotBlank;

public record PinnedItemRequest(
        @NotBlank String entityType, @NotBlank String entityId, @NotBlank String entityTitle) {}
