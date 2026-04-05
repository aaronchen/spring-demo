package cc.desuka.demo.dto;

import java.util.List;
import java.util.UUID;

public record TaskUpdateCriteria(
        List<Long> tagIds,
        UUID assigneeId,
        Long expectedVersion,
        List<String> checklistTexts,
        List<Boolean> checklistChecked,
        List<UUID> blockedByIds,
        List<UUID> blocksIds) {

    public TaskUpdateCriteria(List<Long> tagIds, UUID assigneeId, Long expectedVersion) {
        this(tagIds, assigneeId, expectedVersion, null, null, null, null);
    }
}
