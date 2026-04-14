package cc.desuka.demo.service;

import cc.desuka.demo.audit.AuditDetails;
import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.model.Tag;
import cc.desuka.demo.repository.TagRepository;
import cc.desuka.demo.security.SecurityUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tag write operations (create, delete). Counterpart to {@link TagQueryService} (reads). */
@Service
@Transactional
public class TagService {

    private final TagRepository tagRepository;
    private final TagQueryService tagQueryService;
    private final ApplicationEventPublisher eventPublisher;

    public TagService(
            TagRepository tagRepository,
            TagQueryService tagQueryService,
            ApplicationEventPublisher eventPublisher) {
        this.tagRepository = tagRepository;
        this.tagQueryService = tagQueryService;
        this.eventPublisher = eventPublisher;
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
        Tag tag = tagQueryService.getTagById(id);
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
