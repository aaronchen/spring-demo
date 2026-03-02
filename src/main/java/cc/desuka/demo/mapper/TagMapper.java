package cc.desuka.demo.mapper;

import cc.desuka.demo.dto.TagRequest;
import cc.desuka.demo.dto.TagResponse;
import cc.desuka.demo.model.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TagMapper {

    TagResponse toResponse(Tag tag);

    List<TagResponse> toResponseList(List<Tag> tags);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tasks", ignore = true)
    Tag toEntity(TagRequest request);
}
