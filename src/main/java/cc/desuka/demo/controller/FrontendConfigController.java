package cc.desuka.demo.controller;

import cc.desuka.demo.config.AppRoutesProperties;
import cc.desuka.demo.model.Priority;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.util.RouteTemplate;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FrontendConfigController {

    private static final ObjectMapper MAPPER = createMapper();

    private final AppRoutesProperties appRoutes;

    public FrontendConfigController(AppRoutesProperties appRoutes) {
        this.appRoutes = appRoutes;
    }

    @GetMapping(value = "/config.js", produces = "application/javascript")
    public String configJs() throws JsonProcessingException {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("routes", buildRoutesMap());
        config.put("messages", buildMessagesMap());
        config.put(
                "enums",
                Map.of(
                        "taskStatus", buildTaskStatusMap(),
                        "priority", buildPriorityMap()));

        return "(function() {\n"
                + RouteTemplate.JS_CLASS
                + "window.APP_CONFIG = "
                + MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(config)
                + ";\n})();\n";
    }

    private Map<String, RouteTemplate> buildRoutesMap() {
        Map<String, RouteTemplate> routes = new LinkedHashMap<>();
        for (Field field : AppRoutesProperties.class.getDeclaredFields()) {
            if (field.getType() != RouteTemplate.class) continue;
            field.setAccessible(true);
            try {
                routes.put(field.getName(), (RouteTemplate) field.get(appRoutes));
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to read route field: " + field.getName(), e);
            }
        }
        return routes;
    }

    // NOTE: Uses JVM default locale, not the browser's Accept-Language. Fine while
    // the app is single-locale. For i18n, switch to MessageSource (which is locale-aware)
    // and pass the request Locale — but that conflicts with content-hash caching since
    // config.js would need to vary per locale (separate cache key or a non-cached endpoint).
    private Map<String, String> buildMessagesMap() {
        ResourceBundle bundle = ResourceBundle.getBundle("messages");
        Map<String, String> messages = new TreeMap<>();
        for (String key : bundle.keySet()) {
            messages.put(key, bundle.getString(key));
        }
        return messages;
    }

    private Map<String, Map<String, Object>> buildTaskStatusMap() {
        Map<String, Map<String, Object>> map = new LinkedHashMap<>();
        for (TaskStatus status : TaskStatus.values()) {
            map.put(
                    status.name(),
                    Map.of(
                            "css", status.getCssClass(),
                            "btnCss", status.getBtnClass(),
                            "icon", status.getIcon(),
                            "chartColor", status.getChartColor(),
                            "terminal", status.isTerminal()));
        }
        return map;
    }

    private Map<String, Map<String, Object>> buildPriorityMap() {
        Map<String, Map<String, Object>> map = new LinkedHashMap<>();
        for (Priority priority : Priority.values()) {
            map.put(
                    priority.name(),
                    Map.of(
                            "css", priority.getCssClass(),
                            "btnCss", priority.getBtnClass(),
                            "icon", priority.getIcon(),
                            "chartColor", priority.getChartColor()));
        }
        return map;
    }

    private static ObjectMapper createMapper() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(RouteTemplate.class, new RouteTemplateSerializer());
        return new ObjectMapper().registerModule(module);
    }

    /**
     * Serializes a {@link RouteTemplate} as {@code new Route("template")} — a raw JavaScript
     * expression, not a JSON string. Only used by this controller's private {@code MAPPER}.
     */
    private static class RouteTemplateSerializer extends JsonSerializer<RouteTemplate> {

        private static final ObjectMapper STRING_MAPPER = new ObjectMapper();

        @Override
        public void serialize(
                RouteTemplate value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            String escaped = STRING_MAPPER.writeValueAsString(value.getTemplate());
            gen.writeRawValue("new Route(" + escaped + ")");
        }
    }
}
