package cc.desuka.demo.controller;

import cc.desuka.demo.config.AppRoutesProperties;
import cc.desuka.demo.config.UserPreferences;
import cc.desuka.demo.dto.ChangePasswordRequest;
import cc.desuka.demo.dto.ProfileRequest;
import cc.desuka.demo.model.User;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.security.SecurityUtils;
import cc.desuka.demo.service.UserPreferenceService;
import cc.desuka.demo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;
    private final UserPreferenceService userPreferenceService;
    private final PasswordEncoder passwordEncoder;
    private final AppRoutesProperties appRoutes;

    public ProfileController(
            UserService userService,
            UserPreferenceService userPreferenceService,
            PasswordEncoder passwordEncoder,
            AppRoutesProperties appRoutes) {
        this.userService = userService;
        this.userPreferenceService = userPreferenceService;
        this.passwordEncoder = passwordEncoder;
        this.appRoutes = appRoutes;
    }

    // GET /profile - Show profile page
    @GetMapping
    public String showProfile(
            @AuthenticationPrincipal CustomUserDetails currentDetails, Model model) {
        User user = currentDetails.getUser();
        ProfileRequest profileRequest = new ProfileRequest();
        profileRequest.setId(user.getId());
        profileRequest.setName(user.getName());
        profileRequest.setEmail(user.getEmail());
        model.addAttribute("profileRequest", profileRequest);
        model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        return "profile/profile";
    }

    // POST /profile - Update name and email
    @PostMapping
    public String updateProfile(
            @Valid @ModelAttribute ProfileRequest profileRequest,
            BindingResult result,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
            return "profile/profile";
        }
        User updated =
                userService.updateProfile(
                        currentDetails.getUser().getId(),
                        profileRequest.getName(),
                        profileRequest.getEmail());
        SecurityUtils.refreshCachedUser(updated);
        redirectAttributes.addFlashAttribute("profileSaved", true);
        return "redirect:" + appRoutes.getProfile();
    }

    // POST /profile/password - Change password
    @PostMapping("/password")
    public String changePassword(
            @Valid @ModelAttribute ChangePasswordRequest changePasswordRequest,
            BindingResult result,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            Model model,
            RedirectAttributes redirectAttributes) {
        User user = currentDetails.getUser();
        // Verify current password
        if (!result.hasErrors()
                && !passwordEncoder.matches(
                        changePasswordRequest.getCurrentPassword(), user.getPassword())) {
            result.rejectValue("currentPassword", "profile.password.current.invalid");
        }
        // Verify passwords match
        if (!result.hasErrors()
                && !changePasswordRequest
                        .getNewPassword()
                        .equals(changePasswordRequest.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "profile.password.mismatch");
        }
        if (result.hasErrors()) {
            // Re-populate profile form for the page
            ProfileRequest profileRequest = new ProfileRequest();
            profileRequest.setId(user.getId());
            profileRequest.setName(user.getName());
            profileRequest.setEmail(user.getEmail());
            model.addAttribute("profileRequest", profileRequest);
            model.addAttribute("passwordErrors", true);
            return "profile/profile";
        }
        String encoded = passwordEncoder.encode(changePasswordRequest.getNewPassword());
        userService.changePassword(user.getId(), encoded);
        // Update the security context so the session stays valid
        user.setPassword(encoded);
        redirectAttributes.addFlashAttribute("passwordChanged", true);
        return "redirect:" + appRoutes.getProfile();
    }

    // POST /profile/preferences - Save user preferences
    @PostMapping("/preferences")
    public String savePreferences(
            @RequestParam String taskView,
            @RequestParam String defaultUserFilter,
            @RequestParam(defaultValue = "false") boolean dueReminder,
            @AuthenticationPrincipal CustomUserDetails currentDetails,
            RedirectAttributes redirectAttributes) {
        Long userId = currentDetails.getUser().getId();
        userPreferenceService.save(userId, UserPreferences.KEY_TASK_VIEW, taskView);
        userPreferenceService.save(
                userId, UserPreferences.KEY_DEFAULT_USER_FILTER, defaultUserFilter);
        userPreferenceService.save(
                userId, UserPreferences.KEY_DUE_REMINDER, String.valueOf(dueReminder));
        redirectAttributes.addFlashAttribute("preferencesSaved", true);
        return "redirect:" + appRoutes.getProfile();
    }
}
