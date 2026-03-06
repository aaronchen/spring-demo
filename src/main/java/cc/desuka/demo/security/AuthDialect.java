package cc.desuka.demo.security;

import cc.desuka.demo.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.dialect.AbstractDialect;
import org.thymeleaf.dialect.IExpressionObjectDialect;
import org.thymeleaf.expression.IExpressionObjectFactory;

import java.util.Collections;
import java.util.Set;

/**
 * Thymeleaf dialect that registers the {@code #auth} expression object.
 *
 * <p>Being a {@link Component @Component}, Spring Boot auto-discovers this dialect
 * and registers it with the Thymeleaf engine — no manual configuration needed.
 *
 * <p>Templates can then use expressions like {@code ${#auth.isOwner(task)}} to
 * centralize ownership checks instead of duplicating inline boolean logic.
 *
 * @see AuthExpressions
 */
@Component
public class AuthDialect extends AbstractDialect implements IExpressionObjectDialect {

    public AuthDialect() {
        super("auth");
    }

    @Override
    public IExpressionObjectFactory getExpressionObjectFactory() {
        return new AuthExpressionFactory();
    }

    /**
     * Factory that creates a fresh {@link AuthExpressions} per template rendering,
     * populated with the current user from {@link SecurityContextHolder}.
     */
    private static class AuthExpressionFactory implements IExpressionObjectFactory {

        private static final String EXPRESSION_OBJECT_NAME = "auth";

        @Override
        public Set<String> getAllExpressionObjectNames() {
            return Collections.singleton(EXPRESSION_OBJECT_NAME);
        }

        @Override
        public Object buildObject(IExpressionContext context, String expressionObjectName) {
            if (!EXPRESSION_OBJECT_NAME.equals(expressionObjectName)) {
                return null;
            }
            return new AuthExpressions(resolveCurrentUser());
        }

        @Override
        public boolean isCacheable(String expressionObjectName) {
            // User changes per-request; do not cache across renders.
            return false;
        }

        private User resolveCurrentUser() {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && auth.getPrincipal() instanceof CustomUserDetails details) {
                return details.getUser();
            }
            return null;
        }
    }
}
