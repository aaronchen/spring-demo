package cc.desuka.demo.dto;

import cc.desuka.demo.model.TaskStatus;
import java.util.UUID;
import lombok.Data;

@Data
public class TaskDependencyResponse {
    private UUID id;
    private String title;
    private TaskStatus status;
}
