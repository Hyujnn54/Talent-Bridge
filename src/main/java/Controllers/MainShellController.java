package Controllers;

import Controllers.interview.CalendarViewController;
import Models.user.Admin;
import Models.user.Candidate;
import Models.user.Recruiter;
import Models.user.User;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import Utils.Session;

import java.io.IOException;

public class MainShellController {

    private static MainShellController instance;
    public static MainShellController getInstance() { return instance; }

    // Navigation buttons
    @FXML private Button btnInterviews;
    @FXML private Button btnApplications;
    @FXML private Button btnJobOffers;
    @FXML private Button btnEvents;
    @FXML private Button btnPastEvents;
    @FXML private Button btnCalendar;
    @FXML private Button btnStatistics;
    @FXML private Button btnDashboard;
    @FXML private Button btnUserDashboard;
    @FXML private Button btnAdminStats;
    @FXML private Button btnAdminApplications;
    @FXML private Button btnFullscreenToggle;

    // Top-bar buttons
    @FXML private Button btnDisconnect;
    @FXML private Button btnUserProfile;
    @FXML private Button btnNotifications;
    @FXML private Button btnThemeToggle;

    @FXML private Label lblUserName;
    @FXML private Label lblUserRole;
    @FXML private Label lblSidebarUserName;
    @FXML private Label lblSidebarUserRole;

    @FXML private StackPane contentArea;
    @FXML private BorderPane rootPane;

    private String activePage = "/views/application/Applications.fxml";
    private boolean darkMode  = false;

    @FXML
    public void initialize() {
        instance = this;
        User u = Session.getCurrentUser();
        String name = (u != null && u.getFirstName() != null && !u.getFirstName().isBlank())
                ? u.getFirstName() + (u.getLastName() != null ? " " + u.getLastName() : "")
                : (u != null ? u.getEmail() : "Utilisateur");
        String role = getRoleLabel(u);

        if (lblUserName != null)        lblUserName.setText(name);
        if (lblUserRole != null)        lblUserRole.setText(role);
        if (lblSidebarUserName != null) lblSidebarUserName.setText(name);
        if (lblSidebarUserRole != null) lblSidebarUserRole.setText(role);

        applyRoleToShell();

        // Staggered sidebar animation
        Button[] navBtns = {btnUserDashboard, btnApplications, btnInterviews, btnJobOffers,
                btnEvents, btnPastEvents, btnCalendar, btnStatistics, btnDashboard,
                btnAdminStats, btnAdminApplications, btnFullscreenToggle};
        for (int i = 0; i < navBtns.length; i++) {
            Button btn = navBtns[i];
            if (btn == null || !btn.isManaged()) continue;
            btn.setOpacity(0);
            btn.setTranslateX(-12);
            final int delay = i * 45;
            Timeline tl = new Timeline(
                new KeyFrame(Duration.millis(delay)),
                new KeyFrame(Duration.millis(delay + 280),
                    new KeyValue(btn.opacityProperty(), 1.0),
                    new KeyValue(btn.translateXProperty(), 0))
            );
            tl.play();
        }

        if (u instanceof Admin) {
            handleDashboardNav();
        } else {
            handleUserDashboardNav();
        }
    }

