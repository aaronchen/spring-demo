package cc.desuka.demo;

import cc.desuka.demo.model.Task;
import cc.desuka.demo.repository.TaskRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

  private final TaskRepository taskRepository;

  public DataLoader(TaskRepository taskRepository) {
    this.taskRepository = taskRepository;
  }

  @Override
  public void run(String... args) {
    List<Task> tasks = List.of(
        seedTask("Fix login rate-limit bug", "Users can brute-force login when retry windows overlap.", true, 120),
        seedTask("Write API pagination docs", "Document cursor and offset pagination behavior for public endpoints.", true, 88),
        seedTask("Refactor task search endpoint", "Split query parsing from repository filters for easier testing.", false, 12),
        seedTask("Add unit tests for TaskService", "Cover edge cases around empty queries and completed filters.", false, 9),
        seedTask("Configure nightly database backup", "Run encrypted dumps at 02:00 and ship to backup bucket.", true, 76),
        seedTask("Review PR #142 payment retry logic", "Validate exponential backoff and max-attempt handling.", false, 5),
        seedTask("Prepare sprint 12 demo script", "Draft talking points and assign demo owners per feature.", true, 40),
        seedTask("Update onboarding checklist", "Add VPN setup, MFA enrollment, and local env verification.", true, 32),
        seedTask("Draft Q2 hiring plan", "Propose headcount by team with expected start dates.", false, 22),
        seedTask("Clean up stale feature flags", "Remove flags older than 90 days that are fully rolled out.", true, 65),
        seedTask("Investigate intermittent 502 errors", "Trace gateway timeouts during peak traffic windows.", false, 3),
        seedTask("Add index on tasks(created_at)", "Improve sort performance for newest-first task list queries.", true, 55),
        seedTask("Migrate email templates to MJML", "Standardize transactional templates with shared components.", false, 18),
        seedTask("Implement CSV export for reports", "Allow finance users to export filtered revenue reports.", false, 14),
        seedTask("Validate webhook signatures", "Reject unsigned requests and log verification failures.", true, 47),
        seedTask("Improve mobile nav spacing", "Fix cramped tap targets on iPhone SE viewport widths.", true, 71),
        seedTask("Resolve SonarQube critical warnings", "Address nullability and SQL injection findings.", false, 8),
        seedTask("Set up synthetic uptime monitor", "Ping health endpoint from three regions every minute.", true, 59),
        seedTask("Publish incident postmortem", "Share timeline, root cause, and corrective actions.", true, 28),
        seedTask("Archive completed Jira epics", "Close old epics and link final release notes.", true, 102),
        seedTask("Reconcile Stripe payout mismatch", "Compare daily ledger against Stripe payout exports.", false, 16),
        seedTask("Confirm SOC2 evidence uploads", "Verify controls evidence was uploaded for the current cycle.", false, 11),
        seedTask("Rotate production API keys", "Issue new keys and revoke previous key set after cutover.", true, 91),
        seedTask("Tune Redis cache eviction policy", "Adjust maxmemory-policy to reduce cache thrashing.", false, 7),
        seedTask("Add dark launch toggle for search", "Enable partial rollout for new relevance algorithm.", true, 36),
        seedTask("Create customer interview summary", "Compile pain points from five recent enterprise calls.", true, 24),
        seedTask("Build churn prediction dashboard", "Visualize risk segments and weekly movement trends.", false, 19),
        seedTask("Update legal terms banner copy", "Reflect new arbitration clause and contact address.", true, 83),
        seedTask("Schedule quarterly access review", "Collect manager approvals for privileged access.", false, 13),
        seedTask("Patch OpenSSL dependency in gateway", "Upgrade to patched version and rerun security scan.", true, 52),
        seedTask("Improve autocomplete relevance", "Boost exact title matches and recent item activity.", false, 6),
        seedTask("Triage support inbox backlog", "Categorize unresolved tickets and assign owners.", false, 4),
        seedTask("Finalize vendor security questionnaire", "Complete questionnaire for enterprise prospect.", true, 61),
        seedTask("Draft release notes v1.9.0", "Summarize new features, bug fixes, and known issues.", false, 10),
        seedTask("Automate weekly KPI email", "Send Monday metrics digest to leadership distribution list.", true, 43),
        seedTask("Standardize error response format", "Return consistent codes, messages, and trace IDs.", false, 15),
        seedTask("Fix timezone handling in reminders", "Store reminder time in UTC and render in user locale.", false, 2),
        seedTask("Add optimistic locking to task edits", "Prevent lost updates on concurrent edits.", false, 17),
        seedTask("Review analytics event taxonomy", "Merge duplicate events and clarify naming conventions.", true, 45),
        seedTask("Backfill missing audit logs", "Reconstruct 30 days of missing admin action entries.", true, 57),
        seedTask("Cleanup orphaned S3 objects", "Remove abandoned uploads older than 60 days.", false, 20),
        seedTask("Benchmark image compression pipeline", "Compare AVIF and WebP latency/quality tradeoffs.", true, 67),
        seedTask("Add retry with jitter for outbound calls", "Reduce thundering herd effect during provider outages.", false, 1),
        seedTask("Document rollback steps for deploys", "Provide one-click rollback and data migration notes.", true, 73),
        seedTask("Implement role-based route guards", "Restrict admin routes based on assigned role claims.", false, 23),
        seedTask("Split monolith billing module", "Extract invoice generation into a standalone service.", false, 27),
        seedTask("Run load test for checkout API", "Simulate 1k RPS and record p95 latency changes.", true, 49),
        seedTask("Track conversion funnel drop-offs", "Add instrumentation between plan select and checkout complete.", false, 26),
        seedTask("Update accessibility labels on forms", "Fix missing aria-label attributes on custom controls.", true, 54),
        seedTask("Verify GDPR delete flow end-to-end", "Ensure user data removal across primary and analytics stores.", false, 29),
        seedTask("Train support team on new workflow", "Host session and share escalation matrix updates.", true, 41),
        seedTask("Add product tour for first-time users", "Show guided highlights for dashboard and task creation.", false, 21),
        seedTask("Rework pricing page FAQ content", "Address common objections and billing edge cases.", true, 64),
        seedTask("Set SLA alerts for failed jobs", "Trigger pager alerts when SLA thresholds are crossed.", false, 25),
        seedTask("Build monthly revenue cohort report", "Group customer spend by signup month and segment.", false, 33),
        seedTask("Fix duplicate invoice generation", "Guard against duplicate retries in invoice worker.", true, 58),
        seedTask("Enable Brotli compression in CDN", "Lower transfer size for JS and CSS bundles.", true, 84),
        seedTask("Add smoke tests to CI pipeline", "Run critical-path API tests after every deployment.", false, 30),
        seedTask("Remove deprecated v1 auth endpoint", "Deprecate endpoint and update client SDK references.", true, 96),
        seedTask("Investigate memory spike after deploy", "Profile heap growth in API pods after rollout.", false, 0),
        seedTask("Create runbook for cache warmup", "Document warmup order and expected completion metrics.", true, 50),
        seedTask("Normalize phone number formatting", "Store phone numbers in E.164 before persistence.", true, 69),
        seedTask("Add search by assignee email", "Support filtering tasks via assignee email query.", false, 31),
        seedTask("Improve dashboard first paint time", "Defer non-critical widgets and optimize bundle splits.", false, 34),
        seedTask("Define data retention policy", "Set retention periods for logs, events, and attachments.", true, 79),
        seedTask("Audit admin permissions by role", "Review and remove unnecessary high-privilege grants.", false, 35),
        seedTask("Integrate feature flags with LaunchDarkly", "Sync environment defaults and rollout rules.", false, 37),
        seedTask("Refine recommendation model thresholds", "Tune precision/recall based on last month results.", true, 93),
        seedTask("Set up canary deployment strategy", "Ship 10 percent traffic first and auto-halt on errors.", false, 39),
        seedTask("Add localization for Spanish locale", "Translate primary navigation, forms, and empty states.", false, 42),
        seedTask("Reconcile warehouse inventory variance", "Investigate mismatch between WMS and finance system.", true, 99),
        seedTask("Validate tax calculations for EU orders", "Confirm VAT rates and reverse-charge handling.", false, 44),
        seedTask("Replace cron digest with queue worker", "Move digest processing to resilient queued jobs.", true, 62),
        seedTask("Add idempotency keys to order creation", "Prevent duplicate orders on network retries.", false, 38),
        seedTask("Clean and dedupe CRM contact list", "Merge duplicates and normalize account ownership.", true, 86),
        seedTask("Publish engineering onboarding video", "Record environment setup and workflow walkthrough.", true, 92),
        seedTask("Implement optimistic UI for comments", "Show immediate updates before server confirmation.", false, 46),
        seedTask("Add server-side sort by priority", "Support ascending and descending priority ordering.", false, 48),
        seedTask("Create template library for campaigns", "Provide reusable email blocks for marketing team.", true, 72),
        seedTask("Tune PostgreSQL connection pool limits", "Reduce saturation during peak cron execution.", false, 53),
        seedTask("Prepare board update metrics deck", "Assemble growth, churn, and profitability slides.", true, 80),
        seedTask("Investigate abandoned cart email delay", "Trace delay between event ingestion and send job.", false, 51),
        seedTask("Add image alt-text review checklist", "Ensure every uploaded image includes alt text.", true, 77),
        seedTask("Upgrade Kubernetes cluster to 1.30", "Validate API compatibility and rollout node groups.", false, 56),
        seedTask("Configure WAF rules for bot traffic", "Block common scrapers and challenge suspicious IPs.", false, 63),
        seedTask("Build internal status page widget", "Display service health states inside admin dashboard.", true, 87),
        seedTask("Add export to PDF for invoices", "Generate branded invoice PDFs with payment metadata.", false, 60),
        seedTask("Run privacy impact assessment", "Assess analytics events against privacy principles.", true, 95),
        seedTask("Improve checkout error copy", "Rewrite payment errors with actionable guidance.", true, 68),
        seedTask("Add fraud-score threshold alerts", "Alert risk team when transaction scores exceed threshold.", false, 66),
        seedTask("Migrate legacy cron jobs to scheduler", "Consolidate jobs under centralized scheduler service.", false, 74),
        seedTask("Fix race in notification batching", "Synchronize batch close and delivery scheduling.", false, 70),
        seedTask("Create QA plan for mobile release", "Define device matrix and regression checklist.", true, 78),
        seedTask("Update partner API auth docs", "Clarify token rotation and expiration semantics.", true, 82),
        seedTask("Reduce noisy alerts from staging", "Adjust thresholds and silence non-actionable failures.", false, 75),
        seedTask("Add retention chart to dashboard", "Expose 30, 60, and 90 day retention segments.", false, 81),
        seedTask("Implement bulk task completion action", "Allow selecting multiple tasks for one-click completion.", false, 85),
        seedTask("Validate backups with restore drill", "Perform restore rehearsal and capture RTO metrics.", true, 97),
        seedTask("Document event bus architecture decision", "Record why Kafka was chosen over alternatives.", true, 101),
        seedTask("Plan offsite agenda and logistics", "Finalize venue, agenda, and travel schedule for team offsite.", false, 89)
    );

    taskRepository.saveAll(tasks);

    System.out.println("Realistic seed data loaded.");
    System.out.println("Tasks in database: " + taskRepository.count());
  }

  private Task seedTask(String title, String description, boolean completed, int daysAgo) {
    Task task = new Task(title, description);
    task.setCompleted(completed);
    task.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
    return task;
  }
}
