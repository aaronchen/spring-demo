package cc.desuka.demo.service;

import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.Tag;
import cc.desuka.demo.repository.TagRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only tag lookups. Counterpart to {@link TagService} (writes). */
@Service
@Transactional(readOnly = true)
public class TagQueryService {

    private final TagRepository tagRepository;

    public TagQueryService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public List<Tag> getAllTags() {
        return tagRepository.findAllByOrderByNameAsc();
    }

    public Tag getTagById(Long id) {
        return tagRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Tag.class, id));
    }

    public Set<Tag> findAllByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return new LinkedHashSet<>();
        return new LinkedHashSet<>(tagRepository.findAllById(ids));
    }

    public int countTasksByTagId(Long tagId) {
        return tagRepository.countTasksByTagId(tagId);
    }
}
