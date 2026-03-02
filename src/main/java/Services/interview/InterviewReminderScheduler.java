package Services.interview;

import Models.interview.Interview;
import Services.user.SMSService;
import Utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Interview Reminder Scheduler
 *
 * Runs in background and checks every 5 minutes for interviews
 * scheduled ~24 hours from now. Sends both Email and SMS reminders.
 *
 * DEBUG: All steps are logged to stdout so you can see exactly
 *        what is happening at each check cycle.
 *
 * TEST:  Call InterviewReminderScheduler.runTestNow() from anywhere
 *        to immediately send a reminder for every upcoming interview
 *        regardless of time window — useful during development.
 */
public class InterviewReminderScheduler {

    private static Timer schedulerTimer;

    private static final long CHECK_INTERVAL_MS = 5 * 60 * 1000L; // 5 minutes
    private static boolean isRunning = false;

    /** DEBUG MODE: When true, sends reminders for ANY upcoming interview (not just 20-26h window) */
    private static final boolean DEBUG_MODE = true;

    private static final DateTimeFormatter LOG_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /** Start the background scheduler (called once from Main.start()). */
    public static synchronized void start() {
        if (isRunning) {
            System.out.println("[Scheduler] Already running – skip double-start.");
            return;
        }
        isRunning = true;
        schedulerTimer = new Timer("InterviewReminderScheduler", true); // daemon thread

        // Run once immediately so we don't wait 5 minutes on first launch
        System.out.println("[Scheduler] Starting up at " + LocalDateTime.now().format(LOG_FMT));
        if (DEBUG_MODE) {
            System.out.println("[Scheduler] ⚠️  DEBUG MODE ENABLED - Reminders will be sent for ANY upcoming interview");
            System.out.println("[Scheduler]    (not just 20-26h before). Set DEBUG_MODE=false for production.");
        }
        checkAndSendReminders();

        // Then repeat every 5 minutes
        schedulerTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkAndSendReminders();
            }
        }, CHECK_INTERVAL_MS, CHECK_INTERVAL_MS);

        System.out.println("[Scheduler] Background scheduler active — checks every 5 minutes.");
    }

    /** Stop the scheduler gracefully (called from Main on window close). */
    public static synchronized void stop() {
        if (schedulerTimer != null) {
            schedulerTimer.cancel();
            schedulerTimer = null;
        }
        isRunning = false;
        System.out.println("[Scheduler] Stopped.");
    }

    // -------------------------------------------------------------------------
    // Core check loop
    // -------------------------------------------------------------------------

    private static void checkAndSendReminders() {
        LocalDateTime now = LocalDateTime.now();
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║ [Scheduler] CHECK CYCLE at " + now.format(LOG_FMT) + "");
        System.out.println("║ Mode: " + (DEBUG_MODE ? "DEBUG (any upcoming interview)" : "PRODUCTION (20-26h window)") + "");
        System.out.println("╚════════════════════════════════════════════════════════════╝");

        List<Interview> interviews;
        try {
            interviews = InterviewService.getAll();
        } catch (Exception e) {
            System.err.println("[Scheduler] ❌ ERROR: Could not load interviews from DB: " + e.getMessage());
            return;
        }

        System.out.println("[Scheduler] Total interviews in DB: " + interviews.size());

        int alreadySent = 0;
        int tooEarly = 0;
        int tooLate = 0;
        int sent = 0;

        for (Interview interview : interviews) {
            if (interview.getId() == null) continue;

            // Check if reminder was already sent (from database reminder_sent field)
            if (hasReminderBeenSent(interview.getId())) {
                alreadySent++;
                System.out.println("[Scheduler]    Interview #" + interview.getId()
                    + " at " + interview.getScheduledAt().format(LOG_FMT)
                    + " — already sent ✓ (skip)");
                continue;
            }

            ReminderStatus status = getReminderStatus(interview.getScheduledAt());

            switch (status) {
                case SEND_NOW:
                    System.out.println("[Scheduler] ➜ Interview #" + interview.getId()
                        + " scheduled at " + interview.getScheduledAt().format(LOG_FMT)
                        + " — SENDING REMINDER...");
                    boolean ok = sendReminderForInterview(interview);
                    if (ok) {
                        // Mark reminder as sent in database
                        markReminderAsSent(interview.getId());
                        sent++;
                    }
                    break;
                case TOO_EARLY:
                    tooEarly++;
                    System.out.println("[Scheduler]    Interview #" + interview.getId()
                        + " at " + interview.getScheduledAt().format(LOG_FMT));
                    break;
                case TOO_LATE:
                    tooLate++;
                    System.out.println("[Scheduler]    Interview #" + interview.getId()
                        + " at " + interview.getScheduledAt().format(LOG_FMT));
                    break;
            }
        }

        System.out.println("[Scheduler] ═════════════════════════════════════════════════");
        System.out.println("[Scheduler] SUMMARY: "
            + "✉️ " + sent + " sent | "
            + "↺ " + alreadySent + " already sent | "
            + "⏳ " + tooEarly + " too early | "
            + "⏸ " + tooLate + " too late");
        System.out.println("[Scheduler] ═════════════════════════════════════════════════\n");
    }

    // -------------------------------------------------------------------------
    // Time window logic
    // -------------------------------------------------------------------------

    /** Reminder window: between 20 h and 26 h before the interview. */
    private static final long WINDOW_LOWER_HOURS = 20;
    private static final long WINDOW_UPPER_HOURS = 26;

    private enum ReminderStatus { SEND_NOW, TOO_EARLY, TOO_LATE }

    private static ReminderStatus getReminderStatus(LocalDateTime interviewTime) {
        if (interviewTime == null) return ReminderStatus.TOO_LATE;

        LocalDateTime now = LocalDateTime.now();
        long minutesUntil = ChronoUnit.MINUTES.between(now, interviewTime);
        long hoursUntil = minutesUntil / 60;
        long daysUntil = hoursUntil / 24;

        // DEBUG MODE: Send reminders for ANY upcoming interview
        if (DEBUG_MODE) {
            if (minutesUntil < 0) {
                System.out.println("[Scheduler]       Time check: " + daysUntil + "d " + (Math.abs(hoursUntil) % 24) + "h ago → TOO_LATE (past)");
                return ReminderStatus.TOO_LATE; // Interview already passed
            }
            if (minutesUntil > (30 * 24 * 60)) {
                System.out.println("[Scheduler]       Time check: " + daysUntil + "d " + (hoursUntil % 24) + "h away → TOO_EARLY (>30 days)");
                return ReminderStatus.TOO_EARLY; // More than 30 days away
            }
            System.out.println("[Scheduler]       Time check: " + daysUntil + "d " + (hoursUntil % 24) + "h away → SEND_NOW ✅");
            return ReminderStatus.SEND_NOW;
        }

        // PRODUCTION MODE: Use strict 20-26 hour window
        long lower = WINDOW_LOWER_HOURS * 60; // 1200 minutes
        long upper = WINDOW_UPPER_HOURS * 60; // 1560 minutes

        if (minutesUntil < lower) {
            System.out.println("[Scheduler]       Time check: " + hoursUntil + "h away → TOO_LATE (<20h)");
            return ReminderStatus.TOO_LATE;
        }
        if (minutesUntil > upper) {
            System.out.println("[Scheduler]       Time check: " + hoursUntil + "h away → TOO_EARLY (>26h)");
            return ReminderStatus.TOO_EARLY;
        }

        System.out.println("[Scheduler]       Time check: " + hoursUntil + "h away → SEND_NOW (within 20-26h) ✅");
        return ReminderStatus.SEND_NOW;
    }

    // -------------------------------------------------------------------------
    // Send reminder (email + SMS)
    // -------------------------------------------------------------------------

    /**
     * Looks up the candidate's email + phone from users table, then sends email and SMS.
     * @return true if at least one notification was dispatched
     */
    private static boolean sendReminderForInterview(Interview interview) {
        CandidateContact contact = getCandidateContact(interview.getApplicationId());

        if (contact == null) {
            System.err.println("[Scheduler]    No contact found for application #"
                + interview.getApplicationId() + " — skipping.");
            return false;
        }

        System.out.println("[Scheduler]    Candidate : " + contact.fullName());
        System.out.println("[Scheduler]    Email     : " + contact.email());
        System.out.println("[Scheduler]    Phone     : " + contact.phone() + " (from users.phone)");
        boolean dispatched = false;

        // --- Email to CANDIDATE ---
        if (contact.email() != null && !contact.email().isBlank()) {
            System.out.println("[Scheduler]    Sending EMAIL (candidate) to " + contact.email() + " ...");
            boolean emailOk = InterviewEmailService.sendReminder(
                    interview, contact.email(), contact.fullName());
            if (emailOk) {
                System.out.println("[Scheduler]    EMAIL (candidate) dispatched OK.");
                dispatched = true;
            } else {
                System.err.println("[Scheduler]    EMAIL (candidate) failed (Brevo returned error).");
            }
        } else {
            System.out.println("[Scheduler]    No candidate email address — skipping candidate email.");
        }

        // --- Email to RECRUITER ---
        RecruiterContact recruiter = getRecruiterContact(interview.getRecruiterId());
        if (recruiter != null && recruiter.email() != null && !recruiter.email().isBlank()) {
            System.out.println("[Scheduler]    Sending EMAIL (recruiter) to " + recruiter.email() + " ...");
            boolean recOk = InterviewEmailService.sendReminderToRecruiter(
                    interview, recruiter.email(), recruiter.fullName(), contact.fullName());
            if (recOk) {
                System.out.println("[Scheduler]    EMAIL (recruiter) dispatched OK.");
                dispatched = true;
            } else {
                System.err.println("[Scheduler]    EMAIL (recruiter) failed (Brevo returned error).");
            }
        }

        // --- SMS to CANDIDATE (uses users.phone) ---
        String rawPhone = contact.phone();
        String normalizedPhone = rawPhone != null ? SMSService.normalizePhone(rawPhone) : "";

        if (!normalizedPhone.isBlank() && SMSService.isValidPhoneNumber(normalizedPhone)) {
            try {
                System.out.println("[Scheduler]    Sending SMS to " + normalizedPhone + " ...");
                SMSService.sendInterviewReminder(interview, normalizedPhone, contact.fullName());
                System.out.println("[Scheduler]    SMS dispatched OK.");
                dispatched = true;
            } catch (Exception e) {
                System.err.println("[Scheduler]    SMS FAILED: " + e.getMessage());
            }
        } else {
            System.out.println("[Scheduler]    Phone '" + rawPhone
                + "' -> normalized '" + normalizedPhone
                + "' -> invalid or empty, skipping SMS.");
        }

        return dispatched;
    }

    // -------------------------------------------------------------------------
    // Database contact lookup
    // -------------------------------------------------------------------------

    private record CandidateContact(String email, String phone, String firstName, String lastName) {
        String fullName() {
            String fn = (firstName != null ? firstName.trim() : "");
            String ln = (lastName  != null ? lastName.trim()  : "");
            String full = (fn + " " + ln).trim();
            return full.isEmpty() ? "Candidat" : full;
        }
    }

    /**
     * Fetch email + phone for the candidate linked to a job_application row.
     *
     * Schema path:
     *   interview.application_id → job_application.candidate_id → users (id, email, phone)
     *
     * We always use users.phone (the phone the user REGISTERED with),
     * NOT job_application.phone (which is a CV-submission field and may be empty/wrong).
     */
    private static CandidateContact getCandidateContact(Long applicationId) {
        if (applicationId == null) {
            System.err.println("[Scheduler]    applicationId is null — cannot look up contact.");
            return null;
        }

        // JOIN: job_application → users via candidate_id
        // Use users.phone (registered phone) NOT ja.phone (CV phone)
        String sql = "SELECT u.email, u.phone, u.first_name, u.last_name " +
                     "FROM job_application ja " +
                     "JOIN users u ON ja.candidate_id = u.id " +
                     "WHERE ja.id = ?";

        System.out.println("[Scheduler]    Looking up contact for application #" + applicationId);
        System.out.println("[Scheduler]    SQL: " + sql.replace("?", applicationId.toString()));

        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, applicationId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String email     = rs.getString("email");
                        String phone     = rs.getString("phone");   // users.phone = real registered number
                        String firstName = rs.getString("first_name");
                        String lastName  = rs.getString("last_name");
                        System.out.println("[Scheduler]    Found: email=" + email
                            + ", phone=" + phone
                            + ", name=" + firstName + " " + lastName);
                        return new CandidateContact(email, phone, firstName, lastName);
                    } else {
                        System.err.println("[Scheduler]    No users row found for application id=" + applicationId);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[Scheduler]    DB error fetching candidate contact: " + e.getMessage());
        }
        return null;
    }

    private record RecruiterContact(String email, String firstName, String lastName) {
        String fullName() {
            String fn = (firstName != null ? firstName.trim() : "");
            String ln = (lastName  != null ? lastName.trim()  : "");
            String full = (fn + " " + ln).trim();
            return full.isEmpty() ? "Recruteur" : full;
        }
    }

    /** Fetch recruiter email from users via recruiter.user_id. */
    private static RecruiterContact getRecruiterContact(Long recruiterId) {
        if (recruiterId == null) return null;
        String sql = "SELECT u.email, u.first_name, u.last_name " +
                     "FROM recruiter r JOIN users u ON r.user_id = u.id " +
                     "WHERE r.id = ?";
        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, recruiterId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next())
                        return new RecruiterContact(rs.getString("email"),
                                rs.getString("first_name"), rs.getString("last_name"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[Scheduler]    DB error fetching recruiter contact: " + e.getMessage());
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Database reminder tracking (persistent across app restarts)
    // -------------------------------------------------------------------------

    /**
     * Check if a reminder has already been sent for this interview.
     * Checks the interview table's reminder_sent column.
     */
    private static boolean hasReminderBeenSent(Long interviewId) {
        if (interviewId == null) return false;
        String sql = "SELECT reminder_sent FROM interview WHERE id = ?";
        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, interviewId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getBoolean("reminder_sent");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[Scheduler] DB error checking reminder status for interview #" + interviewId + ": " + e.getMessage());
        }
        return false;
    }

    /**
     * Mark a reminder as sent in the database.
     * Updates the interview table's reminder_sent column to true.
     */
    private static void markReminderAsSent(Long interviewId) {
        if (interviewId == null) return;
        String sql = "UPDATE interview SET reminder_sent = true WHERE id = ?";
        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, interviewId);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    System.out.println("[Scheduler]    Marked interview #" + interviewId + " reminder as sent in DB.");
                }
            }
        } catch (SQLException e) {
            System.err.println("[Scheduler] DB error marking reminder as sent for interview #" + interviewId + ": " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // TEST utilities
    // -------------------------------------------------------------------------

    /**
     * TEST METHOD — call this to immediately send reminders for ALL upcoming
     * interviews regardless of the 24-hour window. Useful during development.
     *
     * Usage (from any controller or the IDE's evaluate expression):
     *   InterviewReminderScheduler.runTestNow();
     */
    public static void runTestNow() {
        System.out.println("\n[Scheduler-TEST] ===== FORCED TEST RUN =====");
        System.out.println("[Scheduler-TEST] Ignoring time window — sending for all upcoming interviews.");

        List<Interview> interviews;
        try {
            interviews = InterviewService.getAll();
        } catch (Exception e) {
            System.err.println("[Scheduler-TEST] Cannot load interviews: " + e.getMessage());
            return;
        }

        System.out.println("[Scheduler-TEST] Found " + interviews.size() + " interviews in DB.");
        int count = 0;

        for (Interview interview : interviews) {
            if (interview.getId() == null) continue;
            if (interview.getScheduledAt() == null) continue;
            // Only skip interviews already in the past (more than 1 hour ago)
            if (interview.getScheduledAt().isBefore(LocalDateTime.now().minusHours(1))) {
                System.out.println("[Scheduler-TEST]   Skipping past interview #" + interview.getId());
                continue;
            }
            System.out.println("[Scheduler-TEST]   Processing interview #" + interview.getId()
                + " at " + interview.getScheduledAt().format(LOG_FMT));
            sendReminderForInterview(interview);
            count++;
        }

        System.out.println("[Scheduler-TEST] Done. Processed " + count + " interview(s).");
        System.out.println("[Scheduler-TEST] ===========================\n");
    }

    /**
     * TEST METHOD — send a reminder for one specific interview ID.
     *
     * Usage:
     *   InterviewReminderScheduler.testForInterview(42L);
     */
    public static void testForInterview(Long interviewId) {
        System.out.println("\n[Scheduler-TEST] Testing reminder for interview #" + interviewId);
        try {
            List<Interview> all = InterviewService.getAll();
            Interview target = all.stream()
                .filter(i -> interviewId.equals(i.getId()))
                .findFirst().orElse(null);

            if (target == null) {
                System.err.println("[Scheduler-TEST] Interview #" + interviewId + " not found in DB.");
                return;
            }
            System.out.println("[Scheduler-TEST] Interview found: scheduled at "
                + target.getScheduledAt().format(LOG_FMT));
            sendReminderForInterview(target);
        } catch (Exception e) {
            System.err.println("[Scheduler-TEST] Error: " + e.getMessage());
        }
    }

    /**
     * TEST METHOD — print the status of all interviews to the console
     * so you can see which ones are in / out of the reminder window.
     */
    public static void printDiagnostics() {
        System.out.println("\n[Scheduler-DIAG] ===== DIAGNOSTICS =====");
        System.out.println("[Scheduler-DIAG] Current time : " + LocalDateTime.now().format(LOG_FMT));
        System.out.println("[Scheduler-DIAG] Reminder window: " + WINDOW_LOWER_HOURS
            + "h – " + WINDOW_UPPER_HOURS + "h before interview");
        System.out.println("[Scheduler-DIAG] Scheduler running: " + isRunning);
        System.out.println();

        try {
            List<Interview> interviews = InterviewService.getAll();
            System.out.println("[Scheduler-DIAG] Interviews in DB: " + interviews.size());
            System.out.println();

            for (Interview i : interviews) {
                if (i.getId() == null || i.getScheduledAt() == null) continue;
                long minsUntil = ChronoUnit.MINUTES.between(LocalDateTime.now(), i.getScheduledAt());
                ReminderStatus status = getReminderStatus(i.getScheduledAt());
                boolean sent = hasReminderBeenSent(i.getId());

                System.out.printf("[Scheduler-DIAG]   #%-4d  %-20s  %+7d min  status=%-10s  reminderSent=%s%n",
                    i.getId(),
                    i.getScheduledAt().format(LOG_FMT),
                    minsUntil,
                    status,
                    sent ? "YES" : "no");
            }
        } catch (Exception e) {
            System.err.println("[Scheduler-DIAG] Cannot load interviews: " + e.getMessage());
        }

        System.out.println("[Scheduler-DIAG] ========================\n");
    }

    // -------------------------------------------------------------------------
    // Status accessors
    // -------------------------------------------------------------------------

    public static boolean isRunning() { return isRunning; }

    /** Force-reset a reminder so it can be re-sent (useful during testing). */
    public static void resetReminder(Long interviewId) {
        String sql = "UPDATE interview SET reminder_sent = false WHERE id = ?";
        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, interviewId);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    System.out.println("[Scheduler] Reminder reset for interview #" + interviewId);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Scheduler] DB error resetting reminder: " + e.getMessage());
        }
    }

    /** Reset ALL sent reminders so they will be re-sent. */
    public static void resetAllReminders() {
        String sql = "UPDATE interview SET reminder_sent = false";
        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int rows = ps.executeUpdate();
                System.out.println("[Scheduler] All reminders reset (" + rows + " interviews).");
            }
        } catch (SQLException e) {
            System.err.println("[Scheduler] DB error resetting all reminders: " + e.getMessage());
        }
    }
}




