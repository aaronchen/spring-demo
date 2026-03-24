package cc.desuka.demo.service;

import cc.desuka.demo.config.UserPreferences;
import cc.desuka.demo.model.NotificationType;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.repository.NotificationRepository;
import cc.desuka.demo.util.Messages;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Centralized home for all {@code @Scheduled} jobs. Keeping scheduled methods in one place makes it
 * easy to find, audit, and adjust cron expressions.
 */
@Service
public class ScheduledTaskService {

    private final TaskQueryService taskQueryService;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final UserPreferenceService userPreferenceService;
    private final SettingService settingService;
    private final Messages messages;

    public ScheduledTaskService(
            TaskQueryService taskQueryService,
            NotificationService notificationService,
            NotificationRepository notificationRepository,
            UserPreferenceService userPreferenceService,
            SettingService settingService,
            Messages messages) {
        this.taskQueryService = taskQueryService;
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
        this.userPreferenceService = userPreferenceService;
        this.settingService = settingService;
        this.messages = messages;
    }

    /**
     * Sends due-date reminder notifications for tasks due tomorrow. Runs daily at 8:00 AM. Only
     * notifies users who have the preference enabled.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void sendDueReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Task> tasks = taskQueryService.getTasksDueOn(tomorrow);

        for (Task task : tasks) {
            if (task.getUser() == null) continue;

            UserPreferences prefs = userPreferenceService.load(task.getUser().getId());
            if (!prefs.isDueReminder()) continue;

            String message = messages.get("notification.task.dueReminder", task.getTitle());
            notificationService.create(
                    task.getUser(),
                    null,
                    NotificationType.TASK_DUE_REMINDER,
                    message,
                    "/tasks/" + task.getId());
        }
    }

    /**
     * Purges old notifications based on the admin-configured retention period. Runs daily at 3:00
     * AM.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeOldNotifications() {
        int purgeDays = settingService.load().getNotificationPurgeDays();
        notificationRepository.deleteByCreatedAtBefore(
                LocalDateTime.now().minus(purgeDays, ChronoUnit.DAYS));
    }
}
