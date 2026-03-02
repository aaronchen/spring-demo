package cc.desuka.demo.config;

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
}
