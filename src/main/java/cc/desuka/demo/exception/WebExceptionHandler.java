package cc.desuka.demo.exception;

import cc.desuka.demo.service.SettingQueryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global exception handler for Thymeleaf web controllers.
 *
 * <p>Returns rendered error pages instead of JSON. REST API errors are handled by {@link
 * ApiExceptionHandler}, which has higher precedence.
 *
 * <p>Two cases:
 *
 * <ul>
 *   <li>{@link EntityNotFoundException} → 404 page with the entity's error message
 *   <li>Any other {@link Exception} → 500 page (message not exposed to the user)
 * </ul>
 *
 * <p>{@code AccessDeniedException} is re-thrown so Spring Security's {@code
 * ExceptionTranslationFilter} can intercept it and render {@code templates/error/403.html} via
 * {@code BasicErrorController}. Without this, the catch-all {@code Exception} handler would swallow
 * it and return a 500 page.
 *
 * <p>{@code @ModelAttribute} methods from {@code GlobalModelAttributes} don't run for exception
 * handlers, so we inject {@code SettingService} to add settings (theme, site name) to each {@code
 * ModelAndView} manually.
 */
@ControllerAdvice
public class WebExceptionHandler {

    private final SettingQueryService settingQueryService;

    public WebExceptionHandler(SettingQueryService settingQueryService) {
        this.settingQueryService = settingQueryService;
    }

    // Re-throw so Spring Security's ExceptionTranslationFilter handles it → error/403.html.
    // Without this, the catch-all Exception handler below would swallow it as a 500.
    @ExceptionHandler(AccessDeniedException.class)
    public void handleAccessDenied(AccessDeniedException ex) throws AccessDeniedException {
        throw ex;
    }

    @ExceptionHandler(StaleDataException.class)
    public Object handleConflict(StaleDataException ex, HttpServletRequest request) {
        if (isHtmxRequest(request)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        }
        ModelAndView mav = new ModelAndView("error/409");
        mav.setStatus(HttpStatus.CONFLICT);
        mav.addObject("message", ex.getMessage());
        mav.addObject("settings", settingQueryService.load());
        return mav;
    }

    @ExceptionHandler({CyclicDependencyException.class, BlockedTaskException.class})
    public Object handleBadRequest(RuntimeException ex, HttpServletRequest request) {
        if (isHtmxRequest(request)) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
        ModelAndView mav = new ModelAndView("error/400");
        mav.setStatus(HttpStatus.BAD_REQUEST);
        mav.addObject("message", ex.getMessage());
        mav.addObject("settings", settingQueryService.load());
        return mav;
    }

    @ExceptionHandler({EntityNotFoundException.class, NoResourceFoundException.class})
    public Object handleNotFound(Exception ex, HttpServletRequest request) {
        if (isHtmxRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
        ModelAndView mav = new ModelAndView("error/404");
        mav.setStatus(HttpStatus.NOT_FOUND);
        mav.addObject("message", ex.getMessage());
        mav.addObject("settings", settingQueryService.load());
        return mav;
    }

    @ExceptionHandler(Exception.class)
    public Object handleServerError(Exception ex, HttpServletRequest request) {
        if (isHtmxRequest(request)) {
            return ResponseEntity.internalServerError().build();
        }
        ModelAndView mav = new ModelAndView("error/500");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        mav.addObject("settings", settingQueryService.load());
        return mav;
    }

    private boolean isHtmxRequest(HttpServletRequest request) {
        return "true".equals(request.getHeader("HX-Request"));
    }
}
