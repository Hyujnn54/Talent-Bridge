package Controllers.interview;

import Models.interview.Interview;
import Models.interview.InterviewFeedback;
import Services.interview.InterviewFeedbackService;
import Services.interview.InterviewService;
import Utils.MyDatabase;
import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class InterviewManagementController {

    @FXML private VBox  interviewsListContainer;
    @FXML private Button btnScheduleNew;
    @FXML private VBox  editDialog;
    @FXML private HBox  bottomActionButtons;
    @FXML private VBox  rightPanelPlaceholder;
    @FXML private VBox  rightPanel;
    @FXML private VBox  rightPanelContent;

    // Search / sort bar
    @FXML private VBox              searchBox;
    @FXML private ComboBox<String>  comboSearchCriteria;
    @FXML private TextField         txtSearchInterview;
    @FXML private DatePicker        dateSearchPicker;
    @FXML private ComboBox<String>  comboSearchMode;
    @FXML private ComboBox<String>  comboSearchStatus;
    @FXML private Button            btnSearchInterview;
    @FXML private Button            btnClearSearch;
    @FXML private ComboBox<String>  comboSortBy;
    @FXML private ComboBox<String>  comboSortDir;

    // Feedback panel
    @FXML private VBox             feedbackPanel;
    @FXML private ComboBox<String> comboFeedbackDecision;
    @FXML private TextField        txtFeedbackScore;
    @FXML private Label            lblScoreIndicator;
    @FXML private TextArea         txtFeedbackComments;
    @FXML private Button           btnUpdateFeedbackAction;
    @FXML private Button           btnDeleteFeedback;

    // Edit form
    @FXML private DatePicker       datePicker;
    @FXML private TextField        txtTime;
    @FXML private TextField        txtDuration;
    @FXML private ComboBox<String> comboMode;
    @FXML private TextField        txtMeetingLink;
    @FXML private HBox             meetingLinkBox;
    @FXML private Button           btnGenerateMeetingLink;
    @FXML private Button           btnOpenMeetingLink;
    @FXML private TextField        txtLocation;
    @FXML private TextArea         txtNotes;
    @FXML private Label            lblMeetingLink;
    @FXML private Label            lblLocation;
    @FXML private Button           btnSave;
    @FXML private TextField        txtApplicationId;
    @FXML private TextField        txtRecruiterId;
    @FXML private ComboBox<String> comboStatus;

    private Interview selectedInterview = null;
    private boolean   isEditMode        = false;

    // The full list currently displayed (for in-place sort without re-fetching)
    private List<Interview> currentList = new ArrayList<>();

    // =========================================================================
    // Init
    // =========================================================================

    @FXML
    public void initialize() {
        Utils.DatabaseSchemaUtil.cleanupCorruptedData();
        Utils.DatabaseSchemaUtil.verifyInterviewData();
        setupComboBoxes();
        loadInterviews();
        // hide legacy panels (now invisible in FXML, just ensure managed=false)
        if (editDialog           != null) { editDialog.setVisible(false);           editDialog.setManaged(false); }
        if (bottomActionButtons  != null) { bottomActionButtons.setVisible(false);  bottomActionButtons.setManaged(false); }
        if (feedbackPanel        != null) { feedbackPanel.setVisible(false);        feedbackPanel.setManaged(false); }
        if (rightPanelContent    != null) { rightPanelContent.setVisible(false);    rightPanelContent.setManaged(false); }
        if (rightPanelPlaceholder!= null) { rightPanelPlaceholder.setVisible(true); rightPanelPlaceholder.setManaged(true); }
    }

    private void setupComboBoxes() {
        // Edit-form mode
        if (comboMode != null) {
            comboMode.setItems(FXCollections.observableArrayList("En Ligne", "Sur Site"));
            comboMode.valueProperty().addListener((obs, o, n) -> toggleModeFields(n));
        }

        // Feedback decision
        if (comboFeedbackDecision != null)
            comboFeedbackDecision.setItems(FXCollections.observableArrayList("Accepté", "Rejeté"));

        // Score live indicator
        if (txtFeedbackScore != null && lblScoreIndicator != null) {
            txtFeedbackScore.textProperty().addListener((obs, o, n) -> {
                try {
                    if (!n.trim().isEmpty()) {
                        int s = Integer.parseInt(n);
                        if (s >= 70) { lblScoreIndicator.setText("✓ ÉLEVÉ");  lblScoreIndicator.setStyle("-fx-text-fill:#28a745;-fx-font-size:12px;-fx-font-weight:600;"); }
                        else if (s >= 50) { lblScoreIndicator.setText("⚠ MOYEN"); lblScoreIndicator.setStyle("-fx-text-fill:#f0ad4e;-fx-font-size:12px;-fx-font-weight:600;"); }
                        else { lblScoreIndicator.setText("✗ FAIBLE"); lblScoreIndicator.setStyle("-fx-text-fill:#dc3545;-fx-font-size:12px;-fx-font-weight:600;"); }
                    } else lblScoreIndicator.setText("");
                } catch (NumberFormatException e) {
                    lblScoreIndicator.setText("Invalide"); lblScoreIndicator.setStyle("-fx-text-fill:#dc3545;-fx-font-size:12px;-fx-font-weight:600;");
                }
            });
        }

        // Search criteria
        if (comboSearchCriteria != null) {
            comboSearchCriteria.setItems(FXCollections.observableArrayList("Nom", "Date", "Mode", "Statut", "Lieu"));
            comboSearchCriteria.setValue("Nom");
            // Dynamically swap input widget when criteria changes
            comboSearchCriteria.valueProperty().addListener((obs, o, n) -> updateSearchInputVisibility(n));
        }

        // Search mode combo
        if (comboSearchMode != null)
            comboSearchMode.setItems(FXCollections.observableArrayList("En Ligne", "Sur Site"));

        // Search status combo
        if (comboSearchStatus != null)
            comboSearchStatus.setItems(FXCollections.observableArrayList("Planifié", "Terminé", "Annulé"));

        // Sort combos
        if (comboSortBy != null)
            comboSortBy.setItems(FXCollections.observableArrayList(
                "Date (planifiée)", "Durée", "Mode", "Statut", "Nom candidat"));
        if (comboSortBy != null) comboSortBy.setValue("Date (planifiée)");

        if (comboSortDir != null)
            comboSortDir.setItems(FXCollections.observableArrayList("Croissant ↑", "Décroissant ↓"));
        if (comboSortDir != null) comboSortDir.setValue("Croissant ↑");

        updateUIForRole();
    }

    /** Swap the search input widget (TextField / DatePicker / ComboBox) based on criteria */
    private void updateSearchInputVisibility(String criteria) {
        boolean showText   = criteria == null || criteria.equals("Nom") || criteria.equals("Lieu");
        boolean showDate   = "Date".equals(criteria);
        boolean showMode   = "Mode".equals(criteria);
        boolean showStatus = "Statut".equals(criteria);

        if (txtSearchInterview  != null) { txtSearchInterview.setVisible(showText);   txtSearchInterview.setManaged(showText); }
        if (dateSearchPicker    != null) { dateSearchPicker.setVisible(showDate);      dateSearchPicker.setManaged(showDate); }
        if (comboSearchMode     != null) { comboSearchMode.setVisible(showMode);       comboSearchMode.setManaged(showMode); }
        if (comboSearchStatus   != null) { comboSearchStatus.setVisible(showStatus);   comboSearchStatus.setManaged(showStatus); }
    }

    private void toggleModeFields(String mode) {
        if (mode == null) return;
        boolean online = "En Ligne".equals(mode) || "ONLINE".equals(mode);
        if (txtMeetingLink != null) { txtMeetingLink.setVisible(online);  txtMeetingLink.setManaged(online); }
        if (lblMeetingLink != null) { lblMeetingLink.setVisible(online);  lblMeetingLink.setManaged(online); }
        if (meetingLinkBox != null) { meetingLinkBox.setVisible(online);  meetingLinkBox.setManaged(online); }
        if (txtLocation    != null) { txtLocation.setVisible(!online);    txtLocation.setManaged(!online); }
        if (lblLocation    != null) { lblLocation.setVisible(!online);    lblLocation.setManaged(!online); }
    }

    private void updateUIForRole() {
        boolean isRecruiter = Utils.UserContext.getRole() == Utils.UserContext.Role.RECRUITER;
        boolean isCandidate = Utils.UserContext.getRole() == Utils.UserContext.Role.CANDIDATE;

        if (btnScheduleNew != null) { btnScheduleNew.setVisible(false); btnScheduleNew.setManaged(false); }

        // Show search/sort bar for both recruiter and candidate
        if (searchBox != null) {
            boolean show = isRecruiter || isCandidate;
            searchBox.setVisible(show); searchBox.setManaged(show);
        }
    }

    // =========================================================================
    // Search
    // =========================================================================

    @FXML
    private void handleSearchInterview() {
        String criteria = comboSearchCriteria != null ? comboSearchCriteria.getValue() : "Nom";
        List<Interview> base = getRoleFilteredInterviews(InterviewService.getAll());
        List<Interview> filtered;

        switch (criteria) {
            case "Date" -> {
                LocalDate chosen = dateSearchPicker != null ? dateSearchPicker.getValue() : null;
                if (chosen == null) { displayFilteredInterviews(base); return; }
                filtered = base.stream()
                    .filter(iv -> iv.getScheduledAt().toLocalDate().equals(chosen))
                    .toList();
            }
            case "Mode" -> {
                String sel = comboSearchMode != null ? comboSearchMode.getValue() : null;
                if (sel == null) { displayFilteredInterviews(base); return; }
                String dbMode = "En Ligne".equals(sel) ? "ONLINE" : "ON_SITE";
                filtered = base.stream()
                    .filter(iv -> dbMode.equals(iv.getMode()))
                    .toList();
            }
            case "Statut" -> {
                String sel = comboSearchStatus != null ? comboSearchStatus.getValue() : null;
                if (sel == null) { displayFilteredInterviews(base); return; }
                String dbStatus = switch (sel) {
                    case "Planifié" -> "SCHEDULED";
                    case "Terminé"  -> "DONE";
                    case "Annulé"   -> "CANCELLED";
                    default -> sel;
                };
                filtered = base.stream()
                    .filter(iv -> dbStatus.equalsIgnoreCase(iv.getStatus()))
                    .toList();
            }
            case "Nom" -> {
                String kw = txtSearchInterview != null ? txtSearchInterview.getText().trim().toLowerCase() : "";
                if (kw.isEmpty()) { displayFilteredInterviews(base); return; }
                filtered = base.stream()
                    .filter(iv -> getCandidateNameForSearch(iv.getApplicationId()).toLowerCase().contains(kw))
                    .toList();
            }
            case "Lieu" -> {
                String kw = txtSearchInterview != null ? txtSearchInterview.getText().trim().toLowerCase() : "";
                if (kw.isEmpty()) { displayFilteredInterviews(base); return; }
                filtered = base.stream()
                    .filter(iv -> {
                        String loc  = iv.getLocation()    != null ? iv.getLocation().toLowerCase()    : "";
                        String link = iv.getMeetingLink() != null ? iv.getMeetingLink().toLowerCase() : "";
                        return loc.contains(kw) || link.contains(kw);
                    })
                    .toList();
            }
            default -> filtered = base;
        }
        currentList = new ArrayList<>(filtered);
        displayFilteredInterviews(filtered);
    }

    @FXML
    private void handleClearSearch() {
        if (txtSearchInterview  != null) txtSearchInterview.clear();
        if (dateSearchPicker    != null) dateSearchPicker.setValue(null);
        if (comboSearchMode     != null) comboSearchMode.setValue(null);
        if (comboSearchStatus   != null) comboSearchStatus.setValue(null);
        if (comboSearchCriteria != null) { comboSearchCriteria.setValue("Nom"); updateSearchInputVisibility("Nom"); }
        if (comboSortBy  != null) comboSortBy.setValue("Date (planifiée)");
        if (comboSortDir != null) comboSortDir.setValue("Croissant ↑");
        loadInterviews();
    }

    // =========================================================================
    // Sort
    // =========================================================================

    @FXML
    private void handleApplySort() {
        if (currentList.isEmpty()) return;
        String by  = comboSortBy  != null ? comboSortBy.getValue()  : "Date (planifiée)";
        String dir = comboSortDir != null ? comboSortDir.getValue() : "Croissant ↑";
        boolean asc = dir == null || dir.startsWith("C");

        Comparator<Interview> cmp = switch (by != null ? by : "") {
            case "Durée"        -> Comparator.comparingInt(Interview::getDurationMinutes);
            case "Mode"         -> Comparator.comparing(iv -> iv.getMode() != null ? iv.getMode() : "");
            case "Statut"       -> Comparator.comparing(iv -> iv.getStatus() != null ? iv.getStatus() : "");
            case "Nom candidat" -> Comparator.comparing(iv -> getCandidateNameForSearch(iv.getApplicationId()));
            default             -> Comparator.comparing(Interview::getScheduledAt);
        };
        if (!asc) cmp = cmp.reversed();

        List<Interview> sorted = new ArrayList<>(currentList);
        sorted.sort(cmp);
        displayFilteredInterviews(sorted);
    }

    // ── DB helper: get candidate name from application id ─────────────────────
    private String getCandidateNameForSearch(Long appId) {
        if (appId == null) return "";
        String sql = "SELECT u.first_name, u.last_name FROM job_application ja " +
                     "JOIN users u ON ja.candidate_id = u.id WHERE ja.id = ?";
        try (PreparedStatement ps = MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
            ps.setLong(1, appId);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return (rs.getString("first_name") + " " + rs.getString("last_name")).trim();
        } catch (Exception e) { System.err.println("getCandidateNameForSearch: " + e.getMessage()); }
        return "";
    }

    @FXML
    private void handleScheduleNew() {
        showAlert("Info", "Les entretiens sont planifies depuis la section Candidatures.", Alert.AlertType.INFORMATION);
    }

    private void displayFilteredInterviews(List<Interview> interviews) {
        if (interviewsListContainer == null) return;
        interviewsListContainer.getChildren().clear();
        if (interviews.isEmpty()) {
            Label empty = new Label("Aucun entretien correspondant trouve");
            empty.setStyle("-fx-text-fill: #8FA3B8; -fx-font-size: 15px; -fx-padding: 40 0;");
            interviewsListContainer.getChildren().add(empty);
            return;
        }
        for (int i = 0; i < interviews.size(); i++) {
            VBox cardNode = createModernInterviewCard(interviews.get(i));
            cardNode.setOpacity(0);
            cardNode.setTranslateY(10);
            interviewsListContainer.getChildren().add(cardNode);
            int delay = i * 55;
            javafx.animation.Timeline tl = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.millis(delay)),
                new javafx.animation.KeyFrame(Duration.millis(delay + 260),
                    new javafx.animation.KeyValue(cardNode.opacityProperty(), 1.0),
                    new javafx.animation.KeyValue(cardNode.translateYProperty(), 0))
            );
            tl.play();
        }
        // Re-patch dark theme on dynamically-built cards
        Controllers.MainShellController shell = Controllers.MainShellController.getInstance();
        if (shell != null) shell.repatchDark(interviewsListContainer);
    }

    @FXML
    private void handleCancelEdit() {
        // no-op: edit is now done in the collapsible right panel section
    }

    @FXML
    private void handleSaveInterview() {
        if (!isEditMode) {
            showAlert("Info", "Les entretiens sont crees depuis les Candidatures.", Alert.AlertType.INFORMATION);
            return;
        }
        if (!validateInput()) return;

        try {
            LocalDateTime scheduledAt = LocalDateTime.of(
                datePicker.getValue(),
                LocalTime.parse(txtTime.getText().trim(), DateTimeFormatter.ofPattern("HH:mm"))
            );
            int duration = Integer.parseInt(txtDuration.getText().trim());
            String dbMode = convertModeToDatabase(comboMode.getValue());

            if (selectedInterview != null) {
                selectedInterview.setScheduledAt(scheduledAt);
                selectedInterview.setDurationMinutes(duration);
                selectedInterview.setMode(dbMode);
                if ("ONLINE".equals(dbMode)) {
                    selectedInterview.setMeetingLink(txtMeetingLink.getText() != null ? txtMeetingLink.getText().trim() : "");
                    selectedInterview.setLocation(null);
                } else {
                    selectedInterview.setLocation(txtLocation.getText() != null ? txtLocation.getText().trim() : "");
                    selectedInterview.setMeetingLink(null);
                }
                selectedInterview.setNotes(txtNotes.getText() != null ? txtNotes.getText().trim() : "");
                try {
                    InterviewService.updateInterview(selectedInterview.getId(), selectedInterview);
                } catch (RuntimeException e) {
                    showAlert("Erreur DB", "Echec mise a jour: " + e.getMessage(), Alert.AlertType.ERROR);
                    return;
                }
                loadInterviews();
                if (selectedInterview != null) showRightPanel(selectedInterview);
                showAlert("Succes", "Entretien mis a jour !", Alert.AlertType.INFORMATION);
            }
        } catch (Exception e) {
            showAlert("Erreur", "Echec: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void loadInterviews() {
        if (interviewsListContainer == null) return;
        List<Interview> interviews = getRoleFilteredInterviews(InterviewService.getAll());
        // Default sort: ascending by date
        interviews = new ArrayList<>(interviews);
        interviews.sort(Comparator.comparing(Interview::getScheduledAt));
        currentList = new ArrayList<>(interviews);
        displayFilteredInterviews(interviews);
    }

    /** Filter a list by the current user's role */
    private List<Interview> getRoleFilteredInterviews(List<Interview> all) {
        Utils.UserContext.Role role = Utils.UserContext.getRole();
        if (role == Utils.UserContext.Role.RECRUITER) {
            Long rid = Utils.UserContext.getRecruiterId();
            return all.stream().filter(i -> rid != null && rid.equals(i.getRecruiterId())).toList();
        } else if (role == Utils.UserContext.Role.CANDIDATE) {
            Long cid = Utils.UserContext.getCandidateId();
            List<Long> appIds = Services.application.ApplicationService.getByCandidateId(cid)
                    .stream().map(a -> a.id()).toList();
            return all.stream().filter(i -> i.getApplicationId() != null && appIds.contains(i.getApplicationId())).toList();
        }
        return all; // ADMIN sees all
    }

    private VBox createModernInterviewCard(Interview interview) {
        VBox card = new VBox(10);
        card.getStyleClass().add("interview-card");
        card.setPadding(new Insets(16));
        card.setMaxWidth(Double.MAX_VALUE);

        boolean isRecruiter = Utils.UserContext.getRole() == Utils.UserContext.Role.RECRUITER;
        boolean isPast      = interview.getScheduledAt() != null
                              && interview.getScheduledAt().isBefore(java.time.LocalDateTime.now());

        String[] info        = getCandidateAndOfferInfo(interview.getApplicationId());
        String candidateName = info[0];
        String offerTitle    = info[1];
        String extra         = info[2];

        // ── Card background: muted grey for past, normal white for upcoming ──
        if (isPast) {
            card.setStyle("-fx-background-color:#F0F2F6; -fx-background-radius:14;"
                    + "-fx-border-color:#D8DCE6; -fx-border-width:1; -fx-border-radius:14;"
                    + "-fx-opacity:0.82; -fx-cursor:hand;");
        } else {
            card.setStyle("-fx-background-color:white; -fx-background-radius:14;"
                    + "-fx-border-color:#E8EEF8; -fx-border-width:1.5; -fx-border-radius:14;"
                    + "-fx-effect:dropshadow(gaussian,rgba(91,163,245,0.10),10,0,0,2); -fx-cursor:hand;");
        }

        // ── Hover animation (only upcoming) ──────────────────────────────────
        if (!isPast) {
            card.setOnMouseEntered(e -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(120), card);
                st.setToX(1.010); st.setToY(1.010); st.play();
            });
            card.setOnMouseExited(e -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(120), card);
                st.setToX(1.0); st.setToY(1.0); st.play();
            });
        }

        // ── Header: title + status badge + PAST label ─────────────────────────
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        String titleColor = isPast ? "#8FA3B8" : "#2c3e50";
        Label titleLbl = new Label(isRecruiter ? "👤  " + candidateName : "💼  " + offerTitle);
        titleLbl.setStyle("-fx-font-size:14px; -fx-font-weight:700; -fx-text-fill:" + titleColor + ";");
        titleLbl.setWrapText(true);
        HBox.setHgrow(titleLbl, Priority.ALWAYS);

        // Past ribbon for recruiter
        if (isPast && isRecruiter) {
            Label pastTag = new Label("⏮  Passé");
            pastTag.setStyle("-fx-background-color:#E8ECF2; -fx-text-fill:#7A8899;"
                    + "-fx-font-size:10px; -fx-font-weight:700; -fx-padding:3 9;"
                    + "-fx-background-radius:8; -fx-border-color:#C8CED8;"
                    + "-fx-border-width:1; -fx-border-radius:8;");
            header.getChildren().addAll(titleLbl, pastTag);
        } else {
            String sc = getStatusBadgeColor(interview.getStatus());
            Label badge = new Label(formatStatusLabel(interview.getStatus()));
            badge.setStyle("-fx-background-color:" + sc + "22; -fx-text-fill:" + sc + ";"
                    + "-fx-font-size:10px; -fx-font-weight:700; -fx-padding:3 10;"
                    + "-fx-background-radius:10; -fx-border-color:" + sc + "55;"
                    + "-fx-border-width:1; -fx-border-radius:10;");
            header.getChildren().addAll(titleLbl, badge);
        }
        card.getChildren().add(header);

        // ── Sub-label ─────────────────────────────────────────────────────────
        String subColor = isPast ? "#A0AEBF" : "#5BA3F5";
        if (isRecruiter) {
            Label offerLbl = new Label("💼  " + offerTitle);
            offerLbl.setStyle("-fx-font-size:12px; -fx-text-fill:" + subColor + "; -fx-font-weight:600;");
            offerLbl.setWrapText(true);
            card.getChildren().add(offerLbl);
        } else if (!extra.isBlank()) {
            Label extraLbl = new Label("🏢  " + extra);
            extraLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#8FA3B8;");
            card.getChildren().add(extraLbl);
        }

        // ── Details strip ─────────────────────────────────────────────────────
        String detailsBg = isPast ? "#EAEDF3" : "#F7FAFF";
        HBox details = new HBox(20);
        details.setStyle("-fx-background-color:" + detailsBg + "; -fx-background-radius:8; -fx-padding:9 12;");
        details.getChildren().addAll(
            createInfoBox("📅 Date", formatDateTime(interview.getScheduledAt())),
            createInfoBox("⏱ Durée", interview.getDurationMinutes() + " min"),
            createInfoBox("🎯 Mode",  formatMode(interview.getMode()))
        );
        card.getChildren().add(details);

        // ── Past result banner (candidate only) ───────────────────────────────
        if (!isRecruiter && isPast) {
            boolean hasFb = checkIfFeedbackExists(interview.getId());
            if (hasFb) {
                var fbs = InterviewFeedbackService.getByInterviewId(interview.getId());
                if (!fbs.isEmpty()) {
                    InterviewFeedback fb = fbs.get(0);
                    boolean accepted = "ACCEPTED".equalsIgnoreCase(fb.getDecision());
                    String bg    = accepted ? "#D4EDDA" : "#F8D7DA";
                    String color = accepted ? "#155724" : "#721C24";
                    HBox banner  = new HBox(8);
                    banner.setAlignment(Pos.CENTER_LEFT);
                    banner.setStyle("-fx-background-color:" + bg + "; -fx-background-radius:8; -fx-padding:9 12;");
                    Label iconLbl = new Label(accepted ? "✅" : "❌");
                    iconLbl.setStyle("-fx-font-size:16px;");
                    Label resLbl = new Label(accepted ? "Retenu(e)" : "Non retenu(e)");
                    resLbl.setStyle("-fx-font-size:13px; -fx-font-weight:700; -fx-text-fill:" + color + ";");
                    if (fb.getOverallScore() != null) {
                        Label scoreLbl = new Label(" · " + fb.getOverallScore() + "/100");
                        scoreLbl.setStyle("-fx-font-size:12px; -fx-text-fill:" + color + ";");
                        banner.getChildren().addAll(iconLbl, resLbl, scoreLbl);
                    } else {
                        banner.getChildren().addAll(iconLbl, resLbl);
                    }
                    card.getChildren().add(banner);
                }
            } else {
                Label wait = new Label("⏳  Résultats en attente");
                wait.setStyle("-fx-background-color:#FFF3CD; -fx-text-fill:#7D5A00;"
                        + "-fx-padding:8 12; -fx-background-radius:8; -fx-font-size:11px; -fx-font-weight:600;");
                card.getChildren().add(wait);
            }
        }

        // ── Urgency hint (candidate, future) ─────────────────────────────────
        if (!isRecruiter && !isPast) {
            long h = java.time.temporal.ChronoUnit.HOURS.between(
                    java.time.LocalDateTime.now(), interview.getScheduledAt());
            if (h >= 0 && h <= 48) {
                Label urgLbl = new Label(h < 24
                        ? "🔥  Dans " + h + "h — Préparez-vous !"
                        : "⚡  Demain — Bonne chance !");
                urgLbl.setStyle("-fx-background-color:#FFF3CD; -fx-text-fill:#7D5A00;"
                        + "-fx-padding:7 11; -fx-background-radius:7; -fx-font-size:11px; -fx-font-weight:700;");
                card.getChildren().add(urgLbl);
            }
        }

        // ── Recruiter: action buttons ─────────────────────────────────────────
        if (isRecruiter) {
            HBox btnRow = new HBox(8);
            btnRow.setAlignment(Pos.CENTER_RIGHT);

            if (!isPast) {
                // Upcoming: full "Gérer" button
                Button btnManage = new Button("⚙  Gérer");
                btnManage.setStyle("-fx-background-color:#EBF4FF; -fx-text-fill:#5BA3F5;"
                        + "-fx-font-size:12px; -fx-font-weight:700; -fx-padding:7 16;"
                        + "-fx-background-radius:8; -fx-cursor:hand;"
                        + "-fx-border-color:#CCDFF8; -fx-border-width:1; -fx-border-radius:8;");
                btnManage.setOnMouseEntered(e -> btnManage.setStyle(
                        "-fx-background-color:#5BA3F5; -fx-text-fill:white;"
                        + "-fx-font-size:12px; -fx-font-weight:700; -fx-padding:7 16;"
                        + "-fx-background-radius:8; -fx-cursor:hand;"));
                btnManage.setOnMouseExited(e -> btnManage.setStyle(
                        "-fx-background-color:#EBF4FF; -fx-text-fill:#5BA3F5;"
                        + "-fx-font-size:12px; -fx-font-weight:700; -fx-padding:7 16;"
                        + "-fx-background-radius:8; -fx-cursor:hand;"
                        + "-fx-border-color:#CCDFF8; -fx-border-width:1; -fx-border-radius:8;"));
                btnManage.setOnAction(e -> {
                    selectedInterview = interview;
                    highlightSelectedCard(card);
                    showRightPanel(interview);
                });
                btnRow.getChildren().add(btnManage);
            } else {
                // Past: only "Voir retour" (feedback view)
                Button btnFeedback = new Button("📊  Voir retour");
                btnFeedback.setStyle("-fx-background-color:#EAEDF3; -fx-text-fill:#7A8899;"
                        + "-fx-font-size:12px; -fx-font-weight:600; -fx-padding:7 14;"
                        + "-fx-background-radius:8; -fx-cursor:hand;"
                        + "-fx-border-color:#C8CED8; -fx-border-width:1; -fx-border-radius:8;");
                btnFeedback.setOnMouseEntered(e -> btnFeedback.setStyle(
                        "-fx-background-color:#C8CED8; -fx-text-fill:#4A5568;"
                        + "-fx-font-size:12px; -fx-font-weight:600; -fx-padding:7 14;"
                        + "-fx-background-radius:8; -fx-cursor:hand;"));
                btnFeedback.setOnMouseExited(e -> btnFeedback.setStyle(
                        "-fx-background-color:#EAEDF3; -fx-text-fill:#7A8899;"
                        + "-fx-font-size:12px; -fx-font-weight:600; -fx-padding:7 14;"
                        + "-fx-background-radius:8; -fx-cursor:hand;"
                        + "-fx-border-color:#C8CED8; -fx-border-width:1; -fx-border-radius:8;"));
                btnFeedback.setOnAction(e -> {
                    selectedInterview = interview;
                    highlightSelectedCard(card);
                    showRightPanel(interview);
                });
                btnRow.getChildren().add(btnFeedback);
            }
            card.getChildren().add(btnRow);
        }

        // Clicking anywhere on the card also opens the right panel
        card.setOnMouseClicked(e -> {
            selectedInterview = interview;
            highlightSelectedCard(card);
            showRightPanel(interview);
        });

        return card;
    }

    /** Returns [candidateName, offerTitle, extraDetail] */
    private String[] getCandidateAndOfferInfo(Long appId) {
        if (appId == null) return new String[]{"Candidat inconnu", "Offre inconnue", ""};
        String sql = """
            SELECT u.first_name, u.last_name, jo.title, jo.location, jo.contract_type
            FROM job_application ja
            JOIN users u    ON ja.candidate_id = u.id
            JOIN job_offer jo ON ja.offer_id   = jo.id
            WHERE ja.id = ?
            """;
        try (java.sql.PreparedStatement ps =
                Utils.MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
            ps.setLong(1, appId);
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String name  = (rs.getString("first_name") + " " + rs.getString("last_name")).trim();
                String title = rs.getString("title");
                String loc   = rs.getString("location");
                String ct    = rs.getString("contract_type");
                String extra = (loc != null && !loc.isBlank() ? loc : "")
                             + (ct  != null && !ct.isBlank()  ? (loc!=null&&!loc.isBlank()?" · ":"") + ct : "");
                return new String[]{
                    name.isEmpty()  ? "Candidat #" + appId : name,
                    title != null   ? title : "Offre #" + appId,
                    extra
                };
            }
        } catch (Exception e) { System.err.println("getCandidateAndOfferInfo: " + e.getMessage()); }
        return new String[]{"Candidat #" + appId, "Candidature #" + appId, ""};
    }

    private String formatStatusLabel(String status) {
        if (status == null) return "Planifie";
        return switch (status.toUpperCase()) {
            case "SCHEDULED"   -> "Planifie";
            case "DONE", "COMPLETED" -> "Termine";
            case "CANCELLED"   -> "Annule";
            case "RESCHEDULED" -> "Replanifie";
            default -> status;
        };
    }

    private String getStatusBadgeColor(String status) {
        if (status == null) return "#5BA3F5";
        return switch (status.toUpperCase()) {
            case "SCHEDULED"   -> "#5BA3F5";
            case "DONE", "COMPLETED" -> "#2ECC71";
            case "CANCELLED"   -> "#E74C3C";
            case "RESCHEDULED" -> "#F39C12";
            default -> "#6C757D";
        };
    }

    private String formatMode(String mode) {
        if (mode == null) return "N/A";
        return switch (mode) {
            case "ONLINE"  -> "En ligne";
            case "ON_SITE" -> "Sur site";
            default -> mode;
        };
    }

    private VBox createInfoBox(String label, String value) {
        VBox box = new VBox(5);

        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("info-label");

        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("info-value");

        box.getChildren().addAll(labelNode, valueNode);
        return box;
    }

    private HBox createActionButtons(Interview interview) {
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        boolean isRecruiter = Utils.UserContext.getRole() == Utils.UserContext.Role.RECRUITER;

        // Only recruiters can manage feedback
        if (!isRecruiter) {
            // Candidates see interview result if feedback exists
            boolean hasFeedback = checkIfFeedbackExists(interview.getId());
            if (hasFeedback) {
                var feedbacks = InterviewFeedbackService.getByInterviewId(interview.getId());
                if (!feedbacks.isEmpty()) {
                    InterviewFeedback feedback = feedbacks.get(0);
                    String result = calculateResult(feedback);

                    Label resultLabel = new Label(result);
                    if ("ACCEPTED".equals(result)) {
                        resultLabel.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-padding: 6 14; -fx-background-radius: 6; -fx-font-weight: 700; -fx-font-size: 13px;");
                    } else {
                        resultLabel.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-padding: 6 14; -fx-background-radius: 6; -fx-font-weight: 700; -fx-font-size: 13px;");
                    }
                    actionBox.getChildren().add(resultLabel);
                }
            } else {
                Label candidateMsg = new Label("En Attente de Révision");
                candidateMsg.setStyle("-fx-text-fill: #f0ad4e; -fx-font-size: 12px; -fx-font-weight: 600;");
                actionBox.getChildren().add(candidateMsg);
            }
            return actionBox;
        }

        // Recruiter sees feedback action buttons in the card itself
        return actionBox;
    }

    // =========================================================================
    // Right-panel builder (called on card click)
    // =========================================================================

    private void showRightPanel(Interview interview) {
        if (rightPanelContent == null) return;

        // Show content, hide placeholder
        if (rightPanelPlaceholder != null) { rightPanelPlaceholder.setVisible(false); rightPanelPlaceholder.setManaged(false); }
        rightPanelContent.setVisible(true);
        rightPanelContent.setManaged(true);
        rightPanelContent.getChildren().clear();

        boolean isRecruiter = Utils.UserContext.getRole() == Utils.UserContext.Role.RECRUITER;
        boolean isPast      = interview.getScheduledAt() != null
                              && interview.getScheduledAt().isBefore(java.time.LocalDateTime.now());
        String[] info = getCandidateAndOfferInfo(interview.getApplicationId());

        // ── Interview detail card ─────────────────────────────────────────────
        VBox detailCard = new VBox(12);
        detailCard.setStyle("-fx-background-color:white; -fx-background-radius:14; -fx-padding:20;"
                + "-fx-border-color:#E8EEF8; -fx-border-width:1; -fx-border-radius:14;"
                + "-fx-effect:dropshadow(gaussian,rgba(91,163,245,0.10),12,0,0,3);");

        // Header
        HBox dHdr = new HBox(10); dHdr.setAlignment(Pos.CENTER_LEFT);
        Label dTitle = new Label("📋  Détails de l'entretien");
        dTitle.setStyle("-fx-font-size:15px; -fx-font-weight:700; -fx-text-fill:#2c3e50;");
        dTitle.setWrapText(true); HBox.setHgrow(dTitle, Priority.ALWAYS);
        String sc = getStatusBadgeColor(interview.getStatus());
        Label sBadge = new Label(formatStatusLabel(interview.getStatus()));
        sBadge.setStyle("-fx-background-color:" + sc + "22; -fx-text-fill:" + sc + ";"
                + "-fx-font-size:11px; -fx-font-weight:700; -fx-padding:4 12;"
                + "-fx-background-radius:12; -fx-border-color:" + sc + "55;"
                + "-fx-border-width:1; -fx-border-radius:12;");
        dHdr.getChildren().addAll(dTitle, sBadge);
        detailCard.getChildren().add(dHdr);

        // Names
        Label candidateLbl = new Label("👤  " + info[0]);
        candidateLbl.setStyle("-fx-font-size:13px; -fx-font-weight:700; -fx-text-fill:#2c3e50;");
        candidateLbl.setWrapText(true);
        Label offerLbl = new Label("💼  " + info[1]);
        offerLbl.setStyle("-fx-font-size:13px; -fx-text-fill:#5BA3F5; -fx-font-weight:600;");
        offerLbl.setWrapText(true);
        detailCard.getChildren().addAll(candidateLbl, offerLbl);

        // Info grid
        GridPane grid = new GridPane();
        grid.setHgap(16); grid.setVgap(10);
        grid.setStyle("-fx-background-color:#F7FAFF; -fx-background-radius:10; -fx-padding:14;");
        grid.getColumnConstraints().addAll(
            colConstraint(130, false),
            colConstraint(0, true)
        );
        int row = 0;
        addDetailRow(grid, row++, "📅 Date & Heure", formatDateTime(interview.getScheduledAt()));
        addDetailRow(grid, row++, "⏱ Durée",         interview.getDurationMinutes() + " minutes");
        addDetailRow(grid, row++, "🎯 Mode",          formatMode(interview.getMode()));
        if ("ONLINE".equals(interview.getMode()) && interview.getMeetingLink() != null && !interview.getMeetingLink().isBlank()) {
            // ── Clickable meeting link ───────────────────────────────────────
            Label linkLbl = new Label("🔗 Lien");
            linkLbl.setStyle("-fx-font-size:12px; -fx-font-weight:700; -fx-text-fill:#8FA3B8;");

            javafx.scene.control.Hyperlink hyperlink = new javafx.scene.control.Hyperlink(interview.getMeetingLink());
            hyperlink.setStyle("-fx-font-size:13px; -fx-text-fill:#1565C0; -fx-font-weight:600; "
                    + "-fx-border-color:transparent; -fx-padding:0; -fx-underline:true; -fx-cursor:hand;");
            hyperlink.setWrapText(true);
            hyperlink.setMaxWidth(Double.MAX_VALUE);
            final String url = interview.getMeetingLink();
            hyperlink.setOnAction(e -> {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                } catch (Exception ex) {
                    System.err.println("[MeetingLink] Could not open browser: " + ex.getMessage());
                }
            });

            GridPane.setConstraints(linkLbl, 0, row);
            GridPane.setConstraints(hyperlink, 1, row);
            GridPane.setHgrow(hyperlink, javafx.scene.layout.Priority.ALWAYS);
            grid.getChildren().addAll(linkLbl, hyperlink);
            row++;
        } else if ("ON_SITE".equals(interview.getMode()) && interview.getLocation() != null && !interview.getLocation().isBlank()) {
            addDetailRow(grid, row++, "📍 Lieu",      interview.getLocation());
        }
        if (interview.getNotes() != null && !interview.getNotes().isBlank()) {
            addDetailRow(grid, row++, "📝 Notes",     interview.getNotes());
        }
        detailCard.getChildren().add(grid);

        // ── Join Meeting button (ONLINE only) ──────────────────────────────
        if ("ONLINE".equals(interview.getMode())
                && interview.getMeetingLink() != null
                && !interview.getMeetingLink().isBlank()) {
            javafx.scene.control.Button joinBtn = new javafx.scene.control.Button("🔗  Rejoindre la réunion");
            joinBtn.setMaxWidth(Double.MAX_VALUE);
            joinBtn.setStyle("-fx-background-color:#1565C0; -fx-text-fill:white; -fx-font-size:13px;"
                    + "-fx-font-weight:700; -fx-padding:11 0; -fx-background-radius:8; -fx-cursor:hand;");
            joinBtn.setOnMouseEntered(e -> joinBtn.setStyle(
                    "-fx-background-color:#0D47A1; -fx-text-fill:white; -fx-font-size:13px;"
                    + "-fx-font-weight:700; -fx-padding:11 0; -fx-background-radius:8; -fx-cursor:hand;"));
            joinBtn.setOnMouseExited(e -> joinBtn.setStyle(
                    "-fx-background-color:#1565C0; -fx-text-fill:white; -fx-font-size:13px;"
                    + "-fx-font-weight:700; -fx-padding:11 0; -fx-background-radius:8; -fx-cursor:hand;"));
            final String meetUrl = interview.getMeetingLink();
            joinBtn.setOnAction(e -> {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(meetUrl));
                } catch (Exception ex) {
                    System.err.println("[MeetingLink] Could not open browser: " + ex.getMessage());
                }
            });
            detailCard.getChildren().add(joinBtn);
        }

        rightPanelContent.getChildren().add(detailCard);

        // ── Recruiter: edit section (only for upcoming) + feedback section ──────
        if (isRecruiter) {
            if (!isPast) {
                rightPanelContent.getChildren().add(buildEditSection(interview));
                // Feedback only allowed after the interview has passed
                VBox earlyNotice = new VBox(8);
                earlyNotice.setStyle("-fx-background-color:#FFF3CD; -fx-background-radius:12; -fx-padding:14 18;"
                        + "-fx-border-color:#FFE082; -fx-border-width:1; -fx-border-radius:12;");
                Label earlyIcon = new Label("⏳");
                earlyIcon.setStyle("-fx-font-size:20px;");
                Label earlyTitle = new Label("Retour disponible après l'entretien");
                earlyTitle.setStyle("-fx-font-size:13px; -fx-font-weight:700; -fx-text-fill:#7D5A00;");
                earlyTitle.setWrapText(true);
                Label earlySub = new Label("Vous pourrez rédiger le retour (feedback) une fois la date de l'entretien passée.");
                earlySub.setStyle("-fx-font-size:12px; -fx-text-fill:#A07000;");
                earlySub.setWrapText(true);
                earlyNotice.getChildren().addAll(earlyIcon, earlyTitle, earlySub);
                rightPanelContent.getChildren().add(earlyNotice);
            } else {
                // Past: show a clear notice instead of the edit form
                VBox pastNotice = new VBox(8);
                pastNotice.setStyle("-fx-background-color:#F0F2F6; -fx-background-radius:12; -fx-padding:16 18;"
                        + "-fx-border-color:#D0D5E0; -fx-border-width:1; -fx-border-radius:12;");
                Label icon = new Label("🔒");
                icon.setStyle("-fx-font-size:22px;");
                Label title = new Label("Entretien passé — modification désactivée");
                title.setStyle("-fx-font-size:13px; -fx-font-weight:700; -fx-text-fill:#5A6A7A;");
                title.setWrapText(true);
                Label sub = new Label("Cet entretien a déjà eu lieu. Vous pouvez uniquement consulter ou ajouter un retour (feedback).");
                sub.setStyle("-fx-font-size:12px; -fx-text-fill:#8FA3B8;");
                sub.setWrapText(true);
                pastNotice.getChildren().addAll(icon, title, sub);
                rightPanelContent.getChildren().add(pastNotice);
                rightPanelContent.getChildren().add(buildFeedbackSection(interview));
            }
        }

        // Fade in animation
        rightPanelContent.setOpacity(0);
        rightPanelContent.setTranslateX(16);
        javafx.animation.Timeline tl = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.millis(220),
                new javafx.animation.KeyValue(rightPanelContent.opacityProperty(),    1.0),
                new javafx.animation.KeyValue(rightPanelContent.translateXProperty(), 0))
        );
        tl.play();

        // Apply dark theme to freshly built panel content
        Controllers.MainShellController shell = Controllers.MainShellController.getInstance();
        if (shell != null) shell.repatchDark(rightPanelContent);
    }

    private javafx.scene.layout.ColumnConstraints colConstraint(double pref, boolean grow) {
        javafx.scene.layout.ColumnConstraints c = new javafx.scene.layout.ColumnConstraints();
        if (pref > 0) { c.setMinWidth(pref); c.setPrefWidth(pref); }
        if (grow) c.setHgrow(Priority.ALWAYS);
        return c;
    }

    private void addDetailRow(GridPane g, int row, String lbl, String val) {
        Label l = new Label(lbl);
        l.setStyle("-fx-font-size:12px; -fx-font-weight:700; -fx-text-fill:#8FA3B8;");
        Label v = new Label(val);
        v.setStyle("-fx-font-size:13px; -fx-text-fill:#2c3e50; -fx-font-weight:500;");
        v.setWrapText(true);
        GridPane.setConstraints(l, 0, row);
        GridPane.setConstraints(v, 1, row);
        g.getChildren().addAll(l, v);
    }

    /** Builds a collapsible edit section for the recruiter's right panel */
    private VBox buildEditSection(Interview interview) {
        VBox section = new VBox(0);
        section.setStyle("-fx-background-color:white; -fx-background-radius:14; -fx-padding:0;"
                + "-fx-border-color:#E8EEF8; -fx-border-width:1; -fx-border-radius:14;"
                + "-fx-effect:dropshadow(gaussian,rgba(91,163,245,0.08),10,0,0,2);");

        // ── Toggle header ──────────────────────────────────────────────────
        HBox hdr = new HBox(10); hdr.setAlignment(Pos.CENTER_LEFT);
        hdr.setStyle("-fx-background-color:white; -fx-background-radius:14; -fx-padding:16 20; -fx-cursor:hand;");
        Label hdrLbl = new Label("✏  Modifier l'entretien");
        hdrLbl.setStyle("-fx-font-size:14px; -fx-font-weight:700; -fx-text-fill:#2c3e50;");
        HBox.setHgrow(hdrLbl, Priority.ALWAYS);
        Label arrow = new Label("▼");
        arrow.setStyle("-fx-font-size:11px; -fx-text-fill:#8FA3B8;");
        hdr.getChildren().addAll(hdrLbl, arrow);

        // ── Form body (initially hidden) ──────────────────────────────────
        VBox body = new VBox(10);
        body.setStyle("-fx-padding:0 18 18 18;");
        body.setVisible(false); body.setManaged(false);

        // Sync hidden @FXML fields
        if (datePicker  != null) datePicker.setValue(interview.getScheduledAt().toLocalDate());
        if (txtTime     != null) txtTime.setText(interview.getScheduledAt().format(DateTimeFormatter.ofPattern("HH:mm")));
        if (txtDuration != null) txtDuration.setText(String.valueOf(interview.getDurationMinutes()));
        if (comboMode   != null) { comboMode.setItems(FXCollections.observableArrayList("En Ligne", "Sur Site")); comboMode.setValue(convertModeToDisplay(interview.getMode())); }
        if (txtMeetingLink != null) txtMeetingLink.setText(interview.getMeetingLink() != null ? interview.getMeetingLink() : "");
        if (txtLocation    != null) txtLocation.setText(interview.getLocation() != null ? interview.getLocation() : "");
        if (txtNotes       != null) txtNotes.setText(interview.getNotes() != null ? interview.getNotes() : "");
        isEditMode = true; selectedInterview = interview;

        // ── Date row: DatePicker + time field side by side ─────────────────
        addSectionLabel(body, "Date & Heure  (doit être dans le futur)");
        DatePicker dpVisible = new DatePicker(interview.getScheduledAt().toLocalDate());
        dpVisible.setMaxWidth(Double.MAX_VALUE); dpVisible.setPrefWidth(170);
        // Disable selection of past dates in the calendar popup
        dpVisible.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override public void updateItem(java.time.LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (item.isBefore(java.time.LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color:#F0F2F6; -fx-text-fill:#C0C8D4;");
                }
            }
        });
        dpVisible.valueProperty().addListener((obs, o, n) -> { if (datePicker != null) datePicker.setValue(n); });

        TextField timeVisible = new TextField(interview.getScheduledAt().format(DateTimeFormatter.ofPattern("HH:mm")));
        timeVisible.setPromptText("HH:mm");
        timeVisible.setPrefWidth(80); timeVisible.setMinWidth(80); timeVisible.setMaxWidth(90);
        timeVisible.textProperty().addListener((obs, o, n) -> { if (txtTime != null) txtTime.setText(n); });

        HBox dateRow = new HBox(8, dpVisible, timeVisible);
        dateRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(dpVisible, Priority.ALWAYS);
        dateRow.setMaxWidth(Double.MAX_VALUE);
        body.getChildren().add(dateRow);

        // ── Duration ──────────────────────────────────────────────────────
        addSectionLabel(body, "Durée (minutes)");
        TextField durVisible = new TextField(String.valueOf(interview.getDurationMinutes()));
        durVisible.setPromptText("ex: 60"); durVisible.setMaxWidth(Double.MAX_VALUE);
        durVisible.textProperty().addListener((obs, o, n) -> { if (txtDuration != null) txtDuration.setText(n); });
        body.getChildren().add(durVisible);

        // ── Mode ──────────────────────────────────────────────────────────
        addSectionLabel(body, "Mode d'entretien");
        ComboBox<String> modeVisible = new ComboBox<>(FXCollections.observableArrayList("En Ligne", "Sur Site"));
        modeVisible.setValue(convertModeToDisplay(interview.getMode()));
        modeVisible.setMaxWidth(Double.MAX_VALUE);

        // ── Meeting link row (online) ─────────────────────────────────────
        boolean startOnline = "ONLINE".equals(interview.getMode()) || "En Ligne".equals(convertModeToDisplay(interview.getMode()));

        VBox linkBox = new VBox(6);
        addSectionLabel(linkBox, "Lien de réunion");
        TextField linkVisible = new TextField(interview.getMeetingLink() != null ? interview.getMeetingLink() : "");
        linkVisible.setPromptText("https://meet.jit.si/..."); linkVisible.setMaxWidth(Double.MAX_VALUE);
        linkVisible.textProperty().addListener((obs, o, n) -> { if (txtMeetingLink != null) txtMeetingLink.setText(n); });
        Button genLinkBtn = new Button("🔗  Générer un lien Jitsi Meet");
        genLinkBtn.setMaxWidth(Double.MAX_VALUE);
        genLinkBtn.setStyle("-fx-background-color:#E8F4FD; -fx-text-fill:#1B6CB0; -fx-font-size:12px;"
                + "-fx-font-weight:600; -fx-padding:8 0; -fx-background-radius:8; -fx-cursor:hand;"
                + "-fx-border-color:#BBDEFB; -fx-border-width:1; -fx-border-radius:8;");
        genLinkBtn.setOnAction(e -> {
            Long iid = interview.getId() != null ? interview.getId()
                     : (long)(Math.random() * 90000 + 10000);
            java.time.LocalDateTime when = interview.getScheduledAt() != null
                     ? interview.getScheduledAt() : java.time.LocalDateTime.now();
            String link = Services.interview.MeetingService.generateMeetingLink(iid, when);
            linkVisible.setText(link);
            if (txtMeetingLink != null) txtMeetingLink.setText(link);
        });
        linkBox.getChildren().addAll(linkVisible, genLinkBtn);
        linkBox.setVisible(startOnline); linkBox.setManaged(startOnline);

        // ── Location row (on-site) ────────────────────────────────────────
        VBox locBox = new VBox(6);
        addSectionLabel(locBox, "Lieu (adresse)");
        TextField locVisible = new TextField(interview.getLocation() != null ? interview.getLocation() : "");
        locVisible.setPromptText("ex: 12 rue de la Paix, Tunis"); locVisible.setMaxWidth(Double.MAX_VALUE);
        locVisible.textProperty().addListener((obs, o, n) -> { if (txtLocation != null) txtLocation.setText(n); });
        locBox.getChildren().add(locVisible);
        locBox.setVisible(!startOnline); locBox.setManaged(!startOnline);

        // Mode change listener — swap link/loc boxes
        modeVisible.valueProperty().addListener((obs, o, n) -> {
            if (comboMode != null) comboMode.setValue(n);
            toggleModeFields(n);
            boolean online = "En Ligne".equals(n);
            linkBox.setVisible(online); linkBox.setManaged(online);
            locBox.setVisible(!online); locBox.setManaged(!online);
        });
        body.getChildren().add(modeVisible);
        body.getChildren().addAll(linkBox, locBox);

        // ── Notes ─────────────────────────────────────────────────────────
        addSectionLabel(body, "Notes");
        TextArea notesVisible = new TextArea(interview.getNotes() != null ? interview.getNotes() : "");
        notesVisible.setPromptText("Notes additionnelles..."); notesVisible.setPrefRowCount(3); notesVisible.setWrapText(true);
        notesVisible.setMaxWidth(Double.MAX_VALUE);
        notesVisible.textProperty().addListener((obs, o, n) -> { if (txtNotes != null) txtNotes.setText(n); });
        body.getChildren().add(notesVisible);

        // ── Save button ───────────────────────────────────────────────────
        Button saveBtn = new Button("💾  Enregistrer les modifications");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setStyle("-fx-background-color:#5BA3F5; -fx-text-fill:white; -fx-font-size:13px;"
                + "-fx-font-weight:700; -fx-padding:11 0; -fx-background-radius:9; -fx-cursor:hand;");
        saveBtn.setOnMouseEntered(e -> saveBtn.setStyle("-fx-background-color:#4A90E2; -fx-text-fill:white; -fx-font-size:13px; -fx-font-weight:700; -fx-padding:11 0; -fx-background-radius:9; -fx-cursor:hand;"));
        saveBtn.setOnMouseExited(e -> saveBtn.setStyle("-fx-background-color:#5BA3F5; -fx-text-fill:white; -fx-font-size:13px; -fx-font-weight:700; -fx-padding:11 0; -fx-background-radius:9; -fx-cursor:hand;"));
        saveBtn.setOnAction(e -> {
            // Read values directly from visible fields
            if (dpVisible.getValue() == null) { showAlert("Erreur", "Veuillez selectionner une date.", Alert.AlertType.WARNING); return; }
            if (timeVisible.getText().isBlank()) { showAlert("Erreur", "Veuillez entrer une heure (HH:mm).", Alert.AlertType.WARNING); return; }
            try {
                java.time.LocalTime t = java.time.LocalTime.parse(timeVisible.getText().trim(), DateTimeFormatter.ofPattern("HH:mm"));
                LocalDateTime scheduledAt = LocalDateTime.of(dpVisible.getValue(), t);

                // ── Validate: new date/time must be in the future ──────────
                if (!scheduledAt.isAfter(LocalDateTime.now())) {
                    showAlert("Date invalide",
                            "La nouvelle date et heure doivent être dans le futur.\n"
                            + "Vous ne pouvez pas replanifier un entretien dans le passé.",
                            Alert.AlertType.WARNING);
                    // Reset DatePicker highlight to signal the error
                    dpVisible.setStyle("-fx-border-color:#E53935; -fx-border-width:2; -fx-border-radius:6;");
                    return;
                }
                dpVisible.setStyle(""); // clear error highlight

                int dur = 60;
                try { dur = Integer.parseInt(durVisible.getText().trim()); } catch (NumberFormatException ignore) {}
                String dbMode = convertModeToDatabase(modeVisible.getValue());
                selectedInterview.setScheduledAt(scheduledAt);
                selectedInterview.setDurationMinutes(dur);
                selectedInterview.setMode(dbMode);
                if ("ONLINE".equals(dbMode)) {
                    selectedInterview.setMeetingLink(linkVisible.getText().trim());
                    selectedInterview.setLocation(null);
                } else {
                    selectedInterview.setLocation(locVisible.getText().trim());
                    selectedInterview.setMeetingLink(null);
                }
                selectedInterview.setNotes(notesVisible.getText().trim());
                InterviewService.updateInterview(selectedInterview.getId(), selectedInterview);
                showAlert("Succès", "Entretien mis à jour avec succès !", Alert.AlertType.INFORMATION);
                loadInterviews();
                showRightPanel(selectedInterview);
            } catch (Exception ex) {
                showAlert("Erreur", "Format invalide : " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });
        body.getChildren().add(saveBtn);

        // ── Delete button ─────────────────────────────────────────────────
        Button delBtn = new Button("🗑  Supprimer l'entretien");
        delBtn.setMaxWidth(Double.MAX_VALUE);
        delBtn.setStyle("-fx-background-color:#FFF0F0; -fx-text-fill:#E53935; -fx-font-size:12px;"
                + "-fx-font-weight:600; -fx-padding:9 0; -fx-background-radius:9; -fx-cursor:hand;"
                + "-fx-border-color:#FFCDD2; -fx-border-width:1; -fx-border-radius:9;");
        delBtn.setOnAction(e -> handleDeleteInterview());
        body.getChildren().add(delBtn);

        // ── Toggle collapse/expand ────────────────────────────────────────
        hdr.setOnMouseClicked(e -> {
            boolean nowVisible = !body.isVisible();
            body.setVisible(nowVisible); body.setManaged(nowVisible);
            arrow.setText(nowVisible ? "▲" : "▼");
            hdr.setStyle("-fx-background-color:" + (nowVisible ? "#F7FAFF" : "white")
                    + "; -fx-background-radius:14; -fx-padding:16 20; -fx-cursor:hand;");
        });

        section.getChildren().addAll(hdr, body);
        return section;
    }

    /** Small helper to add a styled section label to any VBox */
    private void addSectionLabel(VBox parent, String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:11px; -fx-font-weight:700; -fx-text-fill:#8FA3B8; -fx-padding:4 0 2 0;");
        parent.getChildren().add(l);
    }

    private void updateModeFieldsInBody(VBox body, String mode) {
        // kept for backward compat — no-op, mode switching now handled inline
    }

    private void addFormRow(GridPane g, int row, String labelText, javafx.scene.Node field) {
        Label l = new Label(labelText);
        l.setStyle("-fx-font-size:12px; -fx-font-weight:600; -fx-text-fill:#5A7080;");
        l.setMinWidth(100);
        GridPane.setConstraints(l, 0, row, 1, 1, null, null, null, null, new Insets(0,0,0,0));
        GridPane.setConstraints(field, 1, row);
        g.getChildren().addAll(l, field);
    }

    private void addFormRowNode(GridPane g, int row, String labelText, javafx.scene.Node field, String rowKey) {
        Label l = new Label(labelText);
        l.setStyle("-fx-font-size:12px; -fx-font-weight:600; -fx-text-fill:#5A7080;");
        l.setMinWidth(100);
        if (rowKey != null) l.setUserData(rowKey);
        if (rowKey != null) field.setUserData(rowKey);
        GridPane.setConstraints(l, 0, row);
        GridPane.setConstraints(field, 1, row);
        g.getChildren().addAll(l, field);
    }

    /** Builds a collapsible feedback section for the recruiter's right panel */
    private VBox buildFeedbackSection(Interview interview) {
        boolean hasFb = checkIfFeedbackExists(interview.getId());
        InterviewFeedback existing = hasFb
                ? InterviewFeedbackService.getByInterviewId(interview.getId()).stream().findFirst().orElse(null)
                : null;

        VBox section = new VBox(0);
        section.setStyle("-fx-background-color:white; -fx-background-radius:14; -fx-padding:0;"
                + "-fx-border-color:#E8EEF8; -fx-border-width:1; -fx-border-radius:14;"
                + "-fx-effect:dropshadow(gaussian,rgba(91,163,245,0.08),10,0,0,2);");

        // ── Toggle header ─────────────────────────────────────────────────────
        HBox hdr = new HBox(10); hdr.setAlignment(Pos.CENTER_LEFT);
        hdr.setStyle("-fx-background-color:white; -fx-background-radius:14; -fx-padding:16 20; -fx-cursor:hand;");

        Label hdrLbl = new Label("📊  Retour d'entretien");
        hdrLbl.setStyle("-fx-font-size:14px; -fx-font-weight:700; -fx-text-fill:#2c3e50;");
        HBox.setHgrow(hdrLbl, Priority.ALWAYS);

        // Status pill
        String pillBg = hasFb ? "#D4EDDA" : "#FFF3CD";
        String pillFg = hasFb ? "#155724" : "#7D5A00";
        Label pill = new Label(hasFb ? "✓ Rempli" : "⚠ En attente");
        pill.setStyle("-fx-background-color:" + pillBg + "; -fx-text-fill:" + pillFg + ";"
                + "-fx-font-size:11px; -fx-font-weight:700; -fx-padding:3 10; -fx-background-radius:10;");

        Label arrow = new Label("▼");
        arrow.setStyle("-fx-font-size:11px; -fx-text-fill:#8FA3B8;");
        hdr.getChildren().addAll(hdrLbl, pill, arrow);

        // ── Form body ─────────────────────────────────────────────────────────
        VBox body = new VBox(12);
        body.setStyle("-fx-padding:0 20 18 20;");
        body.setVisible(false); body.setManaged(false);

        // Sync hidden @FXML fields
        selectedInterview = interview;
        if (comboFeedbackDecision != null) {
            comboFeedbackDecision.setItems(FXCollections.observableArrayList("Accepté", "Rejeté"));
        }

        // Decision
        ComboBox<String> decisionVisible = new ComboBox<>(FXCollections.observableArrayList("Accepté", "Rejeté"));
        decisionVisible.setMaxWidth(Double.MAX_VALUE);
        decisionVisible.setPromptText("Sélectionner une décision...");
        if (existing != null) decisionVisible.setValue(
                "ACCEPTED".equals(existing.getDecision()) ? "Accepté" : "Rejeté");
        decisionVisible.valueProperty().addListener((obs, o, n) -> {
            if (comboFeedbackDecision != null) comboFeedbackDecision.setValue(n);
        });

        // Score
        TextField scoreVisible = new TextField(existing != null && existing.getOverallScore() != null
                ? String.valueOf(existing.getOverallScore()) : "");
        scoreVisible.setPromptText("Score 0-100");
        scoreVisible.setMaxWidth(Double.MAX_VALUE);
        scoreVisible.textProperty().addListener((obs, o, n) -> { if (txtFeedbackScore != null) txtFeedbackScore.setText(n); });

        // Score indicator label (visible)
        Label scoreInd = new Label();
        scoreInd.setStyle("-fx-font-size:12px; -fx-font-weight:600;");
        scoreVisible.textProperty().addListener((obs, o, n) -> {
            try {
                int s = Integer.parseInt(n.trim());
                if (s >= 70) { scoreInd.setText("✓ ÉLEVÉ");  scoreInd.setStyle("-fx-text-fill:#28a745; -fx-font-size:12px; -fx-font-weight:600;"); }
                else if (s >= 50) { scoreInd.setText("⚠ MOYEN"); scoreInd.setStyle("-fx-text-fill:#f0ad4e; -fx-font-size:12px; -fx-font-weight:600;"); }
                else { scoreInd.setText("✗ FAIBLE"); scoreInd.setStyle("-fx-text-fill:#dc3545; -fx-font-size:12px; -fx-font-weight:600;"); }
            } catch (Exception ignored) { scoreInd.setText(""); }
        });

        // Comments
        TextArea commentsVisible = new TextArea(existing != null && existing.getComment() != null
                ? existing.getComment() : "");
        commentsVisible.setPromptText("Commentaires..."); commentsVisible.setPrefRowCount(4); commentsVisible.setWrapText(true);
        commentsVisible.textProperty().addListener((obs, o, n) -> { if (txtFeedbackComments != null) txtFeedbackComments.setText(n); });

        // Sync hidden fields now
        if (comboFeedbackDecision != null) comboFeedbackDecision.setValue(decisionVisible.getValue());
        if (txtFeedbackScore != null) txtFeedbackScore.setText(scoreVisible.getText());
        if (txtFeedbackComments != null) txtFeedbackComments.setText(commentsVisible.getText());

        GridPane fg = new GridPane();
        fg.setHgap(14); fg.setVgap(11);
        fg.getColumnConstraints().addAll(colConstraint(110, false), colConstraint(0, true));
        addFormRow(fg, 0, "Décision", decisionVisible);
        HBox scoreRow = new HBox(10, scoreVisible, scoreInd); HBox.setHgrow(scoreVisible, Priority.ALWAYS);
        scoreRow.setAlignment(Pos.CENTER_LEFT);
        addFormRow(fg, 1, "Score", scoreRow);
        addFormRow(fg, 2, "Commentaires", commentsVisible);
        body.getChildren().add(fg);

        // Save button
        String saveLbl = hasFb ? "💾  Mettre à jour le retour" : "💾  Créer le retour";
        Button saveBtn = new Button(saveLbl);
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setStyle("-fx-background-color:#2ECC71; -fx-text-fill:white; -fx-font-size:13px;"
                + "-fx-font-weight:700; -fx-padding:11 0; -fx-background-radius:9; -fx-cursor:hand;");
        saveBtn.setOnAction(e -> handleUpdateFeedbackAction());
        body.getChildren().add(saveBtn);

        // Delete button (only if feedback exists)
        if (hasFb && existing != null) {
            Button delBtn = new Button("🗑  Supprimer le retour");
            delBtn.setMaxWidth(Double.MAX_VALUE);
            delBtn.setStyle("-fx-background-color:#FFF0F0; -fx-text-fill:#E53935; -fx-font-size:12px;"
                    + "-fx-font-weight:600; -fx-padding:9 0; -fx-background-radius:9; -fx-cursor:hand;"
                    + "-fx-border-color:#FFCDD2; -fx-border-width:1; -fx-border-radius:9;");
            final InterviewFeedback fb = existing;
            delBtn.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Supprimer le retour");
                confirm.setHeaderText(null);
                confirm.setContentText("Supprimer définitivement ce retour ?");
                confirm.showAndWait().ifPresent(r -> {
                    if (r == ButtonType.OK) {
                        InterviewFeedbackService.deleteFeedback(fb.getId());
                        loadInterviews();
                        showRightPanel(interview);
                    }
                });
            });
            body.getChildren().add(delBtn);
        }

        // Toggle
        hdr.setOnMouseClicked(e -> {
            boolean nowVisible = !body.isVisible();
            body.setVisible(nowVisible); body.setManaged(nowVisible);
            arrow.setText(nowVisible ? "▲" : "▼");
            hdr.setStyle("-fx-background-color:" + (nowVisible ? "#F7FAFF" : "white")
                    + "; -fx-background-radius:14; -fx-padding:16 20; -fx-cursor:hand;");
            if (nowVisible) {
                // re-sync hidden fields when panel opens
                if (comboFeedbackDecision != null) comboFeedbackDecision.setValue(decisionVisible.getValue());
                if (txtFeedbackScore     != null) txtFeedbackScore.setText(scoreVisible.getText());
                if (txtFeedbackComments  != null) txtFeedbackComments.setText(commentsVisible.getText());
                selectedInterview = interview;
            }
        });

        section.getChildren().addAll(hdr, body);
        return section;
    }

    // ── These are kept to satisfy FXML @FXML references that still exist ──────

    private void viewFeedback(Interview interview) { /* replaced by buildFeedbackSection */ }
    private void updateFeedback(Interview interview) { selectedInterview = interview; showFeedbackPanelForInterview(interview); }
    private void createFeedback(Interview interview) {
        selectedInterview = interview;
        if (comboFeedbackDecision != null) comboFeedbackDecision.setValue(null);
        if (txtFeedbackScore != null) txtFeedbackScore.setText("");
        if (txtFeedbackComments != null) txtFeedbackComments.setText("");
        if (feedbackPanel != null) { feedbackPanel.setVisible(true); feedbackPanel.setManaged(true); }
        if (rightPanelPlaceholder != null) { rightPanelPlaceholder.setVisible(false); rightPanelPlaceholder.setManaged(false); }
    }

    private void deleteFeedbackForInterview(Interview interview) {
        var feedbacks = InterviewFeedbackService.getByInterviewId(interview.getId());
        if (!feedbacks.isEmpty()) {
            InterviewFeedback existing = feedbacks.get(0);
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Supprimer le Retour");
            confirm.setHeaderText(null);
            confirm.setContentText("Cette action ne peut pas être annulée.");
            confirm.showAndWait().ifPresent(r -> {
                if (r == ButtonType.OK) {
                    InterviewFeedbackService.deleteFeedback(existing.getId());
                    showAlert("Succès", "Retour supprimé.", Alert.AlertType.INFORMATION);
                    loadInterviews();
                }
            });
        }
    }

    private String calculateResult(InterviewFeedback feedback) {
        // Use the decision field from database
        if (feedback.getDecision() != null) {
            return feedback.getDecision(); // ACCEPTED or REJECTED
        }
        return "PENDING";
    }

    private void showFeedbackPanelForInterview(Interview interview) {
        if (feedbackPanel == null) return;

        // CRITICAL: Set the selected interview for the update handler
        selectedInterview = interview;
        System.out.println("[DEBUG] showFeedbackPanelForInterview - selectedInterview set to ID: " + interview.getId());

        // Hide edit dialog if open
        if (editDialog != null) { editDialog.setVisible(false); editDialog.setManaged(false); }

        // Get existing feedback
        var feedbacks = InterviewFeedbackService.getByInterviewId(interview.getId());
        InterviewFeedback existingFeedback = feedbacks.isEmpty() ? null : feedbacks.get(0);

        // Pre-fill if exists
        if (existingFeedback != null) {
            // Convert DB enum to French display value
            String decision = existingFeedback.getDecision();
            String decisionDisplay = "ACCEPTED".equals(decision) ? "Accepté"
                                   : "REJECTED".equals(decision) ? "Rejeté"
                                   : decision;

            if (comboFeedbackDecision != null) {
                comboFeedbackDecision.setValue(decisionDisplay);
            }

            if (existingFeedback.getOverallScore() != null) {
                txtFeedbackScore.setText(String.valueOf(existingFeedback.getOverallScore()));
            } else {
                txtFeedbackScore.setText("");
            }
            txtFeedbackComments.setText(existingFeedback.getComment() != null ? existingFeedback.getComment() : "");

            // Set button text for updating
            if (btnUpdateFeedbackAction != null) {
                btnUpdateFeedbackAction.setText("💾 Mettre à Jour Retour");
            }
        } else {
            if (comboFeedbackDecision != null) comboFeedbackDecision.setValue(null);
            txtFeedbackScore.setText("");
            txtFeedbackComments.setText("");

            // Set button text for creating
            if (btnUpdateFeedbackAction != null) {
                btnUpdateFeedbackAction.setText("💾 Créer Retour");
            }
        }

        // Always hide delete button in the panel - delete is only available on the card
        if (btnDeleteFeedback != null) {
            btnDeleteFeedback.setVisible(false);
            btnDeleteFeedback.setManaged(false);
        }

        feedbackPanel.setVisible(true);
        feedbackPanel.setManaged(true);
        // Hide placeholder when panel is shown
        if (rightPanelPlaceholder != null) {
            rightPanelPlaceholder.setVisible(false);
            rightPanelPlaceholder.setManaged(false);
        }
    }

    @FXML
    private void handleUpdateFeedbackAction() {
        System.out.println("\n!!!!!!!!!!!!! UPDATE BUTTON CLICKED !!!!!!!!!!!!!");

        if (selectedInterview == null) {
            System.err.println("ERROR: No interview selected");
            showAlert("Erreur", "Aucun entretien sélectionné.", Alert.AlertType.ERROR);
            return;
        }

        System.out.println("\n============ FEEDBACK UPDATE STARTED ============");
        System.out.println("Interview ID: " + selectedInterview.getId());

        try {
            // Validation - decision is required
            if (comboFeedbackDecision == null || comboFeedbackDecision.getValue() == null || comboFeedbackDecision.getValue().trim().isEmpty()) {
                System.err.println("VALIDATION ERROR: Decision not selected");
                showAlert("Erreur de Validation", "Veuillez sélectionner une décision (Accepté ou Rejeté).", Alert.AlertType.WARNING);
                return;
            }

            // Validation - score is required
            if (txtFeedbackScore.getText().trim().isEmpty()) {
                System.err.println("VALIDATION ERROR: Score is empty");
                showAlert("Erreur de Validation", "Veuillez entrer un score.", Alert.AlertType.WARNING);
                return;
            }

            int overallScore;
            try {
                overallScore = Integer.parseInt(txtFeedbackScore.getText().trim());
            } catch (NumberFormatException e) {
                System.err.println("VALIDATION ERROR: Score is not a number: " + txtFeedbackScore.getText());
                showAlert("Erreur de Validation", "Le score doit être un nombre valide.", Alert.AlertType.WARNING);
                return;
            }

            if (overallScore < 0 || overallScore > 100) {
                System.err.println("VALIDATION ERROR: Score out of range: " + overallScore);
                showAlert("Erreur de Validation", "Le score doit être entre 0 et 100.", Alert.AlertType.WARNING);
                return;
            }

            String comment = txtFeedbackComments.getText();
            String decisionDisplay = comboFeedbackDecision.getValue();
            // Convert French display value to DB enum
            String decision = "Accepté".equals(decisionDisplay) ? "ACCEPTED"
                            : "Rejeté".equals(decisionDisplay)   ? "REJECTED"
                            : decisionDisplay; // fallback (already in English)
            Long recruiterId = (long) getEffectiveRecruiterIdForInterview(selectedInterview);

            System.out.println("Form Values:");
            System.out.println("  - Decision: " + decision);
            System.out.println("  - Score: " + overallScore);
            System.out.println("  - Comment length: " + (comment != null ? comment.length() : 0));
            System.out.println("  - Recruiter ID: " + recruiterId);

            // Check if feedback exists
            var feedbacks = InterviewFeedbackService.getByInterviewId(selectedInterview.getId());
            boolean isUpdate = !feedbacks.isEmpty();

            System.out.println("Feedback Status: " + (isUpdate ? "UPDATE MODE" : "CREATE MODE"));

            InterviewFeedback fb;
            if (isUpdate) {
                fb = feedbacks.get(0);
                System.out.println("Existing Feedback ID: " + fb.getId());
                System.out.println("Current values in DB - Decision: " + fb.getDecision() + ", Score: " + fb.getOverallScore());
            } else {
                fb = new InterviewFeedback();
                System.out.println("Creating new feedback object");
            }

            // Set all fields
            fb.setInterviewId(selectedInterview.getId());
            fb.setRecruiterId(recruiterId);
            fb.setOverallScore(overallScore);
            fb.setDecision(decision);
            fb.setComment(comment);

            System.out.println("Updated object values - Decision: " + fb.getDecision() + ", Score: " + fb.getOverallScore());

            // Save to database
            if (isUpdate) {
                System.out.println("Calling InterviewFeedbackService.updateFeedback() with ID: " + fb.getId());
                InterviewFeedbackService.updateFeedback(fb.getId(), fb);
                System.out.println("✓ Update completed successfully");
                showAlert("Succes", "Retour mis a jour avec succes.", Alert.AlertType.INFORMATION);
            } else {
                System.out.println("Calling InterviewFeedbackService.addFeedback()");
                InterviewFeedbackService.addFeedback(fb);
                System.out.println("✓ Create completed successfully");
                showAlert("Succes", "Retour cree avec succes.", Alert.AlertType.INFORMATION);
            }

            // Send acceptance email if decision is ACCEPTED
            if ("ACCEPTED".equals(decision)) {
                sendAcceptanceEmail(selectedInterview);
            }

            System.out.println("============ FEEDBACK UPDATE COMPLETED ============\n");

            hideFeedbackPanel();
            loadInterviews();
        } catch (Exception e) {
            System.err.println("ERROR during feedback save: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Échec de la sauvegarde du retour: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleDeleteFeedback() {
        if (selectedInterview == null) return;

        var feedbacks = InterviewFeedbackService.getByInterviewId(selectedInterview.getId());
        if (!feedbacks.isEmpty()) {
            InterviewFeedback existing = feedbacks.get(0);

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Supprimer le Retour");
            confirm.setHeaderText("Supprimer ce retour?");
            confirm.setContentText("Cette action ne peut pas être annulée.");
            confirm.showAndWait().ifPresent(r -> {
                if (r == ButtonType.OK) {
                    InterviewFeedbackService.deleteFeedback(existing.getId());
                    showAlert("Succès", "Retour supprimé avec succès.", Alert.AlertType.INFORMATION);
                    hideFeedbackPanel();
                    loadInterviews();
                }
            });
        }
    }

    @FXML
    private void handleCancelFeedback() {
        hideFeedbackPanel();
    }

    private void sendAcceptanceEmail(Interview interview) {
        if (interview == null || interview.getApplicationId() == null) return;
        new Thread(() -> {
            try {
                // Fetch candidate email + name + job offer details
                String sql = """
                    SELECT u.email, u.first_name, u.last_name,
                           jo.title, jo.location, jo.contract_type, jo.description
                    FROM job_application ja
                    JOIN users    u  ON ja.candidate_id = u.id
                    JOIN job_offer jo ON ja.offer_id    = jo.id
                    WHERE ja.id = ?
                    """;
                try (java.sql.PreparedStatement ps =
                        Utils.MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
                    ps.setLong(1, interview.getApplicationId());
                    java.sql.ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        String email     = rs.getString("email");
                        String firstName = rs.getString("first_name");
                        String lastName  = rs.getString("last_name");
                        String fullName  = (firstName + " " + lastName).trim();
                        String jobTitle  = rs.getString("title");
                        String location  = rs.getString("location");
                        String contract  = rs.getString("contract_type");
                        String desc      = rs.getString("description");
                        if (email != null && !email.isBlank()) {
                            Services.interview.InterviewEmailService.sendAcceptanceNotification(
                                email, fullName, jobTitle, location, contract, desc);
                            System.out.println("[InterviewController] Acceptance email (Brevo) sent to: " + email);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[InterviewController] Failed to send acceptance email: " + e.getMessage());
            }
        }, "AcceptanceEmailThread").start();
    }

    private void hideFeedbackPanel() {
        if (feedbackPanel != null) {
            feedbackPanel.setVisible(false);
            feedbackPanel.setManaged(false);
        }
        // Show placeholder if edit dialog is also hidden
        if (rightPanelPlaceholder != null
                && (editDialog == null || !editDialog.isVisible())) {
            rightPanelPlaceholder.setVisible(true);
            rightPanelPlaceholder.setManaged(true);
        }
    }

    private int getEffectiveRecruiterIdForInterview(Interview interview) {
        // Use the recruiter_id already on the interview row.
        // When you add authentication later, replace with current user id.
        return interview != null ? interview.getRecruiterId().intValue() : 0;
    }

    private boolean checkIfFeedbackExists(Long interviewId) {
        try {
            return InterviewFeedbackService.existsForInterview(interviewId);
        } catch (Exception e) {
            System.err.println("Error checking feedback existence: " + e.getMessage());
            return false;
        }
    }

    private void showEditDialog(Interview interview) {
        if (editDialog != null) {
            isEditMode = interview != null;
            selectedInterview = interview;

            if (isEditMode) {
                // Fill form with existing data for update
                datePicker.setValue(interview.getScheduledAt().toLocalDate());
                txtTime.setText(interview.getScheduledAt().format(DateTimeFormatter.ofPattern("HH:mm")));
                txtDuration.setText(String.valueOf(interview.getDurationMinutes()));
                comboMode.setValue(interview.getMode());
                txtMeetingLink.setText(interview.getMeetingLink());
                txtLocation.setText(interview.getLocation());
                txtNotes.setText(interview.getNotes());
                btnSave.setText("Update Interview");
                toggleModeFields(interview.getMode()); // Set visibility based on mode
                System.out.println("Edit dialog opened for update - Interview ID: " + interview.getId());
            } else {
                // Clear form for new interview with some default values
                datePicker.setValue(LocalDate.now().plusDays(1));
                txtTime.setText("14:00"); // Default to 2 PM
                txtDuration.setText("60"); // Default to 60 minutes
                comboMode.setValue("ON_SITE"); // Default to ON_SITE (matches database enum)
                txtMeetingLink.setText("");
                txtLocation.setText("");
                txtNotes.setText("");
                btnSave.setText("Create Interview");
                toggleModeFields("ON_SITE"); // Set visibility for default mode
                System.out.println("Edit dialog opened for new interview");
            }

        editDialog.setVisible(true);
        editDialog.setManaged(true);
        // Hide placeholder
        if (rightPanelPlaceholder != null) {
            rightPanelPlaceholder.setVisible(false);
            rightPanelPlaceholder.setManaged(false);
        }
    }
    }

    private void hideEditDialog() {
        if (editDialog != null) {
            editDialog.setVisible(false);
            editDialog.setManaged(false);
            isEditMode = false;
        }
        // Show placeholder if feedback panel is also hidden
        if (rightPanelPlaceholder != null
                && (feedbackPanel == null || !feedbackPanel.isVisible())) {
            rightPanelPlaceholder.setVisible(true);
            rightPanelPlaceholder.setManaged(true);
        }
    }

    private void showBottomActionButtons() { /* replaced by right panel */ }
    private void hideBottomActionButtons() { /* replaced by right panel */ }

    @FXML
    private void handleUpdateInterview() {
        if (selectedInterview != null) {
            showRightPanel(selectedInterview);
        } else {
            showAlert("Attention", "Veuillez selectionner un entretien.", Alert.AlertType.WARNING);
        }
    }

    @FXML
    private void handleDeleteInterview() {
        if (selectedInterview == null) {
            showAlert("Attention", "Veuillez sélectionner un entretien à supprimer", Alert.AlertType.WARNING);
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmer la Suppression");
        confirmAlert.setHeaderText("Supprimer l'Entretien");
        confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer cet entretien? Cette action ne peut pas être annulée.");

        confirmAlert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    InterviewService.delete(selectedInterview.getId());
                    showAlert("Succes", "Entretien supprime avec succes !", Alert.AlertType.INFORMATION);
                    selectedInterview = null;
                    hideBottomActionButtons();
                    // Reset right panel to placeholder
                    if (rightPanelContent != null) { rightPanelContent.setVisible(false); rightPanelContent.setManaged(false); rightPanelContent.getChildren().clear(); }
                    if (rightPanelPlaceholder != null) { rightPanelPlaceholder.setVisible(true); rightPanelPlaceholder.setManaged(true); }
                    loadInterviews();
                } catch (Exception e) {
                    showAlert("Erreur", "Échec de la suppression de l'entretien: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private boolean validateInput() {
        System.out.println("Validating input...");

        // Check if form fields are properly initialized
        if (datePicker == null) {
            System.out.println("DatePicker is null!");
            showAlert("Erreur", "Formulaire non correctement initialisé. Veuillez réessayer.", Alert.AlertType.ERROR);
            return false;
        }

        if (datePicker.getValue() == null) {
            showAlert("Erreur de Validation", "Veuillez sélectionner une date", Alert.AlertType.WARNING);
            return false;
        }

        if (txtTime == null || txtTime.getText().trim().isEmpty()) {
            showAlert("Erreur de Validation", "Veuillez entrer une heure", Alert.AlertType.WARNING);
            return false;
        }

        try {
            LocalTime.parse(txtTime.getText().trim(), DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            showAlert("Erreur de Validation", "L'heure doit être au format HH:mm (ex: 14:30)", Alert.AlertType.WARNING);
            return false;
        }

        if (txtDuration == null || txtDuration.getText().trim().isEmpty()) {
            showAlert("Erreur de Validation", "Veuillez entrer la durée en minutes", Alert.AlertType.WARNING);
            return false;
        }

        try {
            int duration = Integer.parseInt(txtDuration.getText().trim());
            if (duration <= 0 || duration > 480) {
                showAlert("Erreur de Validation", "La durée doit être entre 1 et 480 minutes", Alert.AlertType.WARNING);
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Erreur de Validation", "La durée doit être un nombre valide", Alert.AlertType.WARNING);
            return false;
        }

        if (comboMode == null || comboMode.getValue() == null) {
            showAlert("Erreur de Validation", "Veuillez sélectionner un mode d'entretien", Alert.AlertType.WARNING);
            return false;
        }

        System.out.println("Validation passed successfully");
        return true;
    }

    private void highlightSelectedCard(VBox card) {
        // Reset all cards
        for (javafx.scene.Node node : interviewsListContainer.getChildren()) {
            if (node instanceof VBox) {
                node.getStyleClass().removeAll("card-selected");
            }
        }
        // Highlight selected
        card.getStyleClass().add("card-selected");
    }

    private String getStatusClass(String status) {
        if (status == null) return "status-scheduled";
        return switch (status.toUpperCase()) {
            case "COMPLETED" -> "status-completed";
            case "CANCELLED" -> "status-cancelled";
            case "RESCHEDULED" -> "status-pending";
            default -> "status-scheduled";
        };
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy - HH:mm"));
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Convert French display mode to database enum value
     */
    private String convertModeToDatabase(String displayMode) {
        if (displayMode == null) return "ONLINE";
        if ("En Ligne".equals(displayMode)) return "ONLINE";
        if ("Sur Site".equals(displayMode)) return "ON_SITE";
        // If already a database value, return as-is
        return displayMode;
    }

    /**
     * Convert database mode value to French display name
     */
    private String convertModeToDisplay(String dbMode) {
        if (dbMode == null) return "En Ligne";
        if ("ONLINE".equals(dbMode)) return "En Ligne";
        if ("ON_SITE".equals(dbMode)) return "Sur Site";
        // If already a display value, return as-is
        return dbMode;
    }

    /**
     * Convert French feedback decision to database value
     */
    private String convertDecisionToDatabase(String displayDecision) {
        if (displayDecision == null) return "ACCEPTED";
        if ("Accepté".equals(displayDecision)) return "ACCEPTED";
        if ("Rejeté".equals(displayDecision)) return "REJECTED";
        return displayDecision;
    }

    /**
     * Convert database decision value to French display name
     */
    private String convertDecisionToDisplay(String dbDecision) {
        if (dbDecision == null) return "Accepté";
        if ("ACCEPTED".equals(dbDecision)) return "Accepté";
        if ("REJECTED".equals(dbDecision)) return "Rejeté";
        return dbDecision;
    }

    @FXML
    private void handleGenerateMeetingLink() {
        try {
            if (datePicker.getValue() == null || txtTime.getText().trim().isEmpty()) {
                showAlert("Erreur", "Veuillez sélectionner une date et une heure avant de générer le lien.", Alert.AlertType.WARNING);
                return;
            }
            java.time.LocalTime time = java.time.LocalTime.parse(txtTime.getText().trim());
            LocalDateTime scheduledAt = LocalDateTime.of(datePicker.getValue(), time);
            int duration = 60;
            try { duration = Integer.parseInt(txtDuration.getText().trim()); } catch (Exception ignored) {}

            // Use applicationId from hidden field if available
            Long appId = null;
            if (txtApplicationId != null && txtApplicationId.getText() != null && !txtApplicationId.getText().isBlank()) {
                try { appId = Long.parseLong(txtApplicationId.getText().trim()); } catch (Exception ignored) {}
            }
            if (appId == null && selectedInterview != null) {
                appId = selectedInterview.getApplicationId();
            }
            if (appId == null) appId = 0L;

            String link = Services.interview.MeetingService.generateMeetingLink(appId, scheduledAt, duration);
            if (txtMeetingLink != null) {
                txtMeetingLink.setText(link);
                txtMeetingLink.setStyle("-fx-background-color: #d4edda; -fx-background-radius: 6;");
            }
            if (btnOpenMeetingLink != null) {
                btnOpenMeetingLink.setVisible(true);
                btnOpenMeetingLink.setManaged(true);
            }
        } catch (Exception e) {
            showAlert("Erreur", "Impossible de générer le lien : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleOpenMeetingLink() {
        if (txtMeetingLink == null || txtMeetingLink.getText().isBlank()) {
            showAlert("Erreur", "Aucun lien de réunion disponible.", Alert.AlertType.WARNING);
            return;
        }
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(txtMeetingLink.getText().trim()));
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir le lien : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
}
