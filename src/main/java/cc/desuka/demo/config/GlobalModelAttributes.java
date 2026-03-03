package cc.desuka.demo.config;

import cc.desuka.demo.model.User;
import cc.desuka.demo.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

  private final AppRoutesProperties appRoutes;

  public GlobalModelAttributes(AppRoutesProperties appRoutes) {
    this.appRoutes = appRoutes;
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
}
