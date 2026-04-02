package cc.desuka.demo.audit;

import java.util.Map;

public interface Auditable {
    Map<String, AuditField> toAuditSnapshot();
}
