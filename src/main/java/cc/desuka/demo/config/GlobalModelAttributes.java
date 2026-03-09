package cc.desuka.demo.config;

import cc.desuka.demo.model.User;
import cc.desuka.demo.security.CustomUserDetails;
import cc.desuka.demo.service.SettingService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

  private final AppRoutesProperties appRoutes;
  private final SettingService settingService;

  public GlobalModelAttributes(AppRoutesProperties appRoutes,
                                SettingService settingService) {
    this.appRoutes = appRoutes;
    this.settingService = settingService;
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
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated()
        && auth.getPrincipal() instanceof CustomUserDetails details) {
      return details.getUser();
    }
    return null;
  }

  /** Exposes all settings as a typed object: ${settings.theme}, etc. */
  @ModelAttribute("settings")
  public Settings settings() {
    return settingService.load();
  }
}
