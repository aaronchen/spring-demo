package cc.desuka.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

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
 */
@ControllerAdvice
public class WebExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ModelAndView handleNotFound(EntityNotFoundException ex) {
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
