package cc.desuka.demo.validation;

import static org.assertj.core.api.Assertions.assertThat;

import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.UserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({UniqueValidator.class, ValidationAutoConfiguration.class})
class UniqueValidatorTest {

    @Autowired private TestEntityManager em;
    @Autowired private UserRepository userRepository;
    @Autowired private Validator validator;

    @Unique(entity = User.class, field = User.FIELD_EMAIL, message = "Email already exists")
    static class TestUserDto {
        private UUID id;
        private String email;

        TestUserDto(UUID id, String email) {
            this.id = id;
            this.email = email;
        }

        public UUID getId() {
            return id;
        }

        public String getEmail() {
            return email;
        }
    }

    @BeforeEach
    void setUp() {
        em.persist(new User("Alice", "alice@example.com", "password", Role.USER));
        em.flush();
    }

    @Test
    void uniqueEmail_passes() {
        TestUserDto dto = new TestUserDto(null, "new@example.com");

        Set<ConstraintViolation<TestUserDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    void duplicateEmail_fails() {
        TestUserDto dto = new TestUserDto(null, "alice@example.com");

        Set<ConstraintViolation<TestUserDto>> violations = validator.validate(dto);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Email already exists");
    }

    @Test
    void duplicateEmail_caseInsensitive() {
        TestUserDto dto = new TestUserDto(null, "ALICE@example.com");

        Set<ConstraintViolation<TestUserDto>> violations = validator.validate(dto);

        assertThat(violations).hasSize(1);
    }

    @Test
    void selfExclusion_onUpdate_passes() {
        User existing = userRepository.findByEmail("alice@example.com").orElseThrow();
        TestUserDto dto = new TestUserDto(existing.getId(), "alice@example.com");

        Set<ConstraintViolation<TestUserDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    void nullEmail_passes_letNotBlankHandleIt() {
        TestUserDto dto = new TestUserDto(null, null);

        Set<ConstraintViolation<TestUserDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    void blankEmail_passes_letNotBlankHandleIt() {
        TestUserDto dto = new TestUserDto(null, "  ");

        Set<ConstraintViolation<TestUserDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }
}
