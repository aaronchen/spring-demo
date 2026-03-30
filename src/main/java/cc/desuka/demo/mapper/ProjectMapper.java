package cc.desuka.demo.mapper;

import cc.desuka.demo.dto.ProjectRequest;
import cc.desuka.demo.model.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    ProjectRequest toRequest(Project project);

    @Mapping(target = Project.FIELD_ID, ignore = true)
    @Mapping(target = Project.FIELD_STATUS, ignore = true)
    @Mapping(target = Project.FIELD_CREATED_BY, ignore = true)
    @Mapping(target = Project.FIELD_CREATED_AT, ignore = true)
    @Mapping(target = Project.FIELD_UPDATED_AT, ignore = true)
    @Mapping(target = Project.FIELD_MEMBERS, ignore = true)
    @Mapping(target = Project.FIELD_TASKS, ignore = true)
    @Mapping(target = Project.FIELD_SPRINTS, ignore = true)
    Project toEntity(ProjectRequest request);
}
