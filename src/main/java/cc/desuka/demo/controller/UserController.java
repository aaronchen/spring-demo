package cc.desuka.demo.controller;

import cc.desuka.demo.service.UserService;
import cc.desuka.demo.util.HtmxUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String listUsers(
            @RequestParam(required = false, defaultValue = "") String search,
            Model model, HttpServletRequest request) {
        model.addAttribute("users", userService.searchUsers(search));
        model.addAttribute("search", search);
        if (HtmxUtils.isHtmxRequest(request)) {
            return "users/user-table";
        }
        return "users/users";
    }
}
