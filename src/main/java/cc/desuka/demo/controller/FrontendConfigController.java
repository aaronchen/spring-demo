package cc.desuka.demo.controller;

import cc.desuka.demo.config.AppRoutesProperties;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
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
          api: "%s",
          audit: "%s"
        },
        messages: %s
      };
      """.formatted(escapeJs(appRoutes.getTasks()), escapeJs(appRoutes.getApi()),
                    escapeJs(appRoutes.getAudit()), buildMessagesJson());
  }

  // NOTE: Uses JVM default locale, not the browser's Accept-Language. Fine while
  // the app is single-locale. For i18n, switch to MessageSource (which is locale-aware)
  // and pass the request Locale — but that conflicts with content-hash caching since
  // config.js would need to vary per locale (separate cache key or a non-cached endpoint).
  private String buildMessagesJson() {
    ResourceBundle bundle = ResourceBundle.getBundle("messages");
    String entries = bundle.keySet().stream()
        .sorted()
        .map(key -> "\"" + escapeJs(key) + "\":\"" + escapeJs(bundle.getString(key)) + "\"")
        .collect(Collectors.joining(","));
    return "{" + entries + "}";
  }

  private static String escapeJs(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
