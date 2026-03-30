package cc.desuka.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class SprintRequest {

    @NotBlank(message = "{sprint.name.notBlank}")
    @Size(min = 1, max = 100, message = "{sprint.name.size}")
    private String name;

    @Size(max = 500, message = "{sprint.goal.size}")
    private String goal;

    @NotNull(message = "{sprint.startDate.notNull}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull(message = "{sprint.endDate.notNull}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}
