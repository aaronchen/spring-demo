package cc.desuka.demo.controller;

import cc.desuka.demo.audit.AuditDetails;
import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.dto.RegistrationRequest;
import cc.desuka.demo.model.User;
import cc.desuka.demo.service.SettingService;
import cc.desuka.demo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;
    private final SettingService settingService;

    public RegistrationController(UserService userService, PasswordEncoder passwordEncoder,
                                  ApplicationEventPublisher eventPublisher,
                                  SettingService settingService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.settingService = settingService;
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        if (!settingService.load().isRegistrationEnabled()) {
            return "redirect:/login";
        }
        model.addAttribute("registrationRequest", new RegistrationRequest());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegistrationRequest registrationRequest,
                           BindingResult result) {
        if (!settingService.load().isRegistrationEnabled()) {
            return "redirect:/login";
        }

        // Cross-field validation: passwords must match
        if (!registrationRequest.getPassword().equals(registrationRequest.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "register.error.passwordMismatch",
                    "Passwords do not match.");
        }

        // Duplicate email check
        if (userService.findByEmail(registrationRequest.getEmail()).isPresent()) {
            result.rejectValue("email", "register.error.emailExists",
                    "An account with this email already exists.");
        }

        if (result.hasErrors()) {
            return "register";
        }

        User user = new User(
                registrationRequest.getName(),
                registrationRequest.getEmail(),
                passwordEncoder.encode(registrationRequest.getPassword())
        );
        User saved = userService.createUser(user);
        eventPublisher.publishEvent(new AuditEvent(
                AuditEvent.USER_REGISTERED, User.class, saved.getId(),
                saved.getEmail(), AuditDetails.toJson(saved.toAuditSnapshot())));

        return "redirect:/login?registered";
    }
}
