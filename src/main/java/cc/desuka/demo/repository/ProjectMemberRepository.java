package cc.desuka.demo.repository;

import cc.desuka.demo.model.ProjectMember;
import cc.desuka.demo.model.ProjectRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    @EntityGraph(attributePaths = {"user"})
    List<ProjectMember> findByProjectId(Long projectId);

    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    long countByProjectIdAndRole(Long projectId, ProjectRole role);

    @EntityGraph(attributePaths = {"project", "project.createdBy"})
    List<ProjectMember> findByUserId(Long userId);

    void deleteByProjectIdAndUserId(Long projectId, Long userId);
}
