package cc.desuka.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import cc.desuka.demo.exception.EntityNotFoundException;
import cc.desuka.demo.model.Tag;
import cc.desuka.demo.repository.TagRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TagQueryServiceTest {

    @Mock private TagRepository tagRepository;

    @InjectMocks private TagQueryService tagQueryService;

    @Test
    void getAllTags_returnsSorted() {
        List<Tag> tags = List.of(new Tag("Alpha"), new Tag("Beta"));
        when(tagRepository.findAllByOrderByNameAsc()).thenReturn(tags);

        assertThat(tagQueryService.getAllTags()).isEqualTo(tags);
    }

    @Test
    void getTagById_notFound_throws() {
        when(tagRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagQueryService.getTagById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findAllByIds_nullOrEmpty_returnsEmptySet() {
        assertThat(tagQueryService.findAllByIds(null)).isEmpty();
        assertThat(tagQueryService.findAllByIds(List.of())).isEmpty();
    }

    @Test
    void findAllByIds_delegatesToRepository() {
        Tag tag = new Tag("Work");
        tag.setId(1L);
        when(tagRepository.findAllById(List.of(1L))).thenReturn(List.of(tag));

        assertThat(tagQueryService.findAllByIds(List.of(1L))).containsExactly(tag);
    }
}
