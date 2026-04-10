package cc.desuka.demo.repository;

import cc.desuka.demo.model.UserPreference;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    List<UserPreference> findByUserId(UUID userId);

    Optional<UserPreference> findByUserIdAndKey(UUID userId, String key);

    void deleteByUserId(UUID userId);
}
