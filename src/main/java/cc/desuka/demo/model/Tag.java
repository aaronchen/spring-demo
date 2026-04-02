package cc.desuka.demo.model;

import cc.desuka.demo.audit.AuditField;
import cc.desuka.demo.audit.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

// Inverse side of the @ManyToMany relationship — Task owns the join table.
// mappedBy = "tags" points to the field name in Task, not a column or table name.
// No @JoinTable here: putting it on the inverse side would create a second (redundant) join table.
@Entity
@Table(name = "tags")
public class Tag implements Auditable {

    public static final String FIELD_ID = "id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_TASKS = "tasks";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "{tag.name.notBlank}")
    @Size(max = 50, message = "{tag.name.size}")
    @Column(nullable = false, unique = true)
    private String name;

    // LAZY: don't load all tasks that share this tag unless explicitly requested.
    // No cascade: deleting a tag removes rows from task_tags, but leaves the tasks intact.
    @ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
    private Set<Task> tasks = new LinkedHashSet<>();

    public Tag() {}

    public Tag(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Task> getTasks() {
        return tasks;
    }

    public void setTasks(Set<Task> tasks) {
        this.tasks = tasks;
    }

    @Override
    public Map<String, AuditField> toAuditSnapshot() {
        return Map.of(FIELD_NAME, AuditField.text(name));
    }

    // equals/hashCode on id only — same reason as always for JPA entities:
    // LAZY proxies only have id populated; comparing by name would give wrong results.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag)) return false;
        Tag tag = (Tag) o;
        return Objects.equals(id, tag.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
