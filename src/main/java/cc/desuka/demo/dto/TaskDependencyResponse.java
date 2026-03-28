package cc.desuka.demo.dto;

import cc.desuka.demo.model.TaskStatus;
import lombok.Data;

@Data
public class TaskDependencyResponse {
    private Long id;
    private String title;
    private TaskStatus status;
}
