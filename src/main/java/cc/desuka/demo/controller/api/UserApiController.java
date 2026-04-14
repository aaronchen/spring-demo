package cc.desuka.demo.controller.api;

import cc.desuka.demo.dto.UserRequest;
import cc.desuka.demo.dto.UserResponse;
import cc.desuka.demo.mapper.UserMapper;
import cc.desuka.demo.model.User;
import cc.desuka.demo.security.SecurityUtils;
import cc.desuka.demo.service.UserQueryService;
import cc.desuka.demo.service.UserService;
import cc.desuka.demo.util.Messages;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserApiController {

    private final UserQueryService userQueryService;
    private final UserService userService;
    private final UserMapper userMapper;
    private final Messages messages;

    public UserApiController(
            UserQueryService userQueryService,
            UserService userService,
            UserMapper userMapper,
            Messages messages) {
        this.userQueryService = userQueryService;
        this.userService = userService;
        this.userMapper = userMapper;
        this.messages = messages;
    }

    // GET /api/users
    // GET /api/users?q=ali
    @GetMapping
    public List<UserResponse> getAllUsers(@RequestParam(required = false) String q) {
        return userMapper.toResponseList(userQueryService.searchEnabledUsers(q));
    }

    // GET /api/users/{id}
    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable UUID id) {
        return userMapper.toResponse(userQueryService.getUserById(id));
    }

    // POST /api/users
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody UserRequest request) {
        User user = userMapper.toEntity(request);
        return userMapper.toResponse(userService.createUser(user));
    }

    // DELETE /api/users/{id}
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable UUID id) {
        if (SecurityUtils.isCurrentUser(id)) {
            throw new IllegalArgumentException(messages.get("admin.users.self.cannotRemove"));
        }
        userService.deleteUser(id);
    }
}
