package cc.desuka.demo.config;

import cc.desuka.demo.model.User;
import cc.desuka.demo.security.SecurityUtils;
import cc.desuka.demo.service.SettingService;
import cc.desuka.demo.service.UserPreferenceService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

  private final AppRoutesProperties appRoutes;
  private final SettingService settingService;
  private final UserPreferenceService userPreferenceService;

  public GlobalModelAttributes(AppRoutesProperties appRoutes,
                                SettingService settingService,
                                UserPreferenceService userPreferenceService) {
    this.appRoutes = appRoutes;
    this.settingService = settingService;
    this.userPreferenceService = userPreferenceService;
  }

  @ModelAttribute("appRoutes")
  public AppRoutesProperties appRoutes() {
    return appRoutes;
  }

  // Injects ${currentUser} into every Thymeleaf template.
  // Templates use it for ownership checks: ${currentUser != null && currentUser.id == task.user.id}
  // Returns null on the login page and error pages where no session exists.
  @ModelAttribute("currentUser")
  public User currentUser() {
    return SecurityUtils.getCurrentUser();
  }

  /** Exposes all settings as a typed object: ${settings.theme}, etc. */
  @ModelAttribute("settings")
  public Settings settings() {
    return settingService.load();
  }

  /** Exposes per-user preferences: ${userPreferences.taskView}, etc. Returns defaults when not logged in. */
  @ModelAttribute("userPreferences")
  public UserPreferences userPreferences() {
    User user = SecurityUtils.getCurrentUser();
    if (user == null) {
      return new UserPreferences();
    }
    return userPreferenceService.load(user.getId());
  }
}
