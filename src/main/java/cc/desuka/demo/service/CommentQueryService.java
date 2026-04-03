package cc.desuka.demo.service;

import cc.desuka.demo.repository.CommentRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Read-only comment lookups for cross-service use. Breaks circular dependency: CommentService →
 * UserService → CommentService.
 */
@Service
public class CommentQueryService {

    private final CommentRepository commentRepository;

    public CommentQueryService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public long countByUserId(UUID userId) {
        return commentRepository.countByUserId(userId);
    }
}
