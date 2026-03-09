package cc.desuka.demo.controller.admin;

import cc.desuka.demo.dto.AdminUserRequest;
import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.User;
import cc.desuka.demo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class UserManagementController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public UserManagementController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        if (!model.containsAttribute("adminUserRequest")) {
            model.addAttribute("adminUserRequest", new AdminUserRequest());
        }
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("roles", Role.values());
        return "admin/users";
    }

    @PostMapping("/users")
    public String createUser(@Valid @ModelAttribute AdminUserRequest adminUserRequest,
                             BindingResult result, Model model,
                             RedirectAttributes redirectAttributes) {
        if (userService.findByEmail(adminUserRequest.getEmail()).isPresent()) {
            result.rejectValue("email", "admin.users.error.emailExists");
        }

        if (result.hasErrors()) {
            model.addAttribute("users", userService.getAllUsers());
            model.addAttribute("roles", Role.values());
            model.addAttribute("showCreateForm", true);
            return "admin/users";
        }

        User user = new User(
                adminUserRequest.getName(),
                adminUserRequest.getEmail(),
                passwordEncoder.encode(adminUserRequest.getPassword()),
                adminUserRequest.getRole()
        );
        userService.createUser(user);
        redirectAttributes.addFlashAttribute("userCreated", true);
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/role")
    public String changeRole(@PathVariable Long id, @RequestParam Role role) {
        userService.updateRole(id, role);
        return "redirect:/admin/users";
    }
}
