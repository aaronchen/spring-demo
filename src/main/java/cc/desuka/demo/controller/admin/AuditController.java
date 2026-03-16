package cc.desuka.demo.controller.admin;

import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.service.AuditLogService;
import cc.desuka.demo.util.HtmxUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Controller
@RequestMapping("/admin/audit")
public class AuditController {

    private final AuditLogService auditLogService;

    public AuditController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String auditLog(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 50, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request,
            Model model) {

        Instant fromInstant = from != null ? from.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant toInstant = to != null ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;

        model.addAttribute("categories", AuditEvent.CATEGORIES);
        model.addAttribute("auditPage",
            auditLogService.searchAuditLogs(category, search, fromInstant, toInstant, pageable));

        if (HtmxUtils.isHtmxRequest(request)) {
            return "admin/audit-table";
        }
        return "admin/audit";
    }
}
