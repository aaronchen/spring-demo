package cc.desuka.demo.mapper;

import cc.desuka.demo.dto.TaskRequest;
import cc.desuka.demo.dto.TaskResponse;
import cc.desuka.demo.model.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

// componentModel = "spring" tells MapStruct to generate a @Component,
// so Spring can inject this mapper just like any other bean.
@Mapper(componentModel = "spring")
public interface TaskMapper {

    // MapStruct matches fields by name. Task and TaskResponse share the same
    // field names (id, title, description, completed, createdAt), so no
    // extra configuration is needed here.
    TaskResponse toResponse(Task task);

    // MapStruct knows how to map Task → TaskResponse, so it automatically
    // implements this list variant by calling toResponse() for each element.
    List<TaskResponse> toResponseList(List<Task> tasks);

    // TaskRequest only carries title and description — the remaining Task
    // fields are set by the DB (id) or by application logic (completed,
    // createdAt). @Mapping(ignore = true) makes that intent explicit and
    // silences the "unmapped target" warning.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "completed", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Task toEntity(TaskRequest request);
}
