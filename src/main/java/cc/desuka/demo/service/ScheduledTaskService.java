package cc.desuka.demo.service;

import cc.desuka.demo.config.UserPreferences;
import cc.desuka.demo.model.NotificationType;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.repository.NotificationRepository;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

/**
 * Centralized home for all {@code @Scheduled} jobs.
 * Keeping scheduled methods in one place makes it easy to find,
 * audit, and adjust cron expressions.
 */
@Service
public class ScheduledTaskService {

    private final TaskService taskService;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final UserPreferenceService userPreferenceService;
    private final SettingService settingService;
    private final MessageSource messageSource;

    public ScheduledTaskService(TaskService taskService,
                                NotificationService notificationService,
                                NotificationRepository notificationRepository,
                                UserPreferenceService userPreferenceService,
                                SettingService settingService,
                                MessageSource messageSource) {
        this.taskService = taskService;
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
        this.userPreferenceService = userPreferenceService;
        this.settingService = settingService;
        this.messageSource = messageSource;
    }

    /**
     * Sends due-date reminder notifications for tasks due tomorrow.
     * Runs daily at 8:00 AM. Only notifies users who have the preference enabled.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void sendDueReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Task> tasks = taskService.getTasksDueOn(tomorrow);

        for (Task task : tasks) {
            if (task.getUser() == null) continue;

            UserPreferences prefs = userPreferenceService.load(task.getUser().getId());
            if (!prefs.isDueReminder()) continue;

            String message = messageSource.getMessage("notification.task.dueReminder",
                    new Object[]{task.getTitle()}, Locale.getDefault());
            notificationService.create(task.getUser(), null, NotificationType.TASK_DUE_REMINDER,
                    message, "/tasks/" + task.getId());
        }
    }

    /**
     * Purges old notifications based on the admin-configured retention period.
     * Runs daily at 3:00 AM.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeOldNotifications() {
        int purgeDays = settingService.load().getNotificationPurgeDays();
        notificationRepository.deleteByCreatedAtBefore(
                LocalDateTime.now().minus(purgeDays, ChronoUnit.DAYS));
    }
}
