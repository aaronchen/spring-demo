package cc.desuka.demo.exception;

import cc.desuka.demo.util.Messages;
import java.util.HashMap;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Exception handler for REST API endpoints ({@code /api/**}). Returns RFC 9457 ProblemDetail
 * responses ({@code application/problem+json}).
 *
 * <p>Scoped to {@code cc.desuka.demo.controller.api} so it only advises {@code @RestController}
 * beans, not Thymeleaf web controllers. Web UI errors are handled by {@link WebExceptionHandler}.
 *
 * <p>Ordered at highest precedence so it wins over {@link WebExceptionHandler} if both could
 * theoretically match the same controller.
 */
@RestControllerAdvice(basePackages = "cc.desuka.demo.controller.api")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private final Messages messages;

    public ApiExceptionHandler(Messages messages) {
        this.messages = messages;
    }

    // 400 — validation errors from @Valid on @RequestBody
    // Overrides the default handler to include per-field error details.
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));
        problem.setProperty("errors", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    // 400 — invalid arguments (self-reference, same project, duplicate dependency)
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 403 — ownership or role check failed (thrown by OwnershipGuard.requireAccess)
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
    }

    // 404 — entity not found (Task, User, Tag, etc.)
    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleNotFound(EntityNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // 409 — optimistic locking conflict (stale version)
    @ExceptionHandler(StaleDataException.class)
    public ProblemDetail handleConflict(StaleDataException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    // 409 — pin limit reached
    @ExceptionHandler(PinLimitReachedException.class)
    public ProblemDetail handlePinLimitReached(PinLimitReachedException ex) {
        String detail = messages.get("pins.limit.reached", ex.getLimit());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
        problem.setProperty("limit", ex.getLimit());
        return problem;
    }

    // 409 — task has unresolved blockers
    @ExceptionHandler(BlockedTaskException.class)
    public ProblemDetail handleBlockedTask(BlockedTaskException ex) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setProperty("blockers", ex.getBlockerNames());
        return problem;
    }

    // 422 — cyclic dependency
    @ExceptionHandler(CyclicDependencyException.class)
    public ProblemDetail handleCyclicDependency(CyclicDependencyException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
    }

    // 500 — catch-all for unexpected errors; message omitted to avoid leaking internals
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleAllExceptions(Exception ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }
}
