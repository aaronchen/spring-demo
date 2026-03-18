package cc.desuka.demo;

import cc.desuka.demo.audit.AuditDetails;
import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.model.AuditLog;
import cc.desuka.demo.model.ChecklistItem;
import cc.desuka.demo.model.Comment;
import cc.desuka.demo.model.Notification;
import cc.desuka.demo.model.NotificationType;
import cc.desuka.demo.model.Priority;
import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.Setting;
import cc.desuka.demo.config.Settings;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.model.Tag;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.AuditLogRepository;
import cc.desuka.demo.repository.CommentRepository;
import cc.desuka.demo.repository.NotificationRepository;
import cc.desuka.demo.repository.SettingRepository;
import cc.desuka.demo.repository.TagRepository;
import cc.desuka.demo.repository.TaskRepository;
import cc.desuka.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DataLoader implements CommandLineRunner {

  private final TaskRepository taskRepository;
  private final TagRepository tagRepository;
  private final UserRepository userRepository;
  private final CommentRepository commentRepository;
  private final NotificationRepository notificationRepository;
  private final AuditLogRepository auditLogRepository;
  private final SettingRepository settingRepository;
  private final PasswordEncoder passwordEncoder;

  public DataLoader(TaskRepository taskRepository, TagRepository tagRepository,
                    UserRepository userRepository, CommentRepository commentRepository,
                    NotificationRepository notificationRepository,
                    AuditLogRepository auditLogRepository,
                    SettingRepository settingRepository,
                    PasswordEncoder passwordEncoder) {
    this.taskRepository = taskRepository;
    this.tagRepository = tagRepository;
    this.userRepository = userRepository;
    this.commentRepository = commentRepository;
    this.notificationRepository = notificationRepository;
    this.auditLogRepository = auditLogRepository;
    this.settingRepository = settingRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public void run(String... args) {
    List<Task> tasks = new ArrayList<>();

    // ── Original 100 tasks (unique dates, spread across 120 days) ─────────────
    tasks.addAll(List.of(
        seedTask("Fix login rate-limit bug", "Users can brute-force login when retry windows overlap.", TaskStatus.COMPLETED,120),
        seedTask("Write API pagination docs", "Document cursor and offset pagination behavior for public endpoints.", TaskStatus.COMPLETED,88),
        seedTask("Refactor task search endpoint", "Split query parsing from repository filters for easier testing.", TaskStatus.IN_PROGRESS,12),
        seedTask("Add unit tests for TaskService", "Cover edge cases around empty queries and completed filters.", TaskStatus.OPEN,9),
        seedTask("Configure nightly database backup", "Run encrypted dumps at 02:00 and ship to backup bucket.", TaskStatus.COMPLETED,76),
        seedTask("Review PR #142 payment retry logic", "Validate exponential backoff and max-attempt handling.", TaskStatus.IN_PROGRESS,5),
        seedTask("Prepare sprint 12 demo script", "Draft talking points and assign demo owners per feature.", TaskStatus.COMPLETED,40),
        seedTask("Update onboarding checklist", "Add VPN setup, MFA enrollment, and local env verification.", TaskStatus.COMPLETED,32),
        seedTask("Draft Q2 hiring plan", "Propose headcount by team with expected start dates.", TaskStatus.OPEN,22),
        seedTask("Clean up stale feature flags", "Remove flags older than 90 days that are fully rolled out.", TaskStatus.COMPLETED,65),
        seedTask("Investigate intermittent 502 errors", "Trace gateway timeouts during peak traffic windows.", TaskStatus.IN_PROGRESS,3),
        seedTask("Add index on tasks(created_at)", "Improve sort performance for newest-first task list queries.", TaskStatus.COMPLETED,55),
        seedTask("Migrate email templates to MJML", "Standardize transactional templates with shared components.", TaskStatus.IN_PROGRESS,18),
        seedTask("Implement CSV export for reports", "Allow finance users to export filtered revenue reports.", TaskStatus.IN_PROGRESS,14),
        seedTask("Validate webhook signatures", "Reject unsigned requests and log verification failures.", TaskStatus.COMPLETED,47),
        seedTask("Improve mobile nav spacing", "Fix cramped tap targets on iPhone SE viewport widths.", TaskStatus.COMPLETED,71),
        seedTask("Resolve SonarQube critical warnings", "Address nullability and SQL injection findings.", TaskStatus.OPEN,8),
        seedTask("Set up synthetic uptime monitor", "Ping health endpoint from three regions every minute.", TaskStatus.COMPLETED,59),
        seedTask("Publish incident postmortem", "Share timeline, root cause, and corrective actions.", TaskStatus.COMPLETED,28),
        seedTask("Archive completed Jira epics", "Close old epics and link final release notes.", TaskStatus.COMPLETED,102)
    ));
    tasks.addAll(List.of(
        seedTask("Reconcile Stripe payout mismatch", "Compare daily ledger against Stripe payout exports.", TaskStatus.IN_PROGRESS,16),
        seedTask("Confirm SOC2 evidence uploads", "Verify controls evidence was uploaded for the current cycle.", TaskStatus.OPEN,11),
        seedTask("Rotate production API keys", "Issue new keys and revoke previous key set after cutover.", TaskStatus.COMPLETED,91),
        seedTask("Tune Redis cache eviction policy", "Adjust maxmemory-policy to reduce cache thrashing.", TaskStatus.OPEN,7),
        seedTask("Add dark launch toggle for search", "Enable partial rollout for new relevance algorithm.", TaskStatus.COMPLETED,36),
        seedTask("Create customer interview summary", "Compile pain points from five recent enterprise calls.", TaskStatus.COMPLETED,24),
        seedTask("Build churn prediction dashboard", "Visualize risk segments and weekly movement trends.", TaskStatus.IN_PROGRESS,19),
        seedTask("Update legal terms banner copy", "Reflect new arbitration clause and contact address.", TaskStatus.COMPLETED,83),
        seedTask("Schedule quarterly access review", "Collect manager approvals for privileged access.", TaskStatus.OPEN,13),
        seedTask("Patch OpenSSL dependency in gateway", "Upgrade to patched version and rerun security scan.", TaskStatus.COMPLETED,52),
        seedTask("Improve autocomplete relevance", "Boost exact title matches and recent item activity.", TaskStatus.OPEN,6),
        seedTask("Triage support inbox backlog", "Categorize unresolved tickets and assign owners.", TaskStatus.IN_PROGRESS,4),
        seedTask("Finalize vendor security questionnaire", "Complete questionnaire for enterprise prospect.", TaskStatus.COMPLETED,61),
        seedTask("Draft release notes v1.9.0", "Summarize new features, bug fixes, and known issues.", TaskStatus.OPEN,10),
        seedTask("Automate weekly KPI email", "Send Monday metrics digest to leadership distribution list.", TaskStatus.COMPLETED,43),
        seedTask("Standardize error response format", "Return consistent codes, messages, and trace IDs.", TaskStatus.OPEN,15),
        seedTask("Fix timezone handling in reminders", "Store reminder time in UTC and render in user locale.", TaskStatus.IN_PROGRESS,2),
        seedTask("Add optimistic locking to task edits", "Prevent lost updates on concurrent edits.", TaskStatus.OPEN,17),
        seedTask("Review analytics event taxonomy", "Merge duplicate events and clarify naming conventions.", TaskStatus.COMPLETED,45),
        seedTask("Backfill missing audit logs", "Reconstruct 30 days of missing admin action entries.", TaskStatus.COMPLETED,57)
    ));
    tasks.addAll(List.of(
        seedTask("Cleanup orphaned S3 objects", "Remove abandoned uploads older than 60 days.", TaskStatus.OPEN,20),
        seedTask("Benchmark image compression pipeline", "Compare AVIF and WebP latency/quality tradeoffs.", TaskStatus.COMPLETED,67),
        seedTask("Add retry with jitter for outbound calls", "Reduce thundering herd effect during provider outages.", TaskStatus.OPEN,1),
        seedTask("Document rollback steps for deploys", "Provide one-click rollback and data migration notes.", TaskStatus.COMPLETED,73),
        seedTask("Implement role-based route guards", "Restrict admin routes based on assigned role claims.", TaskStatus.OPEN,23),
        seedTask("Split monolith billing module", "Extract invoice generation into a standalone service.", TaskStatus.OPEN,27),
        seedTask("Run load test for checkout API", "Simulate 1k RPS and record p95 latency changes.", TaskStatus.COMPLETED,49),
        seedTask("Track conversion funnel drop-offs", "Add instrumentation between plan select and checkout complete.", TaskStatus.OPEN,26),
        seedTask("Update accessibility labels on forms", "Fix missing aria-label attributes on custom controls.", TaskStatus.COMPLETED,54),
        seedTask("Verify GDPR delete flow end-to-end", "Ensure user data removal across primary and analytics stores.", TaskStatus.OPEN,29),
        seedTask("Train support team on new workflow", "Host session and share escalation matrix updates.", TaskStatus.COMPLETED,41),
        seedTask("Add product tour for first-time users", "Show guided highlights for dashboard and task creation.", TaskStatus.OPEN,21),
        seedTask("Rework pricing page FAQ content", "Address common objections and billing edge cases.", TaskStatus.COMPLETED,64),
        seedTask("Set SLA alerts for failed jobs", "Trigger pager alerts when SLA thresholds are crossed.", TaskStatus.OPEN,25),
        seedTask("Build monthly revenue cohort report", "Group customer spend by signup month and segment.", TaskStatus.OPEN,33),
        seedTask("Fix duplicate invoice generation", "Guard against duplicate retries in invoice worker.", TaskStatus.COMPLETED,58),
        seedTask("Enable Brotli compression in CDN", "Lower transfer size for JS and CSS bundles.", TaskStatus.COMPLETED,84),
        seedTask("Add smoke tests to CI pipeline", "Run critical-path API tests after every deployment.", TaskStatus.OPEN,30),
        seedTask("Remove deprecated v1 auth endpoint", "Deprecate endpoint and update client SDK references.", TaskStatus.COMPLETED,96),
        seedTask("Investigate memory spike after deploy", "Profile heap growth in API pods after rollout.", TaskStatus.OPEN,0)
    ));
    tasks.addAll(List.of(
        seedTask("Create runbook for cache warmup", "Document warmup order and expected completion metrics.", TaskStatus.COMPLETED,50),
        seedTask("Normalize phone number formatting", "Store phone numbers in E.164 before persistence.", TaskStatus.COMPLETED,69),
        seedTask("Add search by assignee email", "Support filtering tasks via assignee email query.", TaskStatus.OPEN,31),
        seedTask("Improve dashboard first paint time", "Defer non-critical widgets and optimize bundle splits.", TaskStatus.OPEN,34),
        seedTask("Define data retention policy", "Set retention periods for logs, events, and attachments.", TaskStatus.COMPLETED,79),
        seedTask("Audit admin permissions by role", "Review and remove unnecessary high-privilege grants.", TaskStatus.OPEN,35),
        seedTask("Integrate feature flags with LaunchDarkly", "Sync environment defaults and rollout rules.", TaskStatus.OPEN,37),
        seedTask("Refine recommendation model thresholds", "Tune precision/recall based on last month results.", TaskStatus.COMPLETED,93),
        seedTask("Set up canary deployment strategy", "Ship 10 percent traffic first and auto-halt on errors.", TaskStatus.OPEN,39),
        seedTask("Add localization for Spanish locale", "Translate primary navigation, forms, and empty states.", TaskStatus.OPEN,42),
        seedTask("Reconcile warehouse inventory variance", "Investigate mismatch between WMS and finance system.", TaskStatus.COMPLETED,99),
        seedTask("Validate tax calculations for EU orders", "Confirm VAT rates and reverse-charge handling.", TaskStatus.OPEN,44),
        seedTask("Replace cron digest with queue worker", "Move digest processing to resilient queued jobs.", TaskStatus.COMPLETED,62),
        seedTask("Add idempotency keys to order creation", "Prevent duplicate orders on network retries.", TaskStatus.OPEN,38),
        seedTask("Clean and dedupe CRM contact list", "Merge duplicates and normalize account ownership.", TaskStatus.COMPLETED,86),
        seedTask("Publish engineering onboarding video", "Record environment setup and workflow walkthrough.", TaskStatus.COMPLETED,92),
        seedTask("Implement optimistic UI for comments", "Show immediate updates before server confirmation.", TaskStatus.OPEN,46),
        seedTask("Add server-side sort by priority", "Support ascending and descending priority ordering.", TaskStatus.OPEN,48),
        seedTask("Create template library for campaigns", "Provide reusable email blocks for marketing team.", TaskStatus.COMPLETED,72),
        seedTask("Tune PostgreSQL connection pool limits", "Reduce saturation during peak cron execution.", TaskStatus.OPEN,53)
    ));
    tasks.addAll(List.of(
        seedTask("Prepare board update metrics deck", "Assemble growth, churn, and profitability slides.", TaskStatus.COMPLETED,80),
        seedTask("Investigate abandoned cart email delay", "Trace delay between event ingestion and send job.", TaskStatus.OPEN,51),
        seedTask("Add image alt-text review checklist", "Ensure every uploaded image includes alt text.", TaskStatus.COMPLETED,77),
        seedTask("Upgrade Kubernetes cluster to 1.30", "Validate API compatibility and rollout node groups.", TaskStatus.OPEN,56),
        seedTask("Configure WAF rules for bot traffic", "Block common scrapers and challenge suspicious IPs.", TaskStatus.OPEN,63),
        seedTask("Build internal status page widget", "Display service health states inside admin dashboard.", TaskStatus.COMPLETED,87),
        seedTask("Add export to PDF for invoices", "Generate branded invoice PDFs with payment metadata.", TaskStatus.OPEN,60),
        seedTask("Run privacy impact assessment", "Assess analytics events against privacy principles.", TaskStatus.COMPLETED,95),
        seedTask("Improve checkout error copy", "Rewrite payment errors with actionable guidance.", TaskStatus.COMPLETED,68),
        seedTask("Add fraud-score threshold alerts", "Alert risk team when transaction scores exceed threshold.", TaskStatus.OPEN,66),
        seedTask("Migrate legacy cron jobs to scheduler", "Consolidate jobs under centralized scheduler service.", TaskStatus.OPEN,74),
        seedTask("Fix race in notification batching", "Synchronize batch close and delivery scheduling.", TaskStatus.OPEN,70),
        seedTask("Create QA plan for mobile release", "Define device matrix and regression checklist.", TaskStatus.COMPLETED,78),
        seedTask("Update partner API auth docs", "Clarify token rotation and expiration semantics.", TaskStatus.COMPLETED,82),
        seedTask("Reduce noisy alerts from staging", "Adjust thresholds and silence non-actionable failures.", TaskStatus.OPEN,75),
        seedTask("Add retention chart to dashboard", "Expose 30, 60, and 90 day retention segments.", TaskStatus.OPEN,81),
        seedTask("Implement bulk task completion action", "Allow selecting multiple tasks for one-click completion.", TaskStatus.OPEN,85),
        seedTask("Validate backups with restore drill", "Perform restore rehearsal and capture RTO metrics.", TaskStatus.COMPLETED,97),
        seedTask("Document event bus architecture decision", "Record why Kafka was chosen over alternatives.", TaskStatus.COMPLETED,101),
        seedTask("Plan offsite agenda and logistics", "Finalize venue, agenda, and travel schedule for team offsite.", TaskStatus.OPEN,89)
    ));

    // ── 200 additional tasks grouped by date (10 per day) ─────────────────────
    // Intentional date clusters for multi-sort testing.
    // Each group shares the same daysAgo, so tasks differ only by title/description/completed.

    // Day 0 – today
    tasks.addAll(List.of(
        seedTask("Draft sprint planning agenda", "Outline goals, capacity, and backlog items for next sprint.", TaskStatus.OPEN,0),
        seedTask("Review A/B test results", "Analyze click-through rates from last week's experiment.", TaskStatus.OPEN,0),
        seedTask("Fix padding on mobile cards", "Adjust card padding for screens under 400px wide.", TaskStatus.COMPLETED,0),
        seedTask("Update team OKR tracker", "Reflect progress from last week across all objectives.", TaskStatus.OPEN,0),
        seedTask("Check CDN cache-hit ratio", "Review Cloudflare analytics for cache performance.", TaskStatus.COMPLETED,0),
        seedTask("Write weekly engineering blog post", "Summarize key technical decisions made this week.", TaskStatus.OPEN,0),
        seedTask("Test email unsubscribe flow", "Verify one-click unsubscribe complies with CAN-SPAM.", TaskStatus.COMPLETED,0),
        seedTask("Sync with design on new icons", "Align on icon set before frontend integration begins.", TaskStatus.OPEN,0),
        seedTask("Deploy hotfix for null pointer", "Push fix for NPE in invoice rendering to production.", TaskStatus.COMPLETED,0),
        seedTask("Archive old release branches", "Delete merged release branches older than 6 months.", TaskStatus.OPEN,0)
    ));

    // Day 1
    tasks.addAll(List.of(
        seedTask("Refactor user permission checks", "Extract permission logic into a reusable guard utility.", TaskStatus.OPEN,1),
        seedTask("Add API rate limit headers", "Return X-RateLimit-Remaining and X-RateLimit-Reset headers.", TaskStatus.COMPLETED,1),
        seedTask("Review marketing site copy", "Proofread homepage and pricing page for tone consistency.", TaskStatus.OPEN,1),
        seedTask("Create DB migration for tags", "Add tags table and task_tags join table.", TaskStatus.OPEN,1),
        seedTask("Investigate slow report query", "Profile the monthly revenue report query taking 12s.", TaskStatus.COMPLETED,1),
        seedTask("Update Dockerfile base image", "Upgrade from node:18 to node:22 in Dockerfile.", TaskStatus.COMPLETED,1),
        seedTask("Prepare legal hold notice", "Draft preservation notice for pending litigation matter.", TaskStatus.OPEN,1),
        seedTask("Fix broken pagination in API", "Handle edge case when page exceeds total page count.", TaskStatus.COMPLETED,1),
        seedTask("Audit log retention review", "Confirm audit logs are retained per compliance policy.", TaskStatus.OPEN,1),
        seedTask("Update status page incident", "Post resolution details and timeline for yesterday's outage.", TaskStatus.COMPLETED,1)
    ));

    // Day 2
    tasks.addAll(List.of(
        seedTask("Implement password complexity rules", "Enforce minimum length, mixed case, and special characters.", TaskStatus.OPEN,2),
        seedTask("Create product demo video", "Record 90-second walkthrough of core task management flow.", TaskStatus.OPEN,2),
        seedTask("Migrate user avatars to S3", "Move avatar storage from local disk to object storage.", TaskStatus.COMPLETED,2),
        seedTask("Add logging to payment webhook", "Log raw payload and parsed fields before processing.", TaskStatus.OPEN,2),
        seedTask("Review competitor pricing changes", "Analyze pricing updates from three main competitors.", TaskStatus.COMPLETED,2),
        seedTask("Fix CORS policy for staging", "Allow staging subdomain in CORS allowed origins list.", TaskStatus.COMPLETED,2),
        seedTask("Draft data breach response plan", "Define notification timeline and escalation procedures.", TaskStatus.OPEN,2),
        seedTask("Optimize bundle splitting config", "Tune Webpack chunks for better code splitting.", TaskStatus.COMPLETED,2),
        seedTask("Document database schema changes", "Update schema docs for tags and assignments tables.", TaskStatus.OPEN,2),
        seedTask("Resolve merge conflict in main", "Fix conflicts from parallel feature branches.", TaskStatus.COMPLETED,2)
    ));

    // Day 3
    tasks.addAll(List.of(
        seedTask("Set up Grafana dashboard", "Create panels for API latency, error rate, and throughput.", TaskStatus.COMPLETED,3),
        seedTask("Review pull request feedback", "Address comments on the auth refactor PR.", TaskStatus.OPEN,3),
        seedTask("Add input sanitization to search", "Strip HTML and SQL metacharacters from search input.", TaskStatus.COMPLETED,3),
        seedTask("Draft partner integration guide", "Write step-by-step OAuth2 integration guide for partners.", TaskStatus.OPEN,3),
        seedTask("Clean up test data in staging", "Remove test accounts created during load testing.", TaskStatus.COMPLETED,3),
        seedTask("Configure alerting for disk usage", "Alert when disk exceeds 80 percent on app servers.", TaskStatus.OPEN,3),
        seedTask("Update changelog for v2.0.0", "Document all breaking changes and migration steps.", TaskStatus.COMPLETED,3),
        seedTask("Fix email encoding for special chars", "Correct UTF-8 encoding issue in email subject lines.", TaskStatus.OPEN,3),
        seedTask("Coordinate infrastructure upgrade", "Schedule maintenance window for database version upgrade.", TaskStatus.COMPLETED,3),
        seedTask("Review SaaS vendor contracts", "Check renewal dates and pricing for three key tools.", TaskStatus.OPEN,3)
    ));

    // Day 4
    tasks.addAll(List.of(
        seedTask("Write API migration guide for v2", "Document breaking changes and client update steps.", TaskStatus.OPEN,4),
        seedTask("Fix hover state on action buttons", "Restore missing hover background on card action buttons.", TaskStatus.COMPLETED,4),
        seedTask("Automate dependency vulnerability scan", "Add OWASP dependency check to CI pipeline.", TaskStatus.OPEN,4),
        seedTask("Prepare sales enablement deck", "Create slides for technical deep-dive sales calls.", TaskStatus.COMPLETED,4),
        seedTask("Resolve DNS propagation issue", "Investigate DNS TTL causing stale routing after migration.", TaskStatus.OPEN,4),
        seedTask("Add content security policy header", "Configure CSP header to prevent XSS attacks.", TaskStatus.COMPLETED,4),
        seedTask("Implement user preferences API", "Store UI preferences like theme and density per user.", TaskStatus.OPEN,4),
        seedTask("Benchmark concurrent write performance", "Measure throughput under 500 concurrent insert operations.", TaskStatus.COMPLETED,4),
        seedTask("Update terms of service document", "Reflect new data processing and AI feature disclosures.", TaskStatus.OPEN,4),
        seedTask("Fix broken links in email templates", "Update outdated URLs in transactional email footers.", TaskStatus.COMPLETED,4)
    ));

    // Day 5
    tasks.addAll(List.of(
        seedTask("Build task assignment feature", "Allow tasks to be assigned to team members by email.", TaskStatus.OPEN,5),
        seedTask("Add export button to reports page", "Trigger CSV download from the reports overview screen.", TaskStatus.COMPLETED,5),
        seedTask("Review open support tickets", "Triage tickets older than 48 hours without a response.", TaskStatus.OPEN,5),
        seedTask("Implement soft delete for tasks", "Mark tasks as deleted instead of removing from database.", TaskStatus.COMPLETED,5),
        seedTask("Update privacy policy document", "Add language for new data processing activities.", TaskStatus.OPEN,5),
        seedTask("Test SMS two-factor authentication", "Verify OTP delivery and expiry across mobile carriers.", TaskStatus.COMPLETED,5),
        seedTask("Analyze search abandonment rate", "Check where users abandon search without clicking results.", TaskStatus.OPEN,5),
        seedTask("Fix session timeout behavior", "Log out user gracefully with redirect on session expiry.", TaskStatus.COMPLETED,5),
        seedTask("Prepare Q3 budget forecast", "Estimate infrastructure and headcount costs for Q3.", TaskStatus.OPEN,5),
        seedTask("Add health check to all services", "Expose /health endpoint returning service status and version.", TaskStatus.COMPLETED,5)
    ));

    // Day 6
    tasks.addAll(List.of(
        seedTask("Set up distributed tracing", "Integrate OpenTelemetry SDK across backend services.", TaskStatus.OPEN,6),
        seedTask("Review customer success metrics", "Analyze NPS, CSAT, and expansion revenue trends.", TaskStatus.COMPLETED,6),
        seedTask("Build reusable form validation library", "Create shared validators for common field patterns.", TaskStatus.OPEN,6),
        seedTask("Implement multi-tenant data isolation", "Enforce tenant ID checks at repository layer.", TaskStatus.COMPLETED,6),
        seedTask("Draft API deprecation notice", "Notify API consumers of v1 endpoint sunset timeline.", TaskStatus.OPEN,6),
        seedTask("Fix image upload size validation", "Enforce 10MB limit and return clear error message.", TaskStatus.COMPLETED,6),
        seedTask("Create database ERD diagram", "Generate and publish current entity relationship diagram.", TaskStatus.OPEN,6),
        seedTask("Add retry logic to file uploads", "Resume interrupted uploads using chunked transfer protocol.", TaskStatus.COMPLETED,6),
        seedTask("Review quarterly OKR results", "Score objective completion and document key learnings.", TaskStatus.OPEN,6),
        seedTask("Configure log rotation policy", "Set retention to 30 days and compress archives after 7.", TaskStatus.COMPLETED,6)
    ));

    // Day 7
    tasks.addAll(List.of(
        seedTask("Refactor notification templates", "Extract content into separate template files per channel.", TaskStatus.OPEN,7),
        seedTask("Set up code coverage reporting", "Integrate Jacoco and publish HTML reports to CI artifacts.", TaskStatus.COMPLETED,7),
        seedTask("Write integration tests for API", "Cover happy path and error cases for tasks CRUD API.", TaskStatus.OPEN,7),
        seedTask("Add audit trail for role changes", "Log who changed a user role and when.", TaskStatus.COMPLETED,7),
        seedTask("Resolve test flakiness in CI", "Identify and fix intermittently failing Cypress tests.", TaskStatus.OPEN,7),
        seedTask("Design new empty state screens", "Create illustrations for empty task list and search states.", TaskStatus.COMPLETED,7),
        seedTask("Build team performance report", "Aggregate task completion rates by team for last month.", TaskStatus.OPEN,7),
        seedTask("Update API client SDKs", "Regenerate SDKs from updated OpenAPI specification.", TaskStatus.COMPLETED,7),
        seedTask("Review feature flag audit log", "Check which flags were enabled and disabled last week.", TaskStatus.OPEN,7),
        seedTask("Add custom domain support", "Allow users to use their own domain for the platform.", TaskStatus.COMPLETED,7)
    ));

    // Day 8
    tasks.addAll(List.of(
        seedTask("Analyze conversion rate by plan", "Compare trial-to-paid conversion across pricing tiers.", TaskStatus.OPEN,8),
        seedTask("Add pagination to search results", "Implement offset and size params for search API.", TaskStatus.COMPLETED,8),
        seedTask("Write ADR for message queue choice", "Document why RabbitMQ was chosen over SQS.", TaskStatus.OPEN,8),
        seedTask("Fix chart colors in dark mode", "Ensure chart palette meets WCAG contrast in dark mode.", TaskStatus.COMPLETED,8),
        seedTask("Add bulk import for tasks", "Support CSV upload to create tasks in bulk.", TaskStatus.OPEN,8),
        seedTask("Review infrastructure alert fatigue", "Consolidate noisy alerts and raise meaningful thresholds.", TaskStatus.COMPLETED,8),
        seedTask("Implement granular permissions", "Add action-level permissions beyond role-level access.", TaskStatus.OPEN,8),
        seedTask("Refactor date utility functions", "Replace custom date logic with standard library methods.", TaskStatus.COMPLETED,8),
        seedTask("Build client activity feed", "Show recent task events in client-facing account view.", TaskStatus.OPEN,8),
        seedTask("Update security incident playbook", "Add steps for credential exposure and data leak scenarios.", TaskStatus.COMPLETED,8)
    ));

    // Day 10
    tasks.addAll(List.of(
        seedTask("Implement webhook retry mechanism", "Retry failed webhooks with exponential backoff up to 5 times.", TaskStatus.OPEN,10),
        seedTask("Set up A/B testing framework", "Integrate framework to run controlled UI experiments.", TaskStatus.COMPLETED,10),
        seedTask("Create onboarding email sequence", "Write 5-part welcome series for new user activation.", TaskStatus.OPEN,10),
        seedTask("Validate PCI DSS compliance scope", "Review card data flows against PCI DSS requirements.", TaskStatus.COMPLETED,10),
        seedTask("Fix timestamp display in UI", "Show relative timestamps and add absolute on hover.", TaskStatus.OPEN,10),
        seedTask("Document internal API contracts", "Write service interface contracts for all internal APIs.", TaskStatus.COMPLETED,10),
        seedTask("Build admin user management page", "Allow admins to view, suspend, and delete users.", TaskStatus.OPEN,10),
        seedTask("Optimize image loading on mobile", "Use lazy loading and responsive srcset for all images.", TaskStatus.COMPLETED,10),
        seedTask("Investigate memory leak in worker", "Profile job queue worker for retained object references.", TaskStatus.OPEN,10),
        seedTask("Review open source license usage", "Audit dependencies for GPL or copyleft license conflicts.", TaskStatus.COMPLETED,10)
    ));

    // Day 14
    tasks.addAll(List.of(
        seedTask("Add multi-factor authentication", "Support TOTP authenticator apps as second factor.", TaskStatus.OPEN,14),
        seedTask("Build task due date reminder", "Send notification 24 hours before task due date.", TaskStatus.COMPLETED,14),
        seedTask("Revamp error page design", "Redesign 404 and 500 error pages with helpful messaging.", TaskStatus.OPEN,14),
        seedTask("Implement search analytics", "Track search queries, results count, and click positions.", TaskStatus.COMPLETED,14),
        seedTask("Update billing portal link", "Replace legacy billing URL with new customer portal link.", TaskStatus.OPEN,14),
        seedTask("Add pagination to team list", "Support page and size params in GET /api/teams.", TaskStatus.COMPLETED,14),
        seedTask("Fix sort order for pinned tasks", "Ensure pinned tasks always appear at top of list.", TaskStatus.OPEN,14),
        seedTask("Write runbook for incident response", "Outline first responder steps for production incidents.", TaskStatus.COMPLETED,14),
        seedTask("Migrate config to environment vars", "Replace hardcoded config values with env var references.", TaskStatus.OPEN,14),
        seedTask("Benchmark new search indexer", "Compare response times for old vs new index implementation.", TaskStatus.COMPLETED,14)
    ));

    // Day 15
    tasks.addAll(List.of(
        seedTask("Profile cold start latency", "Measure Lambda initialization time for all functions.", TaskStatus.OPEN,15),
        seedTask("Create UX writing style guide", "Standardize button labels, error messages, and tooltips.", TaskStatus.COMPLETED,15),
        seedTask("Add recursive task support", "Allow tasks to have sub-tasks with completion rollup.", TaskStatus.OPEN,15),
        seedTask("Review CI pipeline efficiency", "Find and eliminate redundant build and test steps.", TaskStatus.COMPLETED,15),
        seedTask("Implement payment dunning flow", "Retry failed payments and notify users before deactivation.", TaskStatus.OPEN,15),
        seedTask("Fix filter state on back navigation", "Restore filter and sort state when user navigates back.", TaskStatus.COMPLETED,15),
        seedTask("Build customer health score model", "Combine usage, NPS, and support signals into health score.", TaskStatus.OPEN,15),
        seedTask("Update contributing guidelines", "Add PR template, commit message format, and review process.", TaskStatus.COMPLETED,15),
        seedTask("Audit user session management", "Review session duration, invalidation, and storage.", TaskStatus.OPEN,15),
        seedTask("Add inline editing for task titles", "Allow users to edit task title directly in the list view.", TaskStatus.COMPLETED,15)
    ));

    // Day 21
    tasks.addAll(List.of(
        seedTask("Redesign notification preferences", "Allow users to configure per-channel notification settings.", TaskStatus.OPEN,21),
        seedTask("Investigate failed payments queue", "Trace stuck payments in retry queue and resolve blockers.", TaskStatus.COMPLETED,21),
        seedTask("Create reusable modal component", "Build accessible modal supporting confirm and form patterns.", TaskStatus.OPEN,21),
        seedTask("Add task priority field", "Add priority enum (low, medium, high) to task model.", TaskStatus.COMPLETED,21),
        seedTask("Audit external API usage", "List all third-party API calls with auth and rate limits.", TaskStatus.OPEN,21),
        seedTask("Write contract tests for billing", "Cover critical billing scenarios with provider contract tests.", TaskStatus.COMPLETED,21),
        seedTask("Improve search result ranking", "Boost recent and highly-engaged tasks in search results.", TaskStatus.OPEN,21),
        seedTask("Finalize office lease renewal", "Submit signed renewal documents for main office lease.", TaskStatus.COMPLETED,21),
        seedTask("Set up Dependabot alerts", "Enable automated dependency vulnerability notifications.", TaskStatus.OPEN,21),
        seedTask("Review engineering interview rubric", "Update scoring criteria to reflect current hiring bar.", TaskStatus.COMPLETED,21)
    ));

    // Day 25
    tasks.addAll(List.of(
        seedTask("Design mobile push notifications", "Define notification types and content for mobile app.", TaskStatus.OPEN,25),
        seedTask("Build audit report export", "Generate PDF audit trail report for compliance reviews.", TaskStatus.COMPLETED,25),
        seedTask("Implement resource-based authorization", "Restrict task access to owner and assigned members.", TaskStatus.OPEN,25),
        seedTask("Add task watchers feature", "Allow users to subscribe to updates on any task.", TaskStatus.COMPLETED,25),
        seedTask("Investigate Redis connection pool exhaustion", "Trace and resolve ECONNREFUSED errors under load.", TaskStatus.OPEN,25),
        seedTask("Create developer portal", "Build self-service docs and API key management interface.", TaskStatus.COMPLETED,25),
        seedTask("Review accessibility audit findings", "Address all WCAG 2.1 Level AA failures in report.", TaskStatus.OPEN,25),
        seedTask("Optimize cold cache response time", "Pre-warm critical caches on deployment.", TaskStatus.COMPLETED,25),
        seedTask("Define engineering career ladder", "Document IC and management tracks with level criteria.", TaskStatus.OPEN,25),
        seedTask("Add scheduled task feature", "Let users set tasks to recur daily, weekly, or monthly.", TaskStatus.COMPLETED,25)
    ));

    // Day 30
    tasks.addAll(List.of(
        seedTask("Build custom report builder", "Allow users to configure columns and filters in reports.", TaskStatus.OPEN,30),
        seedTask("Add keyboard shortcuts guide", "Document and implement common keyboard shortcuts.", TaskStatus.COMPLETED,30),
        seedTask("Review third-party data processing", "Assess DPA compliance for all active data processors.", TaskStatus.OPEN,30),
        seedTask("Implement cursor-based pagination", "Replace offset pagination with cursor for scalability.", TaskStatus.COMPLETED,30),
        seedTask("Create engineering values doc", "Write down team values and decision-making principles.", TaskStatus.OPEN,30),
        seedTask("Fix tooltip positioning on mobile", "Correct tooltip overflow on small viewport widths.", TaskStatus.COMPLETED,30),
        seedTask("Add task comment threads", "Allow users to comment on tasks with threaded replies.", TaskStatus.OPEN,30),
        seedTask("Automate API contract tests in CI", "Run Pact consumer and provider tests on every PR.", TaskStatus.COMPLETED,30),
        seedTask("Prepare Q2 board presentation", "Assemble KPI slides and narrative for board meeting.", TaskStatus.OPEN,30),
        seedTask("Analyze task completion trends", "Chart weekly completion rate changes over last quarter.", TaskStatus.COMPLETED,30)
    ));

    // Day 45
    tasks.addAll(List.of(
        seedTask("Migrate to TypeScript strict mode", "Enable strict null checks across all TypeScript files.", TaskStatus.OPEN,45),
        seedTask("Implement SSO with Okta", "Add SAML 2.0 integration for enterprise customer SSO.", TaskStatus.COMPLETED,45),
        seedTask("Design dashboard customization", "Allow users to pin and reorder dashboard widgets.", TaskStatus.OPEN,45),
        seedTask("Add geolocation to audit logs", "Record approximate user location on sensitive actions.", TaskStatus.COMPLETED,45),
        seedTask("Create internal wiki homepage", "Design landing page with quick links and team directory.", TaskStatus.OPEN,45),
        seedTask("Upgrade Spring Boot to latest patch", "Apply latest Spring Boot security patch release.", TaskStatus.COMPLETED,45),
        seedTask("Write data model migration guide", "Document how to migrate from v1 to v2 data schema.", TaskStatus.OPEN,45),
        seedTask("Validate email DNS configuration", "Check SPF, DKIM, and DMARC records for deliverability.", TaskStatus.COMPLETED,45),
        seedTask("Implement request tracing headers", "Add trace and span IDs to all outbound service calls.", TaskStatus.OPEN,45),
        seedTask("Review and refresh team handbook", "Update policies, links, and org chart references.", TaskStatus.COMPLETED,45)
    ));

    // Day 50
    tasks.addAll(List.of(
        seedTask("Evaluate container security scanning", "Compare Snyk, Trivy, and Aqua for image vulnerability scanning.", TaskStatus.OPEN,50),
        seedTask("Build self-service onboarding wizard", "Guide new users through setup in 5 interactive steps.", TaskStatus.COMPLETED,50),
        seedTask("Implement optimistic locking retry", "Auto-retry writes on version conflict with backoff.", TaskStatus.OPEN,50),
        seedTask("Review API authentication methods", "Evaluate API key vs JWT vs OAuth2 for external access.", TaskStatus.COMPLETED,50),
        seedTask("Add typing indicators to comments", "Show when a teammate is composing a comment.", TaskStatus.OPEN,50),
        seedTask("Audit service account permissions", "Remove excessive IAM permissions from CI service accounts.", TaskStatus.COMPLETED,50),
        seedTask("Create customer journey map", "Visualize touchpoints from trial signup to renewal.", TaskStatus.OPEN,50),
        seedTask("Optimize SQL N+1 query in task list", "Batch-load related data instead of per-row queries.", TaskStatus.COMPLETED,50),
        seedTask("Implement feature request voting", "Let users upvote product feature requests in the portal.", TaskStatus.OPEN,50),
        seedTask("Document event sourcing approach", "Write guide on event store schema and replay strategy.", TaskStatus.COMPLETED,50)
    ));

    // Day 60
    tasks.addAll(List.of(
        seedTask("Plan infrastructure cost review", "Identify top 10 cost drivers and reduction opportunities.", TaskStatus.OPEN,60),
        seedTask("Add task templates feature", "Allow saving and reusing common task structures.", TaskStatus.COMPLETED,60),
        seedTask("Refactor authentication middleware", "Consolidate duplicate auth logic across API routes.", TaskStatus.OPEN,60),
        seedTask("Conduct usability testing session", "Run 5 user tests on the new task creation workflow.", TaskStatus.COMPLETED,60),
        seedTask("Update disaster recovery runbook", "Revise RTO and RPO targets and recovery procedures.", TaskStatus.OPEN,60),
        seedTask("Fix memory usage in data export", "Stream large exports to disk instead of buffering in RAM.", TaskStatus.COMPLETED,60),
        seedTask("Set up cross-region replication", "Enable database replication to secondary region.", TaskStatus.OPEN,60),
        seedTask("Create partner portal wireframes", "Design screens for partner dashboard and API credentials.", TaskStatus.COMPLETED,60),
        seedTask("Improve test isolation in suite", "Ensure each test resets state independently.", TaskStatus.OPEN,60),
        seedTask("Add two-pane layout option", "Build split view with task list and detail panel.", TaskStatus.COMPLETED,60)
    ));

    // Day 90
    tasks.addAll(List.of(
        seedTask("Evaluate new logging platform", "Compare Datadog, Elastic, and Loki for cost and features.", TaskStatus.OPEN,90),
        seedTask("Archive inactive user accounts", "Flag accounts with no activity in 18+ months.", TaskStatus.COMPLETED,90),
        seedTask("Redesign mobile task creation", "Improve tap targets and form layout on small screens.", TaskStatus.OPEN,90),
        seedTask("Add task history timeline", "Show chronological log of status and field changes.", TaskStatus.COMPLETED,90),
        seedTask("Conduct annual access audit", "Verify all employee access matches current role.", TaskStatus.OPEN,90),
        seedTask("Write system design document", "Document architecture decisions for upcoming platform rewrite.", TaskStatus.COMPLETED,90),
        seedTask("Implement SCIM provisioning", "Support automated user provisioning from identity providers.", TaskStatus.OPEN,90),
        seedTask("Analyze query plan for slow report", "Run EXPLAIN ANALYZE and add missing index.", TaskStatus.COMPLETED,90),
        seedTask("Set up preview environments", "Spin up ephemeral environments per pull request.", TaskStatus.OPEN,90),
        seedTask("Plan annual company offsite", "Book venue and draft 3-day agenda for all-hands retreat.", TaskStatus.COMPLETED,90)
    ));

    // Day 120
    tasks.addAll(List.of(
        seedTask("Evaluate GraphQL migration", "Assess effort and benefits of migrating REST APIs to GraphQL.", TaskStatus.OPEN,120),
        seedTask("Build notification digest feature", "Bundle non-urgent alerts into hourly or daily digests.", TaskStatus.COMPLETED,120),
        seedTask("Implement advanced search filters", "Add date range, assignee, and priority filter support.", TaskStatus.OPEN,120),
        seedTask("Create service mesh evaluation", "Compare Istio, Linkerd, and Consul Connect for service mesh.", TaskStatus.COMPLETED,120),
        seedTask("Draft annual engineering roadmap", "Define major initiatives and technical bets for the year.", TaskStatus.OPEN,120),
        seedTask("Implement zero-downtime migrations", "Run database migrations without locking tables.", TaskStatus.COMPLETED,120),
        seedTask("Build pipeline for model training", "Automate data prep, training, and evaluation steps.", TaskStatus.OPEN,120),
        seedTask("Set up chaos engineering tests", "Run weekly chaos experiments to validate resilience.", TaskStatus.COMPLETED,120),
        seedTask("Evaluate edge caching strategy", "Assess CDN and edge caching for API response caching.", TaskStatus.OPEN,120),
        seedTask("Document API versioning strategy", "Define version lifecycle policy and deprecation timeline.", TaskStatus.COMPLETED,120)
    ));

    // Seed 50 users — saved first so tasks can reference them.
    // Diverse names; emails follow firstname.lastname@example.com.
    // BCrypt encode once — encoding is intentionally slow, so we reuse the same hash.
    // All seeded users share the password "password" for development convenience.
    String encoded = passwordEncoder.encode("password");
    List<User> users = userRepository.saveAll(List.of(
        new User("Alice Johnson", "alice.johnson@example.com", encoded, Role.ADMIN),
        new User("Bob Smith", "bob.smith@example.com", encoded),
        new User("Carol Williams", "carol.williams@example.com", encoded),
        new User("David Brown", "david.brown@example.com", encoded),
        new User("Eva Martinez", "eva.martinez@example.com", encoded),
        new User("Frank Lee", "frank.lee@example.com", encoded),
        new User("Grace Kim", "grace.kim@example.com", encoded),
        new User("Henry Davis", "henry.davis@example.com", encoded),
        new User("Isabel Garcia", "isabel.garcia@example.com", encoded),
        new User("James Wilson", "james.wilson@example.com", encoded),
        new User("Karen Chen", "karen.chen@example.com", encoded),
        new User("Liam Taylor", "liam.taylor@example.com", encoded),
        new User("Mia Anderson", "mia.anderson@example.com", encoded),
        new User("Noah Thomas", "noah.thomas@example.com", encoded),
        new User("Olivia Jackson", "olivia.jackson@example.com", encoded),
        new User("Patrick White", "patrick.white@example.com", encoded),
        new User("Quinn Harris", "quinn.harris@example.com", encoded),
        new User("Rachel Martin", "rachel.martin@example.com", encoded),
        new User("Samuel Thompson", "samuel.thompson@example.com", encoded),
        new User("Tina Garcia", "tina.garcia@example.com", encoded),
        new User("Uma Patel", "uma.patel@example.com", encoded),
        new User("Victor Nguyen", "victor.nguyen@example.com", encoded),
        new User("Wendy Clark", "wendy.clark@example.com", encoded),
        new User("Xavier Lewis", "xavier.lewis@example.com", encoded),
        new User("Yara Robinson", "yara.robinson@example.com", encoded),
        new User("Zane Walker", "zane.walker@example.com", encoded),
        new User("Amber Hall", "amber.hall@example.com", encoded),
        new User("Blake Allen", "blake.allen@example.com", encoded),
        new User("Chloe Young", "chloe.young@example.com", encoded),
        new User("Derek Hernandez", "derek.hernandez@example.com", encoded),
        new User("Elena King", "elena.king@example.com", encoded),
        new User("Felix Wright", "felix.wright@example.com", encoded),
        new User("Gabriela Lopez", "gabriela.lopez@example.com", encoded),
        new User("Hugo Scott", "hugo.scott@example.com", encoded),
        new User("Iris Green", "iris.green@example.com", encoded),
        new User("Julian Adams", "julian.adams@example.com", encoded),
        new User("Kira Baker", "kira.baker@example.com", encoded),
        new User("Lucas Gonzalez", "lucas.gonzalez@example.com", encoded),
        new User("Maya Nelson", "maya.nelson@example.com", encoded),
        new User("Nathan Carter", "nathan.carter@example.com", encoded),
        new User("Opal Mitchell", "opal.mitchell@example.com", encoded),
        new User("Pedro Perez", "pedro.perez@example.com", encoded),
        new User("Rosa Roberts", "rosa.roberts@example.com", encoded),
        new User("Sean Turner", "sean.turner@example.com", encoded),
        new User("Tara Phillips", "tara.phillips@example.com", encoded),
        new User("Ulrich Campbell", "ulrich.campbell@example.com", encoded),
        new User("Vera Parker", "vera.parker@example.com", encoded),
        new User("Walter Evans", "walter.evans@example.com", encoded),
        new User("Xena Edwards", "xena.edwards@example.com", encoded),
        new User("Yusuf Collins", "yusuf.collins@example.com", encoded)
    ));

    // Seed tags — three orthogonal dimensions so combinations feel natural:
    //   domain  (Work / Personal / Home)
    //   priority (Urgent / Someday)
    //   type     (Meeting / Research / Errand)
    // A task tagged "Work + Urgent + Meeting" or "Personal + Errand" reads like real data.
    List<Tag> tags = tagRepository.saveAll(List.of(
        new Tag("Work"),
        new Tag("Personal"),
        new Tag("Home"),
        new Tag("Urgent"),
        new Tag("Someday"),
        new Tag("Meeting"),
        new Tag("Research"),
        new Tag("Errand")
    ));

    // Tags are split into three orthogonal dimensions by index:
    //   domain   → indices 0-2: Work, Personal, Home
    //   priority → indices 3-4: Urgent, Someday
    //   type     → indices 5-7: Meeting, Research, Errand
    //
    // Each task gets a domain tag (always), then optionally a priority and/or type tag,
    // producing natural combinations like "Work + Urgent + Meeting" or "Home + Errand".
    // Every 7th task gets no tags — demonstrates the empty join table case.
    List<Tag> domain   = tags.subList(0, 3); // Work, Personal, Home
    List<Tag> priority = tags.subList(3, 5); // Urgent, Someday
    List<Tag> type     = tags.subList(5, 8); // Meeting, Research, Errand

    for (int i = 0; i < tasks.size(); i++) {
      if (i % 7 == 6) continue; // ~14% of tasks get no tags
      List<Tag> taskTags = new ArrayList<>();
      taskTags.add(domain.get(i % domain.size()));           // always a domain
      if (i % 3 != 0) taskTags.add(priority.get(i % priority.size())); // ~67% get a priority
      if (i % 2 == 0) taskTags.add(type.get(i % type.size()));         // ~50% get a type
      tasks.get(i).setTags(taskTags);
    }

    // Assign priority and due dates.
    // Priority distribution: ~20% HIGH, ~50% MEDIUM, ~30% LOW.
    // Due dates: ~60% of tasks get a due date; spread from 10 days ago to 30 days from now.
    for (int i = 0; i < tasks.size(); i++) {
      if (i % 5 == 0) tasks.get(i).setPriority(Priority.HIGH);
      else if (i % 5 < 3) tasks.get(i).setPriority(Priority.MEDIUM);
      else tasks.get(i).setPriority(Priority.LOW);

      if (i % 5 != 3) { // ~80% get a due date
        // Spread due dates: from 10 days ago to 30 days from now
        int daysOffset = -10 + (i % 41); // -10 to +30
        tasks.get(i).setDueDate(LocalDate.now().plusDays(daysOffset));
      }
    }

    // Assign start dates and completed dates based on status.
    // IN_PROGRESS/COMPLETED tasks get a start date a few days after creation.
    // COMPLETED tasks get a completedAt timestamp.
    for (int i = 0; i < tasks.size(); i++) {
      Task t = tasks.get(i);
      if (t.getStatus() == TaskStatus.IN_PROGRESS || t.getStatus() == TaskStatus.COMPLETED) {
        t.setStartDate(t.getCreatedAt().toLocalDate().plusDays(1 + (i % 3)));
      }
      if (t.getStatus() == TaskStatus.COMPLETED) {
        t.setCompletedAt(t.getCreatedAt().plusDays(2 + (i % 5)));
      }
    }

    // Assign users to tasks — ~80% assigned, every 5th task unassigned.
    // Demonstrates nullable @ManyToOne: some tasks have no owner.
    for (int i = 0; i < tasks.size(); i++) {
      if (i % 5 != 4) {
        tasks.get(i).setUser(users.get(i % users.size()));
      }
    }

    taskRepository.saveAll(tasks);

    // ── Comments ──────────────────────────────────────────────────────────
    // Seed sample comments on ~30% of tasks. Each gets 1–3 comments from random users.
    String[] sampleComments = {
        "Working on this now.",
        "Need more details before proceeding.",
        "This is blocked by another task.",
        "Almost done, should be ready by tomorrow.",
        "Let's discuss this in the next standup.",
        "Updated the approach based on feedback.",
        "Can someone review this?",
        "Moved to next sprint.",
        "This turned out to be more complex than expected.",
        "Done! Ready for review.",
        "Added unit tests for this.",
        "Dependencies are now resolved.",
        "Talked to the team about this — going ahead.",
        "Lowered priority for now.",
        "Good progress today.",
    };
    List<Comment> comments = new ArrayList<>();
    for (int i = 0; i < tasks.size(); i++) {
      if (i % 3 != 0) continue; // ~33% of tasks get comments
      Task task = tasks.get(i);
      int commentCount = 1 + (i % 3); // 1–3 comments per task
      for (int c = 0; c < commentCount; c++) {
        Comment comment = new Comment();
        comment.setText(sampleComments[(i + c) % sampleComments.length]);
        comment.setTask(task);
        comment.setUser(users.get((i + c + 1) % users.size()));
        comment.setCreatedAt(task.getCreatedAt().plusHours(c + 1));
        comments.add(comment);
      }
    }
    commentRepository.saveAll(comments);

    // ── Checklist items ───────────────────────────────────────────────────
    // Seed checklist items on ~20% of tasks. Each gets 2–5 items,
    // with some items marked checked on completed/in-progress tasks.
    String[] checklistTexts = {
        "Review requirements",
        "Write implementation",
        "Add unit tests",
        "Update documentation",
        "Code review",
        "Deploy to staging",
        "Run integration tests",
        "Get sign-off",
        "Update changelog",
        "Notify stakeholders",
    };
    for (int i = 0; i < tasks.size(); i++) {
      if (i % 5 != 0) continue; // ~20% of tasks get checklists
      Task task = tasks.get(i);
      int itemCount = 2 + (i % 4); // 2–5 items
      for (int c = 0; c < itemCount; c++) {
        ChecklistItem item = new ChecklistItem(checklistTexts[(i + c) % checklistTexts.length], c);
        // Check some items: completed tasks → all checked; in-progress → first half checked
        if (task.getStatus() == TaskStatus.COMPLETED) {
          item.setChecked(true);
        } else if (task.getStatus() == TaskStatus.IN_PROGRESS) {
          item.setChecked(c < itemCount / 2);
        }
        item.setTask(task);
        task.getChecklistItems().add(item);
      }
    }
    // Ensure demo users have tasks due tomorrow (for due-date reminder demo).
    LocalDate tomorrow = LocalDate.now().plusDays(1);
    List<Task> dueTomorrowTasks = new ArrayList<>();
    for (Task t : tasks) {
      if (dueTomorrowTasks.size() >= 3) break;
      if (t.getUser() != null && t.getUser().equals(users.get(0))
          && t.getStatus() != TaskStatus.COMPLETED) {
        t.setDueDate(tomorrow);
        dueTomorrowTasks.add(t);
      }
    }

    taskRepository.saveAll(tasks);

    // ── Notifications ───────────────────────────────────────────────────
    // Seed due-date reminder notifications for Alice's tasks due tomorrow,
    // so the notification bell has content on first boot.
    List<Notification> notifications = new ArrayList<>();
    for (Task t : dueTomorrowTasks) {
      Notification n = new Notification(
          t.getUser(), null, NotificationType.TASK_DUE_REMINDER,
          "Due tomorrow: " + t.getTitle(), "/tasks/" + t.getId());
      notifications.add(n);
    }
    notificationRepository.saveAll(notifications);

    // ── Curated demo data for Alice & Bob ────────────────────────────────
    seedDemoInteractions(users, tags);

    // ── Settings ──────────────────────────────────────────────────────────
    settingRepository.save(new Setting(Settings.KEY_THEME, Settings.THEME_WORKSHOP));

    System.out.println("Seed data loaded: " + userRepository.count() + " users, "
        + tagRepository.count() + " tags, "
        + taskRepository.count() + " tasks, "
        + commentRepository.count() + " comments, "
        + notificationRepository.count() + " notifications, "
        + auditLogRepository.count() + " audit logs, "
        + settingRepository.count() + " settings.");
  }

  /**
   * Creates hand-crafted tasks, comments, @mentions, and notifications between
   * Alice (admin) and Bob (user) so both accounts have rich, realistic data
   * on first login. Also involves Carol (users[2]) for multi-party interactions.
   */
  private void seedDemoInteractions(List<User> users, List<Tag> tags) {
    User alice = users.get(0); // admin
    User bob   = users.get(1);
    User carol = users.get(2);
    User david = users.get(3);
    User eva   = users.get(4);
    LocalDateTime now = LocalDateTime.now();
    LocalDate today = LocalDate.now();

    Tag work    = tags.get(0);
    Tag urgent  = tags.get(3);
    Tag meeting = tags.get(5);
    Tag research = tags.get(6);

    // ── Task 1: Alice assigned to Bob, with comment thread ────────────
    Task t1 = new Task("Set up CI/CD pipeline for staging", "Configure GitHub Actions workflow with build, test, and deploy stages. Use Docker-based runners for consistency.");
    t1.setUser(bob);
    t1.setStatus(TaskStatus.IN_PROGRESS);
    t1.setPriority(Priority.HIGH);
    t1.setTags(List.of(work, urgent));
    t1.setStartDate(today.minusDays(5));
    t1.setDueDate(today.plusDays(2));
    t1.setCreatedAt(now.minusDays(5));
    addChecklist(t1, List.of(
        checklist("Configure build stage", 0, true),
        checklist("Add test stage with coverage", 1, true),
        checklist("Set up staging deploy", 2, false),
        checklist("Add Slack notifications", 3, false),
        checklist("Document pipeline in wiki", 4, false)
    ));
    t1 = taskRepository.save(t1);

    Comment c1a = comment(t1, alice, "Hey @[Bob Smith](userId:" + bob.getId() + "), I've outlined the stages we need. Can you start with the build config?", now.minusDays(5).plusHours(1));
    Comment c1b = comment(t1, bob, "On it! I'll base it on the Docker runner setup from the docs. Should have the build stage done today.", now.minusDays(5).plusHours(3));
    Comment c1c = comment(t1, alice, "Build and test stages look great. For the deploy stage, make sure to use the staging secrets from the vault.", now.minusDays(3).plusHours(2));
    Comment c1d = comment(t1, bob, "Good call. I also added a coverage threshold — it'll fail the build if coverage drops below 80%. Working on the deploy stage now.", now.minusDays(2).plusHours(4));
    commentRepository.saveAll(List.of(c1a, c1b, c1c, c1d));

    // Bob gets notification: assigned by Alice
    notificationRepository.save(new Notification(bob, alice, NotificationType.TASK_ASSIGNED,
        "Alice Johnson assigned you: Set up CI/CD pipeline for staging", "/tasks/" + t1.getId()));

    // ── Task 2: Bob assigned to Alice, overdue ────────────────────────
    Task t2 = new Task("Write API rate limiting design doc", "Document the approach for API rate limiting including per-user quotas, sliding window algorithm, and Redis-backed counters.");
    t2.setUser(alice);
    t2.setStatus(TaskStatus.IN_PROGRESS);
    t2.setPriority(Priority.HIGH);
    t2.setTags(List.of(work, research));
    t2.setStartDate(today.minusDays(10));
    t2.setDueDate(today.minusDays(1)); // overdue
    t2.setCreatedAt(now.minusDays(10));
    addChecklist(t2, List.of(
        checklist("Research sliding window algorithms", 0, true),
        checklist("Draft architecture section", 1, true),
        checklist("Add Redis schema design", 2, false),
        checklist("Get review from team", 3, false)
    ));
    t2 = taskRepository.save(t2);

    Comment c2a = comment(t2, bob, "@[Alice Johnson](userId:" + alice.getId() + ") I started a draft with the requirements. Can you fill in the Redis schema section?", now.minusDays(10).plusHours(2));
    Comment c2b = comment(t2, alice, "Sure! I've been reading about sliding window counters. Will draft the Redis section this week.", now.minusDays(9).plusHours(5));
    Comment c2c = comment(t2, carol, "Hey @[Alice Johnson](userId:" + alice.getId() + "), we used a similar approach at my previous company. Happy to review when it's ready.", now.minusDays(7).plusHours(3));
    Comment c2d = comment(t2, alice, "Thanks @[Carol Williams](userId:" + carol.getId() + ")! I'll tag you once the draft is complete. Still working through the edge cases for burst traffic.", now.minusDays(5).plusHours(1));
    commentRepository.saveAll(List.of(c2a, c2b, c2c, c2d));

    // Alice gets notification: assigned by Bob
    notificationRepository.save(new Notification(alice, bob, NotificationType.TASK_ASSIGNED,
        "Bob Smith assigned you: Write API rate limiting design doc", "/tasks/" + t2.getId()));
    // Alice gets overdue notification
    notificationRepository.save(new Notification(alice, null, NotificationType.TASK_OVERDUE,
        "Overdue: Write API rate limiting design doc", "/tasks/" + t2.getId()));

    // ── Task 3: Completed task with full conversation ─────────────────
    Task t3 = new Task("Migrate user auth to Spring Security 7", "Upgrade from Spring Security 6 to 7. Update filter chain config, switch to new authorization model, and verify all endpoints.");
    t3.setUser(alice);
    t3.setStatus(TaskStatus.COMPLETED);
    t3.setPriority(Priority.HIGH);
    t3.setTags(List.of(work, urgent));
    t3.setStartDate(today.minusDays(14));
    t3.setDueDate(today.minusDays(5));
    t3.setCompletedAt(now.minusDays(6));
    t3.setCreatedAt(now.minusDays(14));
    addChecklist(t3, List.of(
        checklist("Update SecurityConfig", 0, true),
        checklist("Migrate custom filters", 1, true),
        checklist("Update test security config", 2, true),
        checklist("Verify all protected endpoints", 3, true),
        checklist("Update documentation", 4, true)
    ));
    t3 = taskRepository.save(t3);

    Comment c3a = comment(t3, alice, "Starting the migration. The new authorization model in Spring Security 7 is quite different — @[Bob Smith](userId:" + bob.getId() + ") heads up if you see any auth issues.", now.minusDays(14).plusHours(3));
    Comment c3b = comment(t3, bob, "Thanks for the heads up. I'll keep an eye on the API endpoints I own.", now.minusDays(13).plusHours(1));
    Comment c3c = comment(t3, david, "I ran into a similar migration last month. The biggest gotcha is the new default CSRF handling — it changed from opt-out to opt-in for API endpoints.", now.minusDays(12).plusHours(6));
    Comment c3d = comment(t3, alice, "Good tip @[David Brown](userId:" + david.getId() + ")! I just hit that exact issue. Fixed by explicitly disabling CSRF for /api/** paths.", now.minusDays(11).plusHours(2));
    Comment c3e = comment(t3, alice, "Migration complete. All endpoints verified. Merging to main now.", now.minusDays(6).plusHours(1));
    commentRepository.saveAll(List.of(c3a, c3b, c3c, c3d, c3e));

    // ── Task 4: Bob's task, due soon, with mentions ───────────────────
    Task t4 = new Task("Design WebSocket notification system", "Plan the real-time notification architecture using STOMP over WebSocket. Cover connection management, message routing, and offline fallback.");
    t4.setUser(bob);
    t4.setStatus(TaskStatus.IN_PROGRESS);
    t4.setPriority(Priority.MEDIUM);
    t4.setTags(List.of(work, research));
    t4.setStartDate(today.minusDays(3));
    t4.setDueDate(today.plusDays(4));
    t4.setCreatedAt(now.minusDays(3));
    addChecklist(t4, List.of(
        checklist("Research STOMP protocol", 0, true),
        checklist("Design message schema", 1, true),
        checklist("Plan connection lifecycle", 2, false),
        checklist("Document offline strategy", 3, false)
    ));
    t4 = taskRepository.save(t4);

    Comment c4a = comment(t4, bob, "I've been reading about STOMP vs raw WebSocket. STOMP gives us pub/sub out of the box with Spring's built-in support.", now.minusDays(3).plusHours(4));
    Comment c4b = comment(t4, alice, "Agree on STOMP. @[Eva Martinez](userId:" + eva.getId() + ") has experience with this — might be worth syncing.", now.minusDays(2).plusHours(2));
    Comment c4c = comment(t4, eva, "Happy to help! One thing to watch out for: make sure you handle reconnection gracefully. Users on flaky connections will drop and reconnect frequently.", now.minusDays(2).plusHours(5));
    Comment c4d = comment(t4, bob, "Great point @[Eva Martinez](userId:" + eva.getId() + "). I'll add a reconnection strategy with exponential backoff to the design.", now.minusDays(1).plusHours(3));
    commentRepository.saveAll(List.of(c4a, c4b, c4c, c4d));

    // ── Task 5: Unassigned task (open for anyone) ─────────────────────
    Task t5 = new Task("Update team coding standards", "Review and update the coding standards document. Add sections for new patterns we've adopted: record types, sealed classes, and pattern matching.");
    t5.setUser(null); // unassigned
    t5.setStatus(TaskStatus.OPEN);
    t5.setPriority(Priority.LOW);
    t5.setTags(List.of(work));
    t5.setDueDate(today.plusDays(14));
    t5.setCreatedAt(now.minusDays(2));
    t5 = taskRepository.save(t5);

    Comment c5a = comment(t5, alice, "I'd like to add a section on when to use records vs classes. Anyone want to take this?", now.minusDays(2).plusHours(3));
    Comment c5b = comment(t5, bob, "I can draft the records and sealed classes sections. @[Carol Williams](userId:" + carol.getId() + ") want to handle pattern matching?", now.minusDays(1).plusHours(2));
    Comment c5c = comment(t5, carol, "Sure! I'll write up the pattern matching section with some examples from our codebase.", now.minusDays(1).plusHours(5));
    commentRepository.saveAll(List.of(c5a, c5b, c5c));

    // ── Task 6: Alice's task due tomorrow ─────────────────────────────
    Task t6 = new Task("Prepare sprint retrospective", "Collect feedback from the team, prepare the retro board, and schedule the meeting room. Focus areas: deployment process and code review turnaround.");
    t6.setUser(alice);
    t6.setStatus(TaskStatus.OPEN);
    t6.setPriority(Priority.MEDIUM);
    t6.setTags(List.of(work, meeting));
    t6.setDueDate(today.plusDays(1));
    t6.setCreatedAt(now.minusDays(3));
    t6 = taskRepository.save(t6);

    Comment c6a = comment(t6, bob, "Can we add 'improving test coverage' to the agenda? I think we're slipping on that.", now.minusDays(1).plusHours(4));
    Comment c6b = comment(t6, alice, "Good idea. I'll add it as a discussion point. @[David Brown](userId:" + david.getId() + ") @[Eva Martinez](userId:" + eva.getId() + ") — any other topics you want to cover?", now.minusDays(1).plusHours(6));
    commentRepository.saveAll(List.of(c6a, c6b));

    // Alice: due tomorrow notification
    notificationRepository.save(new Notification(alice, null, NotificationType.TASK_DUE_REMINDER,
        "Due tomorrow: Prepare sprint retrospective", "/tasks/" + t6.getId()));

    // ── Task 7: Bob's completed task ──────────────────────────────────
    Task t7 = new Task("Fix pagination bug on task list", "Page size selector resets to default when navigating to page 2+. The URL param is lost during HTMX partial swap.");
    t7.setUser(bob);
    t7.setStatus(TaskStatus.COMPLETED);
    t7.setPriority(Priority.MEDIUM);
    t7.setTags(List.of(work));
    t7.setStartDate(today.minusDays(4));
    t7.setDueDate(today.minusDays(1));
    t7.setCompletedAt(now.minusDays(2));
    t7.setCreatedAt(now.minusDays(4));
    t7 = taskRepository.save(t7);

    Comment c7a = comment(t7, bob, "Found the issue — buildUrl() was dropping the size param on navigation. Fix is a one-liner.", now.minusDays(3).plusHours(5));
    Comment c7b = comment(t7, alice, "Nice catch! Can you also add a test case for this? We don't want it to regress.", now.minusDays(3).plusHours(7));
    Comment c7c = comment(t7, bob, "Done. Added test and verified manually. Closing this out.", now.minusDays(2).plusHours(1));
    commentRepository.saveAll(List.of(c7a, c7b, c7c));

    // ── Task 8: Alice's task, open, due next week ─────────────────────
    Task t8 = new Task("Plan Q2 engineering roadmap", "Draft the Q2 roadmap covering platform reliability, developer experience, and new feature initiatives. Include capacity estimates and dependencies.");
    t8.setUser(alice);
    t8.setStatus(TaskStatus.OPEN);
    t8.setPriority(Priority.HIGH);
    t8.setTags(List.of(work, meeting));
    t8.setDueDate(today.plusDays(7));
    t8.setCreatedAt(now.minusDays(1));
    addChecklist(t8, List.of(
        checklist("Gather team input on priorities", 0, false),
        checklist("Draft reliability initiatives", 1, false),
        checklist("Draft DX improvements", 2, false),
        checklist("Estimate capacity per initiative", 3, false),
        checklist("Review with engineering leads", 4, false),
        checklist("Present to leadership", 5, false)
    ));
    t8 = taskRepository.save(t8);

    Comment c8a = comment(t8, alice, "Starting to collect input. @[Bob Smith](userId:" + bob.getId() + ") @[Carol Williams](userId:" + carol.getId() + ") @[David Brown](userId:" + david.getId() + ") — please share your top 3 priorities for Q2 by Friday.", now.minusDays(1).plusHours(2));
    Comment c8b = comment(t8, bob, "My top 3: (1) CI/CD improvements, (2) WebSocket notifications, (3) better error handling across the API.", now.minusHours(20));
    Comment c8c = comment(t8, carol, "From my side: (1) test infrastructure, (2) documentation overhaul, (3) accessibility improvements.", now.minusHours(16));
    commentRepository.saveAll(List.of(c8a, c8b, c8c));

    // ── Additional notifications for both users ───────────────────────
    // Bob: comment notifications from Alice on his tasks
    notificationRepository.saveAll(List.of(
        notification(bob, alice, NotificationType.COMMENT_ADDED,
            "Alice Johnson commented on: Set up CI/CD pipeline for staging", "/tasks/" + t1.getId(), now.minusDays(3).plusHours(2)),
        notification(bob, alice, NotificationType.COMMENT_ADDED,
            "Alice Johnson commented on: Design WebSocket notification system", "/tasks/" + t4.getId(), now.minusDays(2).plusHours(2)),
        notification(bob, eva, NotificationType.COMMENT_ADDED,
            "Eva Martinez commented on: Design WebSocket notification system", "/tasks/" + t4.getId(), now.minusDays(2).plusHours(5)),
        notification(bob, alice, NotificationType.COMMENT_MENTIONED,
            "Alice Johnson mentioned you in: Plan Q2 engineering roadmap", "/tasks/" + t8.getId(), now.minusDays(1).plusHours(2))
    ));
    // Alice: comment notifications from Bob on her tasks
    notificationRepository.saveAll(List.of(
        notification(alice, bob, NotificationType.COMMENT_ADDED,
            "Bob Smith commented on: Write API rate limiting design doc", "/tasks/" + t2.getId(), now.minusDays(10).plusHours(2)),
        notification(alice, carol, NotificationType.COMMENT_ADDED,
            "Carol Williams commented on: Write API rate limiting design doc", "/tasks/" + t2.getId(), now.minusDays(7).plusHours(3)),
        notification(alice, bob, NotificationType.COMMENT_ADDED,
            "Bob Smith commented on: Prepare sprint retrospective", "/tasks/" + t6.getId(), now.minusDays(1).plusHours(4)),
        notification(alice, bob, NotificationType.COMMENT_ADDED,
            "Bob Smith commented on: Plan Q2 engineering roadmap", "/tasks/" + t8.getId(), now.minusHours(20)),
        notification(alice, carol, NotificationType.COMMENT_ADDED,
            "Carol Williams commented on: Plan Q2 engineering roadmap", "/tasks/" + t8.getId(), now.minusHours(16)),
        notification(alice, bob, NotificationType.COMMENT_ADDED,
            "Bob Smith commented on: Fix pagination bug on task list", "/tasks/" + t7.getId(), now.minusDays(3).plusHours(5)),
        notification(alice, david, NotificationType.COMMENT_ADDED,
            "David Brown commented on: Migrate user auth to Spring Security 7", "/tasks/" + t3.getId(), now.minusDays(12).plusHours(6))
    ));

    // ── Audit logs for demo tasks ─────────────────────────────────────
    List<AuditLog> auditLogs = new ArrayList<>();

    // Task creation events
    auditLogs.add(auditLog(AuditEvent.TASK_CREATED, Task.class, t1.getId(), alice.getEmail(),
        AuditDetails.toJson(t1.toAuditSnapshot()), t1.getCreatedAt()));
    auditLogs.add(auditLog(AuditEvent.TASK_CREATED, Task.class, t2.getId(), bob.getEmail(),
        AuditDetails.toJson(t2.toAuditSnapshot()), t2.getCreatedAt()));
    auditLogs.add(auditLog(AuditEvent.TASK_CREATED, Task.class, t3.getId(), alice.getEmail(),
        AuditDetails.toJson(t3.toAuditSnapshot()), t3.getCreatedAt()));
    auditLogs.add(auditLog(AuditEvent.TASK_CREATED, Task.class, t4.getId(), bob.getEmail(),
        AuditDetails.toJson(t4.toAuditSnapshot()), t4.getCreatedAt()));
    auditLogs.add(auditLog(AuditEvent.TASK_CREATED, Task.class, t5.getId(), alice.getEmail(),
        AuditDetails.toJson(t5.toAuditSnapshot()), t5.getCreatedAt()));
    auditLogs.add(auditLog(AuditEvent.TASK_CREATED, Task.class, t6.getId(), alice.getEmail(),
        AuditDetails.toJson(t6.toAuditSnapshot()), t6.getCreatedAt()));
    auditLogs.add(auditLog(AuditEvent.TASK_CREATED, Task.class, t7.getId(), bob.getEmail(),
        AuditDetails.toJson(t7.toAuditSnapshot()), t7.getCreatedAt()));
    auditLogs.add(auditLog(AuditEvent.TASK_CREATED, Task.class, t8.getId(), alice.getEmail(),
        AuditDetails.toJson(t8.toAuditSnapshot()), t8.getCreatedAt()));

    // Task update: t3 completed (Spring Security migration)
    Map<String, Object> t3StatusChange = new LinkedHashMap<>();
    t3StatusChange.put("status", Map.of("old", "IN_PROGRESS", "new", "COMPLETED"));
    auditLogs.add(auditLog(AuditEvent.TASK_UPDATED, Task.class, t3.getId(), alice.getEmail(),
        AuditDetails.toJson(t3StatusChange), now.minusDays(6).plusHours(1)));

    // Task update: t7 completed (pagination bug)
    Map<String, Object> t7StatusChange = new LinkedHashMap<>();
    t7StatusChange.put("status", Map.of("old", "IN_PROGRESS", "new", "COMPLETED"));
    auditLogs.add(auditLog(AuditEvent.TASK_UPDATED, Task.class, t7.getId(), bob.getEmail(),
        AuditDetails.toJson(t7StatusChange), now.minusDays(2).plusHours(1)));

    // Task update: t1 checklist progress (Bob checked items)
    Map<String, Object> t1ChecklistUpdate = new LinkedHashMap<>();
    t1ChecklistUpdate.put("checklistItems", Map.of(
        "old", List.of("[ ] Configure build stage", "[ ] Add test stage with coverage", "[ ] Set up staging deploy", "[ ] Add Slack notifications", "[ ] Document pipeline in wiki"),
        "new", List.of("[x] Configure build stage", "[x] Add test stage with coverage", "[ ] Set up staging deploy", "[ ] Add Slack notifications", "[ ] Document pipeline in wiki")
    ));
    auditLogs.add(auditLog(AuditEvent.TASK_UPDATED, Task.class, t1.getId(), bob.getEmail(),
        AuditDetails.toJson(t1ChecklistUpdate), now.minusDays(4).plusHours(6)));

    // Task update: t2 checklist progress (Alice checked items)
    Map<String, Object> t2ChecklistUpdate = new LinkedHashMap<>();
    t2ChecklistUpdate.put("checklistItems", Map.of(
        "old", List.of("[ ] Research sliding window algorithms", "[ ] Draft architecture section", "[ ] Add Redis schema design", "[ ] Get review from team"),
        "new", List.of("[x] Research sliding window algorithms", "[x] Draft architecture section", "[ ] Add Redis schema design", "[ ] Get review from team")
    ));
    auditLogs.add(auditLog(AuditEvent.TASK_UPDATED, Task.class, t2.getId(), alice.getEmail(),
        AuditDetails.toJson(t2ChecklistUpdate), now.minusDays(7).plusHours(5)));

    // Task update: t4 checklist progress (Bob checked items)
    Map<String, Object> t4ChecklistUpdate = new LinkedHashMap<>();
    t4ChecklistUpdate.put("checklistItems", Map.of(
        "old", List.of("[ ] Research STOMP protocol", "[ ] Design message schema", "[ ] Plan connection lifecycle", "[ ] Document offline strategy"),
        "new", List.of("[x] Research STOMP protocol", "[x] Design message schema", "[ ] Plan connection lifecycle", "[ ] Document offline strategy")
    ));
    auditLogs.add(auditLog(AuditEvent.TASK_UPDATED, Task.class, t4.getId(), bob.getEmail(),
        AuditDetails.toJson(t4ChecklistUpdate), now.minusDays(1).plusHours(6)));

    auditLogRepository.saveAll(auditLogs);
  }

  private Comment comment(Task task, User user, String text, LocalDateTime createdAt) {
    Comment c = new Comment();
    c.setTask(task);
    c.setUser(user);
    c.setText(text);
    c.setCreatedAt(createdAt);
    return c;
  }

  private ChecklistItem checklist(String text, int position, boolean checked) {
    ChecklistItem item = new ChecklistItem(text, position);
    item.setChecked(checked);
    return item;
  }

  private void addChecklist(Task task, List<ChecklistItem> items) {
    for (ChecklistItem item : items) {
      item.setTask(task);
      task.getChecklistItems().add(item);
    }
  }

  private AuditLog auditLog(String action, Class<?> entityType, Long entityId,
                             String principal, String details, LocalDateTime timestamp) {
    AuditLog log = new AuditLog();
    log.setAction(action);
    log.setEntityType(entityType.getSimpleName());
    log.setEntityId(entityId);
    log.setPrincipal(principal);
    log.setDetails(details);
    log.setTimestamp(timestamp.atZone(ZoneId.systemDefault()).toInstant());
    return log;
  }

  private Notification notification(User user, User actor, NotificationType type,
                                    String message, String link, LocalDateTime createdAt) {
    Notification n = new Notification(user, actor, type, message, link);
    n.setCreatedAt(createdAt);
    return n;
  }

  private Task seedTask(String title, String description, TaskStatus status, int daysAgo) {
    Task task = new Task(title, description);
    task.setStatus(status);
    // Truncate to midnight so tasks in the same group share an identical createdAt,
    // making the secondary sort (title, completed, etc.) an observable tiebreaker.
    task.setCreatedAt(LocalDateTime.now().toLocalDate().minusDays(daysAgo).atStartOfDay());
    return task;
  }
}
