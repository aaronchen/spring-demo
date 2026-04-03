package cc.desuka.demo.repository;

import cc.desuka.demo.model.Project;
import cc.desuka.demo.model.ProjectStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    @EntityGraph(attributePaths = {"createdBy", "members", "members.user"})
    Optional<Project> findById(UUID id);

    @EntityGraph(attributePaths = {"createdBy"})
    List<Project> findByStatusOrderByNameAsc(ProjectStatus status);

    @EntityGraph(attributePaths = {"createdBy"})
    List<Project> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"createdBy"})
    List<Project> findByStatusOrderByCreatedAtDesc(ProjectStatus status);

    @EntityGraph(attributePaths = {"createdBy"})
    List<Project> findAllByOrderByCreatedAtDesc();
}
