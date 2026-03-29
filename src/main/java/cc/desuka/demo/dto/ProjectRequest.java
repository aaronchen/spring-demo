package cc.desuka.demo.dto;

import cc.desuka.demo.model.Project;
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

    public static ProjectRequest fromEntity(Project project) {
        ProjectRequest request = new ProjectRequest();
        request.setName(project.getName());
        request.setDescription(project.getDescription());
        request.setSprintEnabled(project.isSprintEnabled());
        return request;
    }

    public Project toEntity() {
        Project project = new Project(name, description);
        project.setSprintEnabled(sprintEnabled);
        return project;
    }
}
