package cc.desuka.demo.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

/** Utility class for HTMX-related operations */
public class HtmxUtils {

    /**
     * Check if the request comes from HTMX
     *
     * @param request the HTTP servlet request
     * @return true if the request has the HX-Request header set to "true"
     */
    public static boolean isHtmxRequest(HttpServletRequest request) {
        return "true".equals(request.getHeader("HX-Request"));
    }

    /**
     * Build a 200 OK response that fires an HTMX client-side event via HX-Trigger.
     *
     * @param eventName the event name to fire on the client
     * @return a ResponseEntity with the HX-Trigger header set
     */
    public static ResponseEntity<Void> triggerEvent(String eventName) {
        return ResponseEntity.ok().header("HX-Trigger", eventName).build();
    }

    /**
     * Set an HX-Trigger header on the response that fires a {@code showToast} event, picked up by
     * the global listener in {@code application.js}.
     *
     * @param message the already-resolved message text
     * @param type toast type: "success", "danger", "warning", "info"
     */
    public static String toastTrigger(String message, String type) {
        // JSON: {"showToast":{"message":"...","type":"..."}}
        String escapedMessage = message.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"showToast\":{\"message\":\"" + escapedMessage + "\",\"type\":\"" + type + "\"}}";
    }

    // Private constructor to prevent instantiation
    private HtmxUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
}
