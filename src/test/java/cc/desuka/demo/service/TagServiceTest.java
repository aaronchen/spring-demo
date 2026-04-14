package cc.desuka.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.model.Tag;
import cc.desuka.demo.repository.TagRepository;
import cc.desuka.demo.security.SecurityUtils;
import java.util.LinkedHashSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock private TagRepository tagRepository;
    @Mock private TagQueryService tagQueryService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private TagService tagService;

    @Test
    void createTag_savesAndPublishesAuditEvent() {
        Tag tag = new Tag("NewTag");
        when(tagRepository.save(any(Tag.class)))
                .thenAnswer(
                        inv -> {
                            Tag t = inv.getArgument(0);
                            t.setId(1L);
                            return t;
                        });

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("admin@example.com");

            Tag result = tagService.createTag(tag);

            assertThat(result.getId()).isEqualTo(1L);
            verify(eventPublisher).publishEvent(any(AuditEvent.class));
        }
    }

    @Test
    void deleteTag_publishesAuditEvent() {
        Tag tag = new Tag("ToDelete");
        tag.setId(1L);
        tag.setTasks(new LinkedHashSet<>());
        when(tagQueryService.getTagById(1L)).thenReturn(tag);

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("admin@example.com");

            tagService.deleteTag(1L);

            verify(tagRepository).delete(tag);
            verify(eventPublisher).publishEvent(any(AuditEvent.class));
        }
    }
}
