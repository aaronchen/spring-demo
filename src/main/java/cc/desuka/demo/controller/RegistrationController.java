package cc.desuka.demo.controller;

import cc.desuka.demo.config.AppRoutesProperties;
import cc.desuka.demo.dto.RegistrationRequest;
import cc.desuka.demo.model.User;
import cc.desuka.demo.service.SettingQueryService;
import cc.desuka.demo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RegistrationController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final SettingQueryService settingQueryService;
    private final AppRoutesProperties appRoutes;

    public RegistrationController(
            UserService userService,
            PasswordEncoder passwordEncoder,
            SettingQueryService settingQueryService,
            AppRoutesProperties appRoutes) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.settingQueryService = settingQueryService;
        this.appRoutes = appRoutes;
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        if (!settingQueryService.load().isRegistrationEnabled()) {
            return "redirect:" + appRoutes.getLogin();
        }
        model.addAttribute("registrationRequest", new RegistrationRequest());
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute RegistrationRequest registrationRequest, BindingResult result) {
        if (!settingQueryService.load().isRegistrationEnabled()) {
            return "redirect:" + appRoutes.getLogin();
        }

        // Cross-field validation: passwords must match
        if (!registrationRequest.getPassword().equals(registrationRequest.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "register.error.passwordMismatch");
        }

        if (result.hasErrors()) {
            return "register";
        }

        User user =
                new User(
                        registrationRequest.getName(),
                        registrationRequest.getEmail(),
                        passwordEncoder.encode(registrationRequest.getPassword()));
        userService.registerUser(user);

        return "redirect:" + appRoutes.getLogin().query("registered", "").build();
    }
}
