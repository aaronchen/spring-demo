package cc.desuka.demo.util;

import cc.desuka.demo.config.AppRoutesProperties;

/** Shared constants and utilities for polymorphic entity references (pins, recent views, audit). */
public final class EntityTypes {

    public static final String TASK = "TASK";
    public static final String PROJECT = "PROJECT";

    private EntityTypes() {}

    /**
     * Resolve the detail page URL for an entity by type and ID. Accepts both uppercase constants
     * ("TASK", "PROJECT") used by pins/views and class simple names ("Task", "Project") used by
     * audit. Returns null for unknown or unsupported types (e.g., User, Sprint).
     */
    public static String resolveHref(
            AppRoutesProperties appRoutes, String entityType, String entityId) {
        if (entityType == null || entityId == null) return null;
        if (TASK.equalsIgnoreCase(entityType)) {
            return appRoutes.getTaskDetail().params("taskId", entityId).build();
        }
        if (PROJECT.equalsIgnoreCase(entityType)) {
            return appRoutes.getProjectDetail().params("projectId", entityId).build();
        }
        return null;
    }
}
