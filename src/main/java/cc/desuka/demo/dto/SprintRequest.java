package cc.desuka.demo.dto;

import cc.desuka.demo.model.Sprint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;

@Data
public class SprintRequest {

    @NotBlank(message = "{sprint.name.notBlank}")
    @Size(min = 1, max = 100, message = "{sprint.name.size}")
    private String name;

    @Size(max = 500, message = "{sprint.goal.size}")
    private String goal;

    @NotNull(message = "{sprint.startDate.notNull}")
    private LocalDate startDate;

    @NotNull(message = "{sprint.endDate.notNull}")
    private LocalDate endDate;

    public static SprintRequest fromEntity(Sprint sprint) {
        SprintRequest request = new SprintRequest();
        request.setName(sprint.getName());
        request.setGoal(sprint.getGoal());
        request.setStartDate(sprint.getStartDate());
        request.setEndDate(sprint.getEndDate());
        return request;
    }

    public Sprint toEntity() {
        Sprint sprint = new Sprint();
        sprint.setName(name);
        sprint.setGoal(goal);
        sprint.setStartDate(startDate);
        sprint.setEndDate(endDate);
        return sprint;
    }
}
