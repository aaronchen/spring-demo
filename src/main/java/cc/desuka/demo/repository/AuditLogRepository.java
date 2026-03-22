package cc.desuka.demo.repository;

import cc.desuka.demo.model.AuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(
            String entityType, Long entityId);

    List<AuditLog> findTop10ByActionInOrderByTimestampDesc(List<String> actions);
}
