package cc.desuka.demo.service;

import cc.desuka.demo.audit.AuditDetails;
import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.model.RecurringTaskTemplate;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.repository.RecurringTaskTemplateRepository;
import cc.desuka.demo.repository.TaskRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RecurringTaskGenerationService {

    private static final Logger log = LoggerFactory.getLogger(RecurringTaskGenerationService.class);

    private final RecurringTaskTemplateRepository templateRepository;
    private final TaskRepository taskRepository;
    private final ApplicationEventPublisher eventPublisher;

    public RecurringTaskGenerationService(
            RecurringTaskTemplateRepository templateRepository,
            TaskRepository taskRepository,
            ApplicationEventPublisher eventPublisher) {
        this.templateRepository = templateRepository;
        this.taskRepository = taskRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Generates a task from a specific template immediately, regardless of nextRunDate. Advances
     * the template's nextRunDate and lastGeneratedAt as if it ran on schedule. Uses the provided
     * principal for audit (the user who triggered the generation).
     */
    public void generateFromTemplate(RecurringTaskTemplate template, String principal) {
        generateTask(template, LocalDate.now(), principal);
    }

    /**
     * Generates tasks for all due recurring templates. Called by the scheduled job. If the app was
     * down and missed dates, advances to the next future date without generating multiple tasks.
     */
    public int generateDueTasks() {
        LocalDate today = LocalDate.now();
        List<RecurringTaskTemplate> dueTemplates = templateRepository.findDueTemplates(today);
        int generated = 0;

        for (RecurringTaskTemplate template : dueTemplates) {
            try {
                generateTask(template, today);
                generated++;
            } catch (Exception e) {
                log.error(
                        "Failed to generate task from template {} ({}): {}",
                        template.getId(),
                        template.getTitle(),
                        e.getMessage());
            }
        }

        if (generated > 0) {
            log.info("Generated {} recurring tasks", generated);
        }

        return generated;
    }

    private void generateTask(RecurringTaskTemplate template, LocalDate today) {
        generateTask(template, today, template.getCreatedBy().getEmail());
    }

    private void generateTask(
            RecurringTaskTemplate template, LocalDate today, String auditPrincipal) {
        Task task = new Task();
        task.setTitle(template.getTitle());
        task.setDescription(template.getDescription());
        task.setPriority(template.getPriority());
        task.setEffort(template.getEffort());
        task.setStatus(TaskStatus.OPEN);
        task.setProject(template.getProject());
        task.setTemplate(template);

        // Copy tags
        if (template.getTags() != null && !template.getTags().isEmpty()) {
            task.setTags(new LinkedHashSet<>(template.getTags()));
        }

        // Assign user only if still enabled and a project member
        if (template.getAssignee() != null && template.getAssignee().isEnabled()) {
            task.setUser(template.getAssignee());
        }

        // Set due date relative to generation date
        if (template.getDueDaysAfter() != null) {
            task.setDueDate(today.plusDays(template.getDueDaysAfter()));
        }

        Task saved = taskRepository.save(task);

        // Audit the generated task
        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.TASK_CREATED,
                        Task.class,
                        saved.getId(),
                        auditPrincipal,
                        AuditDetails.toJson(saved.toAuditSnapshot())));

        // Advance the template to the next run date (skip missed dates)
        LocalDate nextRun = template.getNextRunDate();
        while (!nextRun.isAfter(today)) {
            nextRun = template.calculateNextRunDate(nextRun);
        }
        template.setNextRunDate(nextRun);
        template.setLastGeneratedAt(LocalDateTime.now());

        // Auto-disable if end date has been reached
        if (template.getEndDate() != null && nextRun.isAfter(template.getEndDate())) {
            template.setEnabled(false);
        }

        templateRepository.save(template);
    }
}