    private String getRoleLabel(User u) {
        if (u instanceof Admin)     return "Admin";
        if (u instanceof Recruiter) return "Recruteur";
        if (u instanceof Candidate) return "Candidat";
        return "Utilisateur";
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    @FXML private void handleApplicationsNav() {
        activePage = "/views/application/Applications.fxml";
        loadContentView(activePage);
        highlightActiveButton(btnApplications);
    }

    @FXML private void handleUserDashboardNav() {
        User u = Session.getCurrentUser();
        if (u instanceof Recruiter) activePage = "/views/user/RecruiterDashboard.fxml";
        else                        activePage = "/views/user/CandidateDashboard.fxml";
        loadContentView(activePage);
        highlightActiveButton(btnUserDashboard);
    }

    @FXML private void handleInterviewsNav() {
        activePage = "/views/interview/InterviewManagement.fxml";
        loadContentView(activePage);
        highlightActiveButton(btnInterviews);
    }

    @FXML private void handleJobOffersNav() {
        User u = Session.getCurrentUser();
        if (u instanceof Admin)          activePage = "/views/joboffers/JobOffersAdmin.fxml";
        else if (u instanceof Recruiter) activePage = "/views/joboffers/JobOffers.fxml";
        else                             activePage = "/views/joboffers/JobOffersBrowse.fxml";
        loadContentView(activePage);
        highlightActiveButton(btnJobOffers);
    }

    @FXML private void handleEventsNav() {
        User u = Session.getCurrentUser();
        if (u instanceof Recruiter) activePage = "/views/events/RecruiterEvents.fxml";
        else                        activePage = "/views/events/Events.fxml";
        loadContentView(activePage);
        highlightActiveButton(btnEvents);
    }

    @FXML private void handlePastEventsNav() {
        activePage = "/views/events/PastEvents.fxml";
        loadContentView(activePage);
        highlightActiveButton(btnPastEvents);
    }

    @FXML private void handleCalendarNav() {
        CalendarViewController.show();
    }

    @FXML private void handleStatistics() {
        activePage = "/views/joboffers/AnalyticsDashboard.fxml";
        loadContentView(activePage);
        highlightActiveButton(btnStatistics);
    }

    @FXML private void handleDashboardNav() {
        activePage = "/views/user/AdminDashboard.fxml";
        loadContentView(activePage);
        highlightActiveButton(btnDashboard);
    }

    @FXML private void handleAdminStatsNav() {
        activePage = "/views/application/AdminApplicationStatistics.fxml";
        loadContentView(activePage);
        highlightActiveButton(btnAdminStats);
    }

    @FXML private void handleAdminApplicationsNav() {
        activePage = "/views/application/AdminApplications.fxml";
        loadContentView(activePage);
        highlightActiveButton(btnAdminApplications);
    }

    // ─── Top bar ──────────────────────────────────────────────────────────────

    @FXML private void handleDisconnect() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Déconnexion");
        confirmAlert.setHeaderText("Êtes-vous sûr de vouloir vous déconnecter ?");
        confirmAlert.setContentText("Vous serez redirigé vers la page de connexion.");
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    Session.clear();
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/user/Login.fxml"));
                    contentArea.getScene().setRoot(loader.load());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML private void handleUserProfile() {
        loadContentView("/views/user/Profile.fxml");
        highlightActiveButton(null);
    }

    @FXML private void handleNotifications() {
        boolean schedulerRunning = Services.interview.InterviewReminderScheduler.isRunning();

        // Count reminders sent from database
        int remindersSent = 0;
        try {
            java.sql.Connection conn = Utils.MyDatabase.getInstance().getConnection();
            java.sql.Statement stmt = conn.createStatement();
            java.sql.ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM interview WHERE reminder_sent = TRUE");
            if (rs.next()) remindersSent = rs.getInt("cnt");
            rs.close();
            stmt.close();
        } catch (Exception e) {
            System.err.println("Error counting sent reminders: " + e.getMessage());
        }

        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Notifications Système");
        dialog.setHeaderText(null);

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(14);
        content.setPadding(new javafx.geometry.Insets(22));
        content.setPrefWidth(360);
        content.setStyle("-fx-background-color: white;");

        javafx.scene.control.Label title = new javafx.scene.control.Label("Statut du Système");
        title.setStyle("-fx-font-size:17px; -fx-font-weight:700; -fx-text-fill:#2c3e50;");
        javafx.scene.control.Separator sep = new javafx.scene.control.Separator();

        // Scheduler
        javafx.scene.layout.HBox schedulerRow = new javafx.scene.layout.HBox(12);
        schedulerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        schedulerRow.setStyle("-fx-background-color:" + (schedulerRunning ? "#D4EDDA" : "#F8D7DA") + "; -fx-background-radius:10; -fx-padding:12 16;");
        javafx.scene.control.Label schedulerIcon = new javafx.scene.control.Label(schedulerRunning ? "✅" : "⛔");
        schedulerIcon.setStyle("-fx-font-size:18px;");
        javafx.scene.layout.VBox schedulerText = new javafx.scene.layout.VBox(2);
        javafx.scene.control.Label schedulerLabel = new javafx.scene.control.Label("Planificateur: " + (schedulerRunning ? "ACTIF" : "INACTIF"));
        schedulerLabel.setStyle("-fx-font-weight:700; -fx-font-size:13px; -fx-text-fill:" + (schedulerRunning ? "#155724" : "#721C24") + ";");
        javafx.scene.control.Label reminderLabel = new javafx.scene.control.Label(remindersSent + " rappel(s) envoyé(s)");
        reminderLabel.setStyle("-fx-font-size:11px; -fx-text-fill:" + (schedulerRunning ? "#155724" : "#721C24") + ";");
        schedulerText.getChildren().addAll(schedulerLabel, reminderLabel);
        schedulerRow.getChildren().addAll(schedulerIcon, schedulerText);

        // DB
        boolean dbOk;
        try { dbOk = Utils.MyDatabase.getInstance().getConnection() != null; } catch (Exception e) { dbOk = false; }
        javafx.scene.layout.HBox dbRow = new javafx.scene.layout.HBox(12);
        dbRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        dbRow.setStyle("-fx-background-color:" + (dbOk ? "#D4EDDA" : "#F8D7DA") + "; -fx-background-radius:10; -fx-padding:12 16;");
        javafx.scene.control.Label dbIcon = new javafx.scene.control.Label(dbOk ? "🗄" : "⚠");
        dbIcon.setStyle("-fx-font-size:18px;");
        javafx.scene.control.Label dbLabel = new javafx.scene.control.Label("Base de données: " + (dbOk ? "CONNECTÉE" : "DÉCONNECTÉE"));
        dbLabel.setStyle("-fx-font-weight:700; -fx-font-size:13px; -fx-text-fill:" + (dbOk ? "#155724" : "#721C24") + ";");
        dbRow.getChildren().addAll(dbIcon, dbLabel);

        // User
        User u = Session.getCurrentUser();
        String uName = (u != null && u.getFirstName() != null) ? u.getFirstName() + " " + u.getLastName() : "Utilisateur";
        javafx.scene.layout.HBox userRow = new javafx.scene.layout.HBox(12);
        userRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        userRow.setStyle("-fx-background-color:#DCEEFB; -fx-background-radius:10; -fx-padding:12 16;");
        javafx.scene.control.Label userIcon = new javafx.scene.control.Label("👤");
        userIcon.setStyle("-fx-font-size:18px;");
        javafx.scene.layout.VBox userText = new javafx.scene.layout.VBox(2);
        javafx.scene.control.Label userLabel = new javafx.scene.control.Label(uName);
        userLabel.setStyle("-fx-font-weight:700; -fx-font-size:13px; -fx-text-fill:#1565C0;");
        javafx.scene.control.Label roleLabel2 = new javafx.scene.control.Label("Rôle: " + getRoleLabel(u));
        roleLabel2.setStyle("-fx-font-size:11px; -fx-text-fill:#1565C0;");
        userText.getChildren().addAll(userLabel, roleLabel2);
        userRow.getChildren().addAll(userIcon, userText);

        // Test buttons
        javafx.scene.control.Separator sepTest = new javafx.scene.control.Separator();
        javafx.scene.control.Label testTitle = new javafx.scene.control.Label("Tests - Emails et SMS");
        testTitle.setStyle("-fx-font-size:13px; -fx-font-weight:700; -fx-text-fill:#2c3e50;");

        javafx.scene.layout.HBox testButtonsBox = new javafx.scene.layout.HBox(10);
        testButtonsBox.setAlignment(javafx.geometry.Pos.CENTER);

        javafx.scene.control.Button btnTestNow = new javafx.scene.control.Button("🚀 Tester Maintenant");
        btnTestNow.setStyle("-fx-padding: 8 16; -fx-font-size: 11px; -fx-background-color: #0066CC; -fx-text-fill: white; -fx-cursor: hand;");
        btnTestNow.setOnAction(e -> {
            try {
                System.out.println("\n[TEST] Démarrage du test de rappels immédiat...");
                Services.interview.InterviewReminderScheduler.runTestNow();
                System.out.println("[TEST] Test de rappels terminé - vérifiez la console pour les détails\n");
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                alert.setTitle("Test Lancé");
                alert.setHeaderText(null);
                alert.setContentText("Test de rappels lancé!\n\nVérifiez:\n✅ Console pour les logs\n✅ Email pour les messages\n✅ Base de données pour reminder_sent");
                alert.showAndWait();
            } catch (Exception ex) {
                System.err.println("[TEST ERROR] " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        javafx.scene.control.Button btnPrintDiag = new javafx.scene.control.Button("📊 Diagnostics");
        btnPrintDiag.setStyle("-fx-padding: 8 16; -fx-font-size: 11px; -fx-background-color: #28A745; -fx-text-fill: white; -fx-cursor: hand;");
        btnPrintDiag.setOnAction(e -> {
            try {
                System.out.println("\n[DIAGNOSTIC] Affichage des diagnostics...");
                Services.interview.InterviewReminderScheduler.printDiagnostics();
                System.out.println("[DIAGNOSTIC] Affichage terminé\n");
            } catch (Exception ex) {
                System.err.println("[DIAGNOSTIC ERROR] " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        javafx.scene.control.Button btnResetAll = new javafx.scene.control.Button("🔄 Réinitialiser");
        btnResetAll.setStyle("-fx-padding: 8 16; -fx-font-size: 11px; -fx-background-color: #FFC107; -fx-text-fill: black; -fx-cursor: hand;");
        btnResetAll.setOnAction(e -> {
            try {
                System.out.println("\n[RESET] Réinitialisation de tous les rappels...");
                Services.interview.InterviewReminderScheduler.resetAllReminders();
                System.out.println("[RESET] Réinitialisation terminée\n");
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                alert.setTitle("Réinitialisation");
                alert.setHeaderText(null);
                alert.setContentText("✅ Tous les rappels ont été réinitialisés!\n\nVous pouvez maintenant relancer le test avec 'Tester Maintenant'");
                alert.showAndWait();
            } catch (Exception ex) {
                System.err.println("[RESET ERROR] " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        testButtonsBox.getChildren().addAll(btnTestNow, btnPrintDiag, btnResetAll);

        content.getChildren().addAll(title, sep, schedulerRow, dbRow, userRow, sepTest, testTitle, testButtonsBox);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        dialog.getDialogPane().setStyle("-fx-background-color: white;");
        dialog.showAndWait();
    }

    @FXML private void handleThemeToggle() {
        if (rootPane == null) return;
        darkMode = !darkMode;
        if (darkMode) {
            if (!rootPane.getStyleClass().contains("dark")) rootPane.getStyleClass().add("dark");
            rootPane.setStyle("-fx-background-color: #0F1923;");
            if (contentArea != null) contentArea.setStyle("-fx-background-color:#0F1923; -fx-padding:24;");
            if (btnThemeToggle != null) {
                btnThemeToggle.setText("☀");
                btnThemeToggle.setStyle("-fx-background-color:#1A2535; -fx-font-size:16px; -fx-cursor:hand; -fx-background-radius:22; -fx-min-width:40; -fx-min-height:40; -fx-border-color:#243044; -fx-border-width:1; -fx-border-radius:22; -fx-text-fill:#F5C518;");
            }
        } else {
            rootPane.getStyleClass().remove("dark");
            rootPane.setStyle("-fx-background-color: #EBF0F8;");
            if (contentArea != null) contentArea.setStyle("-fx-background-color:#EBF0F8; -fx-padding:24;");
            if (btnThemeToggle != null) {
                btnThemeToggle.setText("🌙");
                btnThemeToggle.setStyle("-fx-background-color:#F0F4FA; -fx-font-size:16px; -fx-cursor:hand; -fx-background-radius:22; -fx-min-width:40; -fx-min-height:40; -fx-border-color:#E4EBF5; -fx-border-width:1; -fx-border-radius:22;");
            }
        }
        if (contentArea != null && !contentArea.getChildren().isEmpty()) {
            javafx.scene.Node current = contentArea.getChildren().get(0);
            if (current instanceof Parent p) {
                if (darkMode) {
                    if (DARK_CSS_URL != null && !p.getStylesheets().contains(DARK_CSS_URL)) p.getStylesheets().add(DARK_CSS_URL);
                    if (!p.getStyleClass().contains("dark")) p.getStyleClass().add("dark");
                    patchNodeDark(p);
                } else {
                    p.getStyleClass().remove("dark");
                    p.getStylesheets().remove(DARK_CSS_URL);
                }
            }
        }
        if (btnThemeToggle != null) {
            ScaleTransition st = new ScaleTransition(Duration.millis(180), btnThemeToggle);
            st.setFromX(0.85); st.setFromY(0.85); st.setToX(1.0); st.setToY(1.0); st.play();
        }
        if (activePage != null && !activePage.isBlank()) loadContentView(activePage);
    }

    @FXML private void handleFullscreenToggle() {
        try {
            org.example.MainFX.toggleFullscreen();
        } catch (Exception e) {
            try {
                javafx.stage.Stage stage = (javafx.stage.Stage) contentArea.getScene().getWindow();
                stage.setFullScreen(!stage.isFullScreen());
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    // ─── Dark theme CSS URLs ──────────────────────────────────────────────────
    private static String DARK_CSS_URL  = null;
    private static String LIGHT_CSS_URL = null;
    static {
        try { DARK_CSS_URL  = MainShellController.class.getResource("/dark-theme.css").toExternalForm(); } catch (Exception ignored) {}
        try { LIGHT_CSS_URL = MainShellController.class.getResource("/styles.css").toExternalForm();    } catch (Exception ignored) {}
    }

    // ─── Content loading ─────────────────────────────────────────────────────

    public void loadContentView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent newContent = loader.load();
            applyThemeToPage(newContent);
            try {
                Object controller = loader.getController();
                controller.getClass().getMethod("setUserRole", String.class)
                    .invoke(controller, getRoleLabel(Session.getCurrentUser()));
            } catch (Exception ignored) {}

            if (!contentArea.getChildren().isEmpty()) {
                Parent old = (Parent) contentArea.getChildren().get(0);
                FadeTransition fadeOut = new FadeTransition(Duration.millis(120), old);
                fadeOut.setFromValue(1.0); fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> {
                    contentArea.getChildren().setAll(newContent);
                    newContent.setOpacity(0); newContent.setTranslateY(14);
                    new Timeline(new KeyFrame(Duration.millis(220),
                        new KeyValue(newContent.opacityProperty(), 1.0),
                        new KeyValue(newContent.translateYProperty(), 0))).play();
                });
                fadeOut.play();
            } else {
                contentArea.getChildren().setAll(newContent);
                newContent.setOpacity(0); newContent.setTranslateY(14);
                new Timeline(new KeyFrame(Duration.millis(250),
                    new KeyValue(newContent.opacityProperty(), 1.0),
                    new KeyValue(newContent.translateYProperty(), 0))).play();
            }
        } catch (IOException e) {
            System.err.println("Error loading view: " + fxmlFile + " — " + e.getMessage());
            Throwable cause = e.getCause();
            while (cause != null) { System.err.println("  Caused by: " + cause.getClass().getName() + ": " + cause.getMessage()); cause = cause.getCause(); }
            e.printStackTrace();
        }
    }

    private void applyThemeToPage(Parent page) {
        if (page == null) return;
        if (darkMode) {
            if (DARK_CSS_URL != null && !page.getStylesheets().contains(DARK_CSS_URL)) page.getStylesheets().add(DARK_CSS_URL);
            if (!page.getStyleClass().contains("dark")) page.getStyleClass().add("dark");
            patchNodeDark(page);
        } else {
            page.getStyleClass().remove("dark");
            page.getStylesheets().remove(DARK_CSS_URL);
        }
    }

    private void patchNodeDark(javafx.scene.Node node) {
        if (node == null) return;
        String style = node.getStyle();
        if (style != null && !style.isBlank()) {
            style = style.replaceAll("(?i)-fx-background-color:\\s*white\\b",       "-fx-background-color:#1A2535");
            style = style.replaceAll("(?i)-fx-background-color:\\s*#ffffff\\b",     "-fx-background-color:#1A2535");
            style = style.replaceAll("(?i)-fx-background-color:\\s*#f8f9fa\\b",     "-fx-background-color:#151F2E");
            style = style.replaceAll("(?i)-fx-background-color:\\s*#f5f6f8\\b",     "-fx-background-color:#0F1923");
            style = style.replaceAll("(?i)-fx-background-color:\\s*#ebf0f8\\b",     "-fx-background-color:#0F1923");
            style = style.replaceAll("(?i)-fx-background-color:\\s*#f0f4fa\\b",     "-fx-background-color:#0F1923");
            style = style.replaceAll("(?i)-fx-background-color:\\s*#f4f6fb\\b",     "-fx-background-color:#0F1923");
            style = style.replaceAll("(?i)-fx-background-color:\\s*#f1f5f9\\b",     "-fx-background-color:#151F2E");
            style = style.replaceAll("(?i)-fx-background-color:\\s*#e8eef8\\b",     "-fx-background-color:#151F2E");
            style = style.replaceAll("(?i)-fx-background-color:\\s*#eef2f7\\b",     "-fx-background-color:#151F2E");
            style = style.replaceAll("(?i)-fx-background-color:\\s*rgba\\(255,\\s*255,\\s*255,[^)]+\\)", "-fx-background-color:#1A2535");
            style = style.replaceAll("(?i)-fx-text-fill:\\s*#2c3e50\\b",            "-fx-text-fill:#E8EEF5");
            style = style.replaceAll("(?i)-fx-text-fill:\\s*#1E293B\\b",            "-fx-text-fill:#E8EEF5");
            style = style.replaceAll("(?i)-fx-text-fill:\\s*#334155\\b",            "-fx-text-fill:#C8D8E8");
            style = style.replaceAll("(?i)-fx-text-fill:\\s*#495057\\b",            "-fx-text-fill:#C8D8E8");
            style = style.replaceAll("(?i)-fx-text-fill:\\s*#475569\\b",            "-fx-text-fill:#C8D8E8");
            style = style.replaceAll("(?i)-fx-text-fill:\\s*black\\b",              "-fx-text-fill:#C8D8E8");
            style = style.replaceAll("(?i)-fx-text-fill:\\s*#6c757d\\b",            "-fx-text-fill:#5A7090");
            style = style.replaceAll("(?i)-fx-text-fill:\\s*#8FA3B8\\b",            "-fx-text-fill:#5A7090");
            style = style.replaceAll("(?i)-fx-border-color:\\s*#E8EEF8\\b",         "-fx-border-color:#243044");
            style = style.replaceAll("(?i)-fx-background-color:\\s*#fafbfe\\b",     "-fx-background-color:#0F1923");
            style = style.replaceAll("(?i)-fx-background-color:\\s*#f8faff\\b",     "-fx-background-color:#151F2E");
            style = style.replaceAll("(?i)-fx-background-color:\\s*#fffbeb\\b",     "-fx-background-color:#1A1500");
            style = style.replaceAll("(?i)-fx-background-color:\\s*#f0fdf4\\b",     "-fx-background-color:#0A1A0D");
            style = style.replaceAll("(?i)-fx-background-color:\\s*#eff6ff\\b",     "-fx-background-color:#0A0F1A");
            style = style.replaceAll("(?i)-fx-background-color:\\s*#fff1f2\\b",     "-fx-background-color:#1A0A0D");
            style = style.replaceAll("(?i)-fx-text-fill:\\s*#374151\\b",            "-fx-text-fill:#C8D8E8");
            style = style.replaceAll("(?i)-fx-text-fill:\\s*#1e293b\\b",            "-fx-text-fill:#E8EEF5");
            style = style.replaceAll("(?i)-fx-text-fill:\\s*#64748b\\b",            "-fx-text-fill:#5A7090");
            style = style.replaceAll("(?i)-fx-text-fill:\\s*#94a3b8\\b",            "-fx-text-fill:#3D5270");
            style = style.replaceAll("(?i)-fx-border-color:\\s*#bfdbfe\\b",         "-fx-border-color:#1B3C60");
            style = style.replaceAll("(?i)-fx-border-color:\\s*#bbf7d0\\b",         "-fx-border-color:#0A2A14");
            style = style.replaceAll("(?i)-fx-border-color:\\s*#fecdd3\\b",         "-fx-border-color:#3A0A14");
            node.setStyle(style);
        }
        if (node instanceof javafx.scene.Parent p) {
            for (javafx.scene.Node child : p.getChildrenUnmodifiable()) patchNodeDark(child);
        }
    }

    public void repatchDark(javafx.scene.Node node) { if (darkMode) patchNodeDark(node); }
    // Keep VBox overload for backward compatibility
    public void repatchDark(javafx.scene.layout.VBox container) { if (darkMode) patchNodeDark(container); }
    public boolean isDarkMode() { return darkMode; }

    // ─── Active button highlight ──────────────────────────────────────────────

    private void highlightActiveButton(Button activeBtn) {
        resetButtonStyles();
        if (activeBtn == null) return;
        activeBtn.getStyleClass().removeAll("sidebar-nav-btn", "sidebar-button");
        activeBtn.getStyleClass().add("sidebar-nav-btn-active");
    }

    private void resetButtonStyles() {
        Button[] navButtons = {btnInterviews, btnApplications, btnJobOffers, btnEvents,
                btnPastEvents, btnCalendar, btnStatistics, btnUserDashboard,
                btnDashboard, btnAdminStats, btnAdminApplications, btnFullscreenToggle};
        for (Button btn : navButtons) {
            if (btn == null) continue;
            btn.getStyleClass().removeAll("sidebar-nav-btn-active", "sidebar-button-active");
            if (!btn.getStyleClass().contains("sidebar-nav-btn")) btn.getStyleClass().add("sidebar-nav-btn");
        }
    }

    // ─── Role-based sidebar visibility ───────────────────────────────────────

    private void applyRoleToShell() {
        User u = Session.getCurrentUser();
        boolean isRecruiter = u instanceof Recruiter;
        boolean isAdmin     = u instanceof Admin;
        boolean isCandidate = u instanceof Candidate;

        if (lblUserRole != null) lblUserRole.setText(getRoleLabel(u));

        show(btnUserDashboard,       !isAdmin);
        show(btnInterviews,         !isAdmin);
        show(btnApplications,       !isAdmin);
        show(btnJobOffers,          true);
        show(btnEvents,             !isAdmin);
        show(btnPastEvents,         isCandidate);
        show(btnCalendar,           !isAdmin);
        show(btnStatistics,         !isCandidate);
        show(btnDashboard,          isAdmin);
        show(btnAdminStats,         isAdmin);
        show(btnAdminApplications,  isAdmin);

        if (btnInterviews != null)   btnInterviews.setText(isRecruiter ? "📅   Entretiens" : "📅   Mes Entretiens");
        if (btnApplications != null) btnApplications.setText(isRecruiter ? "📋   Candidatures" : "📋   Mes Candidatures");
        if (btnJobOffers != null) {
            if (isAdmin)          btnJobOffers.setText("🏢   Gérer les offres");
            else if (isRecruiter) btnJobOffers.setText("🏢   Mes Offres");
            else                  btnJobOffers.setText("🏢   Offres d'emploi");
        }
        if (btnStatistics != null)
            btnStatistics.setText(isAdmin ? "📊   Statistiques des Offres" : "📊   Mes Statistiques");
    }

    private void show(Button btn, boolean visible) {
        if (btn != null) { btn.setVisible(visible); btn.setManaged(visible); }
    }

    private void showError(String header, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur"); a.setHeaderText(header); a.setContentText(content); a.showAndWait();
    }
}


