package cc.desuka.demo.repository;

import cc.desuka.demo.model.Tag;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

    List<Tag> findAllByOrderByNameAsc();

    @Query("SELECT COUNT(t) FROM Task t JOIN t.tags tag WHERE tag.id = :tagId")
    int countTasksByTagId(Long tagId);
}
