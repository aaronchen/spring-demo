package cc.desuka.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "pinned_items",
        uniqueConstraints =
                @UniqueConstraint(columnNames = {"user_id", "entity_type", "entity_id"}))
public class PinnedItem implements OwnedEntity {

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

    @Column(name = "pinned_at", nullable = false)
    private LocalDateTime pinnedAt;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public PinnedItem() {}

    public PinnedItem(User user, String entityType, String entityId, String entityTitle) {
        this.user = user;
        this.entityType = entityType;
        this.entityId = entityId;
        this.entityTitle = entityTitle;
        this.pinnedAt = LocalDateTime.now();
        this.sortOrder = 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
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

    public LocalDateTime getPinnedAt() {
        return pinnedAt;
    }

    public void setPinnedAt(LocalDateTime pinnedAt) {
        this.pinnedAt = pinnedAt;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
