package cc.desuka.demo.repository;

import cc.desuka.demo.model.TaskStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Aggregate projection queries for analytics charts.
 *
 * <p>Uses {@link EntityManager} directly because these queries return {@code Object[]} projections
 * (not entities), and the project-scoping logic (single project, project list, or all) is handled
 * with dynamic WHERE clauses to avoid triplicating every query in a Spring Data interface.
 */
@Repository
public class AnalyticsRepository {

    private final EntityManager em;

    public AnalyticsRepository(EntityManager em) {
        this.em = em;
    }

    // ── Dashboard: project summaries (batch) ────────────────────────────

    public List<Object[]> countByProjectAndStatus(List<UUID> projectIds) {
        String jpql =
                "SELECT t.project.id, t.status, COUNT(t) FROM Task t"
                        + " WHERE t.project.id IN :projectIds"
                        + " GROUP BY t.project.id, t.status";
        return em.createQuery(jpql, Object[].class)
                .setParameter("projectIds", projectIds)
                .getResultList();
    }

    public List<Object[]> countOverdueByProject(
            List<UUID> projectIds, Collection<TaskStatus> terminalStatuses) {
        String jpql =
                "SELECT t.project.id, COUNT(t) FROM Task t"
                        + " WHERE t.project.id IN :projectIds"
                        + " AND t.dueDate < CURRENT_DATE"
                        + " AND t.status NOT IN :terminalStatuses"
                        + " GROUP BY t.project.id";
        return em.createQuery(jpql, Object[].class)
                .setParameter("projectIds", projectIds)
                .setParameter("terminalStatuses", terminalStatuses)
                .getResultList();
    }

    // ── Workload: group by user + status ─────────────────────────────────

