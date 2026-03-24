package cc.desuka.demo.controller.api;

import cc.desuka.demo.dto.UserResponse;
import cc.desuka.demo.mapper.UserMapper;
import cc.desuka.demo.model.ProjectRole;
import cc.desuka.demo.model.User;
import cc.desuka.demo.service.ProjectService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
public class ProjectApiController {

    private final ProjectService projectService;
    private final UserMapper userMapper;

    public ProjectApiController(ProjectService projectService, UserMapper userMapper) {
        this.projectService = projectService;
        this.userMapper = userMapper;
    }

    // GET /api/projects/{id}/members — all enabled members
    @GetMapping("/{id}/members")
    public List<UserResponse> getProjectMembers(@PathVariable Long id) {
        List<User> members =
                projectService.getMembers(id).stream()
                        .map(m -> m.getUser())
                        .filter(User::isEnabled)
                        .toList();
        return userMapper.toResponseList(members);
    }

    // GET /api/projects/{id}/members/assignable — editors and owners only (for task assignment)
    @GetMapping("/{id}/members/assignable")
    public List<UserResponse> getAssignableMembers(@PathVariable Long id) {
        List<User> members =
                projectService.getMembers(id).stream()
                        .filter(m -> m.getRole() != ProjectRole.VIEWER)
                        .map(m -> m.getUser())
                        .filter(User::isEnabled)
                        .toList();
        return userMapper.toResponseList(members);
    }
}
