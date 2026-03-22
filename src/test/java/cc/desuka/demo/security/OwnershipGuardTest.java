package cc.desuka.demo.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class OwnershipGuardTest {

    private OwnershipGuard guard;
    private User alice;
    private User bob;
    private CustomUserDetails aliceDetails;
    private CustomUserDetails bobDetails;

    @BeforeEach
    void setUp() {
        guard = new OwnershipGuard();

        alice = new User("Alice", "alice@example.com", "password", Role.ADMIN);
        alice.setId(1L);
        bob = new User("Bob", "bob@example.com", "password", Role.USER);
        bob.setId(2L);

        aliceDetails = new CustomUserDetails(alice);
        bobDetails = new CustomUserDetails(bob);
    }

    @Test
    void owner_canAccess() {
        Task task = new Task("Test", null);
        task.setUser(bob);

        assertThatCode(() -> guard.requireAccess(task, bobDetails)).doesNotThrowAnyException();
    }

    @Test
    void admin_canAccessAnyEntity() {
        Task task = new Task("Test", null);
        task.setUser(bob); // owned by Bob

        assertThatCode(() -> guard.requireAccess(task, aliceDetails)).doesNotThrowAnyException();
    }

    @Test
    void nonOwnerNonAdmin_throwsAccessDenied() {
        Task task = new Task("Test", null);
        task.setUser(alice); // owned by Alice

        assertThatThrownBy(() -> guard.requireAccess(task, bobDetails))
                .isInstanceOf(AccessDeniedException.class);
    }
}
