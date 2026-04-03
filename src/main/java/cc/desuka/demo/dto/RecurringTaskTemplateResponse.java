package cc.desuka.demo.dto;

import cc.desuka.demo.model.Priority;
import cc.desuka.demo.model.Recurrence;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class RecurringTaskTemplateResponse {

    private Long id;
    private String title;
    private String description;
    private Priority priority;
    private Short effort;
    private Recurrence recurrence;
    private Short dayOfWeek;
    private Short dayOfMonth;
    private Short dueDaysAfter;
    private LocalDate nextRunDate;
    private LocalDate endDate;
    private boolean enabled;
    private LocalDateTime lastGeneratedAt;
    private LocalDateTime createdAt;
    private UUID assigneeId;
    private String assigneeName;
    private String createdByName;
    private List<TagResponse> tags;
}
