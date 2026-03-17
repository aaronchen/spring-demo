package cc.desuka.demo.mapper;

import cc.desuka.demo.dto.TaskRequest;
import cc.desuka.demo.dto.TaskResponse;
import cc.desuka.demo.model.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

// uses = {TagMapper.class, UserMapper.class}: MapStruct auto-discovers these converters.
// - TagMapper.toResponse(Tag) handles List<Tag> → List<TagResponse> for the tags field.
// - UserMapper.toResponse(User) handles User → UserResponse for the user field.
// No extra @Mapping needed for either field — names match on both sides.
@Mapper(componentModel = "spring", uses = {TagMapper.class, UserMapper.class})
public interface TaskMapper {

    // Task.tags (List<Tag>) → TaskResponse.tags (List<TagResponse>)
    // MapStruct calls TagMapper.toResponseList(task.getTags()) automatically via the uses clause.
    TaskResponse toResponse(Task task);

    List<TaskResponse> toResponseList(List<Task> tasks);

    // tags is ignored here — the mapper can't do DB lookups.
    // TaskService.resolveTags() fetches Tag entities from tagIds and sets them on the task.
    @Mapping(target = Task.FIELD_ID, ignore = true)
    @Mapping(target = Task.FIELD_STATUS, ignore = true)
    @Mapping(target = Task.FIELD_CREATED_AT, ignore = true)
    @Mapping(target = Task.FIELD_COMPLETED_AT, ignore = true)
    @Mapping(target = Task.FIELD_UPDATED_AT, ignore = true)
    @Mapping(target = Task.FIELD_TAGS, ignore = true)
    @Mapping(target = Task.FIELD_USER, ignore = true)
    @Mapping(target = Task.FIELD_VERSION, ignore = true)
    @Mapping(target = Task.FIELD_CHECKLIST_ITEMS, ignore = true)
    @Mapping(target = Task.FIELD_CHECKLIST_TOTAL, ignore = true)
    @Mapping(target = Task.FIELD_CHECKLIST_CHECKED, ignore = true)
    Task toEntity(TaskRequest request);
}
