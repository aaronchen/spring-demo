package cc.desuka.demo.repository;

import cc.desuka.demo.model.Comment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Load user eagerly to avoid N+1 when rendering comment author names.
    @EntityGraph(attributePaths = {"user"})
    List<Comment> findByTaskIdOrderByCreatedAtAsc(Long taskId);

    @Modifying
    @Transactional
    void deleteByTaskId(Long taskId);
}
