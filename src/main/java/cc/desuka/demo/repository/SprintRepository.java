package cc.desuka.demo.repository;

import cc.desuka.demo.model.Sprint;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SprintRepository extends JpaRepository<Sprint, Long> {

    List<Sprint> findByProjectIdOrderByStartDateDesc(Long projectId);

    @EntityGraph(attributePaths = {"project"})
    Optional<Sprint> findWithProjectById(Long id);

    /** Find the sprint whose date range contains the given date. */
    @Query(
            "SELECT s FROM Sprint s WHERE s.project.id = :projectId"
                    + " AND s.startDate <= :date AND s.endDate >= :date")
    Optional<Sprint> findActiveByProjectId(Long projectId, LocalDate date);

    /**
     * Check if any sprint in the project overlaps the given date range, excluding a specific
     * sprint.
     */
    @Query(
            "SELECT COUNT(s) > 0 FROM Sprint s WHERE s.project.id = :projectId"
                    + " AND s.id != :excludeId"
                    + " AND s.startDate <= :endDate AND s.endDate >= :startDate")
    boolean existsOverlapping(
            Long projectId, Long excludeId, LocalDate startDate, LocalDate endDate);
}
