package cc.desuka.demo.controller;

import cc.desuka.demo.service.UserQueryService;
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

    private final UserQueryService userQueryService;

    public UserController(UserQueryService userQueryService) {
        this.userQueryService = userQueryService;
    }

    @GetMapping
    public String listUsers(
            @RequestParam(required = false, defaultValue = "") String search,
            Model model,
            HttpServletRequest request) {
        model.addAttribute("users", userQueryService.searchEnabledUsers(search));
        model.addAttribute("search", search);
        if (HtmxUtils.isHtmxRequest(request)) {
            return "users/user-table";
        }
        return "users/users";
    }
}
