package cc.desuka.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class BulkTaskRequest {

    public static final String ACTION_STATUS = "STATUS";
    public static final String ACTION_PRIORITY = "PRIORITY";
    public static final String ACTION_ASSIGN = "ASSIGN";
    public static final String ACTION_EFFORT = "EFFORT";
    public static final String ACTION_SPRINT = "SPRINT";
    public static final String ACTION_DELETE = "DELETE";

    @NotEmpty private List<Long> taskIds;

    @NotBlank private String action;

    private String value;

    public List<Long> getTaskIds() {
        return taskIds;
    }

    public void setTaskIds(List<Long> taskIds) {
        this.taskIds = taskIds;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
