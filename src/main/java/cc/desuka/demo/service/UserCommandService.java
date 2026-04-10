package cc.desuka.demo.service;

import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.PinnedItemRepository;
import cc.desuka.demo.repository.ProjectMemberRepository;
import cc.desuka.demo.repository.RecurringTaskTemplateRepository;
import cc.desuka.demo.repository.UserPreferenceRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cross-cutting user write operations needed by {@link UserService}. Extracted to break circular
 * dependencies — same pattern as {@link TaskCommandService}.
 *
 * <p>Accesses repositories directly for domains whose write services depend on {@code UserService}
 * ({@code UserPreferenceService}, {@code RecurringTaskTemplateService}, {@code ProjectService}).
 * Other domains delegate through their services (no cycle).
 */
@Service
@Transactional
public class UserCommandService {

    private final PinnedItemRepository pinnedItemRepository;
    private final ProjectMemberRepository memberRepository;
    private final RecurringTaskTemplateRepository recurringTaskTemplateRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final NotificationService notificationService;
    private final RecentViewService recentViewService;
    private final SavedViewService savedViewService;
    private final TaskCommandService taskCommandService;

    public UserCommandService(
            PinnedItemRepository pinnedItemRepository,
            ProjectMemberRepository memberRepository,
            RecurringTaskTemplateRepository recurringTaskTemplateRepository,
            UserPreferenceRepository userPreferenceRepository,
            NotificationService notificationService,
            RecentViewService recentViewService,
            SavedViewService savedViewService,
            TaskCommandService taskCommandService) {
        this.pinnedItemRepository = pinnedItemRepository;
        this.memberRepository = memberRepository;
        this.recurringTaskTemplateRepository = recurringTaskTemplateRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.notificationService = notificationService;
        this.recentViewService = recentViewService;
        this.savedViewService = savedViewService;
        this.taskCommandService = taskCommandService;
    }

    public long countRecurringTemplatesCreatedBy(UUID userId) {
        return recurringTaskTemplateRepository.countByCreatedById(userId);
    }

    public void cleanupBeforeDeletion(User user) {
        UUID id = user.getId();

        taskCommandService.unassignTasks(user);
        notificationService.nullActorByUserId(id);
        recurringTaskTemplateRepository.nullAssigneeByUserId(id);

        notificationService.clearAll(id);
        memberRepository.deleteByUserId(id);
        pinnedItemRepository.deleteByUserId(id);
        recentViewService.deleteByUserId(id);
        savedViewService.deleteByUserId(id);
        userPreferenceRepository.deleteByUserId(id);
    }
}
