package cc.desuka.demo.controller.api;

import cc.desuka.demo.model.AuditLog;
import cc.desuka.demo.service.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
public class AuditApiController {

    private final AuditLogService auditLogService;

    public AuditApiController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public Page<AuditLog> getAuditLogs(
            @PageableDefault(size = 50, sort = "timestamp",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return auditLogService.getAuditPage(pageable);
    }
}
