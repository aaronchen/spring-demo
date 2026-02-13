package cc.desuka.demo.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility class for HTMX-related operations
 */
public class HtmxUtils {

  /**
   * Check if the request comes from HTMX
   * @param request the HTTP servlet request
   * @return true if the request has the HX-Request header set to "true"
   */
  public static boolean isHtmxRequest(HttpServletRequest request) {
    return "true".equals(request.getHeader("HX-Request"));
  }

  // Private constructor to prevent instantiation
  private HtmxUtils() {
    throw new UnsupportedOperationException("Utility class");
  }
}
