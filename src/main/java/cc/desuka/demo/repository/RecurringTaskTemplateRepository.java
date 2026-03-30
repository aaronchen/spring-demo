package cc.desuka.demo.repository;

import cc.desuka.demo.model.RecurringTaskTemplate;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RecurringTaskTemplateRepository
        extends JpaRepository<RecurringTaskTemplate, Long> {

    @EntityGraph(attributePaths = {"assignee", "createdBy", "tags"})
    List<RecurringTaskTemplate> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    @EntityGraph(attributePaths = {"project", "assignee", "createdBy", "tags"})
    Optional<RecurringTaskTemplate> findWithDetailsById(Long id);

    /**
     * Find all enabled templates whose next run date is today or earlier, belonging to active
     * (non-archived) and non-sprint-enabled projects.
     */
    @Query(
            "SELECT t FROM RecurringTaskTemplate t "
                    + "JOIN FETCH t.project p "
                    + "LEFT JOIN FETCH t.assignee "
                    + "LEFT JOIN FETCH t.tags "
                    + "WHERE t.enabled = true "
                    + "AND t.nextRunDate <= :today "
                    + "AND (t.endDate IS NULL OR t.endDate >= :today) "
                    + "AND p.status = cc.desuka.demo.model.ProjectStatus.ACTIVE "
                    + "AND p.sprintEnabled = false")
    List<RecurringTaskTemplate> findDueTemplates(LocalDate today);

    /** Disable all enabled templates for a project (used when enabling sprints). */
    @Modifying
    @Query(
            "UPDATE RecurringTaskTemplate t SET t.enabled = false "
                    + "WHERE t.project.id = :projectId AND t.enabled = true")
    int disableAllByProjectId(Long projectId);
}
