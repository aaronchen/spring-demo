package cc.desuka.demo.controller.admin;

import cc.desuka.demo.dto.AdminUserRequest;
import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.User;
import cc.desuka.demo.security.SecurityUtils;
import cc.desuka.demo.service.UserService;
import cc.desuka.demo.util.HtmxUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/users")
public class UserManagementController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public UserManagementController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String listUsers(@RequestParam(required = false) String search,
                            Model model, HttpServletRequest request) {
        populateModel(model, search);
        if (HtmxUtils.isHtmxRequest(request)) {
            return "admin/user-table";
        }
        return "admin/users";
    }

    @GetMapping("/new")
    public String newUserForm(Model model) {
        model.addAttribute("adminUserRequest", new AdminUserRequest());
        model.addAttribute("roles", Role.values());
        model.addAttribute("isEdit", false);
        return "admin/user-modal";
    }

    @GetMapping("/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id);
        AdminUserRequest request = new AdminUserRequest();
        request.setId(id);
        request.setName(user.getName());
        request.setEmail(user.getEmail());
        request.setRole(user.getRole());
        model.addAttribute("adminUserRequest", request);
        model.addAttribute("userId", id);
        model.addAttribute("roles", Role.values());
        model.addAttribute("isEdit", true);
        return "admin/user-modal";
    }

    @PostMapping
    public Object createUser(@Valid @ModelAttribute AdminUserRequest adminUserRequest,
                             BindingResult result, Model model) {
        String pw = adminUserRequest.getPassword();
        if (pw == null) {
            result.rejectValue("password", "user.password.notBlank");
        } else if (pw.length() < 8 || pw.length() > 72) {
            result.rejectValue("password", "user.password.size");
        }

        if (result.hasErrors()) {
            model.addAttribute("roles", Role.values());
            model.addAttribute("isEdit", false);
            return "admin/user-modal";
        }

        User user = new User(
                adminUserRequest.getName(),
                adminUserRequest.getEmail(),
                passwordEncoder.encode(adminUserRequest.getPassword()),
                adminUserRequest.getRole()
        );
        userService.createUser(user);
        return HtmxUtils.triggerEvent("userSaved");
    }

    @PutMapping("/{id}")
    public Object updateUser(@PathVariable Long id,
                             @Valid @ModelAttribute AdminUserRequest adminUserRequest,
                             BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("userId", id);
            model.addAttribute("roles", Role.values());
            model.addAttribute("isEdit", true);
            return "admin/user-modal";
        }

        User updated = userService.updateUser(id, adminUserRequest.getName(),
                adminUserRequest.getEmail(), adminUserRequest.getRole());

        // If editing self, refresh the cached entity in the SecurityContext
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null && currentUser.getId().equals(id)) {
            currentUser.setName(updated.getName());
            currentUser.setEmail(updated.getEmail());
        }

        return HtmxUtils.triggerEvent("userSaved");
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return HtmxUtils.triggerEvent("userSaved");
    }

    @PostMapping("/{id}/disable")
    @ResponseBody
    public ResponseEntity<Void> disableUser(@PathVariable Long id) {
        userService.disableUser(id);
        return HtmxUtils.triggerEvent("userSaved");
    }

    @PostMapping("/{id}/enable")
    @ResponseBody
    public ResponseEntity<Void> enableUser(@PathVariable Long id) {
        userService.enableUser(id);
        return HtmxUtils.triggerEvent("userSaved");
    }

    @PostMapping("/{id}/reset-password")
    @ResponseBody
    public ResponseEntity<Void> resetPassword(@PathVariable Long id,
                                              @RequestParam String password) {
        if (password.length() < 8 || password.length() > 72) {
            return ResponseEntity.badRequest().build();
        }
        userService.resetPassword(id, passwordEncoder.encode(password));
        return HtmxUtils.triggerEvent("passwordReset");
    }

    @GetMapping("/{id}/info")
    @ResponseBody
    public Map<String, Object> getUserInfo(@PathVariable Long id) {
        User user = userService.getUserById(id);
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", user.getName());
        info.put("canDelete", userService.canDelete(id));
        info.put("completedTasks", userService.countCompletedTasks(id));
        info.put("comments", userService.countComments(id));
        info.put("assignedTasks", userService.countAssignedTasks(id));
        return info;
    }

    private void populateModel(Model model, String search) {
        model.addAttribute("users", userService.searchUsers(search));
        model.addAttribute("roles", Role.values());
    }
}
