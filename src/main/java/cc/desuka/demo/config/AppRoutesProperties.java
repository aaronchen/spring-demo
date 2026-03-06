package cc.desuka.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized route configuration for Thymeleaf templates and the /config.js endpoint.
 *
 * <p>Defaults are defined here. Override in any Spring properties source:
 * <pre>
 * # application-prod.properties
 * app.routes.api=https://api.example.com
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "app.routes")
public class AppRoutesProperties {

  private String tasks = "/tasks";
  private String api = "/api";
  private String audit = "/admin/audit";

  public String getTasks() {
    return tasks;
  }

  public void setTasks(String tasks) {
    this.tasks = tasks;
  }

  public String getApi() {
    return api;
  }

  public void setApi(String api) {
    this.api = api;
  }

  public String getAudit() {
    return audit;
  }

  public void setAudit(String audit) {
    this.audit = audit;
  }
}
