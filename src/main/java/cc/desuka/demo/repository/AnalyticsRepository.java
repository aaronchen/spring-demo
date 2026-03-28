package cc.desuka.demo.repository;

import cc.desuka.demo.model.TaskStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
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

    // ── Workload: group by user + status ─────────────────────────────────

    public List<Object[]> countByUserAndStatus(Long projectId, List<Long> projectIds) {
        String jpql =
                "SELECT t.user.id, t.status, COUNT(t) FROM Task t"
                        + projectWhereClause(projectId, projectIds)
                        + " GROUP BY t.user.id, t.status";
        TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class);
        bindProjectParams(query, projectId, projectIds);
        return query.getResultList();
    }

    // ── Burndown: created per day ────────────────────────────────────────

    public List<Object[]> countCreatedPerDay(
            Long projectId, List<Long> projectIds, LocalDateTime from) {
        String jpql =
                "SELECT CAST(t.createdAt AS LocalDate), COUNT(t) FROM Task t"
                        + " WHERE t.createdAt >= :from"
                        + projectAndClause(projectId, projectIds)
                        + " GROUP BY CAST(t.createdAt AS LocalDate)"
                        + " ORDER BY CAST(t.createdAt AS LocalDate)";
        TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class);
        query.setParameter("from", from);
        bindProjectParams(query, projectId, projectIds);
        return query.getResultList();
    }

    // ── Burndown: completed per day ──────────────────────────────────────

    public List<Object[]> countCompletedPerDay(
            Long projectId, List<Long> projectIds, LocalDateTime from) {
        String jpql =
                "SELECT CAST(t.completedAt AS LocalDate), COUNT(t) FROM Task t"
                        + " WHERE t.completedAt IS NOT NULL AND t.completedAt >= :from"
                        + projectAndClause(projectId, projectIds)
                        + " GROUP BY CAST(t.completedAt AS LocalDate)"
                        + " ORDER BY CAST(t.completedAt AS LocalDate)";
        TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class);
        query.setParameter("from", from);
        bindProjectParams(query, projectId, projectIds);
        return query.getResultList();
    }

    // ── Burndown: initial open count at start date ───────────────────────

    public long countOpenAtDate(
            Long projectId,
            List<Long> projectIds,
            LocalDateTime from,
            Collection<TaskStatus> terminalStatuses) {
        String jpql =
                "SELECT COUNT(t) FROM Task t"
                        + " WHERE t.createdAt < :from"
                        + " AND (t.completedAt IS NULL OR t.completedAt >= :from)"
                        + " AND t.status NOT IN :terminalStatuses"
                        + projectAndClause(projectId, projectIds);
        TypedQuery<Long> query = em.createQuery(jpql, Long.class);
        query.setParameter("from", from);
        query.setParameter("terminalStatuses", terminalStatuses);
        bindProjectParams(query, projectId, projectIds);
        return query.getSingleResult();
    }

    // ── Overdue by assignee ──────────────────────────────────────────────

    public List<Object[]> countOverdueByUser(
            Long projectId, List<Long> projectIds, Collection<TaskStatus> terminalStatuses) {
        String jpql =
                "SELECT t.user.id, COUNT(t) FROM Task t"
                        + " WHERE t.dueDate < CURRENT_DATE"
                        + " AND t.status NOT IN :terminalStatuses"
                        + projectAndClause(projectId, projectIds)
                        + " GROUP BY t.user.id";
        TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class);
        query.setParameter("terminalStatuses", terminalStatuses);
        bindProjectParams(query, projectId, projectIds);
        return query.getResultList();
    }

    // ── Effort by assignee ────────────────────────────────────────────────

    public List<Object[]> sumEffortByUser(Long projectId, List<Long> projectIds) {
        String jpql =
                "SELECT t.user.id, SUM(t.effort) FROM Task t"
                        + " WHERE t.effort IS NOT NULL"
                        + projectAndClause(projectId, projectIds)
                        + " GROUP BY t.user.id";
        TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class);
        bindProjectParams(query, projectId, projectIds);
        return query.getResultList();
    }

    // ── Effort completed per day ────────────────────────────────────────

    public List<Object[]> sumEffortCompletedPerDay(
            Long projectId, List<Long> projectIds, LocalDateTime from) {
        String jpql =
                "SELECT CAST(t.completedAt AS LocalDate), SUM(t.effort) FROM Task t"
                        + " WHERE t.completedAt IS NOT NULL AND t.completedAt >= :from"
                        + " AND t.effort IS NOT NULL"
                        + projectAndClause(projectId, projectIds)
                        + " GROUP BY CAST(t.completedAt AS LocalDate)"
                        + " ORDER BY CAST(t.completedAt AS LocalDate)";
        TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class);
        query.setParameter("from", from);
        bindProjectParams(query, projectId, projectIds);
        return query.getResultList();
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /** Returns a WHERE clause for project scoping. Used when there is no preceding WHERE. */
    private String projectWhereClause(Long projectId, List<Long> projectIds) {
        if (projectId != null) {
            return " WHERE t.project.id = :projectId";
        } else if (projectIds != null) {
            return " WHERE t.project.id IN :projectIds";
        }
        return "";
    }

    /** Returns an AND clause for project scoping. Used when there is already a WHERE. */
    private String projectAndClause(Long projectId, List<Long> projectIds) {
        if (projectId != null) {
            return " AND t.project.id = :projectId";
        } else if (projectIds != null) {
            return " AND t.project.id IN :projectIds";
        }
        return "";
    }

    private void bindProjectParams(TypedQuery<?> query, Long projectId, List<Long> projectIds) {
        if (projectId != null) {
            query.setParameter("projectId", projectId);
        } else if (projectIds != null) {
            query.setParameter("projectIds", projectIds);
        }
    }
}
