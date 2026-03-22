package cc.desuka.demo.dto;

import cc.desuka.demo.model.Project;

public record ProjectSummary(
        Long id,
        String name,
        long openTasks,
        long inProgressTasks,
        long inReviewTasks,
        long completedTasks,
        long overdueTasks,
        long totalTasks) {

    public static ProjectSummary of(
            Project project,
            long openTasks,
            long inProgressTasks,
            long inReviewTasks,
            long completedTasks,
            long overdueTasks,
            long totalTasks) {
        return new ProjectSummary(
                project.getId(),
                project.getName(),
                openTasks,
                inProgressTasks,
                inReviewTasks,
                completedTasks,
                overdueTasks,
                totalTasks);
    }
}
