package cc.desuka.demo.dto;

import cc.desuka.demo.model.Sprint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SprintResponse {

    private Long id;
    private String name;
    private String goal;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private LocalDateTime createdAt;

    public static SprintResponse fromEntity(Sprint sprint) {
        SprintResponse response = new SprintResponse();
        response.setId(sprint.getId());
        response.setName(sprint.getName());
        response.setGoal(sprint.getGoal());
        response.setStartDate(sprint.getStartDate());
        response.setEndDate(sprint.getEndDate());
        response.setCreatedAt(sprint.getCreatedAt());

        if (sprint.isPast()) {
            response.setStatus("past");
        } else if (sprint.isActive()) {
            response.setStatus("active");
        } else {
            response.setStatus("future");
        }

        return response;
    }
}
