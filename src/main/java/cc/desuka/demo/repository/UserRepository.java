package cc.desuka.demo.repository;

import cc.desuka.demo.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    List<User> findAllByOrderByNameAsc();

    List<User> findByNameContainingIgnoreCaseOrderByNameAsc(String name);

    List<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByNameAsc(
            String name, String email);

    List<User> findByEnabledTrueOrderByNameAsc();

    List<User>
            findByEnabledTrueAndNameContainingIgnoreCaseOrEnabledTrueAndEmailContainingIgnoreCaseOrderByNameAsc(
                    String name, String email);
}
