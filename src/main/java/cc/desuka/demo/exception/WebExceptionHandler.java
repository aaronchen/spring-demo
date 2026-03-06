package cc.desuka.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global exception handler for Thymeleaf web controllers.
 *
 * <p>Returns rendered error pages instead of JSON. REST API errors are
 * handled by {@link ApiExceptionHandler}, which has higher precedence.
 *
 * <p>Two cases:
 * <ul>
 *   <li>{@link EntityNotFoundException} → 404 page with the entity's error message</li>
 *   <li>Any other {@link Exception}     → 500 page (message not exposed to the user)</li>
 * </ul>
 *
 * <p>{@code AccessDeniedException} is re-thrown so Spring Security's
 * {@code ExceptionTranslationFilter} can intercept it and render
 * {@code templates/error/403.html} via {@code BasicErrorController}.
 * Without this, the catch-all {@code Exception} handler would swallow it
 * and return a 500 page.
 */
@ControllerAdvice
public class WebExceptionHandler {

    // Re-throw so Spring Security's ExceptionTranslationFilter handles it → error/403.html.
    // Without this, the catch-all Exception handler below would swallow it as a 500.
    @ExceptionHandler(AccessDeniedException.class)
    public void handleAccessDenied(AccessDeniedException ex) throws AccessDeniedException {
        throw ex;
    }

    @ExceptionHandler({EntityNotFoundException.class, NoResourceFoundException.class})
    public ModelAndView handleNotFound(Exception ex) {
        ModelAndView mav = new ModelAndView("error/404");
        mav.setStatus(HttpStatus.NOT_FOUND);
        mav.addObject("message", ex.getMessage());
        return mav;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleServerError(Exception ex) {
        ModelAndView mav = new ModelAndView("error/500");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        return mav;
    }
}
