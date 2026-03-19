package cc.desuka.demo.service;

import cc.desuka.demo.repository.CommentRepository;
import org.springframework.stereotype.Service;

/**
 * Read-only comment lookups for cross-service use.
 * Breaks circular dependency: CommentService → UserService → CommentService.
 */
@Service
public class CommentQueryService {

    private final CommentRepository commentRepository;

    public CommentQueryService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public long countByUserId(Long userId) {
        return commentRepository.countByUserId(userId);
    }
}
