package cc.desuka.demo;

import cc.desuka.demo.audit.AuditDetails;
import cc.desuka.demo.audit.AuditEvent;
import cc.desuka.demo.config.Settings;
import cc.desuka.demo.dto.SavedViewData;
import cc.desuka.demo.dto.TaskListQuery;
import cc.desuka.demo.model.AuditLog;
import cc.desuka.demo.model.ChecklistItem;
import cc.desuka.demo.model.Comment;
import cc.desuka.demo.model.Notification;
import cc.desuka.demo.model.NotificationType;
import cc.desuka.demo.model.Priority;
import cc.desuka.demo.model.Project;
import cc.desuka.demo.model.ProjectMember;
import cc.desuka.demo.model.ProjectRole;
import cc.desuka.demo.model.Role;
import cc.desuka.demo.model.SavedView;
import cc.desuka.demo.model.Setting;
import cc.desuka.demo.model.Tag;
import cc.desuka.demo.model.Task;
import cc.desuka.demo.model.TaskStatus;
import cc.desuka.demo.model.TaskStatusFilter;
import cc.desuka.demo.model.User;
import cc.desuka.demo.repository.AuditLogRepository;
import cc.desuka.demo.repository.CommentRepository;
import cc.desuka.demo.repository.NotificationRepository;
import cc.desuka.demo.repository.ProjectMemberRepository;
import cc.desuka.demo.repository.ProjectRepository;
import cc.desuka.demo.repository.SavedViewRepository;
import cc.desuka.demo.repository.SettingRepository;
import cc.desuka.demo.repository.TagRepository;
import cc.desuka.demo.repository.TaskRepository;
import cc.desuka.demo.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DataLoader implements CommandLineRunner {

    private final TaskRepository taskRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final SavedViewRepository savedViewRepository;
    private final SettingRepository settingRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(
            TaskRepository taskRepository,
            TagRepository tagRepository,
            UserRepository userRepository,
            CommentRepository commentRepository,
            NotificationRepository notificationRepository,
            AuditLogRepository auditLogRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            SavedViewRepository savedViewRepository,
            SettingRepository settingRepository,
            PasswordEncoder passwordEncoder) {
        this.taskRepository = taskRepository;
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.notificationRepository = notificationRepository;
        this.auditLogRepository = auditLogRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.savedViewRepository = savedViewRepository;
        this.settingRepository = settingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        // ── Users (20) ──────────────────────────────────────────────────────────
        // BCrypt encode once — encoding is intentionally slow, so we reuse the same hash.
        // All seeded users share the password "password" for development convenience.
        String encoded = passwordEncoder.encode("password");
        List<User> users =
                userRepository.saveAll(
                        List.of(
                                new User(
                                        "Alice Johnson",
                                        "alice.johnson@example.com",
                                        encoded,
                                        Role.ADMIN),
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
                                new User("Tina Garcia", "tina.garcia@example.com", encoded)));

        User alice = users.get(0);
        User bob = users.get(1);
        User carol = users.get(2);
        User david = users.get(3);
        User eva = users.get(4);
        User frank = users.get(5);
        User grace = users.get(6);
        User henry = users.get(7);
        User isabel = users.get(8);
        User james = users.get(9);
        User karen = users.get(10);
        User liam = users.get(11);
        User mia = users.get(12);
        User noah = users.get(13);
        User olivia = users.get(14);
        User patrick = users.get(15);
        User quinn = users.get(16);
        User rachel = users.get(17);
        User samuel = users.get(18);
        User tina = users.get(19);

        // ── Projects ────────────────────────────────────────────────────────────
        Project platformProject =
                new Project(
                        "Platform Engineering",
                        "Core platform infrastructure, CI/CD, and developer experience improvements.");
        platformProject.setCreatedBy(alice);
        platformProject = projectRepository.save(platformProject);

        Project productProject =
                new Project(
                        "Product Development",
                        "Customer-facing features, UX improvements, and product analytics.");
        productProject.setCreatedBy(bob);
        productProject = projectRepository.save(productProject);

        Project securityProject =
                new Project(
                        "Security & Compliance",
                        "Security audits, compliance certifications, and access management.");
        securityProject.setCreatedBy(alice);
        securityProject = projectRepository.save(securityProject);

        Project opsProject =
                new Project(
                        "Operations",
                        "Infrastructure, monitoring, incident response, and cost management.");
        opsProject.setCreatedBy(david);
        opsProject = projectRepository.save(opsProject);

        // ── Project Members ─────────────────────────────────────────────────────
        projectMemberRepository.saveAll(
                List.of(
                        // Platform Engineering: Alice (owner), Bob, Carol, James, Patrick; Mia
                        // (viewer)
                        new ProjectMember(platformProject, alice, ProjectRole.OWNER),
                        new ProjectMember(platformProject, bob, ProjectRole.EDITOR),
                        new ProjectMember(platformProject, carol, ProjectRole.EDITOR),
                        new ProjectMember(platformProject, james, ProjectRole.EDITOR),
                        new ProjectMember(platformProject, patrick, ProjectRole.EDITOR),
                        new ProjectMember(platformProject, mia, ProjectRole.VIEWER),
                        // Product Development: Bob (owner), Alice, Eva, Isabel, Samuel, Olivia;
                        // Liam
                        // (viewer)
                        new ProjectMember(productProject, bob, ProjectRole.OWNER),
                        new ProjectMember(productProject, alice, ProjectRole.EDITOR),
                        new ProjectMember(productProject, eva, ProjectRole.EDITOR),
                        new ProjectMember(productProject, isabel, ProjectRole.EDITOR),
                        new ProjectMember(productProject, samuel, ProjectRole.EDITOR),
                        new ProjectMember(productProject, olivia, ProjectRole.EDITOR),
                        new ProjectMember(productProject, liam, ProjectRole.VIEWER),
                        // Security & Compliance: Alice (owner), David, Grace, Karen; Rachel
                        // (viewer)
                        new ProjectMember(securityProject, alice, ProjectRole.OWNER),
                        new ProjectMember(securityProject, david, ProjectRole.EDITOR),
                        new ProjectMember(securityProject, grace, ProjectRole.EDITOR),
                        new ProjectMember(securityProject, karen, ProjectRole.EDITOR),
                        new ProjectMember(securityProject, rachel, ProjectRole.VIEWER),
                        // Operations: David (owner), Alice, Frank, Henry, Quinn; Noah (viewer)
                        new ProjectMember(opsProject, david, ProjectRole.OWNER),
                        new ProjectMember(opsProject, alice, ProjectRole.EDITOR),
                        new ProjectMember(opsProject, frank, ProjectRole.EDITOR),
                        new ProjectMember(opsProject, henry, ProjectRole.EDITOR),
                        new ProjectMember(opsProject, quinn, ProjectRole.EDITOR),
                        new ProjectMember(opsProject, noah, ProjectRole.VIEWER),
                        // Cross-project: Tina helps on security and ops
                        new ProjectMember(securityProject, tina, ProjectRole.EDITOR),
                        new ProjectMember(opsProject, tina, ProjectRole.EDITOR)));

        // ── Tags ────────────────────────────────────────────────────────────────
        // Three orthogonal dimensions so combinations feel natural:
        //   domain   → Work, Personal, Home
        //   priority → Urgent, Someday
        List<Tag> tags =
                tagRepository.saveAll(
                        List.of(
                                new Tag("Bug"),
                                new Tag("Feature"),
                                new Tag("DevOps"),
                                new Tag("Security"),
                                new Tag("Documentation"),
                                new Tag("Spike"),
                                new Tag("Blocked"),
                                new Tag("Tech Debt")));

        Tag bug = tags.get(0);
        Tag feature = tags.get(1);
        Tag devops = tags.get(2);
        Tag securityTag = tags.get(3);
        Tag documentation = tags.get(4);
        Tag spike = tags.get(5);
        Tag techDebt = tags.get(7);

        // ── Platform Engineering Tasks ──────────────────────────────────────────
        List<Task> tasks = new ArrayList<>();

        Task pe1 =
                task(
                        "Set up Gradle build cache",
                        "Configure remote build cache to speed up CI builds. "
                                + "Expected to reduce average build time by 40%.",
                        platformProject,
                        james,
                        TaskStatus.IN_PROGRESS,
                        Priority.HIGH,
                        7);
        pe1.setStartDate(today.minusDays(5));
        pe1.setDueDate(today.plusDays(3));
        pe1.setTags(Set.of(devops));
        addChecklist(
                pe1,
                List.of(
                        checklist("Evaluate cache backends (Redis vs S3)", 0, true),
                        checklist("Configure Gradle plugin", 1, true),
                        checklist("Set up CI cache credentials", 2, false),
                        checklist("Benchmark build times", 3, false)));
        tasks.add(pe1);

        Task pe2 =
                task(
                        "Migrate CI from Jenkins to GitHub Actions",
                        "Move all build pipelines to GitHub "
                                + "Actions. Update Docker-based runners and deploy workflows.",
                        platformProject,
                        carol,
                        TaskStatus.COMPLETED,
                        Priority.HIGH,
                        30);
        pe2.setStartDate(today.minusDays(28));
        pe2.setDueDate(today.minusDays(10));
        pe2.setCompletedAt(now.minusDays(12));
        pe2.setTags(Set.of(devops));
        tasks.add(pe2);

        Task pe3 =
                task(
                        "Add database migration tooling",
                        "Integrate Flyway for production database "
                                + "migrations. Define migration naming conventions and rollback procedures.",
                        platformProject,
                        patrick,
                        TaskStatus.OPEN,
                        Priority.MEDIUM,
                        5);
        pe3.setDueDate(today.plusDays(10));
        pe3.setTags(Set.of(devops, spike));
        tasks.add(pe3);

        Task pe4 =
                task(
                        "Implement health check endpoints",
                        "Add /health and /readiness endpoints for "
                                + "Kubernetes probes. Include database and cache connectivity checks.",
                        platformProject,
                        carol,
                        TaskStatus.COMPLETED,
                        Priority.MEDIUM,
                        21);
        pe4.setStartDate(today.minusDays(19));
        pe4.setCompletedAt(now.minusDays(14));
        pe4.setTags(Set.of(feature, devops));
        tasks.add(pe4);

        Task pe5 =
                task(
                        "Configure distributed tracing",
                        "Integrate OpenTelemetry SDK for request "
                                + "tracing across services. Set up Jaeger for trace visualization.",
                        platformProject,
                        null,
                        TaskStatus.BACKLOG,
                        Priority.LOW,
                        15);
        pe5.setTags(Set.of(devops, spike));
        tasks.add(pe5);

        Task pe6 =
                task(
                        "Optimize Docker image sizes",
                        "Switch to multi-stage builds and distroless "
                                + "base images. Current API image is 1.2GB — target under 200MB.",
                        platformProject,
                        carol,
                        TaskStatus.IN_REVIEW,
                        Priority.MEDIUM,
                        10);
        pe6.setStartDate(today.minusDays(8));
        pe6.setDueDate(today.plusDays(1));
        pe6.setTags(Set.of(devops, techDebt));
        addChecklist(
                pe6,
                List.of(
                        checklist("Create multi-stage Dockerfile", 0, true),
                        checklist("Switch to distroless base", 1, true),
                        checklist("Verify all runtime deps present", 2, true),
                        checklist("Benchmark image build time", 3, false)));
        tasks.add(pe6);

        Task pe7 =
                task(
                        "Document local development setup",
                        "Write step-by-step guide for new developers "
                                + "covering env setup, database seeding, and running tests locally.",
                        platformProject,
                        mia,
                        TaskStatus.COMPLETED,
                        Priority.LOW,
                        25);
        pe7.setStartDate(today.minusDays(23));
        pe7.setCompletedAt(now.minusDays(18));
        pe7.setTags(Set.of(documentation));
        tasks.add(pe7);

        Task pe8 =
                task(
                        "Set up preview environments per PR",
                        "Spin up ephemeral environments for each "
                                + "pull request using Kubernetes namespaces and automated teardown.",
                        platformProject,
                        james,
                        TaskStatus.IN_PROGRESS,
                        Priority.HIGH,
                        8);
        pe8.setStartDate(today.minusDays(6));
        pe8.setDueDate(today.plusDays(5));
        pe8.setTags(Set.of(devops, feature));
        tasks.add(pe8);

        Task pe9 =
                task(
                        "Refactor shared utility library",
                        "Extract common date, string, and validation "
                                + "utilities into a shared module. Remove duplicated code across services.",
                        platformProject,
                        patrick,
                        TaskStatus.OPEN,
                        Priority.MEDIUM,
                        3);
        pe9.setDueDate(today.plusDays(14));
        pe9.setTags(Set.of(techDebt));
        tasks.add(pe9);

        Task pe10 =
                task(
                        "Add code coverage reporting",
                        "Integrate JaCoCo with CI pipeline. Publish HTML "
                                + "reports as build artifacts and enforce 80% minimum threshold.",
                        platformProject,
                        carol,
                        TaskStatus.COMPLETED,
                        Priority.LOW,
                        18);
        pe10.setStartDate(today.minusDays(16));
        pe10.setCompletedAt(now.minusDays(11));
        pe10.setTags(Set.of(devops));
        tasks.add(pe10);

        Task pe11 =
                task(
                        "Investigate build time regression",
                        "Recent builds taking 12 minutes instead "
                                + "of the usual 6. Profile annotation processing and test execution phases.",
                        platformProject,
                        alice,
                        TaskStatus.IN_PROGRESS,
                        Priority.MEDIUM,
                        2);
        pe11.setStartDate(today.minusDays(1));
        pe11.setDueDate(today.plusDays(2));
        pe11.setTags(Set.of(bug, devops));
        tasks.add(pe11);

        Task pe12 =
                task(
                        "Upgrade Spring Boot to latest patch",
                        "Apply Spring Boot 4.0.3 security patch. "
                                + "Cancelled after discovering we're already on the latest version.",
                        platformProject,
                        null,
                        TaskStatus.CANCELLED,
                        Priority.LOW,
                        14);
        tasks.add(pe12);

        // ── Product Development Tasks ───────────────────────────────────────────

        Task pd1 =
                task(
                        "Redesign task creation modal",
                        "Improve the task creation UX with inline "
                                + "validation, auto-save drafts, and keyboard navigation support.",
                        productProject,
                        eva,
                        TaskStatus.IN_PROGRESS,
                        Priority.HIGH,
                        6);
        pd1.setStartDate(today.minusDays(4));
        pd1.setDueDate(today.plusDays(4));
        pd1.setTags(Set.of(feature));
        addChecklist(
                pd1,
                List.of(
                        checklist("Design new layout mockups", 0, true),
                        checklist("Implement inline validation", 1, true),
                        checklist("Add keyboard shortcuts", 2, false),
                        checklist("User testing with 3 participants", 3, false)));
        tasks.add(pd1);

        Task pd2 =
                task(
                        "Add search filters for date range",
                        "Allow filtering tasks by creation date "
                                + "and due date ranges. Add date picker component to filter bar.",
                        productProject,
                        isabel,
                        TaskStatus.OPEN,
                        Priority.MEDIUM,
                        4);
        pd2.setDueDate(today.plusDays(12));
        pd2.setTags(Set.of(feature));
        tasks.add(pd2);

        Task pd3 =
                task(
                        "Build customer dashboard widgets",
                        "Create configurable dashboard with task "
                                + "summary, recent activity, and deadline calendar widgets.",
                        productProject,
                        samuel,
                        TaskStatus.IN_REVIEW,
                        Priority.HIGH,
                        12);
        pd3.setStartDate(today.minusDays(10));
        pd3.setDueDate(today.plusDays(1));
        pd3.setTags(Set.of(feature));
        addChecklist(
                pd3,
                List.of(
                        checklist("Task summary widget", 0, true),
                        checklist("Recent activity widget", 1, true),
                        checklist("Deadline calendar widget", 2, true),
                        checklist("Widget drag-and-drop reorder", 3, true),
                        checklist("Responsive layout for mobile", 4, false)));
        tasks.add(pd3);

        Task pd4 =
                task(
                        "Implement CSV export for tasks",
                        "Allow users to export filtered task lists "
                                + "as CSV. Include all visible columns and respect current filters.",
                        productProject,
                        olivia,
                        TaskStatus.COMPLETED,
                        Priority.MEDIUM,
                        20);
        pd4.setStartDate(today.minusDays(18));
        pd4.setCompletedAt(now.minusDays(10));
        pd4.setTags(Set.of(feature));
        tasks.add(pd4);

        Task pd5 =
                task(
                        "Fix mobile navigation spacing",
                        "Tap targets on iPhone SE are too close together. "
                                + "Increase spacing and fix hamburger menu alignment.",
                        productProject,
                        eva,
                        TaskStatus.COMPLETED,
                        Priority.HIGH,
                        8);
        pd5.setStartDate(today.minusDays(7));
        pd5.setCompletedAt(now.minusDays(5));
        pd5.setTags(Set.of(bug));
        tasks.add(pd5);

        Task pd6 =
                task(
                        "Add keyboard shortcuts",
                        "Implement keyboard shortcuts for common actions: "
                                + "N for new task, E for edit, / for search, ? for help overlay.",
                        productProject,
                        null,
                        TaskStatus.BACKLOG,
                        Priority.LOW,
                        22);
        pd6.setTags(Set.of(feature));
        tasks.add(pd6);

        Task pd7 =
                task(
                        "Improve empty state designs",
                        "Create friendly empty state illustrations for "
                                + "task list, search results, and project views.",
                        productProject,
                        samuel,
                        TaskStatus.OPEN,
                        Priority.LOW,
                        3);
        pd7.setDueDate(today.plusDays(21));
        pd7.setTags(Set.of(feature));
        tasks.add(pd7);

        Task pd8 =
                task(
                        "Implement bulk task actions",
                        "Allow selecting multiple tasks for bulk status "
                                + "change, assignment, and deletion. Add select-all checkbox to table view.",
                        productProject,
                        isabel,
                        TaskStatus.IN_PROGRESS,
                        Priority.HIGH,
                        5);
        pd8.setStartDate(today.minusDays(3));
        pd8.setDueDate(today.plusDays(7));
        pd8.setTags(Set.of(feature));
        addChecklist(
                pd8,
                List.of(
                        checklist("Add selection checkboxes to table", 0, true),
                        checklist("Build bulk action toolbar", 1, false),
                        checklist("Implement bulk status change", 2, false),
                        checklist("Implement bulk assignment", 3, false),
                        checklist("Add confirmation dialog", 4, false)));
        tasks.add(pd8);

        Task pd9 =
                task(
                        "Add task activity timeline",
                        "Show chronological log of status changes, "
                                + "comments, and field updates on the task detail view.",
                        productProject,
                        eva,
                        TaskStatus.COMPLETED,
                        Priority.MEDIUM,
                        15);
        pd9.setStartDate(today.minusDays(13));
        pd9.setCompletedAt(now.minusDays(7));
        pd9.setTags(Set.of(feature));
        tasks.add(pd9);

        Task pd10 =
                task(
                        "Optimize first paint time",
                        "Defer non-critical dashboard widgets and "
                                + "optimize JavaScript bundle splitting. Target under 1.5s LCP.",
                        productProject,
                        olivia,
                        TaskStatus.IN_REVIEW,
                        Priority.MEDIUM,
                        9);
        pd10.setStartDate(today.minusDays(7));
        pd10.setDueDate(today.plusDays(2));
        pd10.setTags(Set.of(techDebt, spike));
        tasks.add(pd10);

        Task pd11 =
                task(
                        "Add drag-and-drop task reordering",
                        "Enable drag-and-drop in card view "
                                + "for manual task ordering. Persist order per user preference.",
                        productProject,
                        null,
                        TaskStatus.BACKLOG,
                        Priority.MEDIUM,
                        18);
        pd11.setTags(Set.of(feature));
        tasks.add(pd11);

        Task pd12 =
                task(
                        "Build notification preferences page",
                        "Allow users to configure per-type "
                                + "notification settings: email, in-app, and push.",
                        productProject,
                        samuel,
                        TaskStatus.OPEN,
                        Priority.MEDIUM,
                        2);
        pd12.setDueDate(today.plusDays(14));
        pd12.setTags(Set.of(feature));
        tasks.add(pd12);

        // ── Security & Compliance Tasks ─────────────────────────────────────────

        Task sc1 =
                task(
                        "Conduct quarterly access review",
                        "Review all employee access grants against "
                                + "current roles. Collect manager approvals for privileged accounts.",
                        securityProject,
                        grace,
                        TaskStatus.IN_PROGRESS,
                        Priority.HIGH,
                        7);
        sc1.setStartDate(today.minusDays(5));
        sc1.setDueDate(today.plusDays(3));
        sc1.setTags(Set.of(securityTag));
        addChecklist(
                sc1,
                List.of(
                        checklist("Export current access grants", 0, true),
                        checklist("Cross-reference with HR roster", 1, true),
                        checklist("Flag access for terminated employees", 2, true),
                        checklist("Send approval requests to managers", 3, false),
                        checklist("Revoke unapproved access", 4, false)));
        tasks.add(sc1);

        Task sc2 =
                task(
                        "Update data retention policy",
                        "Revise retention periods for logs, audit "
                                + "events, and user data. Align with new privacy regulations.",
                        securityProject,
                        karen,
                        TaskStatus.OPEN,
                        Priority.MEDIUM,
                        4);
        sc2.setDueDate(today.plusDays(14));
        sc2.setTags(Set.of(securityTag, documentation));
        tasks.add(sc2);

        Task sc3 =
                task(
                        "Rotate production API keys",
                        "Issue new API keys for all production services. "
                                + "Coordinate cutover window with ops team to avoid downtime.",
                        securityProject,
                        david,
                        TaskStatus.COMPLETED,
                        Priority.HIGH,
                        16);
        sc3.setStartDate(today.minusDays(14));
        sc3.setCompletedAt(now.minusDays(10));
        sc3.setTags(Set.of(securityTag, devops));
        tasks.add(sc3);

        Task sc4 =
                task(
                        "Complete SOC 2 evidence collection",
                        "Gather evidence for all SOC 2 Type II "
                                + "controls. Coordinate with engineering teams for technical evidence.",
                        securityProject,
                        karen,
                        TaskStatus.IN_PROGRESS,
                        Priority.HIGH,
                        14);
        sc4.setStartDate(today.minusDays(12));
        sc4.setDueDate(today.plusDays(7));
        sc4.setTags(Set.of(securityTag, documentation));
        addChecklist(
                sc4,
                List.of(
                        checklist("Access control evidence", 0, true),
                        checklist("Change management evidence", 1, true),
                        checklist("Incident response evidence", 2, false),
                        checklist("Monitoring and logging evidence", 3, false),
                        checklist("Encryption evidence", 4, false),
                        checklist("Vendor management evidence", 5, false)));
        tasks.add(sc4);

        Task sc5 =
                task(
                        "Audit admin permission grants",
                        "Review and document all admin-level access. "
                                + "Remove unnecessary high-privilege grants following least-privilege principle.",
                        securityProject,
                        grace,
                        TaskStatus.OPEN,
                        Priority.MEDIUM,
                        3);
        sc5.setDueDate(today.plusDays(10));
        sc5.setTags(Set.of(securityTag));
        tasks.add(sc5);

        Task sc6 =
                task(
                        "Implement GDPR delete flow",
                        "Build end-to-end user data deletion. Ensure "
                                + "removal across primary database, analytics, backups, and third-party systems.",
                        securityProject,
                        david,
                        TaskStatus.IN_REVIEW,
                        Priority.HIGH,
                        11);
        sc6.setStartDate(today.minusDays(9));
        sc6.setDueDate(today.plusDays(2));
        sc6.setTags(Set.of(securityTag, feature));
        tasks.add(sc6);

        Task sc7 =
                task(
                        "Review third-party data processors",
                        "Assess DPA compliance for all active "
                                + "data processors. Update vendor registry with processing details.",
                        securityProject,
                        null,
                        TaskStatus.BACKLOG,
                        Priority.MEDIUM,
                        20);
        sc7.setTags(Set.of(securityTag, spike));
        tasks.add(sc7);

        Task sc8 =
                task(
                        "Update incident response playbook",
                        "Revise escalation procedures and "
                                + "notification timelines. Add sections for data breach and ransomware scenarios.",
                        securityProject,
                        grace,
                        TaskStatus.COMPLETED,
                        Priority.MEDIUM,
                        22);
        sc8.setStartDate(today.minusDays(20));
        sc8.setCompletedAt(now.minusDays(13));
        sc8.setTags(Set.of(securityTag, documentation));
        tasks.add(sc8);

        Task sc9 =
                task(
                        "Run penetration test",
                        "Engage external firm for annual penetration test. "
                                + "Scope includes web app, API, and infrastructure layers.",
                        securityProject,
                        alice,
                        TaskStatus.COMPLETED,
                        Priority.HIGH,
                        35);
        sc9.setStartDate(today.minusDays(33));
        sc9.setCompletedAt(now.minusDays(20));
        sc9.setTags(Set.of(securityTag));
        tasks.add(sc9);

        Task sc10 =
                task(
                        "Configure WAF rules for bot traffic",
                        "Block known scrapers and add rate "
                                + "limiting for suspicious IPs. Configure challenge pages for automated traffic.",
                        securityProject,
                        david,
                        TaskStatus.OPEN,
                        Priority.HIGH,
                        2);
        sc10.setDueDate(today.plusDays(5));
        sc10.setTags(Set.of(securityTag, devops));
        tasks.add(sc10);

        Task sc11 =
                task(
                        "Validate PCI DSS scope",
                        "Cancelled — payment processing moved to Stripe, "
                                + "removing PCI scope from our infrastructure.",
                        securityProject,
                        karen,
                        TaskStatus.CANCELLED,
                        Priority.MEDIUM,
                        28);
        sc11.setTags(Set.of(securityTag));
        tasks.add(sc11);

        Task sc12 =
                task(
                        "Set up vulnerability scanning",
                        "Integrate Trivy for container image scanning "
                                + "and OWASP Dependency-Check for library vulnerabilities in CI pipeline.",
                        securityProject,
                        tina,
                        TaskStatus.IN_REVIEW,
                        Priority.MEDIUM,
                        9);
        sc12.setStartDate(today.minusDays(7));
        sc12.setDueDate(today.plusDays(3));
        sc12.setTags(Set.of(securityTag, devops));
        tasks.add(sc12);

        // ── Operations Tasks ────────────────────────────────────────────────────

        Task op1 =
                task(
                        "Set up Grafana monitoring dashboards",
                        "Create panels for API latency, error "
                                + "rates, throughput, and database connection pool metrics.",
                        opsProject,
                        frank,
                        TaskStatus.COMPLETED,
                        Priority.HIGH,
                        18);
        op1.setStartDate(today.minusDays(16));
        op1.setCompletedAt(now.minusDays(9));
        op1.setTags(Set.of(devops));
        tasks.add(op1);

        Task op2 =
                task(
                        "Configure alerting for disk usage",
                        "Set up PagerDuty alerts when disk usage "
                                + "exceeds 80% on app servers. Add Slack notification for 70% warning.",
                        opsProject,
                        henry,
                        TaskStatus.IN_PROGRESS,
                        Priority.MEDIUM,
                        6);
        op2.setStartDate(today.minusDays(4));
        op2.setDueDate(today.plusDays(2));
        op2.setTags(Set.of(devops));
        addChecklist(
                op2,
                List.of(
                        checklist("Define alert thresholds", 0, true),
                        checklist("Configure PagerDuty integration", 1, true),
                        checklist("Set up Slack webhook", 2, false),
                        checklist("Test alert firing and resolution", 3, false)));
        tasks.add(op2);

        Task op3 =
                task(
                        "Investigate intermittent 502 errors",
                        "Trace gateway timeouts during peak "
                                + "traffic windows (2-4pm UTC). Check upstream service response times.",
                        opsProject,
                        frank,
                        TaskStatus.IN_PROGRESS,
                        Priority.HIGH,
                        3);
        op3.setStartDate(today.minusDays(2));
        op3.setDueDate(today.plusDays(1));
        op3.setTags(Set.of(bug));
        tasks.add(op3);

        Task op4 =
                task(
                        "Optimize PostgreSQL connection pool",
                        "Reduce connection saturation during "
                                + "peak cron execution. Evaluate HikariCP pool sizing and idle timeout settings.",
                        opsProject,
                        quinn,
                        TaskStatus.OPEN,
                        Priority.MEDIUM,
                        5);
        op4.setDueDate(today.plusDays(8));
        op4.setTags(Set.of(techDebt, spike));
        tasks.add(op4);

        Task op5 =
                task(
                        "Document disaster recovery procedures",
                        "Write runbook covering database "
                                + "restore, service failover, and DNS cutover. Include RTO/RPO targets.",
                        opsProject,
                        henry,
                        TaskStatus.COMPLETED,
                        Priority.MEDIUM,
                        25);
        op5.setStartDate(today.minusDays(23));
        op5.setCompletedAt(now.minusDays(15));
        op5.setTags(Set.of(documentation));
        tasks.add(op5);

        Task op6 =
                task(
                        "Set up canary deployment strategy",
                        "Ship 10% traffic to new version first. "
                                + "Auto-halt rollout if error rate exceeds threshold.",
                        opsProject,
                        null,
                        TaskStatus.BACKLOG,
                        Priority.HIGH,
                        12);
        op6.setTags(Set.of(devops, feature));
        tasks.add(op6);

        Task op7 =
                task(
                        "Migrate cron jobs to scheduler service",
                        "Consolidate scattered cron jobs "
                                + "under centralized scheduler with retry, logging, and dead-letter handling.",
                        opsProject,
                        quinn,
                        TaskStatus.IN_REVIEW,
                        Priority.MEDIUM,
                        10);
        op7.setStartDate(today.minusDays(8));
        op7.setDueDate(today.plusDays(3));
        op7.setTags(Set.of(devops, techDebt));
        tasks.add(op7);

        Task op8 =
                task(
                        "Reduce noisy staging alerts",
                        "Adjust alert thresholds for staging environment. "
                                + "Silence non-actionable failures that cause alert fatigue.",
                        opsProject,
                        frank,
                        TaskStatus.OPEN,
                        Priority.LOW,
                        4);
        op8.setDueDate(today.plusDays(14));
        op8.setTags(Set.of(devops));
        tasks.add(op8);

        Task op9 =
                task(
                        "Tune Redis cache eviction policy",
                        "Switch from allkeys-lru to volatile-lru. "
                                + "Add TTL to session keys and increase maxmemory to reduce thrashing.",
                        opsProject,
                        henry,
                        TaskStatus.COMPLETED,
                        Priority.LOW,
                        15);
        op9.setStartDate(today.minusDays(13));
        op9.setCompletedAt(now.minusDays(8));
        op9.setTags(Set.of(techDebt));
        tasks.add(op9);

        Task op10 =
                task(
                        "Add smoke tests to deploy pipeline",
                        "Run critical-path API tests after "
                                + "every production deployment. Auto-rollback if tests fail.",
                        opsProject,
                        quinn,
                        TaskStatus.OPEN,
                        Priority.HIGH,
                        2);
        op10.setDueDate(today.plusDays(7));
        op10.setTags(Set.of(devops));
        tasks.add(op10);

        Task op11 =
                task(
                        "Investigate memory spike after deploy",
                        "API pods showing 2x memory usage "
                                + "after latest rollout. Profile heap growth and check for resource leaks.",
                        opsProject,
                        david,
                        TaskStatus.IN_PROGRESS,
                        Priority.HIGH,
                        1);
        op11.setStartDate(today);
        op11.setDueDate(today.plusDays(2));
        op11.setTags(Set.of(bug));
        tasks.add(op11);

        Task op12 =
                task(
                        "Plan infrastructure cost reduction",
                        "Identify top 10 cost drivers and "
                                + "propose optimization opportunities. Target 20% reduction this quarter.",
                        opsProject,
                        null,
                        TaskStatus.BACKLOG,
                        Priority.MEDIUM,
                        8);
        op12.setTags(Set.of(spike));
        tasks.add(op12);

        // ── Task Dependencies ─────────────────────────────────────────────────
        // Set up before saveAll so entities are still new (no @Version conflict)
        // pe3 (DB migration tooling) was blocked by pe2 (CI migration) — now resolved
        pe2.getBlocks().add(pe3);

        // pe5 (distributed tracing) was blocked by pe4 (health checks) — now resolved
        pe4.getBlocks().add(pe5);

        // pe6 (Docker optimization) blocked by pe1 (build cache) — still active
        pe1.getBlocks().add(pe6);

        taskRepository.saveAll(tasks);

        // ── Comments ────────────────────────────────────────────────────────────
        // Meaningful conversations between actual project members.
        List<Comment> comments = new ArrayList<>();

        // Platform Engineering comments
        comments.add(
                comment(
                        pe1,
                        carol,
                        "Have you considered using S3 as the cache backend? "
                                + "It's cheaper than Redis for build artifacts.",
                        now.minusDays(5).plusHours(3)));
        comments.add(
                comment(
                        pe1,
                        james,
                        "Good point. I'll benchmark both. S3 latency might be "
                                + "acceptable since cache hits save minutes of build time anyway.",
                        now.minusDays(5).plusHours(5)));
        comments.add(
                comment(
                        pe1,
                        alice,
                        "Let's go with S3 to start. We can always switch to "
                                + "Redis later if latency becomes an issue.",
                        now.minusDays(4).plusHours(2)));

        comments.add(
                comment(
                        pe6,
                        james,
                        "The distroless base is missing some debug tools. "
                                + "Should we keep a debug variant for staging?",
                        now.minusDays(3).plusHours(4)));
        comments.add(
                comment(
                        pe6,
                        carol,
                        "I'll create a separate debug Dockerfile. Production "
                                + "images should stay minimal.",
                        now.minusDays(3).plusHours(6)));

        comments.add(
                comment(
                        pe8,
                        alice,
                        "Make sure preview envs are auto-cleaned up after PR "
                                + "merge. We don't want orphaned namespaces.",
                        now.minusDays(4).plusHours(1)));
        comments.add(
                comment(
                        pe8,
                        james,
                        "Already included — there's a finalizer that tears down "
                                + "the namespace when the PR is closed or merged.",
                        now.minusDays(3).plusHours(3)));

        comments.add(
                comment(
                        pe11,
                        carol,
                        "I profiled the build — MapStruct annotation processing "
                                + "is taking 4 minutes. Something changed in the latest upgrade.",
                        now.minusDays(1).plusHours(2)));

        // Product Development comments
        comments.add(
                comment(
                        pd1,
                        bob,
                        "The mockups look great. Can we also add a \"quick create\" "
                                + "mode that just takes title and project?",
                        now.minusDays(3).plusHours(2)));
        comments.add(
                comment(
                        pd1,
                        eva,
                        "That's a good idea! I'll add it as a toggle in the modal "
                                + "header. Users can switch between simple and full mode.",
                        now.minusDays(3).plusHours(5)));

        comments.add(
                comment(
                        pd3,
                        bob,
                        "The calendar widget needs to handle tasks without due dates "
                                + "gracefully. Right now they just disappear.",
                        now.minusDays(2).plusHours(3)));
        comments.add(
                comment(
                        pd3,
                        samuel,
                        "Fixed — unscheduled tasks now show in an \"Unscheduled\" "
                                + "section below the calendar grid.",
                        now.minusDays(2).plusHours(6)));
        comments.add(
                comment(
                        pd3,
                        olivia,
                        "Love the widget layout. One suggestion: can we make the "
                                + "activity widget update in real-time via WebSocket?",
                        now.minusDays(1).plusHours(4)));

        comments.add(
                comment(
                        pd8,
                        bob,
                        "Make sure bulk delete requires confirmation. We don't want "
                                + "accidental mass deletion.",
                        now.minusDays(2).plusHours(1)));
        comments.add(
                comment(
                        pd8,
                        isabel,
                        "Already in the checklist — the confirm dialog will show "
                                + "the count of affected tasks before proceeding.",
                        now.minusDays(2).plusHours(3)));

        comments.add(
                comment(
                        pd10,
                        bob,
                        "What's the current LCP? I want to track the improvement.",
                        now.minusDays(3).plusHours(5)));
        comments.add(
                comment(
                        pd10,
                        olivia,
                        "Currently at 2.8s. The main bottleneck is the task count "
                                + "queries — I've added caching and it's down to 1.3s now.",
                        now.minusDays(3).plusHours(8)));

        // Security & Compliance comments
        comments.add(
                comment(
                        sc1,
                        karen,
                        "Three accounts flagged for terminated employees. Sending "
                                + "revocation requests now.",
                        now.minusDays(3).plusHours(2)));
        comments.add(
                comment(
                        sc1,
                        grace,
                        "Also found two service accounts with overly broad "
                                + "permissions. Adding them to the review list.",
                        now.minusDays(3).plusHours(4)));

        comments.add(
                comment(
                        sc4,
                        alice,
                        "The auditors are asking for network diagram evidence. "
                                + "Can we get that from the ops team?",
                        now.minusDays(5).plusHours(3)));
        comments.add(
                comment(
                        sc4,
                        karen,
                        "@[David Brown](userId:"
                                + david.getId()
                                + ") can you "
                                + "share the latest infrastructure diagram?",
                        now.minusDays(5).plusHours(5)));
        comments.add(
                comment(
                        sc4,
                        david,
                        "Shared it in the compliance folder. Let me know if you "
                                + "need anything else.",
                        now.minusDays(4).plusHours(2)));

        comments.add(
                comment(
                        sc6,
                        grace,
                        "The analytics cleanup is trickier than expected — some "
                                + "events are in a denormalized format.",
                        now.minusDays(3).plusHours(4)));
        comments.add(
                comment(
                        sc6,
                        david,
                        "I'll help with the analytics side. We might need to "
                                + "coordinate with the data team for the warehouse cleanup.",
                        now.minusDays(2).plusHours(2)));

        comments.add(
                comment(
                        sc12,
                        grace,
                        "Trivy found 3 critical CVEs in the base image. Already "
                                + "patched in the latest tag.",
                        now.minusDays(4).plusHours(3)));
        comments.add(
                comment(
                        sc12,
                        tina,
                        "I'll add the OWASP check results to the PR template so "
                                + "reviewers can see dependency status at a glance.",
                        now.minusDays(3).plusHours(5)));

        // Operations comments
        comments.add(
                comment(
                        op2,
                        david,
                        "Make sure the 70% warning goes to the #ops-alerts Slack "
                                + "channel, not the general channel.",
                        now.minusDays(3).plusHours(2)));
        comments.add(
                comment(
                        op2,
                        henry,
                        "Good call. I'll also add a runbook link in the alert "
                                + "message so the on-call knows what to do.",
                        now.minusDays(3).plusHours(4)));

        comments.add(
                comment(
                        op3,
                        david,
                        "I see the 502s correlate with a spike in database "
                                + "connections. Might be related to the connection pool issue.",
                        now.minusDays(1).plusHours(3)));
        comments.add(
                comment(
                        op3,
                        frank,
                        "Confirmed — the cron job is exhausting the pool during "
                                + "peak traffic. We need to either stagger the crons or increase the pool.",
                        now.minusDays(1).plusHours(6)));
        comments.add(
                comment(
                        op3,
                        quinn,
                        "I can help stagger the cron schedules. Let me check "
                                + "which jobs can be moved to off-peak hours.",
                        now.minusHours(20)));

        comments.add(
                comment(
                        op7,
                        david,
                        "Make sure we keep backward compatibility with existing "
                                + "cron expressions during the migration.",
                        now.minusDays(4).plusHours(2)));
        comments.add(
                comment(
                        op7,
                        quinn,
                        "The scheduler supports standard cron syntax, so existing "
                                + "expressions will work as-is. Just need to add the retry config.",
                        now.minusDays(4).plusHours(5)));

        comments.add(
                comment(
                        op11,
                        frank,
                        "The heap dump shows a large number of unclosed HTTP "
                                + "connections. Looks like a client connection leak.",
                        now.minusHours(8)));
        comments.add(
                comment(
                        op11,
                        david,
                        "Found it — the new retry logic wasn't closing the "
                                + "response body on error paths. Fix is ready for review.",
                        now.minusHours(4)));

        commentRepository.saveAll(comments);

        // ── Checklist items on additional tasks ─────────────────────────────────
        // Some tasks already have checklists added above. Add a few more for variety.
        addChecklist(
                pe9,
                List.of(
                        checklist("Identify duplicated utilities", 0, false),
                        checklist("Extract shared module", 1, false),
                        checklist("Update all import references", 2, false)));
        addChecklist(
                sc10,
                List.of(
                        checklist("Review current WAF rules", 0, false),
                        checklist("Add bot detection rules", 1, false),
                        checklist("Configure rate limiting", 2, false),
                        checklist("Test with production traffic sample", 3, false)));
        addChecklist(
                op10,
                List.of(
                        checklist("Define critical-path test cases", 0, false),
                        checklist("Write smoke test scripts", 1, false),
                        checklist("Integrate with deploy pipeline", 2, false),
                        checklist("Add auto-rollback trigger", 3, false)));

        taskRepository.saveAll(tasks);

        // ── Notifications ───────────────────────────────────────────────────────
        // Seed notifications so the notification bell has content on first boot.
        List<Notification> notifications = new ArrayList<>();

        // Due-date reminders for tasks due tomorrow
        for (Task t : tasks) {
            if (t.getUser() != null
                    && t.getDueDate() != null
                    && t.getDueDate().equals(today.plusDays(1))
                    && t.getStatus() != TaskStatus.COMPLETED
                    && t.getStatus() != TaskStatus.CANCELLED) {
                notifications.add(
                        new Notification(
                                t.getUser(),
                                null,
                                NotificationType.TASK_DUE_REMINDER,
                                "Due tomorrow: " + t.getTitle(),
                                "/tasks/" + t.getId()));
            }
        }

        // Comment notifications for recent comments
        notifications.add(
                notification(
                        james,
                        carol,
                        NotificationType.COMMENT_ADDED,
                        "Carol Williams commented on: Set up Gradle build cache",
                        "/tasks/" + pe1.getId(),
                        now.minusDays(5).plusHours(3)));
        notifications.add(
                notification(
                        james,
                        alice,
                        NotificationType.COMMENT_ADDED,
                        "Alice Johnson commented on: Set up Gradle build cache",
                        "/tasks/" + pe1.getId(),
                        now.minusDays(4).plusHours(2)));
        notifications.add(
                notification(
                        frank,
                        david,
                        NotificationType.COMMENT_ADDED,
                        "David Brown commented on: Investigate intermittent 502 errors",
                        "/tasks/" + op3.getId(),
                        now.minusDays(1).plusHours(3)));
        notifications.add(
                notification(
                        eva,
                        bob,
                        NotificationType.COMMENT_ADDED,
                        "Bob Smith commented on: Redesign task creation modal",
                        "/tasks/" + pd1.getId(),
                        now.minusDays(3).plusHours(2)));
        notifications.add(
                notification(
                        david,
                        karen,
                        NotificationType.COMMENT_MENTIONED,
                        "Karen Chen mentioned you in: Complete SOC 2 evidence collection",
                        "/tasks/" + sc4.getId(),
                        now.minusDays(5).plusHours(5)));

        notificationRepository.saveAll(notifications);

        // ── Curated demo data for Alice & Bob ───────────────────────────────────
        List<Project> projects =
                List.of(platformProject, productProject, securityProject, opsProject);
        seedDemoInteractions(users, tags, projects);

        // ── Settings ────────────────────────────────────────────────────────────
        settingRepository.save(new Setting(Settings.KEY_THEME, Settings.THEME_WORKSHOP));

        System.out.println(
                "Seed data loaded: "
                        + userRepository.count()
                        + " users, "
                        + projectRepository.count()
                        + " projects, "
                        + tagRepository.count()
                        + " tags, "
                        + taskRepository.count()
                        + " tasks, "
                        + commentRepository.count()
                        + " comments, "
                        + notificationRepository.count()
                        + " notifications, "
                        + auditLogRepository.count()
                        + " audit logs, "
                        + settingRepository.count()
                        + " settings.");
    }

    /**
     * Creates hand-crafted tasks, comments, @mentions, and notifications between Alice (admin) and
     * Bob (user) so both accounts have rich, realistic data on first login. Also involves Carol
     * (users[2]) for multi-party interactions.
     */
    private void seedDemoInteractions(List<User> users, List<Tag> tags, List<Project> projects) {
        User alice = users.get(0); // admin
        User bob = users.get(1);
        User carol = users.get(2);
        User david = users.get(3);
        User eva = users.get(4);
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        Project platform = projects.get(0); // Platform Engineering
        Project product = projects.get(1); // Product Development
        Project security = projects.get(2); // Security & Compliance

        Tag bugTag = tags.get(0);
        Tag featureTag = tags.get(1);
        Tag devopsTag = tags.get(2);
        Tag securityTag = tags.get(3);
        Tag documentationTag = tags.get(4);
        Tag spikeTag = tags.get(5);

        // ── Task 1: Alice assigned to Bob, with comment thread ────────────
        Task t1 =
                new Task(
                        "Set up CI/CD pipeline for staging",
                        "Configure GitHub Actions workflow with build, test, and deploy stages. Use Docker-based runners for consistency.");
        t1.setProject(platform);
        t1.setUser(bob);
        t1.setStatus(TaskStatus.IN_PROGRESS);
        t1.setPriority(Priority.HIGH);
        t1.setTags(Set.of(devopsTag));
        t1.setStartDate(today.minusDays(5));
        t1.setDueDate(today.plusDays(2));
        t1.setCreatedAt(now.minusDays(5));
        addChecklist(
                t1,
                List.of(
                        checklist("Configure build stage", 0, true),
                        checklist("Add test stage with coverage", 1, true),
                        checklist("Set up staging deploy", 2, false),
                        checklist("Add Slack notifications", 3, false),
                        checklist("Document pipeline in wiki", 4, false)));
        t1 = taskRepository.save(t1);

        Comment c1a =
                comment(
                        t1,
                        alice,
                        "Hey @[Bob Smith](userId:"
                                + bob.getId()
                                + "), I've outlined the stages we need. Can you start with the build config?",
                        now.minusDays(5).plusHours(1));
        Comment c1b =
                comment(
                        t1,
                        bob,
                        "On it! I'll base it on the Docker runner setup from the docs. Should have the build stage done today.",
                        now.minusDays(5).plusHours(3));
        Comment c1c =
                comment(
                        t1,
                        alice,
                        "Build and test stages look great. For the deploy stage, make sure to use the staging secrets from the vault.",
                        now.minusDays(3).plusHours(2));
        Comment c1d =
                comment(
                        t1,
                        bob,
                        "Good call. I also added a coverage threshold — it'll fail the build if coverage drops below 80%. Working on the deploy stage now.",
                        now.minusDays(2).plusHours(4));
        commentRepository.saveAll(List.of(c1a, c1b, c1c, c1d));

        // Bob gets notification: assigned by Alice
        notificationRepository.save(
                new Notification(
                        bob,
                        alice,
                        NotificationType.TASK_ASSIGNED,
                        "Alice Johnson assigned you: Set up CI/CD pipeline for staging",
                        "/tasks/" + t1.getId()));

        // ── Task 2: Bob assigned to Alice, overdue ────────────────────────
        Task t2 =
                new Task(
                        "Write API rate limiting design doc",
                        "Document the approach for API rate limiting including per-user quotas, sliding window algorithm, and Redis-backed counters.");
        t2.setProject(platform);
        t2.setUser(alice);
        t2.setStatus(TaskStatus.IN_PROGRESS);
        t2.setPriority(Priority.HIGH);
        t2.setTags(Set.of(documentationTag, spikeTag));
        t2.setStartDate(today.minusDays(10));
        t2.setDueDate(today.minusDays(1)); // overdue
        t2.setCreatedAt(now.minusDays(10));
        addChecklist(
                t2,
                List.of(
                        checklist("Research sliding window algorithms", 0, true),
                        checklist("Draft architecture section", 1, true),
                        checklist("Add Redis schema design", 2, false),
                        checklist("Get review from team", 3, false)));
        t2 = taskRepository.save(t2);

        Comment c2a =
                comment(
                        t2,
                        bob,
                        "@[Alice Johnson](userId:"
                                + alice.getId()
                                + ") I started a draft with the requirements. Can you fill in the Redis schema section?",
                        now.minusDays(10).plusHours(2));
        Comment c2b =
                comment(
                        t2,
                        alice,
                        "Sure! I've been reading about sliding window counters. Will draft the Redis section this week.",
                        now.minusDays(9).plusHours(5));
        Comment c2c =
                comment(
                        t2,
                        carol,
                        "Hey @[Alice Johnson](userId:"
                                + alice.getId()
                                + "), we used a similar approach at my previous company. Happy to review when it's ready.",
                        now.minusDays(7).plusHours(3));
        Comment c2d =
                comment(
                        t2,
                        alice,
                        "Thanks @[Carol Williams](userId:"
                                + carol.getId()
                                + ")! I'll tag you once the draft is complete. Still working through the edge cases for burst traffic.",
                        now.minusDays(5).plusHours(1));
        commentRepository.saveAll(List.of(c2a, c2b, c2c, c2d));

        // Alice gets notification: assigned by Bob
        notificationRepository.save(
                new Notification(
                        alice,
                        bob,
                        NotificationType.TASK_ASSIGNED,
                        "Bob Smith assigned you: Write API rate limiting design doc",
                        "/tasks/" + t2.getId()));
        // Alice gets overdue notification
        notificationRepository.save(
                new Notification(
                        alice,
                        null,
                        NotificationType.TASK_OVERDUE,
                        "Overdue: Write API rate limiting design doc",
                        "/tasks/" + t2.getId()));

        // ── Task 3: Completed task with full conversation ─────────────────
        Task t3 =
                new Task(
                        "Migrate user auth to Spring Security 7",
                        "Upgrade from Spring Security 6 to 7. Update filter chain config, switch to new authorization model, and verify all endpoints.");
        t3.setProject(security);
        t3.setUser(alice);
        t3.setStatus(TaskStatus.COMPLETED);
        t3.setPriority(Priority.HIGH);
        t3.setTags(Set.of(securityTag));
        t3.setStartDate(today.minusDays(14));
        t3.setDueDate(today.minusDays(5));
        t3.setCompletedAt(now.minusDays(6));
        t3.setCreatedAt(now.minusDays(14));
        addChecklist(
                t3,
                List.of(
                        checklist("Update SecurityConfig", 0, true),
                        checklist("Migrate custom filters", 1, true),
                        checklist("Update test security config", 2, true),
                        checklist("Verify all protected endpoints", 3, true),
                        checklist("Update documentation", 4, true)));
        t3 = taskRepository.save(t3);

        Comment c3a =
                comment(
                        t3,
                        alice,
                        "Starting the migration. The new authorization model in Spring Security 7 is quite different — @[Bob Smith](userId:"
                                + bob.getId()
                                + ") heads up if you see any auth issues.",
                        now.minusDays(14).plusHours(3));
        Comment c3b =
                comment(
                        t3,
                        bob,
                        "Thanks for the heads up. I'll keep an eye on the API endpoints I own.",
                        now.minusDays(13).plusHours(1));
        Comment c3c =
                comment(
                        t3,
                        david,
                        "I ran into a similar migration last month. The biggest gotcha is the new default CSRF handling — it changed from opt-out to opt-in for API endpoints.",
                        now.minusDays(12).plusHours(6));
        Comment c3d =
                comment(
                        t3,
                        alice,
                        "Good tip @[David Brown](userId:"
                                + david.getId()
                                + ")! I just hit that exact issue. Fixed by explicitly disabling CSRF for /api/** paths.",
                        now.minusDays(11).plusHours(2));
        Comment c3e =
                comment(
                        t3,
                        alice,
                        "Migration complete. All endpoints verified. Merging to main now.",
                        now.minusDays(6).plusHours(1));
        commentRepository.saveAll(List.of(c3a, c3b, c3c, c3d, c3e));

        // ── Task 4: Bob's task, due soon, with mentions ───────────────────
        Task t4 =
                new Task(
                        "Design WebSocket notification system",
                        "Plan the real-time notification architecture using STOMP over WebSocket. Cover connection management, message routing, and offline fallback.");
        t4.setProject(platform);
        t4.setUser(bob);
        t4.setStatus(TaskStatus.IN_PROGRESS);
        t4.setPriority(Priority.MEDIUM);
        t4.setTags(Set.of(featureTag, spikeTag));
        t4.setStartDate(today.minusDays(3));
        t4.setDueDate(today.plusDays(4));
        t4.setCreatedAt(now.minusDays(3));
        addChecklist(
                t4,
                List.of(
                        checklist("Research STOMP protocol", 0, true),
                        checklist("Design message schema", 1, true),
                        checklist("Plan connection lifecycle", 2, false),
                        checklist("Document offline strategy", 3, false)));
        t4 = taskRepository.save(t4);

        Comment c4a =
                comment(
                        t4,
                        bob,
                        "I've been reading about STOMP vs raw WebSocket. STOMP gives us pub/sub out of the box with Spring's built-in support.",
                        now.minusDays(3).plusHours(4));
        Comment c4b =
                comment(
                        t4,
                        alice,
                        "Agree on STOMP. @[Eva Martinez](userId:"
                                + eva.getId()
                                + ") has experience with this — might be worth syncing.",
                        now.minusDays(2).plusHours(2));
        Comment c4c =
                comment(
                        t4,
                        eva,
                        "Happy to help! One thing to watch out for: make sure you handle reconnection gracefully. Users on flaky connections will drop and reconnect frequently.",
                        now.minusDays(2).plusHours(5));
        Comment c4d =
                comment(
                        t4,
                        bob,
                        "Great point @[Eva Martinez](userId:"
                                + eva.getId()
                                + "). I'll add a reconnection strategy with exponential backoff to the design.",
                        now.minusDays(1).plusHours(3));
        commentRepository.saveAll(List.of(c4a, c4b, c4c, c4d));

        // ── Task 5: Unassigned task (open for anyone) ─────────────────────
        Task t5 =
                new Task(
                        "Update team coding standards",
                        "Review and update the coding standards document. Add sections for new patterns we've adopted: record types, sealed classes, and pattern matching.");
        t5.setProject(platform);
        t5.setUser(null); // unassigned
        t5.setStatus(TaskStatus.OPEN);
        t5.setPriority(Priority.LOW);
        t5.setTags(Set.of(documentationTag));
        t5.setDueDate(today.plusDays(14));
        t5.setCreatedAt(now.minusDays(2));
        t5 = taskRepository.save(t5);

        Comment c5a =
                comment(
                        t5,
                        alice,
                        "I'd like to add a section on when to use records vs classes. Anyone want to take this?",
                        now.minusDays(2).plusHours(3));
        Comment c5b =
                comment(
                        t5,
                        bob,
                        "I can draft the records and sealed classes sections. @[Carol Williams](userId:"
                                + carol.getId()
                                + ") want to handle pattern matching?",
                        now.minusDays(1).plusHours(2));
        Comment c5c =
                comment(
                        t5,
                        carol,
                        "Sure! I'll write up the pattern matching section with some examples from our codebase.",
                        now.minusDays(1).plusHours(5));
        commentRepository.saveAll(List.of(c5a, c5b, c5c));

        // ── Task 6: Alice's task due tomorrow ─────────────────────────────
        Task t6 =
                new Task(
                        "Prepare sprint retrospective",
                        "Collect feedback from the team, prepare the retro board, and schedule the meeting room. Focus areas: deployment process and code review turnaround.");
        t6.setProject(product);
        t6.setUser(alice);
        t6.setStatus(TaskStatus.OPEN);
        t6.setPriority(Priority.MEDIUM);
        t6.setTags(Set.of(documentationTag));
        t6.setDueDate(today.plusDays(1));
        t6.setCreatedAt(now.minusDays(3));
        t6 = taskRepository.save(t6);

        Comment c6a =
                comment(
                        t6,
                        bob,
                        "Can we add 'improving test coverage' to the agenda? I think we're slipping on that.",
                        now.minusDays(1).plusHours(4));
        Comment c6b =
                comment(
                        t6,
                        alice,
                        "Good idea. I'll add it as a discussion point. @[David Brown](userId:"
                                + david.getId()
                                + ") @[Eva Martinez](userId:"
                                + eva.getId()
                                + ") — any other topics you want to cover?",
                        now.minusDays(1).plusHours(6));
        commentRepository.saveAll(List.of(c6a, c6b));

        // Alice: due tomorrow notification
        notificationRepository.save(
                new Notification(
                        alice,
                        null,
                        NotificationType.TASK_DUE_REMINDER,
                        "Due tomorrow: Prepare sprint retrospective",
                        "/tasks/" + t6.getId()));

        // ── Task 7: Bob's completed task ──────────────────────────────────
        Task t7 =
                new Task(
                        "Fix pagination bug on task list",
                        "Page size selector resets to default when navigating to page 2+. The URL param is lost during HTMX partial swap.");
        t7.setProject(product);
        t7.setUser(bob);
        t7.setStatus(TaskStatus.COMPLETED);
        t7.setPriority(Priority.MEDIUM);
        t7.setTags(Set.of(bugTag));
        t7.setStartDate(today.minusDays(4));
        t7.setDueDate(today.minusDays(1));
        t7.setCompletedAt(now.minusDays(2));
        t7.setCreatedAt(now.minusDays(4));
        t7 = taskRepository.save(t7);

        Comment c7a =
                comment(
                        t7,
                        bob,
                        "Found the issue — buildUrl() was dropping the size param on navigation. Fix is a one-liner.",
                        now.minusDays(3).plusHours(5));
        Comment c7b =
                comment(
                        t7,
                        alice,
                        "Nice catch! Can you also add a test case for this? We don't want it to regress.",
                        now.minusDays(3).plusHours(7));
        Comment c7c =
                comment(
                        t7,
                        bob,
                        "Done. Added test and verified manually. Closing this out.",
                        now.minusDays(2).plusHours(1));
        commentRepository.saveAll(List.of(c7a, c7b, c7c));

        // ── Task 8: Alice's task, open, due next week ─────────────────────
        Task t8 =
                new Task(
                        "Plan Q2 engineering roadmap",
                        "Draft the Q2 roadmap covering platform reliability, developer experience, and new feature initiatives. Include capacity estimates and dependencies.");
        t8.setProject(platform);
        t8.setUser(alice);
        t8.setStatus(TaskStatus.OPEN);
        t8.setPriority(Priority.HIGH);
        t8.setTags(Set.of(documentationTag));
        t8.setDueDate(today.plusDays(7));
        t8.setCreatedAt(now.minusDays(1));
        addChecklist(
                t8,
                List.of(
                        checklist("Gather team input on priorities", 0, false),
                        checklist("Draft reliability initiatives", 1, false),
                        checklist("Draft DX improvements", 2, false),
                        checklist("Estimate capacity per initiative", 3, false),
                        checklist("Review with engineering leads", 4, false),
                        checklist("Present to leadership", 5, false)));
        t8 = taskRepository.save(t8);

        Comment c8a =
                comment(
                        t8,
                        alice,
                        "Starting to collect input. @[Bob Smith](userId:"
                                + bob.getId()
                                + ") @[Carol Williams](userId:"
                                + carol.getId()
                                + ") @[David Brown](userId:"
                                + david.getId()
                                + ") — please share your top 3 priorities for Q2 by Friday.",
                        now.minusDays(1).plusHours(2));
        Comment c8b =
                comment(
                        t8,
                        bob,
                        "My top 3: (1) CI/CD improvements, (2) WebSocket notifications, (3) better error handling across the API.",
                        now.minusHours(20));
        Comment c8c =
                comment(
                        t8,
                        carol,
                        "From my side: (1) test infrastructure, (2) documentation overhaul, (3) accessibility improvements.",
                        now.minusHours(16));
        commentRepository.saveAll(List.of(c8a, c8b, c8c));

        // ── Additional notifications for both users ───────────────────────
        // Bob: comment notifications from Alice on his tasks
        notificationRepository.saveAll(
                List.of(
                        notification(
                                bob,
                                alice,
                                NotificationType.COMMENT_ADDED,
                                "Alice Johnson commented on: Set up CI/CD pipeline for staging",
                                "/tasks/" + t1.getId(),
                                now.minusDays(3).plusHours(2)),
                        notification(
                                bob,
                                alice,
                                NotificationType.COMMENT_ADDED,
                                "Alice Johnson commented on: Design WebSocket notification system",
                                "/tasks/" + t4.getId(),
                                now.minusDays(2).plusHours(2)),
                        notification(
                                bob,
                                eva,
                                NotificationType.COMMENT_ADDED,
                                "Eva Martinez commented on: Design WebSocket notification system",
                                "/tasks/" + t4.getId(),
                                now.minusDays(2).plusHours(5)),
                        notification(
                                bob,
                                alice,
                                NotificationType.COMMENT_MENTIONED,
                                "Alice Johnson mentioned you in: Plan Q2 engineering roadmap",
                                "/tasks/" + t8.getId(),
                                now.minusDays(1).plusHours(2))));
        // Alice: comment notifications from Bob on her tasks
        notificationRepository.saveAll(
                List.of(
                        notification(
                                alice,
                                bob,
                                NotificationType.COMMENT_ADDED,
                                "Bob Smith commented on: Write API rate limiting design doc",
                                "/tasks/" + t2.getId(),
                                now.minusDays(10).plusHours(2)),
                        notification(
                                alice,
                                carol,
                                NotificationType.COMMENT_ADDED,
                                "Carol Williams commented on: Write API rate limiting design doc",
                                "/tasks/" + t2.getId(),
                                now.minusDays(7).plusHours(3)),
                        notification(
                                alice,
                                bob,
                                NotificationType.COMMENT_ADDED,
                                "Bob Smith commented on: Prepare sprint retrospective",
                                "/tasks/" + t6.getId(),
                                now.minusDays(1).plusHours(4)),
                        notification(
                                alice,
                                bob,
                                NotificationType.COMMENT_ADDED,
                                "Bob Smith commented on: Plan Q2 engineering roadmap",
                                "/tasks/" + t8.getId(),
                                now.minusHours(20)),
                        notification(
                                alice,
                                carol,
                                NotificationType.COMMENT_ADDED,
                                "Carol Williams commented on: Plan Q2 engineering roadmap",
                                "/tasks/" + t8.getId(),
                                now.minusHours(16)),
                        notification(
                                alice,
                                bob,
                                NotificationType.COMMENT_ADDED,
                                "Bob Smith commented on: Fix pagination bug on task list",
                                "/tasks/" + t7.getId(),
                                now.minusDays(3).plusHours(5)),
                        notification(
                                alice,
                                david,
                                NotificationType.COMMENT_ADDED,
                                "David Brown commented on: Migrate user auth to Spring Security 7",
                                "/tasks/" + t3.getId(),
                                now.minusDays(12).plusHours(6))));

        // ── Task dependencies ────────────────────────────────────────────
        // Platform: CI/CD pipeline must finish before WebSocket notification work
        t1.getBlocks().add(t4);
        // Platform: rate limiting design and WebSocket design both feed into Q2 roadmap
        t2.getBlocks().add(t8);
        t4.getBlocks().add(t8);
        // Product: pagination bug fix was blocking sprint retro (now resolved — t7 is completed)
        t7.getBlocks().add(t6);
        taskRepository.saveAll(List.of(t1, t2, t4, t7));

        // ── Audit logs for demo tasks ─────────────────────────────────────
        List<AuditLog> auditLogs = new ArrayList<>();

        // Task creation events
        auditLogs.add(
                auditLog(
                        AuditEvent.TASK_CREATED,
                        Task.class,
                        t1.getId(),
                        alice.getEmail(),
                        AuditDetails.toJson(t1.toAuditSnapshot()),
                        t1.getCreatedAt()));
        auditLogs.add(
                auditLog(
                        AuditEvent.TASK_CREATED,
                        Task.class,
                        t2.getId(),
                        bob.getEmail(),
                        AuditDetails.toJson(t2.toAuditSnapshot()),
                        t2.getCreatedAt()));
        auditLogs.add(
                auditLog(
                        AuditEvent.TASK_CREATED,
                        Task.class,
                        t3.getId(),
                        alice.getEmail(),
                        AuditDetails.toJson(t3.toAuditSnapshot()),
                        t3.getCreatedAt()));
        auditLogs.add(
                auditLog(
                        AuditEvent.TASK_CREATED,
                        Task.class,
                        t4.getId(),
                        bob.getEmail(),
                        AuditDetails.toJson(t4.toAuditSnapshot()),
                        t4.getCreatedAt()));
        auditLogs.add(
                auditLog(
                        AuditEvent.TASK_CREATED,
                        Task.class,
                        t5.getId(),
                        alice.getEmail(),
                        AuditDetails.toJson(t5.toAuditSnapshot()),
                        t5.getCreatedAt()));
        auditLogs.add(
                auditLog(
                        AuditEvent.TASK_CREATED,
                        Task.class,
                        t6.getId(),
                        alice.getEmail(),
                        AuditDetails.toJson(t6.toAuditSnapshot()),
                        t6.getCreatedAt()));
        auditLogs.add(
                auditLog(
                        AuditEvent.TASK_CREATED,
                        Task.class,
                        t7.getId(),
                        bob.getEmail(),
                        AuditDetails.toJson(t7.toAuditSnapshot()),
                        t7.getCreatedAt()));
        auditLogs.add(
                auditLog(
                        AuditEvent.TASK_CREATED,
                        Task.class,
                        t8.getId(),
                        alice.getEmail(),
                        AuditDetails.toJson(t8.toAuditSnapshot()),
                        t8.getCreatedAt()));

        // Task update: t3 completed (Spring Security migration)
        Map<String, Object> t3StatusChange = new LinkedHashMap<>();
        t3StatusChange.put("status", Map.of("old", "IN_PROGRESS", "new", "COMPLETED"));
        auditLogs.add(
                auditLog(
                        AuditEvent.TASK_UPDATED,
                        Task.class,
                        t3.getId(),
                        alice.getEmail(),
                        AuditDetails.toJson(t3StatusChange),
                        now.minusDays(6).plusHours(1)));

        // Task update: t7 completed (pagination bug)
        Map<String, Object> t7StatusChange = new LinkedHashMap<>();
        t7StatusChange.put("status", Map.of("old", "IN_PROGRESS", "new", "COMPLETED"));
        auditLogs.add(
                auditLog(
                        AuditEvent.TASK_UPDATED,
                        Task.class,
                        t7.getId(),
                        bob.getEmail(),
                        AuditDetails.toJson(t7StatusChange),
                        now.minusDays(2).plusHours(1)));

        // Task update: t1 checklist progress (Bob checked items)
        Map<String, Object> t1ChecklistUpdate = new LinkedHashMap<>();
        t1ChecklistUpdate.put(
                "checklistItems",
                Map.of(
                        "old",
                                List.of(
                                        "[ ] Configure build stage",
                                        "[ ] Add test stage with coverage",
                                        "[ ] Set up staging deploy",
                                        "[ ] Add Slack notifications",
                                        "[ ] Document pipeline in wiki"),
                        "new",
                                List.of(
                                        "[x] Configure build stage",
                                        "[x] Add test stage with coverage",
                                        "[ ] Set up staging deploy",
                                        "[ ] Add Slack notifications",
                                        "[ ] Document pipeline in wiki")));
        auditLogs.add(
                auditLog(
                        AuditEvent.TASK_UPDATED,
                        Task.class,
                        t1.getId(),
                        bob.getEmail(),
                        AuditDetails.toJson(t1ChecklistUpdate),
                        now.minusDays(4).plusHours(6)));

        // Task update: t2 checklist progress (Alice checked items)
        Map<String, Object> t2ChecklistUpdate = new LinkedHashMap<>();
        t2ChecklistUpdate.put(
                "checklistItems",
                Map.of(
                        "old",
                                List.of(
                                        "[ ] Research sliding window algorithms",
                                        "[ ] Draft architecture section",
                                        "[ ] Add Redis schema design",
                                        "[ ] Get review from team"),
                        "new",
                                List.of(
                                        "[x] Research sliding window algorithms",
                                        "[x] Draft architecture section",
                                        "[ ] Add Redis schema design",
                                        "[ ] Get review from team")));
        auditLogs.add(
                auditLog(
                        AuditEvent.TASK_UPDATED,
                        Task.class,
                        t2.getId(),
                        alice.getEmail(),
                        AuditDetails.toJson(t2ChecklistUpdate),
                        now.minusDays(7).plusHours(5)));

        // Task update: t4 checklist progress (Bob checked items)
        Map<String, Object> t4ChecklistUpdate = new LinkedHashMap<>();
        t4ChecklistUpdate.put(
                "checklistItems",
                Map.of(
                        "old",
                                List.of(
                                        "[ ] Research STOMP protocol",
                                        "[ ] Design message schema",
                                        "[ ] Plan connection lifecycle",
                                        "[ ] Document offline strategy"),
                        "new",
                                List.of(
                                        "[x] Research STOMP protocol",
                                        "[x] Design message schema",
                                        "[ ] Plan connection lifecycle",
                                        "[ ] Document offline strategy")));
        auditLogs.add(
                auditLog(
                        AuditEvent.TASK_UPDATED,
                        Task.class,
                        t4.getId(),
                        bob.getEmail(),
                        AuditDetails.toJson(t4ChecklistUpdate),
                        now.minusDays(1).plusHours(6)));

        auditLogRepository.saveAll(auditLogs);

        // ── Saved Views ──────────────────────────────────────────────────────
        var sortPriorityDesc = List.of(new SavedViewData.SortField("priorityOrder", "desc"));
        var sortDueDateAsc = List.of(new SavedViewData.SortField("dueDate", "asc"));
        var sortCreatedAtDesc = List.of(new SavedViewData.SortField("createdAt", "desc"));
        var sortUpdatedAtDesc = List.of(new SavedViewData.SortField("updatedAt", "desc"));

        savedViewRepository.saveAll(
                List.of(
                        savedView(
                                alice,
                                "My High Priority",
                                taskQuery(null, Priority.HIGH, alice.getId()),
                                "table",
                                sortPriorityDesc),
                        savedView(
                                alice,
                                "Overdue Tasks",
                                taskQuery(null, null, null, true),
                                "cards",
                                sortDueDateAsc),
                        savedView(
                                alice,
                                "Board View — In Progress",
                                taskQuery(TaskStatusFilter.IN_PROGRESS, null, null),
                                "board",
                                sortCreatedAtDesc),
                        savedView(
                                bob,
                                "My Open Tasks",
                                taskQuery(TaskStatusFilter.OPEN, null, bob.getId()),
                                "calendar",
                                sortDueDateAsc),
                        savedView(
                                bob,
                                "Critical & High Priority",
                                taskQuery(null, Priority.HIGH, null),
                                "cards",
                                sortPriorityDesc),
                        savedView(
                                bob,
                                "Recently Updated",
                                taskQuery(null, null, null),
                                "board",
                                sortUpdatedAtDesc)));
    }

    private Task task(
            String title,
            String description,
            Project project,
            User user,
            TaskStatus status,
            Priority priority,
            int createdDaysAgo) {
        Task t = new Task(title, description);
        t.setProject(project);
        t.setUser(user);
        t.setStatus(status);
        t.setPriority(priority);
        t.setCreatedAt(LocalDateTime.now().minusDays(createdDaysAgo));
        return t;
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

    private AuditLog auditLog(
            String action,
            Class<?> entityType,
            Long entityId,
            String principal,
            String details,
            LocalDateTime timestamp) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setEntityType(entityType.getSimpleName());
        log.setEntityId(entityId);
        log.setPrincipal(principal);
        log.setDetails(details);
        log.setTimestamp(timestamp.atZone(ZoneId.systemDefault()).toInstant());
        return log;
    }

    private Notification notification(
            User user,
            User actor,
            NotificationType type,
            String message,
            String link,
            LocalDateTime createdAt) {
        Notification n = new Notification(user, actor, type, message, link);
        n.setCreatedAt(createdAt);
        return n;
    }

    private TaskListQuery taskQuery(
            TaskStatusFilter statusFilter, Priority priority, Long selectedUserId) {
        return taskQuery(statusFilter, priority, selectedUserId, false);
    }

    private TaskListQuery taskQuery(
            TaskStatusFilter statusFilter,
            Priority priority,
            Long selectedUserId,
            boolean overdue) {
        TaskListQuery q = new TaskListQuery();
        if (statusFilter != null) q.setStatusFilter(statusFilter);
        q.setPriority(priority);
        q.setSelectedUserId(selectedUserId);
        q.setOverdue(overdue);
        return q;
    }

    private SavedView savedView(
            User user,
            String name,
            TaskListQuery query,
            String view,
            List<SavedViewData.SortField> sort) {
        SavedViewData data = SavedViewData.ofTask(query, view, sort);
        return new SavedView(user, name, data.toJson());
    }
}
