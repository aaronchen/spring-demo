package cc.desuka.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    public static final String FIELD_ID = "id";
    public static final String FIELD_USER = "user";
    public static final String FIELD_ACTOR = "actor";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_MESSAGE = "message";
    public static final String FIELD_LINK = "link";
    public static final String FIELD_READ = "read";
    public static final String FIELD_CREATED_AT = "createdAt";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(length = 500)
    private String link;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Notification() {}

    public Notification(User user, User actor, NotificationType type, String message, String link) {
        this.user = user;
        this.actor = actor;
        this.type = type;
        this.message = message;
        this.link = link;
        this.read = false;
        this.createdAt = LocalDateTime.now();
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

    public User getActor() {
        return actor;
    }

    public void setActor(User actor) {
        this.actor = actor;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
