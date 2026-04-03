package cc.desuka.demo.model;

import cc.desuka.demo.audit.AuditField;
import cc.desuka.demo.audit.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

// Inverse side of the @OneToMany relationship — Task owns the FK column (user_id).
// mappedBy = "user" points to the field name in Task, not a column or table name.
@Entity
@Table(name = "users")
public class User implements Auditable {

    public static final String FIELD_ID = "id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_PASSWORD = "password";
    public static final String FIELD_ROLE = "role";
    public static final String FIELD_ENABLED = "enabled";
    public static final String FIELD_TASKS = "tasks";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "{user.name.notBlank}")
    @Size(max = 100, message = "{user.name.size}")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "{user.email.notBlank}")
    @Size(max = 150, message = "{user.email.size}")
    @Column(nullable = false, unique = true)
    private String email;

    // Nullable: API-created users have no password and cannot log in.
    // DataLoader seeds a BCrypt-encoded password for all seeded users.
    // BCrypt hashes are always 60 characters; 72 gives headroom for other algorithms.
    @Column(length = 72)
    private String password;

    // Stored as the enum name string ("USER", "ADMIN") via @Enumerated(STRING).
    // Defaults to USER — registration and API-created users start here.
    // Only admins can promote via /admin/users.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(nullable = false)
    private boolean enabled = true;

    // LAZY: don't load all tasks for this user unless explicitly requested.
    // No cascade: deleting a user does NOT cascade-delete their tasks.
    // UserService.deleteUser() handles reassignment (sets task.user = null) before deletion.
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<Task> tasks = new LinkedHashSet<>();

    public User() {}

    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public User(String name, String email, String password, Role role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set<Task> getTasks() {
        return tasks;
    }

    public void setTasks(Set<Task> tasks) {
        this.tasks = tasks;
    }

    @Override
    public Map<String, AuditField> toAuditSnapshot() {
        Map<String, AuditField> snapshot = new LinkedHashMap<>();
        snapshot.put(FIELD_NAME, AuditField.text(name));
        snapshot.put(FIELD_EMAIL, AuditField.text(email));
        snapshot.put(FIELD_ROLE, AuditField.enumValue(role));
        snapshot.put(FIELD_ENABLED, AuditField.bool(enabled));
        return snapshot;
    }

    // equals/hashCode use getId() (not field access) — Hibernate LAZY proxies
    // intercept getter calls but direct field access bypasses the proxy and returns null.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return Objects.equals(getId(), user.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
