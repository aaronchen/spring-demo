package cc.desuka.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "recent_views",
        uniqueConstraints =
                @UniqueConstraint(columnNames = {"user_id", "entity_type", "entity_id"}))
public class RecentView implements OwnedEntity {

    public static final String FIELD_ID = "id";
    public static final String FIELD_USER = "user";
    public static final String FIELD_ENTITY_TYPE = "entityType";
    public static final String FIELD_ENTITY_ID = "entityId";
    public static final String FIELD_ENTITY_TITLE = "entityTitle";
    public static final String FIELD_VIEWED_AT = "viewedAt";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "entity_type", nullable = false, length = 20)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @Column(name = "entity_title", nullable = false, length = 200)
    private String entityTitle;

    @Column(nullable = false)
    private LocalDateTime viewedAt;

    public RecentView() {}

    public RecentView(User user, String entityType, String entityId, String entityTitle) {
        this.user = user;
        this.entityType = entityType;
        this.entityId = entityId;
        this.entityTitle = entityTitle;
        this.viewedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getEntityTitle() {
        return entityTitle;
    }

    public void setEntityTitle(String entityTitle) {
        this.entityTitle = entityTitle;
    }

    public LocalDateTime getViewedAt() {
        return viewedAt;
    }

    public void setViewedAt(LocalDateTime viewedAt) {
        this.viewedAt = viewedAt;
    }
}
