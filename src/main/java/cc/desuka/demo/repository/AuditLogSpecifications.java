package cc.desuka.demo.repository;

import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.model.AuditLog;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public class AuditLogSpecifications {

    public static Specification<AuditLog> withCategory(String category) {
        return (root, query, cb) -> {
            if (category == null || category.isBlank()) return cb.conjunction();
            String upper = category.toUpperCase();
            if (!AuditEvent.CATEGORIES.contains(upper)) return cb.conjunction();
            return cb.like(root.get(AuditLog.FIELD_ACTION), upper + "_%");
        };
    }

    public static Specification<AuditLog> withSearch(String search) {
        return (root, query, cb) -> {
            if (search == null || search.trim().isEmpty()) return cb.conjunction();
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get(AuditLog.FIELD_PRINCIPAL)), pattern),
                cb.like(cb.lower(root.get(AuditLog.FIELD_DETAILS)), pattern)
            );
        };
    }

    public static Specification<AuditLog> withFrom(Instant from) {
        return (root, query, cb) -> {
            if (from == null) return cb.conjunction();
            return cb.greaterThanOrEqualTo(root.get(AuditLog.FIELD_TIMESTAMP), from);
        };
    }

    public static Specification<AuditLog> withTo(Instant to) {
        return (root, query, cb) -> {
            if (to == null) return cb.conjunction();
            return cb.lessThan(root.get(AuditLog.FIELD_TIMESTAMP), to);
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
