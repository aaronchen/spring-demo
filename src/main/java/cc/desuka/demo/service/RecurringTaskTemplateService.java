package cc.desuka.demo.service;

import cc.desuka.demo.audit.AuditDetails;
import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.dto.RecurringTaskTemplateRequest;
import cc.desuka.demo.model.Project;
import cc.desuka.demo.model.RecurringTaskTemplate;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.RecurringTaskTemplateRepository;
import cc.desuka.demo.security.SecurityUtils;
import cc.desuka.demo.util.Messages;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RecurringTaskTemplateService {

    private final RecurringTaskTemplateRepository templateRepository;
    private final TaskService taskService;
    private final ProjectQueryService projectQueryService;
    private final TagService tagService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final Messages messages;

    public RecurringTaskTemplateService(
            RecurringTaskTemplateRepository templateRepository,
            TaskService taskService,
            ProjectQueryService projectQueryService,
            TagService tagService,
            UserService userService,
            ApplicationEventPublisher eventPublisher,
            Messages messages) {
        this.templateRepository = templateRepository;
        this.taskService = taskService;
        this.projectQueryService = projectQueryService;
        this.tagService = tagService;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
        this.messages = messages;
    }

    @Transactional(readOnly = true)
    public List<RecurringTaskTemplate> getTemplatesByProject(Long projectId) {
        return templateRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    @Transactional(readOnly = true)
    public RecurringTaskTemplate getTemplateById(Long id) {
        return templateRepository
                .findWithDetailsById(id)
                .orElseThrow(() -> new EntityNotFoundException(messages.get("recurring.notFound")));
    }

    public RecurringTaskTemplate createTemplate(
            Long projectId, RecurringTaskTemplateRequest request) {
        Project project = projectQueryService.getProjectById(projectId);
        validateNotSprintEnabled(project);
        validateEndDate(request);

        RecurringTaskTemplate template = new RecurringTaskTemplate();
        applyRequest(template, request);
        template.setProject(project);
        template.setCreatedBy(SecurityUtils.getCurrentUser());

        RecurringTaskTemplate saved = templateRepository.save(template);

        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.RECURRING_TEMPLATE_CREATED,
                        RecurringTaskTemplate.class,
                        saved.getId(),
                        SecurityUtils.getCurrentPrincipal(),
                        AuditDetails.toJson(saved.toAuditSnapshot())));

        return saved;
    }

    public RecurringTaskTemplate updateTemplate(Long id, RecurringTaskTemplateRequest request) {
        RecurringTaskTemplate template = getTemplateById(id);
        validateEndDate(request);

        Map<String, Object> before = template.toAuditSnapshot();

        applyRequest(template, request);
        RecurringTaskTemplate saved = templateRepository.save(template);

        Map<String, Object> changes = AuditDetails.diff(before, saved.toAuditSnapshot());
        if (!changes.isEmpty()) {
            eventPublisher.publishEvent(
                    new AuditEvent(
                            AuditEvent.RECURRING_TEMPLATE_UPDATED,
                            RecurringTaskTemplate.class,
                            saved.getId(),
                            SecurityUtils.getCurrentPrincipal(),
                            AuditDetails.toJson(changes)));
        }

        return saved;
    }

    public void toggleEnabled(Long id) {
        RecurringTaskTemplate template = getTemplateById(id);
        Map<String, Object> before = template.toAuditSnapshot();

        template.setEnabled(!template.isEnabled());
        RecurringTaskTemplate saved = templateRepository.save(template);

        Map<String, Object> changes = AuditDetails.diff(before, saved.toAuditSnapshot());
        if (!changes.isEmpty()) {
            eventPublisher.publishEvent(
                    new AuditEvent(
                            AuditEvent.RECURRING_TEMPLATE_UPDATED,
                            RecurringTaskTemplate.class,
                            saved.getId(),
                            SecurityUtils.getCurrentPrincipal(),
                            AuditDetails.toJson(changes)));
        }
    }

    /**
     * Disables all enabled recurring templates for a project. Called when sprints are enabled on
     * the project, since recurring tasks and sprints are mutually exclusive.
     */
    public int disableAllForProject(Long projectId) {
        return templateRepository.disableAllByProjectId(projectId);
    }

    public void deleteTemplate(Long id) {
        RecurringTaskTemplate template = getTemplateById(id);
        String snapshot = AuditDetails.toJson(template.toAuditSnapshot());

        taskService.clearTemplateFromTasks(id);

        templateRepository.delete(template);

        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.RECURRING_TEMPLATE_DELETED,
                        RecurringTaskTemplate.class,
                        id,
                        SecurityUtils.getCurrentPrincipal(),
                        snapshot));
    }

    private void applyRequest(
            RecurringTaskTemplate template, RecurringTaskTemplateRequest request) {
        template.setTitle(request.getTitle());
        template.setDescription(request.getDescription());
        template.setPriority(request.getPriority());
        template.setEffort(request.getEffort());
        template.setRecurrence(request.getRecurrence());
        template.setDayOfWeek(request.getDayOfWeek());
        template.setDayOfMonth(request.getDayOfMonth());
        template.setDueDaysAfter(request.getDueDaysAfter());
        template.setNextRunDate(request.getNextRunDate());
        template.setEndDate(request.getEndDate());
        template.setTags(tagService.findAllByIds(request.getTagIds()));
        User assignee =
                request.getAssigneeId() != null
                        ? userService.findUserById(request.getAssigneeId())
                        : null;
        template.setAssignee(assignee);
    }

    private void validateNotSprintEnabled(Project project) {
        if (project.isSprintEnabled()) {
            throw new IllegalArgumentException(messages.get("recurring.error.sprintEnabled"));
        }
    }

    private void validateEndDate(RecurringTaskTemplateRequest request) {
        if (request.getEndDate() != null
                && request.getNextRunDate() != null
                && request.getEndDate().isBefore(request.getNextRunDate())) {
            throw new IllegalArgumentException(messages.get("recurring.error.endBeforeStart"));
        }
    }
}
