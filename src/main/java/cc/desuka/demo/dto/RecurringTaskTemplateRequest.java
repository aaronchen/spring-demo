package cc.desuka.demo.dto;

import cc.desuka.demo.model.Priority;
import cc.desuka.demo.model.Recurrence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class RecurringTaskTemplateRequest {

    @NotBlank(message = "{task.title.notBlank}")
    @Size(min = 1, max = 100, message = "{task.title.size}")
    private String title;

    @Size(max = 500, message = "{task.description.size}")
    private String description;

    private Priority priority = Priority.MEDIUM;

    private Short effort;

    @NotNull(message = "{recurring.recurrence.notNull}")
    private Recurrence recurrence;

    // 1=Monday .. 7=Sunday (for WEEKLY/BIWEEKLY)
    private Short dayOfWeek;

    // 1-31 (for MONTHLY)
    private Short dayOfMonth;

    // Days after generation to set due date; defaults to 0 (due same day)
    private Short dueDaysAfter = 0;

    @NotNull(message = "{recurring.nextRunDate.notNull}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate nextRunDate;

    // null = open-ended
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    private UUID assigneeId;

    private List<Long> tagIds;
}
