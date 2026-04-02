package cc.desuka.demo.service;

import cc.desuka.demo.audit.AuditDetails;
import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.audit.AuditField;
import cc.desuka.demo.model.Project;
import cc.desuka.demo.model.Sprint;
import cc.desuka.demo.repository.SprintRepository;
import cc.desuka.demo.security.SecurityUtils;
import cc.desuka.demo.util.Messages;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SprintService {

    private final SprintRepository sprintRepository;
    private final TaskService taskService;
    private final SprintQueryService sprintQueryService;
    private final ProjectQueryService projectQueryService;
    private final ApplicationEventPublisher eventPublisher;
    private final Messages messages;

    public SprintService(
            SprintRepository sprintRepository,
            TaskService taskService,
            SprintQueryService sprintQueryService,
            ProjectQueryService projectQueryService,
            ApplicationEventPublisher eventPublisher,
            Messages messages) {
        this.sprintRepository = sprintRepository;
        this.taskService = taskService;
        this.sprintQueryService = sprintQueryService;
        this.projectQueryService = projectQueryService;
        this.eventPublisher = eventPublisher;
        this.messages = messages;
    }

    public Sprint createSprint(Long projectId, Sprint sprintDetails) {
        Project project = projectQueryService.getProjectById(projectId);
        validateDates(sprintDetails);
        validateNoOverlap(projectId, 0L, sprintDetails);

        sprintDetails.setProject(project);
        Sprint saved = sprintRepository.save(sprintDetails);

        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.SPRINT_CREATED,
                        Sprint.class,
                        saved.getId(),
                        SecurityUtils.getCurrentPrincipal(),
                        AuditDetails.toJson(saved.toAuditSnapshot())));

        return saved;
    }

    public Sprint updateSprint(Long id, Sprint sprintDetails) {
        Sprint sprint = sprintQueryService.getSprintById(id);

        Map<String, AuditField> before = sprint.toAuditSnapshot();

        sprint.setName(sprintDetails.getName());
        sprint.setGoal(sprintDetails.getGoal());
        sprint.setStartDate(sprintDetails.getStartDate());
        sprint.setEndDate(sprintDetails.getEndDate());
        validateDates(sprint);
        validateNoOverlap(sprint.getProject().getId(), id, sprint);

        Sprint saved = sprintRepository.save(sprint);

        Map<String, Object> changes = AuditDetails.diff(before, saved.toAuditSnapshot());
        if (!changes.isEmpty()) {
            eventPublisher.publishEvent(
                    new AuditEvent(
                            AuditEvent.SPRINT_UPDATED,
                            Sprint.class,
                            saved.getId(),
                            SecurityUtils.getCurrentPrincipal(),
                            AuditDetails.toJson(changes)));
        }

        return saved;
    }

    public void clearSprintAssignments(Long projectId) {
        taskService.clearSprintAssignments(projectId);
    }

    public void deleteSprint(Long id) {
        Sprint sprint = sprintQueryService.getSprintById(id);
        String snapshot = AuditDetails.toJson(sprint.toAuditSnapshot());

        taskService.clearSprintFromTasks(id);

        sprintRepository.delete(sprint);

        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.SPRINT_DELETED,
                        Sprint.class,
                        id,
                        SecurityUtils.getCurrentPrincipal(),
                        snapshot));
    }

    private void validateDates(Sprint sprint) {
        if (sprint.getEndDate() != null
                && sprint.getStartDate() != null
                && !sprint.getEndDate().isAfter(sprint.getStartDate())) {
            throw new IllegalArgumentException(messages.get("sprint.error.endBeforeStart"));
        }
    }

    private void validateNoOverlap(Long projectId, Long excludeId, Sprint sprint) {
        if (sprintRepository.existsOverlapping(
                projectId, excludeId, sprint.getStartDate(), sprint.getEndDate())) {
            throw new IllegalArgumentException(messages.get("sprint.error.overlapping"));
        }
    }
}
