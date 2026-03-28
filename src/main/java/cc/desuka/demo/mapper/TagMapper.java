package cc.desuka.demo.mapper;

import cc.desuka.demo.dto.TagRequest;
import cc.desuka.demo.dto.TagResponse;
import cc.desuka.demo.model.Tag;
import java.util.Collection;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TagMapper {

    TagResponse toResponse(Tag tag);

    List<TagResponse> toResponseList(Collection<Tag> tags);

    @Mapping(target = Tag.FIELD_ID, ignore = true)
    @Mapping(target = Tag.FIELD_TASKS, ignore = true)
    Tag toEntity(TagRequest request);
}
