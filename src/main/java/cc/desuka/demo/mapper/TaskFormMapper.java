package cc.desuka.demo.mapper;

import cc.desuka.demo.dto.TaskFormRequest;
import cc.desuka.demo.model.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TaskFormMapper {

    @Mapping(source = "sprint.id", target = "sprintId")
    @Mapping(source = "template.title", target = "templateName")
    TaskFormRequest toRequest(Task task);

    @Mapping(target = Task.FIELD_ID, ignore = true)
    @Mapping(target = Task.FIELD_CREATED_AT, ignore = true)
    @Mapping(target = Task.FIELD_COMPLETED_AT, ignore = true)
    @Mapping(target = Task.FIELD_UPDATED_AT, ignore = true)
    @Mapping(target = Task.FIELD_TAGS, ignore = true)
    @Mapping(target = Task.FIELD_USER, ignore = true)
    @Mapping(target = Task.FIELD_VERSION, ignore = true)
    @Mapping(target = Task.FIELD_CHECKLIST_ITEMS, ignore = true)
    @Mapping(target = Task.FIELD_COMMENTS, ignore = true)
    @Mapping(target = Task.FIELD_CHECKLIST_TOTAL, ignore = true)
    @Mapping(target = Task.FIELD_CHECKLIST_CHECKED, ignore = true)
    @Mapping(target = Task.FIELD_PROJECT, ignore = true)
    @Mapping(target = Task.FIELD_SPRINT, ignore = true)
    @Mapping(target = Task.FIELD_TEMPLATE, ignore = true)
    @Mapping(target = Task.FIELD_BLOCKED_BY, ignore = true)
    @Mapping(target = Task.FIELD_BLOCKS, ignore = true)
    Task toEntity(TaskFormRequest request);
}
