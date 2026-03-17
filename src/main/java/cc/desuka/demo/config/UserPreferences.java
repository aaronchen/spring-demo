package cc.desuka.demo.config;

/**
 * Typed representation of per-user preferences with defaults.
 * Mirrors the {@link Settings} pattern for site-wide settings.
 *
 * <p>{@link cc.desuka.demo.service.UserPreferenceService#load(Long)} uses Spring's
 * {@code BeanWrapper} to auto-map DB rows to fields by name, with type
 * conversion. Missing keys keep their default.</p>
 *
 * <p><b>To add a new preference:</b></p>
 * <ol>
 *   <li>Add a field with its default value below</li>
 *   <li>Add a {@code KEY_*} constant whose value matches the field name exactly</li>
 * </ol>
 */
public class UserPreferences {

    /**
     * DB key constants — each value must match the corresponding field name
     * exactly (BeanWrapper resolves fields by name).
     */
    public static final String KEY_TASK_VIEW = "taskView";
    public static final String KEY_DEFAULT_USER_FILTER = "defaultUserFilter";
    public static final String KEY_DUE_REMINDER = "dueReminder";

    public static final String VIEW_CARDS = "cards";
    public static final String VIEW_TABLE = "table";
    public static final String VIEW_CALENDAR = "calendar";
    public static final String FILTER_MINE = "mine";
    public static final String FILTER_ALL = "all";

    private String taskView = VIEW_CARDS;
    private String defaultUserFilter = FILTER_MINE;
    private boolean dueReminder = true;

    public String getTaskView() {
        return taskView;
    }

    public void setTaskView(String taskView) {
        this.taskView = taskView;
    }

    public String getDefaultUserFilter() {
        return defaultUserFilter;
    }

    public void setDefaultUserFilter(String defaultUserFilter) {
        this.defaultUserFilter = defaultUserFilter;
    }

    public boolean isDueReminder() {
        return dueReminder;
    }

    public void setDueReminder(boolean dueReminder) {
        this.dueReminder = dueReminder;
    }
}
