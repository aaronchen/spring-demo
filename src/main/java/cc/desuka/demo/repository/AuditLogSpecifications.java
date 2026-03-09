package cc.desuka.demo.repository;

import cc.desuka.demo.model.AuditLog;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public class AuditLogSpecifications {

    public static Specification<AuditLog> withCategory(String category) {
        return (root, query, cb) -> {
            if (category == null || category.isBlank()) return cb.conjunction();
            String prefix = switch (category.toUpperCase()) {
                case "TASK" -> "TASK_%";
                case "USER" -> "USER_%";
                case "TAG"  -> "TAG_%";
                case "AUTH"    -> "LOGIN_%";
                case "SETTING" -> "SETTING_%";
                default        -> null;
            };
            if (prefix == null) return cb.conjunction();
            return cb.like(root.get("action"), prefix);
        };
    }

    public static Specification<AuditLog> withSearch(String search) {
        return (root, query, cb) -> {
            if (search == null || search.trim().isEmpty()) return cb.conjunction();
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("principal")), pattern),
                cb.like(cb.lower(root.get("details")), pattern)
            );
        };
    }

    public static Specification<AuditLog> withFrom(Instant from) {
        return (root, query, cb) -> {
            if (from == null) return cb.conjunction();
            return cb.greaterThanOrEqualTo(root.get("timestamp"), from);
        };
    }

    public static Specification<AuditLog> withTo(Instant to) {
        return (root, query, cb) -> {
            if (to == null) return cb.conjunction();
            return cb.lessThan(root.get("timestamp"), to);
        };
    }

    public static Specification<AuditLog> build(String category, String search,
                                                 Instant from, Instant to) {
        return Specification.where(withCategory(category))
            .and(withSearch(search))
            .and(withFrom(from))
            .and(withTo(to));
    }
}
