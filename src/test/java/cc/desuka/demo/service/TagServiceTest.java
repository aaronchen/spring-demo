package cc.desuka.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.Tag;
import cc.desuka.demo.repository.TagRepository;
import cc.desuka.demo.security.SecurityUtils;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock private TagRepository tagRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private TagService tagService;

    @Test
    void getAllTags_returnsSorted() {
        List<Tag> tags = List.of(new Tag("Alpha"), new Tag("Beta"));
        when(tagRepository.findAllByOrderByNameAsc()).thenReturn(tags);

        assertThat(tagService.getAllTags()).isEqualTo(tags);
    }

    @Test
    void getTagById_notFound_throws() {
        when(tagRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.getTagById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findAllByIds_nullOrEmpty_returnsEmptyList() {
        assertThat(tagService.findAllByIds(null)).isEmpty();
        assertThat(tagService.findAllByIds(List.of())).isEmpty();
    }

    @Test
    void findAllByIds_delegatesToRepository() {
        Tag tag = new Tag("Work");
        tag.setId(1L);
        when(tagRepository.findAllById(List.of(1L))).thenReturn(List.of(tag));

        assertThat(tagService.findAllByIds(List.of(1L))).containsExactly(tag);
    }

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
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentPrincipal).thenReturn("admin@example.com");

            tagService.deleteTag(1L);

            verify(tagRepository).delete(tag);
            verify(eventPublisher).publishEvent(any(AuditEvent.class));
        }
    }
}
