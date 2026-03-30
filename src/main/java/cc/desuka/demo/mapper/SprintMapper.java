package cc.desuka.demo.mapper;

import cc.desuka.demo.dto.SprintRequest;
import cc.desuka.demo.dto.SprintResponse;
import cc.desuka.demo.model.Sprint;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SprintMapper {

    @Mapping(target = "status", expression = "java(sprintStatus(sprint))")
    SprintResponse toResponse(Sprint sprint);

    List<SprintResponse> toResponseList(List<Sprint> sprints);

    SprintRequest toRequest(Sprint sprint);

    @Mapping(target = Sprint.FIELD_ID, ignore = true)
    @Mapping(target = Sprint.FIELD_CREATED_AT, ignore = true)
    @Mapping(target = Sprint.FIELD_UPDATED_AT, ignore = true)
    @Mapping(target = Sprint.FIELD_PROJECT, ignore = true)
    Sprint toEntity(SprintRequest request);

    default String sprintStatus(Sprint sprint) {
        if (sprint.isPast()) return "past";
        if (sprint.isActive()) return "active";
        return "future";
    }
}
