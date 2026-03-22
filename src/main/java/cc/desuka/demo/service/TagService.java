package cc.desuka.demo.service;

import cc.desuka.demo.audit.AuditDetails;
import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.Tag;
import cc.desuka.demo.repository.TagRepository;
import cc.desuka.demo.security.SecurityUtils;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TagService {

    private final TagRepository tagRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TagService(TagRepository tagRepository, ApplicationEventPublisher eventPublisher) {
        this.tagRepository = tagRepository;
        this.eventPublisher = eventPublisher;
    }

    public List<Tag> getAllTags() {
        return tagRepository.findAllByOrderByNameAsc();
    }

    public Tag getTagById(Long id) {
        return tagRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Tag.class, id));
    }

    public List<Tag> findAllByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();
        return tagRepository.findAllById(ids);
    }

    public int countTasksByTagId(Long tagId) {
        return getTagById(tagId).getTasks().size();
    }

    public Tag createTag(Tag tag) {
        Tag saved = tagRepository.save(tag);
        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.TAG_CREATED,
                        Tag.class,
                        saved.getId(),
                        SecurityUtils.getCurrentPrincipal(),
                        AuditDetails.toJson(saved.toAuditSnapshot())));
        return saved;
    }

    public void deleteTag(Long id) {
        Tag tag = getTagById(id);
        String snapshot = AuditDetails.toJson(tag.toAuditSnapshot());
        tagRepository.delete(tag);
        eventPublisher.publishEvent(
                new AuditEvent(
                        AuditEvent.TAG_DELETED,
                        Tag.class,
                        id,
                        SecurityUtils.getCurrentPrincipal(),
                        snapshot));
    }
}
