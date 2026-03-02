package cc.desuka.demo.controller;

import cc.desuka.demo.config.AppRoutesProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FrontendConfigController {

  private final AppRoutesProperties appRoutes;

  public FrontendConfigController(AppRoutesProperties appRoutes) {
    this.appRoutes = appRoutes;
  }

  @GetMapping(value = "/config.js", produces = "application/javascript")
  public String configJs() {
    return """
      window.APP_CONFIG = {
        routes: {
          tasks: "%s",
          api: "%s"
        }
      };
      """.formatted(escapeJs(appRoutes.getTasks()), escapeJs(appRoutes.getApi()));
  }

  private static String escapeJs(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
