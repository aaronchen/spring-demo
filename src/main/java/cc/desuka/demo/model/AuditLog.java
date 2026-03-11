package cc.desuka.demo.model;

import cc.desuka.demo.audit.AuditDetails;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    public static final String FIELD_ACTION = "action";
    public static final String FIELD_PRINCIPAL = "principal";
    public static final String FIELD_DETAILS = "details";
    public static final String FIELD_TIMESTAMP = "timestamp";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action;

    private String entityType;

    private Long entityId;

    @Column(nullable = false)
    private String principal;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    private Instant timestamp;

    public AuditLog() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    // Parsed view of the JSON `details` string — no database column.
    // @Transient: excluded from JPA/Hibernate column mapping.
    // transient: excluded from Java serialization (e.g., HTTP session storage).
    // Lazily parsed on first access via getDetailsMap(); templates use ${entry.detailsMap}.
    @Transient
    private transient Map<String, Object> detailsMap;

    public Map<String, Object> getDetailsMap() {
        if (detailsMap == null) {
            detailsMap = AuditDetails.fromJson(details);
        }
        return detailsMap;
    }

    public void setDetailsMap(Map<String, Object> detailsMap) {
        this.detailsMap = detailsMap;
    }
}
