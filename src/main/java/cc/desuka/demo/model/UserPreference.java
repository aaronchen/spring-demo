package cc.desuka.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Objects;

@Entity
@Table(name = "user_preferences",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "pref_key"}))
public class UserPreference {

    public static final String FIELD_ID = "id";
    public static final String FIELD_USER = "user";
    public static final String FIELD_KEY = "key";
    public static final String FIELD_VALUE = "value";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Size(max = 100)
    @Column(name = "pref_key", nullable = false)
    private String key;

    @Size(max = 500)
    @Column(name = "pref_value")
    private String value;

    public UserPreference() {
    }

    public UserPreference(User user, String key, String value) {
        this.user = user;
        this.key = key;
        this.value = value;
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

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserPreference)) return false;
        UserPreference that = (UserPreference) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
