package cc.desuka.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    // Spring Security handles POST /login automatically via UsernamePasswordAuthenticationFilter.
    // This controller only serves the GET (the login page itself).
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
}
