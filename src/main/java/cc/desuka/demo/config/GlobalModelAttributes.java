package cc.desuka.demo.config;

import cc.desuka.demo.model.User;
import cc.desuka.demo.security.SecurityUtils;
import cc.desuka.demo.service.SettingQueryService;
import cc.desuka.demo.service.UserPreferenceQueryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    private final AppRoutesProperties appRoutes;
    private final SettingQueryService settingQueryService;
    private final UserPreferenceQueryService userPreferenceQueryService;

    public GlobalModelAttributes(
            AppRoutesProperties appRoutes,
            SettingQueryService settingQueryService,
            UserPreferenceQueryService userPreferenceQueryService) {
        this.appRoutes = appRoutes;
        this.settingQueryService = settingQueryService;
        this.userPreferenceQueryService = userPreferenceQueryService;
    }

    @ModelAttribute("appRoutes")
    public AppRoutesProperties appRoutes() {
        return appRoutes;
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    // Injects ${currentUser} into every Thymeleaf template.
    // Templates use it for ownership checks: ${currentUser != null && currentUser.id ==
    // task.user.id}
    // Returns null on the login page and error pages where no session exists.
    @ModelAttribute("currentUser")
    public User currentUser() {
        return SecurityUtils.getCurrentUser();
    }

    /** Exposes all settings as a typed object: ${settings.theme}, etc. */
    @ModelAttribute("settings")
    public Settings settings() {
        return settingQueryService.load();
    }

    /**
     * Exposes per-user preferences: ${userPreferences.taskView}, etc. Returns defaults when not
     * logged in.
     */
    @ModelAttribute("userPreferences")
    public UserPreferences userPreferences() {
        User user = SecurityUtils.getCurrentUser();
        if (user == null) {
            return new UserPreferences();
        }
        return userPreferenceQueryService.load(user.getId());
    }
}
