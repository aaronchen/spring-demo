package cc.desuka.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProjectRequest {

    @NotBlank(message = "{project.name.notBlank}")
    @Size(min = 1, max = 100, message = "{project.name.size}")
    private String name;

    @Size(max = 500, message = "{project.description.size}")
    private String description;

    private boolean sprintEnabled;
}
