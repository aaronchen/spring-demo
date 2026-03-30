package cc.desuka.demo.audit;

import java.util.List;

public class AuditEvent {

    // Audit categories — single source of truth for filter UI and query logic.
    // Each event constant must be prefixed with one of these categories.
    public static final List<String> CATEGORIES =
            List.of(
                    "PROJECT",
                    "SPRINT",
                    "TASK",
                    "RECURRING",
                    "USER",
                    "PROFILE",
                    "COMMENT",
                    "TAG",
                    "AUTH",
                    "SETTING");

    // Project actions
    public static final String PROJECT_CREATED = "PROJECT_CREATED";
    public static final String PROJECT_UPDATED = "PROJECT_UPDATED";
    public static final String PROJECT_ARCHIVED = "PROJECT_ARCHIVED";
    public static final String PROJECT_UNARCHIVED = "PROJECT_UNARCHIVED";
    public static final String PROJECT_DELETED = "PROJECT_DELETED";
    public static final String PROJECT_MEMBER_ADDED = "PROJECT_MEMBER_ADDED";
    public static final String PROJECT_MEMBER_REMOVED = "PROJECT_MEMBER_REMOVED";
    public static final String PROJECT_MEMBER_ROLE_CHANGED = "PROJECT_MEMBER_ROLE_CHANGED";

    // Sprint actions
    public static final String SPRINT_CREATED = "SPRINT_CREATED";
    public static final String SPRINT_UPDATED = "SPRINT_UPDATED";
    public static final String SPRINT_DELETED = "SPRINT_DELETED";

    // Recurring template actions
    public static final String RECURRING_TEMPLATE_CREATED = "RECURRING_TEMPLATE_CREATED";
    public static final String RECURRING_TEMPLATE_UPDATED = "RECURRING_TEMPLATE_UPDATED";
    public static final String RECURRING_TEMPLATE_DELETED = "RECURRING_TEMPLATE_DELETED";

    // Task actions
    public static final String TASK_CREATED = "TASK_CREATED";
    public static final String TASK_UPDATED = "TASK_UPDATED";
    public static final String TASK_DELETED = "TASK_DELETED";

    // User actions (admin)
    public static final String USER_CREATED = "USER_CREATED";
    public static final String USER_UPDATED = "USER_UPDATED";
    public static final String USER_DELETED = "USER_DELETED";
    public static final String USER_DISABLED = "USER_DISABLED";
    public static final String USER_ENABLED = "USER_ENABLED";
    public static final String USER_PASSWORD_RESET = "USER_PASSWORD_RESET";
    public static final String USER_ROLE_CHANGED = "USER_ROLE_CHANGED";
    public static final String USER_REGISTERED = "USER_REGISTERED";

    // Profile actions (self-service)
    public static final String PROFILE_UPDATED = "PROFILE_UPDATED";
    public static final String PROFILE_PASSWORD_CHANGED = "PROFILE_PASSWORD_CHANGED";

    // Comment actions
    public static final String COMMENT_CREATED = "COMMENT_CREATED";
    public static final String COMMENT_DELETED = "COMMENT_DELETED";

    // Tag actions
    public static final String TAG_CREATED = "TAG_CREATED";
    public static final String TAG_DELETED = "TAG_DELETED";

    // Setting actions
    public static final String SETTING_UPDATED = "SETTING_UPDATED";

    // Auth actions
    public static final String AUTH_SUCCESS = "AUTH_SUCCESS";
    public static final String AUTH_FAILURE = "AUTH_FAILURE";

    private final String action;
    private final String entityType;
    private final Long entityId;
    private final String principal;
    private final String details;

    public AuditEvent(
            String action, Class<?> entityType, Long entityId, String principal, String details) {
        this.action = action;
        this.entityType = entityType != null ? entityType.getSimpleName() : null;
        this.entityId = entityId;
        this.principal = principal;
        this.details = details;
    }

    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getPrincipal() {
        return principal;
    }

    public String getDetails() {
        return details;
    }
}
