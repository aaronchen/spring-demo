package cc.desuka.demo.service;

import cc.desuka.demo.config.AppRoutesProperties;
import cc.desuka.demo.config.UserPreferences;
import cc.desuka.demo.model.NotificationType;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.repository.NotificationRepository;
import cc.desuka.demo.util.Messages;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Centralized home for all {@code @Scheduled} jobs. Keeping scheduled methods in one place makes it
 * easy to find, audit, and adjust cron expressions.
 */
@Service
public class ScheduledTaskService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskService.class);

    private final TaskQueryService taskQueryService;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final RecurringTaskGenerationService recurringTaskGenerationService;
    private final UserPreferenceService userPreferenceService;
    private final SettingService settingService;
    private final AppRoutesProperties appRoutes;
    private final Messages messages;

    public ScheduledTaskService(
            TaskQueryService taskQueryService,
            NotificationService notificationService,
            NotificationRepository notificationRepository,
            RecurringTaskGenerationService recurringTaskGenerationService,
            UserPreferenceService userPreferenceService,
            SettingService settingService,
            AppRoutesProperties appRoutes,
            Messages messages) {
        this.taskQueryService = taskQueryService;
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
        this.recurringTaskGenerationService = recurringTaskGenerationService;
        this.userPreferenceService = userPreferenceService;
        this.settingService = settingService;
        this.appRoutes = appRoutes;
        this.messages = messages;
    }

    /**
     * Sends due-date reminder notifications for tasks due tomorrow. Runs daily at 8:00 AM. Only
     * notifies users who have the preference enabled.
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional(readOnly = true)
    public void sendDueReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Task> tasks = taskQueryService.getTasksDueOn(tomorrow);
        log.info("sendDueReminders: starting, dueDate={}, tasksFound={}", tomorrow, tasks.size());

        int sent = 0;
        int skipped = 0;
        int failed = 0;
        for (Task task : tasks) {
            if (task.getUser() == null) {
                skipped++;
                continue;
            }

            UserPreferences prefs = userPreferenceService.load(task.getUser().getId());
            if (!prefs.isDueReminder()) {
                skipped++;
                continue;
            }

            try {
                String message = messages.get("notification.task.dueReminder", task.getTitle());
                notificationService.create(
                        task.getUser(),
                        null,
                        NotificationType.TASK_DUE_REMINDER,
                        message,
                        appRoutes.getTaskDetail().resolve("taskId", task.getId()));
                sent++;
            } catch (Exception e) {
                failed++;
                log.error("sendDueReminders: failed for taskId={}", task.getId(), e);
            }
        }

        log.info(
                "sendDueReminders: complete, sent={}, skipped={}, failed={}",
                sent,
                skipped,
                failed);
    }

    /**
     * Generates tasks from recurring templates that are due. Runs daily at 6:00 AM — before due
     * reminders at 8:00 AM so newly generated tasks with due dates get reminded on the same day.
     */
    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void generateRecurringTasks() {
        log.info("generateRecurringTasks: starting");
        int generated = recurringTaskGenerationService.generateDueTasks();
        log.info("generateRecurringTasks: complete, generated={}", generated);
    }

    /**
     * Purges old notifications based on the admin-configured retention period. Runs daily at 3:00
     * AM.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeOldNotifications() {
        int purgeDays = settingService.load().getNotificationPurgeDays();
        log.info("purgeOldNotifications: starting, retentionDays={}", purgeDays);
        int deleted =
                notificationRepository.deleteByCreatedAtBefore(
                        LocalDateTime.now().minus(purgeDays, ChronoUnit.DAYS));
        log.info("purgeOldNotifications: complete, deleted={}", deleted);
    }
}
