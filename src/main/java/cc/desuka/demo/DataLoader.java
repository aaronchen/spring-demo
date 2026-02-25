package cc.desuka.demo;

import cc.desuka.demo.model.Task;
import cc.desuka.demo.repository.TaskRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

  private final TaskRepository taskRepository;

  public DataLoader(TaskRepository taskRepository) {
    this.taskRepository = taskRepository;
  }

  @Override
  public void run(String... args) {
    List<Task> tasks = new ArrayList<>();

    // ── Original 100 tasks (unique dates, spread across 120 days) ─────────────
    tasks.addAll(List.of(
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
        seedTask("Archive completed Jira epics", "Close old epics and link final release notes.", true, 102)
    ));
    tasks.addAll(List.of(
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
        seedTask("Backfill missing audit logs", "Reconstruct 30 days of missing admin action entries.", true, 57)
    ));
    tasks.addAll(List.of(
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
        seedTask("Investigate memory spike after deploy", "Profile heap growth in API pods after rollout.", false, 0)
    ));
    tasks.addAll(List.of(
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
        seedTask("Tune PostgreSQL connection pool limits", "Reduce saturation during peak cron execution.", false, 53)
    ));
    tasks.addAll(List.of(
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
    ));

    // ── 200 additional tasks grouped by date (10 per day) ─────────────────────
    // Intentional date clusters for multi-sort testing.
    // Each group shares the same daysAgo, so tasks differ only by title/description/completed.

    // Day 0 – today
    tasks.addAll(List.of(
        seedTask("Draft sprint planning agenda", "Outline goals, capacity, and backlog items for next sprint.", false, 0),
        seedTask("Review A/B test results", "Analyze click-through rates from last week's experiment.", false, 0),
        seedTask("Fix padding on mobile cards", "Adjust card padding for screens under 400px wide.", true, 0),
        seedTask("Update team OKR tracker", "Reflect progress from last week across all objectives.", false, 0),
        seedTask("Check CDN cache-hit ratio", "Review Cloudflare analytics for cache performance.", true, 0),
        seedTask("Write weekly engineering blog post", "Summarize key technical decisions made this week.", false, 0),
        seedTask("Test email unsubscribe flow", "Verify one-click unsubscribe complies with CAN-SPAM.", true, 0),
        seedTask("Sync with design on new icons", "Align on icon set before frontend integration begins.", false, 0),
        seedTask("Deploy hotfix for null pointer", "Push fix for NPE in invoice rendering to production.", true, 0),
        seedTask("Archive old release branches", "Delete merged release branches older than 6 months.", false, 0)
    ));

    // Day 1
    tasks.addAll(List.of(
        seedTask("Refactor user permission checks", "Extract permission logic into a reusable guard utility.", false, 1),
        seedTask("Add API rate limit headers", "Return X-RateLimit-Remaining and X-RateLimit-Reset headers.", true, 1),
        seedTask("Review marketing site copy", "Proofread homepage and pricing page for tone consistency.", false, 1),
        seedTask("Create DB migration for tags", "Add tags table and task_tags join table.", false, 1),
        seedTask("Investigate slow report query", "Profile the monthly revenue report query taking 12s.", true, 1),
        seedTask("Update Dockerfile base image", "Upgrade from node:18 to node:22 in Dockerfile.", true, 1),
        seedTask("Prepare legal hold notice", "Draft preservation notice for pending litigation matter.", false, 1),
        seedTask("Fix broken pagination in API", "Handle edge case when page exceeds total page count.", true, 1),
        seedTask("Audit log retention review", "Confirm audit logs are retained per compliance policy.", false, 1),
        seedTask("Update status page incident", "Post resolution details and timeline for yesterday's outage.", true, 1)
    ));

    // Day 2
    tasks.addAll(List.of(
        seedTask("Implement password complexity rules", "Enforce minimum length, mixed case, and special characters.", false, 2),
        seedTask("Create product demo video", "Record 90-second walkthrough of core task management flow.", false, 2),
        seedTask("Migrate user avatars to S3", "Move avatar storage from local disk to object storage.", true, 2),
        seedTask("Add logging to payment webhook", "Log raw payload and parsed fields before processing.", false, 2),
        seedTask("Review competitor pricing changes", "Analyze pricing updates from three main competitors.", true, 2),
        seedTask("Fix CORS policy for staging", "Allow staging subdomain in CORS allowed origins list.", true, 2),
        seedTask("Draft data breach response plan", "Define notification timeline and escalation procedures.", false, 2),
        seedTask("Optimize bundle splitting config", "Tune Webpack chunks for better code splitting.", true, 2),
        seedTask("Document database schema changes", "Update schema docs for tags and assignments tables.", false, 2),
        seedTask("Resolve merge conflict in main", "Fix conflicts from parallel feature branches.", true, 2)
    ));

    // Day 3
    tasks.addAll(List.of(
        seedTask("Set up Grafana dashboard", "Create panels for API latency, error rate, and throughput.", true, 3),
        seedTask("Review pull request feedback", "Address comments on the auth refactor PR.", false, 3),
        seedTask("Add input sanitization to search", "Strip HTML and SQL metacharacters from search input.", true, 3),
        seedTask("Draft partner integration guide", "Write step-by-step OAuth2 integration guide for partners.", false, 3),
        seedTask("Clean up test data in staging", "Remove test accounts created during load testing.", true, 3),
        seedTask("Configure alerting for disk usage", "Alert when disk exceeds 80 percent on app servers.", false, 3),
        seedTask("Update changelog for v2.0.0", "Document all breaking changes and migration steps.", true, 3),
        seedTask("Fix email encoding for special chars", "Correct UTF-8 encoding issue in email subject lines.", false, 3),
        seedTask("Coordinate infrastructure upgrade", "Schedule maintenance window for database version upgrade.", true, 3),
        seedTask("Review SaaS vendor contracts", "Check renewal dates and pricing for three key tools.", false, 3)
    ));

    // Day 4
    tasks.addAll(List.of(
        seedTask("Write API migration guide for v2", "Document breaking changes and client update steps.", false, 4),
        seedTask("Fix hover state on action buttons", "Restore missing hover background on card action buttons.", true, 4),
        seedTask("Automate dependency vulnerability scan", "Add OWASP dependency check to CI pipeline.", false, 4),
        seedTask("Prepare sales enablement deck", "Create slides for technical deep-dive sales calls.", true, 4),
        seedTask("Resolve DNS propagation issue", "Investigate DNS TTL causing stale routing after migration.", false, 4),
        seedTask("Add content security policy header", "Configure CSP header to prevent XSS attacks.", true, 4),
        seedTask("Implement user preferences API", "Store UI preferences like theme and density per user.", false, 4),
        seedTask("Benchmark concurrent write performance", "Measure throughput under 500 concurrent insert operations.", true, 4),
        seedTask("Update terms of service document", "Reflect new data processing and AI feature disclosures.", false, 4),
        seedTask("Fix broken links in email templates", "Update outdated URLs in transactional email footers.", true, 4)
    ));

    // Day 5
    tasks.addAll(List.of(
        seedTask("Build task assignment feature", "Allow tasks to be assigned to team members by email.", false, 5),
        seedTask("Add export button to reports page", "Trigger CSV download from the reports overview screen.", true, 5),
        seedTask("Review open support tickets", "Triage tickets older than 48 hours without a response.", false, 5),
        seedTask("Implement soft delete for tasks", "Mark tasks as deleted instead of removing from database.", true, 5),
        seedTask("Update privacy policy document", "Add language for new data processing activities.", false, 5),
        seedTask("Test SMS two-factor authentication", "Verify OTP delivery and expiry across mobile carriers.", true, 5),
        seedTask("Analyze search abandonment rate", "Check where users abandon search without clicking results.", false, 5),
        seedTask("Fix session timeout behavior", "Log out user gracefully with redirect on session expiry.", true, 5),
        seedTask("Prepare Q3 budget forecast", "Estimate infrastructure and headcount costs for Q3.", false, 5),
        seedTask("Add health check to all services", "Expose /health endpoint returning service status and version.", true, 5)
    ));

    // Day 6
    tasks.addAll(List.of(
        seedTask("Set up distributed tracing", "Integrate OpenTelemetry SDK across backend services.", false, 6),
        seedTask("Review customer success metrics", "Analyze NPS, CSAT, and expansion revenue trends.", true, 6),
        seedTask("Build reusable form validation library", "Create shared validators for common field patterns.", false, 6),
        seedTask("Implement multi-tenant data isolation", "Enforce tenant ID checks at repository layer.", true, 6),
        seedTask("Draft API deprecation notice", "Notify API consumers of v1 endpoint sunset timeline.", false, 6),
        seedTask("Fix image upload size validation", "Enforce 10MB limit and return clear error message.", true, 6),
        seedTask("Create database ERD diagram", "Generate and publish current entity relationship diagram.", false, 6),
        seedTask("Add retry logic to file uploads", "Resume interrupted uploads using chunked transfer protocol.", true, 6),
        seedTask("Review quarterly OKR results", "Score objective completion and document key learnings.", false, 6),
        seedTask("Configure log rotation policy", "Set retention to 30 days and compress archives after 7.", true, 6)
    ));

    // Day 7
    tasks.addAll(List.of(
        seedTask("Refactor notification templates", "Extract content into separate template files per channel.", false, 7),
        seedTask("Set up code coverage reporting", "Integrate Jacoco and publish HTML reports to CI artifacts.", true, 7),
        seedTask("Write integration tests for API", "Cover happy path and error cases for tasks CRUD API.", false, 7),
        seedTask("Add audit trail for role changes", "Log who changed a user role and when.", true, 7),
        seedTask("Resolve test flakiness in CI", "Identify and fix intermittently failing Cypress tests.", false, 7),
        seedTask("Design new empty state screens", "Create illustrations for empty task list and search states.", true, 7),
        seedTask("Build team performance report", "Aggregate task completion rates by team for last month.", false, 7),
        seedTask("Update API client SDKs", "Regenerate SDKs from updated OpenAPI specification.", true, 7),
        seedTask("Review feature flag audit log", "Check which flags were enabled and disabled last week.", false, 7),
        seedTask("Add custom domain support", "Allow users to use their own domain for the platform.", true, 7)
    ));

    // Day 8
    tasks.addAll(List.of(
        seedTask("Analyze conversion rate by plan", "Compare trial-to-paid conversion across pricing tiers.", false, 8),
        seedTask("Add pagination to search results", "Implement offset and size params for search API.", true, 8),
        seedTask("Write ADR for message queue choice", "Document why RabbitMQ was chosen over SQS.", false, 8),
        seedTask("Fix chart colors in dark mode", "Ensure chart palette meets WCAG contrast in dark mode.", true, 8),
        seedTask("Add bulk import for tasks", "Support CSV upload to create tasks in bulk.", false, 8),
        seedTask("Review infrastructure alert fatigue", "Consolidate noisy alerts and raise meaningful thresholds.", true, 8),
        seedTask("Implement granular permissions", "Add action-level permissions beyond role-level access.", false, 8),
        seedTask("Refactor date utility functions", "Replace custom date logic with standard library methods.", true, 8),
        seedTask("Build client activity feed", "Show recent task events in client-facing account view.", false, 8),
        seedTask("Update security incident playbook", "Add steps for credential exposure and data leak scenarios.", true, 8)
    ));

    // Day 10
    tasks.addAll(List.of(
        seedTask("Implement webhook retry mechanism", "Retry failed webhooks with exponential backoff up to 5 times.", false, 10),
        seedTask("Set up A/B testing framework", "Integrate framework to run controlled UI experiments.", true, 10),
        seedTask("Create onboarding email sequence", "Write 5-part welcome series for new user activation.", false, 10),
        seedTask("Validate PCI DSS compliance scope", "Review card data flows against PCI DSS requirements.", true, 10),
        seedTask("Fix timestamp display in UI", "Show relative timestamps and add absolute on hover.", false, 10),
        seedTask("Document internal API contracts", "Write service interface contracts for all internal APIs.", true, 10),
        seedTask("Build admin user management page", "Allow admins to view, suspend, and delete users.", false, 10),
        seedTask("Optimize image loading on mobile", "Use lazy loading and responsive srcset for all images.", true, 10),
        seedTask("Investigate memory leak in worker", "Profile job queue worker for retained object references.", false, 10),
        seedTask("Review open source license usage", "Audit dependencies for GPL or copyleft license conflicts.", true, 10)
    ));

    // Day 14
    tasks.addAll(List.of(
        seedTask("Add multi-factor authentication", "Support TOTP authenticator apps as second factor.", false, 14),
        seedTask("Build task due date reminder", "Send notification 24 hours before task due date.", true, 14),
        seedTask("Revamp error page design", "Redesign 404 and 500 error pages with helpful messaging.", false, 14),
        seedTask("Implement search analytics", "Track search queries, results count, and click positions.", true, 14),
        seedTask("Update billing portal link", "Replace legacy billing URL with new customer portal link.", false, 14),
        seedTask("Add pagination to team list", "Support page and size params in GET /api/teams.", true, 14),
        seedTask("Fix sort order for pinned tasks", "Ensure pinned tasks always appear at top of list.", false, 14),
        seedTask("Write runbook for incident response", "Outline first responder steps for production incidents.", true, 14),
        seedTask("Migrate config to environment vars", "Replace hardcoded config values with env var references.", false, 14),
        seedTask("Benchmark new search indexer", "Compare response times for old vs new index implementation.", true, 14)
    ));

    // Day 15
    tasks.addAll(List.of(
        seedTask("Profile cold start latency", "Measure Lambda initialization time for all functions.", false, 15),
        seedTask("Create UX writing style guide", "Standardize button labels, error messages, and tooltips.", true, 15),
        seedTask("Add recursive task support", "Allow tasks to have sub-tasks with completion rollup.", false, 15),
        seedTask("Review CI pipeline efficiency", "Find and eliminate redundant build and test steps.", true, 15),
        seedTask("Implement payment dunning flow", "Retry failed payments and notify users before deactivation.", false, 15),
        seedTask("Fix filter state on back navigation", "Restore filter and sort state when user navigates back.", true, 15),
        seedTask("Build customer health score model", "Combine usage, NPS, and support signals into health score.", false, 15),
        seedTask("Update contributing guidelines", "Add PR template, commit message format, and review process.", true, 15),
        seedTask("Audit user session management", "Review session duration, invalidation, and storage.", false, 15),
        seedTask("Add inline editing for task titles", "Allow users to edit task title directly in the list view.", true, 15)
    ));

    // Day 21
    tasks.addAll(List.of(
        seedTask("Redesign notification preferences", "Allow users to configure per-channel notification settings.", false, 21),
        seedTask("Investigate failed payments queue", "Trace stuck payments in retry queue and resolve blockers.", true, 21),
        seedTask("Create reusable modal component", "Build accessible modal supporting confirm and form patterns.", false, 21),
        seedTask("Add task priority field", "Add priority enum (low, medium, high) to task model.", true, 21),
        seedTask("Audit external API usage", "List all third-party API calls with auth and rate limits.", false, 21),
        seedTask("Write contract tests for billing", "Cover critical billing scenarios with provider contract tests.", true, 21),
        seedTask("Improve search result ranking", "Boost recent and highly-engaged tasks in search results.", false, 21),
        seedTask("Finalize office lease renewal", "Submit signed renewal documents for main office lease.", true, 21),
        seedTask("Set up Dependabot alerts", "Enable automated dependency vulnerability notifications.", false, 21),
        seedTask("Review engineering interview rubric", "Update scoring criteria to reflect current hiring bar.", true, 21)
    ));

    // Day 25
    tasks.addAll(List.of(
        seedTask("Design mobile push notifications", "Define notification types and content for mobile app.", false, 25),
        seedTask("Build audit report export", "Generate PDF audit trail report for compliance reviews.", true, 25),
        seedTask("Implement resource-based authorization", "Restrict task access to owner and assigned members.", false, 25),
        seedTask("Add task watchers feature", "Allow users to subscribe to updates on any task.", true, 25),
        seedTask("Investigate Redis connection pool exhaustion", "Trace and resolve ECONNREFUSED errors under load.", false, 25),
        seedTask("Create developer portal", "Build self-service docs and API key management interface.", true, 25),
        seedTask("Review accessibility audit findings", "Address all WCAG 2.1 Level AA failures in report.", false, 25),
        seedTask("Optimize cold cache response time", "Pre-warm critical caches on deployment.", true, 25),
        seedTask("Define engineering career ladder", "Document IC and management tracks with level criteria.", false, 25),
        seedTask("Add scheduled task feature", "Let users set tasks to recur daily, weekly, or monthly.", true, 25)
    ));

    // Day 30
    tasks.addAll(List.of(
        seedTask("Build custom report builder", "Allow users to configure columns and filters in reports.", false, 30),
        seedTask("Add keyboard shortcuts guide", "Document and implement common keyboard shortcuts.", true, 30),
        seedTask("Review third-party data processing", "Assess DPA compliance for all active data processors.", false, 30),
        seedTask("Implement cursor-based pagination", "Replace offset pagination with cursor for scalability.", true, 30),
        seedTask("Create engineering values doc", "Write down team values and decision-making principles.", false, 30),
        seedTask("Fix tooltip positioning on mobile", "Correct tooltip overflow on small viewport widths.", true, 30),
        seedTask("Add task comment threads", "Allow users to comment on tasks with threaded replies.", false, 30),
        seedTask("Automate API contract tests in CI", "Run Pact consumer and provider tests on every PR.", true, 30),
        seedTask("Prepare Q2 board presentation", "Assemble KPI slides and narrative for board meeting.", false, 30),
        seedTask("Analyze task completion trends", "Chart weekly completion rate changes over last quarter.", true, 30)
    ));

    // Day 45
    tasks.addAll(List.of(
        seedTask("Migrate to TypeScript strict mode", "Enable strict null checks across all TypeScript files.", false, 45),
        seedTask("Implement SSO with Okta", "Add SAML 2.0 integration for enterprise customer SSO.", true, 45),
        seedTask("Design dashboard customization", "Allow users to pin and reorder dashboard widgets.", false, 45),
        seedTask("Add geolocation to audit logs", "Record approximate user location on sensitive actions.", true, 45),
        seedTask("Create internal wiki homepage", "Design landing page with quick links and team directory.", false, 45),
        seedTask("Upgrade Spring Boot to latest patch", "Apply latest Spring Boot security patch release.", true, 45),
        seedTask("Write data model migration guide", "Document how to migrate from v1 to v2 data schema.", false, 45),
        seedTask("Validate email DNS configuration", "Check SPF, DKIM, and DMARC records for deliverability.", true, 45),
        seedTask("Implement request tracing headers", "Add trace and span IDs to all outbound service calls.", false, 45),
        seedTask("Review and refresh team handbook", "Update policies, links, and org chart references.", true, 45)
    ));

    // Day 50
    tasks.addAll(List.of(
        seedTask("Evaluate container security scanning", "Compare Snyk, Trivy, and Aqua for image vulnerability scanning.", false, 50),
        seedTask("Build self-service onboarding wizard", "Guide new users through setup in 5 interactive steps.", true, 50),
        seedTask("Implement optimistic locking retry", "Auto-retry writes on version conflict with backoff.", false, 50),
        seedTask("Review API authentication methods", "Evaluate API key vs JWT vs OAuth2 for external access.", true, 50),
        seedTask("Add typing indicators to comments", "Show when a teammate is composing a comment.", false, 50),
        seedTask("Audit service account permissions", "Remove excessive IAM permissions from CI service accounts.", true, 50),
        seedTask("Create customer journey map", "Visualize touchpoints from trial signup to renewal.", false, 50),
        seedTask("Optimize SQL N+1 query in task list", "Batch-load related data instead of per-row queries.", true, 50),
        seedTask("Implement feature request voting", "Let users upvote product feature requests in the portal.", false, 50),
        seedTask("Document event sourcing approach", "Write guide on event store schema and replay strategy.", true, 50)
    ));

    // Day 60
    tasks.addAll(List.of(
        seedTask("Plan infrastructure cost review", "Identify top 10 cost drivers and reduction opportunities.", false, 60),
        seedTask("Add task templates feature", "Allow saving and reusing common task structures.", true, 60),
        seedTask("Refactor authentication middleware", "Consolidate duplicate auth logic across API routes.", false, 60),
        seedTask("Conduct usability testing session", "Run 5 user tests on the new task creation workflow.", true, 60),
        seedTask("Update disaster recovery runbook", "Revise RTO and RPO targets and recovery procedures.", false, 60),
        seedTask("Fix memory usage in data export", "Stream large exports to disk instead of buffering in RAM.", true, 60),
        seedTask("Set up cross-region replication", "Enable database replication to secondary region.", false, 60),
        seedTask("Create partner portal wireframes", "Design screens for partner dashboard and API credentials.", true, 60),
        seedTask("Improve test isolation in suite", "Ensure each test resets state independently.", false, 60),
        seedTask("Add two-pane layout option", "Build split view with task list and detail panel.", true, 60)
    ));

    // Day 90
    tasks.addAll(List.of(
        seedTask("Evaluate new logging platform", "Compare Datadog, Elastic, and Loki for cost and features.", false, 90),
        seedTask("Archive inactive user accounts", "Flag accounts with no activity in 18+ months.", true, 90),
        seedTask("Redesign mobile task creation", "Improve tap targets and form layout on small screens.", false, 90),
        seedTask("Add task history timeline", "Show chronological log of status and field changes.", true, 90),
        seedTask("Conduct annual access audit", "Verify all employee access matches current role.", false, 90),
        seedTask("Write system design document", "Document architecture decisions for upcoming platform rewrite.", true, 90),
        seedTask("Implement SCIM provisioning", "Support automated user provisioning from identity providers.", false, 90),
        seedTask("Analyze query plan for slow report", "Run EXPLAIN ANALYZE and add missing index.", true, 90),
        seedTask("Set up preview environments", "Spin up ephemeral environments per pull request.", false, 90),
        seedTask("Plan annual company offsite", "Book venue and draft 3-day agenda for all-hands retreat.", true, 90)
    ));

    // Day 120
    tasks.addAll(List.of(
        seedTask("Evaluate GraphQL migration", "Assess effort and benefits of migrating REST APIs to GraphQL.", false, 120),
        seedTask("Build notification digest feature", "Bundle non-urgent alerts into hourly or daily digests.", true, 120),
        seedTask("Implement advanced search filters", "Add date range, assignee, and priority filter support.", false, 120),
        seedTask("Create service mesh evaluation", "Compare Istio, Linkerd, and Consul Connect for service mesh.", true, 120),
        seedTask("Draft annual engineering roadmap", "Define major initiatives and technical bets for the year.", false, 120),
        seedTask("Implement zero-downtime migrations", "Run database migrations without locking tables.", true, 120),
        seedTask("Build pipeline for model training", "Automate data prep, training, and evaluation steps.", false, 120),
        seedTask("Set up chaos engineering tests", "Run weekly chaos experiments to validate resilience.", true, 120),
        seedTask("Evaluate edge caching strategy", "Assess CDN and edge caching for API response caching.", false, 120),
        seedTask("Document API versioning strategy", "Define version lifecycle policy and deprecation timeline.", true, 120)
    ));

    taskRepository.saveAll(tasks);

    System.out.println("Seed data loaded: " + taskRepository.count() + " tasks.");
  }

  private Task seedTask(String title, String description, boolean completed, int daysAgo) {
    Task task = new Task(title, description);
    task.setCompleted(completed);
    // Truncate to midnight so tasks in the same group share an identical createdAt,
    // making the secondary sort (title, completed, etc.) an observable tiebreaker.
    task.setCreatedAt(LocalDateTime.now().toLocalDate().minusDays(daysAgo).atStartOfDay());
    return task;
  }
}
