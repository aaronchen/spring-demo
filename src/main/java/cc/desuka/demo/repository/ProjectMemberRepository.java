package cc.desuka.demo.repository;

import cc.desuka.demo.model.ProjectMember;
import cc.desuka.demo.model.ProjectRole;
import cc.desuka.demo.model.ProjectStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    @EntityGraph(attributePaths = {"user"})
    List<ProjectMember> findByProjectId(UUID projectId);

    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);

    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

    @EntityGraph(attributePaths = {"project", "project.createdBy"})
    List<ProjectMember> findByUserId(UUID userId);

    @EntityGraph(attributePaths = {"project", "project.createdBy"})
    @Query(
            "SELECT m FROM ProjectMember m "
                    + "WHERE m.user.id = :userId AND m.project.status = :status "
                    + "ORDER BY LOWER(m.project.name) ASC")
    List<ProjectMember> findByUserIdAndProjectStatus(UUID userId, ProjectStatus status);

    @EntityGraph(attributePaths = {"project", "project.createdBy"})
    @Query(
            "SELECT m FROM ProjectMember m "
                    + "WHERE m.user.id = :userId AND m.project.status = :status "
                    + "AND m.role IN :roles ORDER BY LOWER(m.project.name) ASC")
    List<ProjectMember> findByUserIdAndProjectStatusAndRoleIn(
            UUID userId, ProjectStatus status, List<ProjectRole> roles);

    @Query(
            "SELECT m.project.id FROM ProjectMember m "
                    + "WHERE m.user.id = :userId AND m.project.status = :status")
    List<UUID> findProjectIdsByUserIdAndProjectStatus(UUID userId, ProjectStatus status);

    void deleteByProjectIdAndUserId(UUID projectId, UUID userId);

    void deleteByUserId(UUID userId);

    @Query(
            "SELECT COUNT(m) > 0 FROM ProjectMember m "
                    + "WHERE m.user.id = :userId "
                    + "AND m.role = cc.desuka.demo.model.ProjectRole.OWNER "
                    + "AND (SELECT COUNT(o) FROM ProjectMember o "
                    + "WHERE o.project = m.project "
                    + "AND o.role = cc.desuka.demo.model.ProjectRole.OWNER) = 1")
    boolean isSoleOwnerOfAnyProject(UUID userId);
}