    public List<Object[]> countByUserAndStatus(
            UUID projectId, List<UUID> projectIds, Long sprintId) {
        String jpql =
                "SELECT t.user.id, t.status, COUNT(t) FROM Task t"
                        + projectWhereClause(projectId, projectIds)
                        + sprintAndClause(sprintId, projectId == null && projectIds == null)
                        + " GROUP BY t.user.id, t.status";
        TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class);
        bindProjectParams(query, projectId, projectIds);
        bindSprintParam(query, sprintId);
        return query.getResultList();
    }

    // ── Burndown: created per day ────────────────────────────────────────

    public List<Object[]> countCreatedPerDay(
            UUID projectId, List<UUID> projectIds, Long sprintId, LocalDateTime from) {
        String jpql =
                "SELECT CAST(t.createdAt AS LocalDate), COUNT(t) FROM Task t"
                        + " WHERE t.createdAt >= :from"
                        + projectAndClause(projectId, projectIds)
                        + sprintAndClause(sprintId, false)
                        + " GROUP BY CAST(t.createdAt AS LocalDate)"
                        + " ORDER BY CAST(t.createdAt AS LocalDate)";
        TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class);
        query.setParameter("from", from);
        bindProjectParams(query, projectId, projectIds);
        bindSprintParam(query, sprintId);
        return query.getResultList();
    }

    // ── Burndown: completed per day ──────────────────────────────────────

    public List<Object[]> countCompletedPerDay(
            UUID projectId, List<UUID> projectIds, Long sprintId, LocalDateTime from) {
        String jpql =
                "SELECT CAST(t.completedAt AS LocalDate), COUNT(t) FROM Task t"
                        + " WHERE t.completedAt IS NOT NULL AND t.completedAt >= :from"
                        + projectAndClause(projectId, projectIds)
                        + sprintAndClause(sprintId, false)
                        + " GROUP BY CAST(t.completedAt AS LocalDate)"
                        + " ORDER BY CAST(t.completedAt AS LocalDate)";
        TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class);
        query.setParameter("from", from);
        bindProjectParams(query, projectId, projectIds);
        bindSprintParam(query, sprintId);
        return query.getResultList();
    }

    // ── Burndown: initial open count at start date ───────────────────────

    public long countOpenAtDate(
            UUID projectId,
            List<UUID> projectIds,
            Long sprintId,
            LocalDateTime from,
            Collection<TaskStatus> terminalStatuses) {
        String jpql =
                "SELECT COUNT(t) FROM Task t"
                        + " WHERE t.createdAt < :from"
                        + " AND (t.completedAt IS NULL OR t.completedAt >= :from)"
                        + " AND t.status NOT IN :terminalStatuses"
                        + projectAndClause(projectId, projectIds)
                        + sprintAndClause(sprintId, false);
        TypedQuery<Long> query = em.createQuery(jpql, Long.class);
        query.setParameter("from", from);
        query.setParameter("terminalStatuses", terminalStatuses);
        bindProjectParams(query, projectId, projectIds);
        bindSprintParam(query, sprintId);
        return query.getSingleResult();
    }

    // ── Overdue by assignee ──────────────────────────────────────────────

    public List<Object[]> countOverdueByUser(
            UUID projectId,
            List<UUID> projectIds,
            Long sprintId,
            Collection<TaskStatus> terminalStatuses) {
        String jpql =
                "SELECT t.user.id, COUNT(t) FROM Task t"
                        + " WHERE t.dueDate < CURRENT_DATE"
                        + " AND t.status NOT IN :terminalStatuses"
                        + projectAndClause(projectId, projectIds)
                        + sprintAndClause(sprintId, false)
                        + " GROUP BY t.user.id";
        TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class);
        query.setParameter("terminalStatuses", terminalStatuses);
        bindProjectParams(query, projectId, projectIds);
        bindSprintParam(query, sprintId);
        return query.getResultList();
    }

    // ── Effort by assignee ────────────────────────────────────────────────

    public List<Object[]> sumEffortByUser(UUID projectId, List<UUID> projectIds, Long sprintId) {
        String jpql =
                "SELECT t.user.id, SUM(t.effort) FROM Task t"
                        + " WHERE t.effort IS NOT NULL"
                        + projectAndClause(projectId, projectIds)
                        + sprintAndClause(sprintId, false)
                        + " GROUP BY t.user.id";
        TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class);
        bindProjectParams(query, projectId, projectIds);
        bindSprintParam(query, sprintId);
        return query.getResultList();
    }

    // ── Effort completed per day ────────────────────────────────────────

    public List<Object[]> sumEffortCompletedPerDay(
            UUID projectId, List<UUID> projectIds, Long sprintId, LocalDateTime from) {
        String jpql =
                "SELECT CAST(t.completedAt AS LocalDate), SUM(t.effort) FROM Task t"
                        + " WHERE t.completedAt IS NOT NULL AND t.completedAt >= :from"
                        + " AND t.effort IS NOT NULL"
                        + projectAndClause(projectId, projectIds)
                        + sprintAndClause(sprintId, false)
                        + " GROUP BY CAST(t.completedAt AS LocalDate)"
                        + " ORDER BY CAST(t.completedAt AS LocalDate)";
        TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class);
        query.setParameter("from", from);
        bindProjectParams(query, projectId, projectIds);
        bindSprintParam(query, sprintId);
        return query.getResultList();
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /** Returns a WHERE clause for project scoping. Used when there is no preceding WHERE. */
    private String projectWhereClause(UUID projectId, List<UUID> projectIds) {
        if (projectId != null) {
            return " WHERE t.project.id = :projectId";
        } else if (projectIds != null) {
            return " WHERE t.project.id IN :projectIds";
        }
        return "";
    }

    /** Returns an AND clause for project scoping. Used when there is already a WHERE. */
    private String projectAndClause(UUID projectId, List<UUID> projectIds) {
        if (projectId != null) {
            return " AND t.project.id = :projectId";
        } else if (projectIds != null) {
            return " AND t.project.id IN :projectIds";
        }
        return "";
    }

    private void bindProjectParams(TypedQuery<?> query, UUID projectId, List<UUID> projectIds) {
        if (projectId != null) {
            query.setParameter("projectId", projectId);
        } else if (projectIds != null) {
            query.setParameter("projectIds", projectIds);
        }
    }

    /**
     * Returns an AND (or WHERE) clause for sprint scoping.
     *
     * @param firstClause true when there is no preceding WHERE — emits WHERE instead of AND
     */
    private String sprintAndClause(Long sprintId, boolean firstClause) {
        if (sprintId == null) return "";
        String prefix = firstClause ? " WHERE" : " AND";
        return prefix + " t.sprint.id = :sprintId";
    }

    private void bindSprintParam(TypedQuery<?> query, Long sprintId) {
        if (sprintId != null) {
            query.setParameter("sprintId", sprintId);
        }
    }
}
