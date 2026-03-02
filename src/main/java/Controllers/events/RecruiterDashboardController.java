package Controllers.events;

import Models.events.*;
import Models.user.Recruiter;
import Models.user.User;
import Services.user.EmailService;
import Services.events.*;
import Utils.SchemaFixer;
import Utils.Session;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.Node;
import javafx.geometry.Pos;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class RecruiterDashboardController implements Initializable {

    @FXML
    private HBox eventsHBox;

    // Attendees Table
    @FXML
    private TableView<EventRegistration> attendeesTable;
    @FXML
    private TableColumn<EventRegistration, String> eventTitleCol;
    @FXML
    private TableColumn<EventRegistration, String> candLastNameCol;
    @FXML
    private TableColumn<EventRegistration, String> candFirstNameCol;
    @FXML
    private TableColumn<EventRegistration, String> candEmailCol;
    @FXML
    private TableColumn<EventRegistration, String> statusCol;
    @FXML
    private TableColumn<EventRegistration, LocalDateTime> regDateCol;

    @FXML
    private ComboBox<AttendanceStatusEnum> registrationStatusCombo; // kept for backward compat, unused

    @FXML private Button acceptBtn;
    @FXML private Button rejectBtn;

    @FXML private TextField titleField;
    @FXML private TextArea descriptionField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField locationField;
    @FXML private DatePicker dateField;
    @FXML private TextField capacityField;
    @FXML private TextField meetLinkField;
    @FXML private Label titleErrorLabel;
    @FXML private Label typeErrorLabel;
    @FXML private Label locationErrorLabel;
    @FXML private Label dateErrorLabel;
    @FXML private Label capacityErrorLabel;
    @FXML private Label descriptionErrorLabel;
    @FXML private Label meetLinkErrorLabel;
    
    // Search fields
    @FXML private TextField recruiterSearchField;
    @FXML private ComboBox<String> recruiterTypeFilter;

    // Attendees search / filter / sort
    @FXML private TextField attendeesSearchField;
    @FXML private ComboBox<String> attendeesEventFilter;
    @FXML private ComboBox<String> attendeesStatusFilter;
    @FXML private ComboBox<String> attendeesSortCombo;
    @FXML private Label attendeesCountLabel;

    // Statistics Labels
    @FXML private Label totalStatsLabel;
    @FXML private Label confirmedStatsLabel;
    @FXML private Label pendingStatsLabel;
    @FXML private Label rejectedStatsLabel;
    @FXML private Label cancelledStatsLabel;

    @FXML private VBox eventsView;
    @FXML private VBox interviewsView;
    @FXML private Button tabEventsBtn;
    @FXML private Button tabSubscriptionsBtn;

    // Split-pane event list (left)
    @FXML private VBox  eventsListBox;
    @FXML private Label eventsCountLabel;

    // Right detail panel
    @FXML private VBox  rightPlaceholder;
    @FXML private VBox  rightDetail;
    @FXML private Label detailTypeBadge;
    @FXML private Label detailPastBadge;
    @FXML private Label detailDateLabel;
    @FXML private Label detailTitleLabel;
    @FXML private Label detailLocationLabel;
    @FXML private Label detailCapacityLabel;
    @FXML private Label detailDescLabel;

    // Reviews panel (inside detail, past events only)
    @FXML private VBox  reviewsPanelInDetail;
    @FXML private Label reviewsAvgScore;
    @FXML private Label reviewsStarsLabel;
    @FXML private Label reviewsTotalLabel;
    @FXML private VBox  reviewsCardsBox;
    @FXML private Label reviewsEmptyLabel;

    // New event form
    @FXML private VBox     newEventForm;
    @FXML private TextField newTitleField;
    @FXML private ComboBox<String> newTypeCombo;
    @FXML private TextField newLocationField;
    @FXML private DatePicker newDateField;
    @FXML private TextField newCapacityField;
    @FXML private TextArea  newDescriptionField;
    @FXML private Label     newFormErrorLabel;

    // Keep these for backward compat (unused in new UI but referenced elsewhere)
    @FXML private Button eventsBtn;
    @FXML private Button interviewsBtn;

    @FXML
    private Label userNameLabel;
    @FXML
    private Label userRoleLabel;

    private Services.events.EventRegistrationService registrationService;
    private RecruitmentEventService eventService;
    private RecruiterService recruiterService;
    private UserService userService;
    private final Services.events.ReviewService reviewService = new Services.events.ReviewService();
    private EventRecruiter currentRecruiter;
    private RecruitmentEvent selectedEvent;
    private ObservableList<EventRegistration> masterRegistrationList = FXCollections.observableArrayList();

    public RecruiterDashboardController() {
        eventService = new RecruitmentEventService();
        recruiterService = new RecruiterService();
        registrationService = new Services.events.EventRegistrationService();
        userService = new UserService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SchemaFixer.main(null);

        // Ensure only eventsView is shown at startup
        if (eventsView != null)     { eventsView.setVisible(true);  eventsView.setManaged(true); }
        if (interviewsView != null) { interviewsView.setVisible(false); interviewsView.setManaged(false); }

        if (typeCombo != null) setupComboBoxes();
        if (attendeesTable != null) setupAttendeesTable();
        loadRecruiterData();
    }

    private void setupComboBoxes() {
        typeCombo.setItems(FXCollections.observableArrayList("Job_Faire", "WEBINAIRE", "Interview day"));
        if (registrationStatusCombo != null)
            registrationStatusCombo.setItems(FXCollections.observableArrayList(AttendanceStatusEnum.values()));

        // Show/hide meet link field when event type changes
        typeCombo.setOnAction(e -> {
            if (meetLinkField != null) {
                boolean isWebinaire = "WEBINAIRE".equals(typeCombo.getValue());
                meetLinkField.setVisible(isWebinaire);
                meetLinkField.setManaged(isWebinaire);
                if (meetLinkErrorLabel != null) {
                    meetLinkErrorLabel.setVisible(isWebinaire);
                    meetLinkErrorLabel.setManaged(isWebinaire);
                }
            }
        });
        
        if (recruiterTypeFilter != null) {
            recruiterTypeFilter.setItems(FXCollections.observableArrayList("Tous les types", "Job_Faire", "WEBINAIRE", "Interview day"));
            recruiterTypeFilter.setValue("Tous les types");
        }

        if (attendeesStatusFilter != null) {
            attendeesStatusFilter.setItems(FXCollections.observableArrayList(
                "Tous les statuts", "En attente", "Confirmé", "Rejeté", "Annulé"));
        }
        if (attendeesSortCombo != null) {
            attendeesSortCombo.setItems(FXCollections.observableArrayList(
                "Date (plus récent)", "Date (plus ancien)", "Nom A→Z", "Nom Z→A", "Statut"));
        }
    }

    private void setupAttendeesTable() {
        if (eventTitleCol != null) eventTitleCol.setCellValueFactory(cellData -> {
            if (cellData.getValue().getEvent() != null)
                return new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEvent().getTitle());
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });
        if (candLastNameCol != null) candLastNameCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getLastName()));
        if (candFirstNameCol != null) candFirstNameCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFirstName()));
        if (candEmailCol != null) candEmailCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEmail()));

        // Status cell — colored badge
        if (statusCol != null) {
            statusCol.setCellValueFactory(cellData -> {
                AttendanceStatusEnum s = cellData.getValue().getAttendanceStatus();
                return new javafx.beans.property.SimpleStringProperty(s != null ? s.name() : "");
            });
            statusCol.setCellFactory(col -> new TableCell<EventRegistration, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) { setGraphic(null); setText(null); return; }
                    String icon, label, style;
                    switch (item) {
                        case "CONFIRMED":
                            icon = "✅"; label = "Confirmé";
                            style = "-fx-background-color:#DCFCE7; -fx-text-fill:#16A34A;";
                            break;
                        case "REJECTED":
                            icon = "🚫"; label = "Rejeté";
                            style = "-fx-background-color:#FFEDD5; -fx-text-fill:#C2410C;";
                            break;
                        case "CANCELLED":
                            icon = "↩"; label = "Annulé";
                            style = "-fx-background-color:#F1F5F9; -fx-text-fill:#64748B;";
                            break;
                        default: // PENDING / REGISTERED
                            icon = "⏳"; label = "En attente";
                            style = "-fx-background-color:#FEF9C3; -fx-text-fill:#CA8A04;";
                            break;
                    }
                    javafx.scene.control.Label badge = new javafx.scene.control.Label(icon + "  " + label);
                    badge.setStyle(style + " -fx-font-weight:700; -fx-background-radius:20; -fx-padding:3 12; -fx-font-size:11px;");
                    setGraphic(badge);
                    setText(null);
                    setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                }
            });
        }

        // Date cell
        if (regDateCol != null) {
            regDateCol.setCellValueFactory(new PropertyValueFactory<>("registeredAt"));
            regDateCol.setCellFactory(col -> new TableCell<EventRegistration, LocalDateTime>() {
                private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                @Override
                protected void updateItem(LocalDateTime item, boolean empty) {
                    super.updateItem(item, empty);
                    setText((item == null || empty) ? null : fmt.format(item));
                }
            });
        }

        if (attendeesTable != null) {
            // Set constrained resize policy programmatically (avoids FXML parse issue)
            attendeesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            // Row factory — color rows by status
            attendeesTable.setRowFactory(tv -> new TableRow<EventRegistration>() {
                @Override
                protected void updateItem(EventRegistration item, boolean empty) {
                    super.updateItem(item, empty);
                    setTooltip(null);
                    if (item == null || empty) {
                        setStyle("");
                        return;
                    }
                    AttendanceStatusEnum status = item.getAttendanceStatus();
                    if (status == AttendanceStatusEnum.CONFIRMED) {
                        setStyle("-fx-background-color:#F0FDF4;");
                        setTooltip(new Tooltip("Inscription confirmee"));
                    } else if (status == AttendanceStatusEnum.REJECTED) {
                        setStyle("-fx-background-color:#FFF7ED;");
                        setTooltip(new Tooltip("Inscription rejetee par le recruteur - modifiable"));
                    } else if (status == AttendanceStatusEnum.CANCELLED) {
                        setStyle("-fx-background-color:#F8FAFC; -fx-opacity:0.75;");
                        setTooltip(new Tooltip("Annule par le candidat - non modifiable"));
                    } else {
                        // PENDING / REGISTERED
                        setStyle("-fx-background-color:#FFFBEB;");
                        setTooltip(new Tooltip("En attente de decision"));
                    }
                }
            });

            // Selection listener — enable/disable buttons
            attendeesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null) {
                    setButtonState(acceptBtn, false, true);
                    setButtonState(rejectBtn, false, false);
                    return;
                }
                AttendanceStatusEnum s = newVal.getAttendanceStatus();
                // CANCELLED (by candidate) is locked — recruiter cannot touch it
                boolean locked = (s == AttendanceStatusEnum.CANCELLED);
                // Accept: available when not already CONFIRMED and not locked
                boolean canAccept = !locked && s != AttendanceStatusEnum.CONFIRMED;
                // Reject: available when not already REJECTED and not locked
                boolean canReject = !locked && s != AttendanceStatusEnum.REJECTED;
                setButtonState(acceptBtn, canAccept, true);
                setButtonState(rejectBtn, canReject, false);
            });
        }
    }

    private void setupTable() {
        // No longer using TableView setup
    }

    private void updateStatistics() {
        if (masterRegistrationList == null) return;
        long total     = masterRegistrationList.size();
        long confirmed = masterRegistrationList.stream().filter(r -> r.getAttendanceStatus() == AttendanceStatusEnum.CONFIRMED).count();
        long pending   = masterRegistrationList.stream().filter(r -> r.getAttendanceStatus() == AttendanceStatusEnum.PENDING || r.getAttendanceStatus() == AttendanceStatusEnum.REGISTERED).count();
        long rejected  = masterRegistrationList.stream().filter(r -> r.getAttendanceStatus() == AttendanceStatusEnum.REJECTED).count();
        long cancelled = masterRegistrationList.stream().filter(r -> r.getAttendanceStatus() == AttendanceStatusEnum.CANCELLED).count();
        if (totalStatsLabel     != null) totalStatsLabel.setText(String.valueOf(total));
        if (confirmedStatsLabel != null) confirmedStatsLabel.setText(String.valueOf(confirmed));
        if (pendingStatsLabel   != null) pendingStatsLabel.setText(String.valueOf(pending));
        if (rejectedStatsLabel  != null) rejectedStatsLabel.setText(String.valueOf(rejected));
        if (cancelledStatsLabel != null) cancelledStatsLabel.setText(String.valueOf(cancelled));
    }

    private boolean isUrgent(EventRegistration registration) {
        if (registration == null || registration.getEvent() == null) return false;
        AttendanceStatusEnum s = registration.getAttendanceStatus();
        boolean isPending = s == AttendanceStatusEnum.PENDING || s == AttendanceStatusEnum.REGISTERED;
        if (!isPending) return false;
        LocalDateTime eventDate = registration.getEvent().getEventDate();
        if (eventDate == null) return false;
        LocalDateTime now = LocalDateTime.now();
        return eventDate.isAfter(now) && eventDate.isBefore(now.plusHours(48));
    }

    private void checkForUrgentRegistrations() {
        if (currentRecruiter == null) return;

        try {
            // Load all registrations for all the recruiter's events directly from DB
            java.util.List<RecruitmentEvent> events = eventService.getByRecruiter(currentRecruiter.getId());
            java.util.List<EventRegistration> allRegs = new java.util.ArrayList<>();
            for (RecruitmentEvent ev : events) {
                allRegs.addAll(registrationService.getByEvent(ev.getId()));
            }

            long imminentCount = allRegs.stream()
                    .filter(r -> r.getAttendanceStatus() == AttendanceStatusEnum.PENDING
                              && r.getEvent() != null
                              && r.getEvent().getEventDate() != null)
                    .filter(r -> {
                        long h = java.time.temporal.ChronoUnit.HOURS
                                .between(LocalDateTime.now(), r.getEvent().getEventDate());
                        return h >= 0 && h <= 48;
                    })
                    .count();

            long overdueCount = allRegs.stream()
                    .filter(r -> r.getAttendanceStatus() == AttendanceStatusEnum.PENDING
                              && r.getEvent() != null
                              && r.getEvent().getEventDate() != null
                              && r.getEvent().getEventDate().isBefore(LocalDateTime.now()))
                    .count();

            if (overdueCount > 0) {
                Services.joboffers.NotificationService.showWarning(
                    "\u26a0\ufe0f Inscriptions en retard",
                    overdueCount + " candidat(s) en attente pour des \u00e9v\u00e9nements d\u00e9j\u00e0 pass\u00e9s !");
            } else if (imminentCount > 0) {
                Services.joboffers.NotificationService.showWarning(
                    "\u26a0\ufe0f \u00c9v\u00e9nements imminents",
                    imminentCount + " inscription(s) PENDING pour des \u00e9v\u00e9nements dans moins de 48h !");
            }
        } catch (Exception e) {
            System.err.println("[RecruiterDashboard] checkForUrgentRegistrations error: " + e.getMessage());
        }
    }

    private void loadRecruiterData() {
        try {
            Long contextId = Utils.UserContext.getRecruiterId();
            if (contextId == null) {
                System.err.println("[RecruiterDashboard] No recruiter in session.");
                if (eventsListBox != null) {
                    eventsListBox.getChildren().clear();
                    Label msg = new Label("Veuillez vous connecter en tant que recruteur.");
                    msg.setStyle("-fx-font-size:13px; -fx-text-fill:#dc3545; -fx-padding:20;");
                    eventsListBox.getChildren().add(msg);
                }
                return;
            }
            System.out.println("[RecruiterDashboard] Loading data for recruiter id=" + contextId);
            currentRecruiter = recruiterService.getByUserId(contextId);

            if (currentRecruiter == null) {
                System.err.println("[RecruiterDashboard] Recruiter profile not found for id=" + contextId);
                // Try to create a minimal profile from the session user
                Models.user.User sessionUser = Utils.Session.getCurrentUser();
                if (sessionUser instanceof Models.user.Recruiter sr) {
                    currentRecruiter = new EventRecruiter();
                    currentRecruiter.setId(contextId);
                    currentRecruiter.setCompanyName(sr.getCompanyName() != null ? sr.getCompanyName() : "");
                    currentRecruiter.setCompanyLocation(sr.getCompanyLocation() != null ? sr.getCompanyLocation() : "");
                    currentRecruiter.setCompanyDescription("");
                    try { recruiterService.add(currentRecruiter); } catch (Exception ignored) {
                        currentRecruiter = recruiterService.getByUserId(contextId);
                    }
                }
            }

            if (currentRecruiter != null) {
                refreshTable();
                checkForUrgentRegistrations();
                // Update top-bar labels if present
                Models.user.User sessionUser = Utils.Session.getCurrentUser();
                if (sessionUser != null) {
                    String displayName = (sessionUser.getFirstName() != null ? sessionUser.getFirstName() : "")
                            + (sessionUser.getLastName() != null ? " " + sessionUser.getLastName() : "");
                    if (userNameLabel != null) userNameLabel.setText(displayName.trim());
                    if (userRoleLabel != null) {
                        userRoleLabel.setText("RECRUTEUR");
                        userRoleLabel.getStyleClass().add("badge-recruiter");
                    }
                }
            } else {
                if (eventsListBox != null) {
                    eventsListBox.getChildren().clear();
                    Label msg = new Label("Profil recruteur introuvable.");
                    msg.setStyle("-fx-font-size:13px; -fx-text-fill:#dc3545; -fx-padding:20;");
                    eventsListBox.getChildren().add(msg);
                }
            }
        } catch (SQLException e) {
            System.err.println("[RecruiterDashboard] loadRecruiterData error: " + e.getMessage());
        }
    }

    @FXML
    private void refreshTable() {
        if (currentRecruiter == null) {
            System.out.println("[RecruiterDashboard] refreshTable: currentRecruiter is NULL, skipping.");
            return;
        }
        try {
            System.out.println("[RecruiterDashboard] refreshTable: querying events for recruiter id=" + currentRecruiter.getId());
            java.util.List<RecruitmentEvent> events = eventService.getByRecruiter(currentRecruiter.getId());
            System.out.println("[RecruiterDashboard] refreshTable: found " + events.size() + " events");
            // populate split-pane left list if present, else fall back to old HBox
            if (eventsListBox != null) {
                eventsListBox.getChildren().clear();
                if (eventsCountLabel != null) eventsCountLabel.setText(events.size() + " evenement(s)");
                for (RecruitmentEvent event : events) {
                    eventsListBox.getChildren().add(createEventCardNew(event));
                }
                if (events.isEmpty()) {
                    Label empty = new Label("Aucun evenement trouve.");
                    empty.setStyle("-fx-font-size:13px; -fx-text-fill:#94A3B8; -fx-padding:20;");
                    eventsListBox.getChildren().add(empty);
                }
            }
        } catch (SQLException e) {
            showAlert("Erreur Chargement", e.getMessage());
        }
    }

    /** New split-pane style card (left panel). */
    private Node createEventCardNew(RecruitmentEvent event) {
        boolean isPast = event.getEventDate() != null && event.getEventDate().isBefore(LocalDateTime.now());
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color:white; -fx-background-radius:12;" +
                      "-fx-border-color:#E4EBF5; -fx-border-width:1; -fx-border-radius:12;" +
                      "-fx-padding:12 14; -fx-cursor:hand;");

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // type badge
        String[] typeStyle = typeColors(event.getEventType());
        Label typeBadge = new Label(event.getEventType() != null ? event.getEventType() : "");
        typeBadge.setStyle("-fx-background-color:" + typeStyle[0] + "; -fx-text-fill:" + typeStyle[1] + ";" +
                           "-fx-font-size:10px; -fx-font-weight:700; -fx-padding:2 8; -fx-background-radius:20;");

        javafx.scene.layout.Region sp = new javafx.scene.layout.Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label dateLbl = new Label(event.getEventDate() != null
                ? event.getEventDate().format(DateTimeFormatter.ofPattern("dd/MM/yy")) : "");
        dateLbl.setStyle("-fx-font-size:10px; -fx-text-fill:#94A3B8;");

        topRow.getChildren().addAll(typeBadge, sp, dateLbl);
        if (isPast) {
            Label pastBadge = new Label("PASSE");
            pastBadge.setStyle("-fx-background-color:#F1F5F9; -fx-text-fill:#64748B;" +
                               "-fx-font-size:9px; -fx-font-weight:700; -fx-padding:2 6; -fx-background-radius:20;");
            topRow.getChildren().add(0, pastBadge);
        }

        Label titleLbl = new Label(event.getTitle());
        titleLbl.setStyle("-fx-font-size:13px; -fx-font-weight:700; -fx-text-fill:#1E293B;");
        titleLbl.setWrapText(true);

        Label locLbl = new Label("📍 " + (event.getLocation() != null ? event.getLocation() : ""));
        locLbl.setStyle("-fx-font-size:11px; -fx-text-fill:#475569;");

        card.getChildren().addAll(topRow, titleLbl, locLbl);

        // hover
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle()
                .replace("-fx-background-color:white;", "-fx-background-color:#F0F6FF;")
                .replace("-fx-border-color:#E4EBF5;", "-fx-border-color:#BBDEFB;")));
        card.setOnMouseExited(e -> {
            if (selectedEvent == null || selectedEvent.getId() != event.getId())
                resetCardStyle(card);
        });
        card.setOnMouseClicked(e -> selectEventCard(event, card));
        return card;
    }

    private void resetCardStyle(VBox card) {
        card.setStyle("-fx-background-color:white; -fx-background-radius:12;" +
                      "-fx-border-color:#E4EBF5; -fx-border-width:1; -fx-border-radius:12;" +
                      "-fx-padding:12 14; -fx-cursor:hand;");
    }

    private void selectEventCard(RecruitmentEvent event, VBox clickedCard) {
        // Reset all cards
        if (eventsListBox != null) {
            for (Node n : eventsListBox.getChildren()) {
                if (n instanceof VBox c) resetCardStyle(c);
            }
        }
        // Highlight selected
        clickedCard.setStyle("-fx-background-color:#EBF3FF; -fx-background-radius:12;" +
                             "-fx-border-color:#1565C0; -fx-border-width:2; -fx-border-radius:12;" +
                             "-fx-padding:12 14; -fx-cursor:hand;");
        selectedEvent = event;
        populateForm(event);
        showRightDetail(event);
    }

    private void showRightDetail(RecruitmentEvent event) {
        if (rightPlaceholder != null) { rightPlaceholder.setVisible(false); rightPlaceholder.setManaged(false); }
        if (newEventForm != null)     { newEventForm.setVisible(false);     newEventForm.setManaged(false); }
        if (rightDetail != null)      { rightDetail.setVisible(true);       rightDetail.setManaged(true); }

        boolean isPast = event.getEventDate() != null && event.getEventDate().isBefore(LocalDateTime.now());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        if (detailTypeBadge != null) {
            String[] tc = typeColors(event.getEventType());
            detailTypeBadge.setText(event.getEventType() != null ? event.getEventType() : "");
            detailTypeBadge.setStyle("-fx-background-color:" + tc[0] + "; -fx-text-fill:" + tc[1] + ";" +
                    "-fx-font-size:11px; -fx-font-weight:700; -fx-padding:3 10; -fx-background-radius:20;");
        }
        if (detailPastBadge != null) {
            detailPastBadge.setVisible(isPast); detailPastBadge.setManaged(isPast);
        }
        if (detailDateLabel   != null) detailDateLabel.setText(event.getEventDate() != null ? event.getEventDate().format(fmt) : "");
        if (detailTitleLabel  != null) detailTitleLabel.setText(event.getTitle());
        if (detailLocationLabel != null) detailLocationLabel.setText("📍 " + (event.getLocation() != null ? event.getLocation() : ""));
        if (detailCapacityLabel != null) detailCapacityLabel.setText("👥 " + event.getCapacity() + " places");
        if (detailDescLabel   != null) detailDescLabel.setText(event.getDescription() != null ? event.getDescription() : "");

        // Reviews panel — only for past events
        if (reviewsPanelInDetail != null) {
            reviewsPanelInDetail.setVisible(isPast); reviewsPanelInDetail.setManaged(isPast);
            if (isPast) loadReviewsInDetailPanel(event);
        }
    }

    private void loadReviewsInDetailPanel(RecruitmentEvent ev) {
        if (reviewsCardsBox == null) return;
        try {
            java.util.List<Models.events.EventReview> reviews = reviewService.getByEvent(ev.getId());
            reviewsCardsBox.getChildren().clear();

            boolean empty = reviews.isEmpty();
            if (reviewsEmptyLabel != null) { reviewsEmptyLabel.setVisible(empty); reviewsEmptyLabel.setManaged(empty); }

            if (empty) {
                if (reviewsAvgScore  != null) reviewsAvgScore.setText("—");
                if (reviewsStarsLabel!= null) reviewsStarsLabel.setText("");
                if (reviewsTotalLabel!= null) reviewsTotalLabel.setText("Aucun avis");
                return;
            }

            double avg = reviews.stream().mapToInt(Models.events.EventReview::getRating).average().orElse(0);
            if (reviewsAvgScore  != null) reviewsAvgScore.setText(String.format("%.1f", avg));
            int rounded = (int) Math.round(avg);
            if (reviewsStarsLabel!= null) reviewsStarsLabel.setText("★".repeat(rounded) + "☆".repeat(5 - rounded));
            if (reviewsTotalLabel!= null) reviewsTotalLabel.setText(reviews.size() + " avis");

            for (Models.events.EventReview r : reviews) {
                VBox card = new VBox(4);
                card.setStyle("-fx-background-color:#FFFBEB; -fx-background-radius:10;" +
                              "-fx-border-color:#FDE68A; -fx-border-width:1; -fx-border-radius:10;" +
                              "-fx-padding:10 14;");
                String cname = r.getCandidateName() != null && !r.getCandidateName().isBlank()
                        ? r.getCandidateName() : "Candidat";
                String stars = "★".repeat(r.getRating()) + "☆".repeat(5 - r.getRating());
                String rdate = r.getCreatedAt() != null
                        ? r.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
                HBox hdr = new HBox(8);
                hdr.setAlignment(Pos.CENTER_LEFT);
                Label nameLbl = new Label(cname);
                nameLbl.setStyle("-fx-font-size:12px; -fx-font-weight:700; -fx-text-fill:#1E293B;");
                Label starsLbl = new Label(stars);
                starsLbl.setStyle("-fx-font-size:13px; -fx-text-fill:#F59E0B;");
                javafx.scene.layout.Region sp = new javafx.scene.layout.Region();
                HBox.setHgrow(sp, Priority.ALWAYS);
                Label dLbl = new Label(rdate);
                dLbl.setStyle("-fx-font-size:10px; -fx-text-fill:#94A3B8;");
                hdr.getChildren().addAll(nameLbl, starsLbl, sp, dLbl);
                card.getChildren().add(hdr);
                if (r.getComment() != null && !r.getComment().isBlank()) {
                    Label comm = new Label(r.getComment());
                    comm.setWrapText(true);
                    comm.setStyle("-fx-font-size:12px; -fx-text-fill:#475569;");
                    card.getChildren().add(comm);
                }
                reviewsCardsBox.getChildren().add(card);
            }
        } catch (SQLException e) {
            System.err.println("Error loading reviews: " + e.getMessage());
        }
    }

    private String[] typeColors(String type) {
        if (type == null) return new String[]{"#F1F5F9", "#64748B"};
        return switch (type) {
            case "WEBINAIRE"     -> new String[]{"#EBF3FF", "#1565C0"};
            case "Interview day" -> new String[]{"#F0FDF4", "#15803D"};
            default              -> new String[]{"#FFF7ED", "#C2410C"};
        };
    }

    @FXML
    private void handleNewEvent() {
        // Show new event form, hide detail
        selectedEvent = null;
        if (rightPlaceholder != null) { rightPlaceholder.setVisible(false); rightPlaceholder.setManaged(false); }
        if (rightDetail      != null) { rightDetail.setVisible(false);      rightDetail.setManaged(false); }
        if (newEventForm     != null) { newEventForm.setVisible(true);       newEventForm.setManaged(true); }
        // Reset left card selection
        if (eventsListBox != null)
            for (Node n : eventsListBox.getChildren()) if (n instanceof VBox c) resetCardStyle(c);
        // Setup combos for new form
        if (newTypeCombo != null && newTypeCombo.getItems().isEmpty())
            newTypeCombo.getItems().addAll("Job_Faire", "WEBINAIRE", "Interview day");
    }

    @FXML
    private void handleCancelNewEvent() {
        if (newEventForm     != null) { newEventForm.setVisible(false);      newEventForm.setManaged(false); }
        if (rightPlaceholder != null) { rightPlaceholder.setVisible(true);   rightPlaceholder.setManaged(true); }
    }

    @FXML
    private void handleClear() {
        clearForm();
        selectedEvent = null;
        if (eventsListBox != null) for (Node n : eventsListBox.getChildren()) if (n instanceof VBox c) resetCardStyle(c);
        if (rightDetail      != null) { rightDetail.setVisible(false);      rightDetail.setManaged(false); }
        if (newEventForm     != null) { newEventForm.setVisible(false);      newEventForm.setManaged(false); }
        if (rightPlaceholder != null) { rightPlaceholder.setVisible(true);   rightPlaceholder.setManaged(true); }
    }

    @FXML
    private void handleUpdate() {
        if (selectedEvent == null) {
            showAlert("Avertissement", "Veuillez sélectionner un événement en cliquant sur sa carte.");
            return;
        }
        if (!validateForm()) return;
        try {
            updateEventFromForm(selectedEvent);
            eventService.update(selectedEvent);
            refreshTable();
            // re-select the updated event
            showRightDetail(selectedEvent);
        } catch (SQLException e) {
            showAlert("Erreur Modification", e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedEvent == null) {
            showAlert("Avertissement", "Veuillez sélectionner un événement en cliquant sur sa carte.");
            return;
        }
        try {
            eventService.delete(selectedEvent.getId());
            selectedEvent = null;
            refreshTable();
            if (rightDetail      != null) { rightDetail.setVisible(false);     rightDetail.setManaged(false); }
            if (rightPlaceholder != null) { rightPlaceholder.setVisible(true);  rightPlaceholder.setManaged(true); }
        } catch (SQLException e) {
            showAlert("Erreur Suppression", e.getMessage());
        }
    }

    @FXML
    private void handleAddNew() {
        if (currentRecruiter == null) { showAlert("Erreur", "Profil recruteur non chargé."); return; }
        String title    = newTitleField    != null ? newTitleField.getText()    : "";
        String location = newLocationField != null ? newLocationField.getText() : "";
        String capStr   = newCapacityField != null ? newCapacityField.getText() : "0";
        String desc     = newDescriptionField != null ? newDescriptionField.getText() : "";
        String type     = newTypeCombo != null ? newTypeCombo.getValue() : null;
        java.time.LocalDate date = newDateField != null ? newDateField.getValue() : null;

        if (title.isBlank() || type == null || location.isBlank() || date == null) {
            if (newFormErrorLabel != null) {
                newFormErrorLabel.setText("Titre, type, lieu et date sont obligatoires.");
                newFormErrorLabel.setVisible(true); newFormErrorLabel.setManaged(true);
            }
            return;
        }
        try {
            int cap = Integer.parseInt(capStr.isBlank() ? "0" : capStr);
            RecruitmentEvent ev = new RecruitmentEvent();
            ev.setRecruiterId(currentRecruiter.getId());
            ev.setTitle(title); ev.setEventType(type); ev.setLocation(location);
            ev.setEventDate(date.atStartOfDay()); ev.setCapacity(cap); ev.setDescription(desc);
            ev.setCreatedAt(LocalDateTime.now());
            eventService.add(ev);
            // clear form
            if (newTitleField != null) newTitleField.clear();
            if (newLocationField != null) newLocationField.clear();
            if (newCapacityField != null) newCapacityField.clear();
            if (newDescriptionField != null) newDescriptionField.clear();
            if (newDateField != null) newDateField.setValue(null);
            if (newTypeCombo != null) newTypeCombo.setValue(null);
            if (newFormErrorLabel != null) { newFormErrorLabel.setVisible(false); newFormErrorLabel.setManaged(false); }
            handleCancelNewEvent();
            refreshTable();
        } catch (NumberFormatException e) {
            if (newFormErrorLabel != null) {
                newFormErrorLabel.setText("Capacite invalide.");
                newFormErrorLabel.setVisible(true); newFormErrorLabel.setManaged(true);
            }
        } catch (SQLException e) {
            showAlert("Erreur Ajout", e.getMessage());
        }
    }

    /** Load registrations for a specific event into the attendees table. */
    private void refreshAttendees(long eventId) {
        try {
            java.util.List<EventRegistration> regs = registrationService.getByEvent(eventId);
            masterRegistrationList.setAll(regs);
            populateAttendeesEventFilter();
            applyAttendeesFilter();
            updateStatistics();
        } catch (SQLException e) {
            showAlert("Erreur Chargement", e.getMessage());
        }
    }
    /** Load all registrations for all recruiter events into the attendees table. */
    private void refreshAllAttendees() {
        if (currentRecruiter == null) return;
        try {
            java.util.List<RecruitmentEvent> events = eventService.getByRecruiter(currentRecruiter.getId());
            java.util.List<EventRegistration> all = new java.util.ArrayList<>();
            for (RecruitmentEvent ev : events) {
                all.addAll(registrationService.getByEvent(ev.getId()));
            }
            masterRegistrationList.setAll(all);
            populateAttendeesEventFilter();
            applyAttendeesFilter();
            updateStatistics();
        } catch (SQLException e) {
            showAlert("Erreur Chargement", e.getMessage());
        }
    }

    @FXML
    private void handleRecruiterSearch() {
        String query = recruiterSearchField != null ? recruiterSearchField.getText().toLowerCase() : "";
        String typeFilter = recruiterTypeFilter != null ? recruiterTypeFilter.getValue() : "Tous les types";
        if (currentRecruiter == null) return;
        try {
            java.util.List<RecruitmentEvent> allEvents = eventService.getByRecruiter(currentRecruiter.getId());
            java.util.List<RecruitmentEvent> filtered = new java.util.ArrayList<>();
            for (RecruitmentEvent event : allEvents) {
                boolean matchesTitle = query.isEmpty() || event.getTitle().toLowerCase().contains(query);
                boolean matchesType  = "Tous les types".equals(typeFilter) || typeFilter == null
                        || event.getEventType().equals(typeFilter);
                if (matchesTitle && matchesType) filtered.add(event);
            }
            if (eventsListBox != null) {
                eventsListBox.getChildren().clear();
                if (eventsCountLabel != null) eventsCountLabel.setText(filtered.size() + " evenement(s)");
                for (RecruitmentEvent ev : filtered) eventsListBox.getChildren().add(createEventCardNew(ev));
            }
        } catch (SQLException e) {
            showAlert("Erreur Chargement", e.getMessage());
        }
    }

    private void populateAttendeesEventFilter() {
        if (attendeesEventFilter == null) return;
        java.util.Set<String> titles = new java.util.LinkedHashSet<>();
        titles.add("Tous les événements");
        for (EventRegistration r : masterRegistrationList) {
            if (r.getEvent() != null && r.getEvent().getTitle() != null) {
                titles.add(r.getEvent().getTitle());
            }
        }
        String current = attendeesEventFilter.getValue();
        attendeesEventFilter.setItems(FXCollections.observableArrayList(titles));
        if (current != null && titles.contains(current)) attendeesEventFilter.setValue(current);
        else attendeesEventFilter.setValue("Tous les événements");
    }

    private void applyAttendeesFilter() {
        if (attendeesTable == null) return;

        String query = attendeesSearchField != null ? attendeesSearchField.getText().toLowerCase().trim() : "";
        String eventFilter = attendeesEventFilter != null ? attendeesEventFilter.getValue() : "Tous les événements";
        String statusFilter = attendeesStatusFilter != null ? attendeesStatusFilter.getValue() : "Tous les statuts";
        String sortBy = attendeesSortCombo != null ? attendeesSortCombo.getValue() : null;

        java.util.List<EventRegistration> filtered = new java.util.ArrayList<>();
        for (EventRegistration r : masterRegistrationList) {
            boolean matchesQuery = query.isEmpty()
                || (r.getLastName() != null && r.getLastName().toLowerCase().contains(query))
                || (r.getFirstName() != null && r.getFirstName().toLowerCase().contains(query))
                || (r.getEmail() != null && r.getEmail().toLowerCase().contains(query));

            boolean matchesEvent = "Tous les événements".equals(eventFilter) || eventFilter == null
                || (r.getEvent() != null && eventFilter.equals(r.getEvent().getTitle()));

            boolean matchesStatus = "Tous les statuts".equals(statusFilter) || statusFilter == null
                || (r.getAttendanceStatus() != null && statusMatchesFrench(r.getAttendanceStatus(), statusFilter));

            if (matchesQuery && matchesEvent && matchesStatus) {
                filtered.add(r);
            }
        }

        // Always: PENDING/REGISTERED first, then CONFIRMED, then CANCELLED as secondary sort
        java.util.Comparator<EventRegistration> primarySort = (a, b) -> {
            int ra = statusOrder(a.getAttendanceStatus());
            int rb = statusOrder(b.getAttendanceStatus());
            return Integer.compare(ra, rb);
        };

        // EventUser secondary sort
        java.util.Comparator<EventRegistration> secondarySort = null;
        if ("Date (plus récent)".equals(sortBy)) {
            secondarySort = (a, b) -> {
                if (a.getRegisteredAt() == null) return 1;
                if (b.getRegisteredAt() == null) return -1;
                return b.getRegisteredAt().compareTo(a.getRegisteredAt());
            };
        } else if ("Date (plus ancien)".equals(sortBy)) {
            secondarySort = (a, b) -> {
                if (a.getRegisteredAt() == null) return 1;
                if (b.getRegisteredAt() == null) return -1;
                return a.getRegisteredAt().compareTo(b.getRegisteredAt());
            };
        } else if ("Nom A→Z".equals(sortBy)) {
            secondarySort = (a, b) -> {
                String la = a.getLastName() != null ? a.getLastName() : "";
                String lb = b.getLastName() != null ? b.getLastName() : "";
                return la.compareToIgnoreCase(lb);
            };
        } else if ("Nom Z→A".equals(sortBy)) {
            secondarySort = (a, b) -> {
                String la = a.getLastName() != null ? a.getLastName() : "";
                String lb = b.getLastName() != null ? b.getLastName() : "";
                return lb.compareToIgnoreCase(la);
            };
        }

        filtered.sort(secondarySort != null ? primarySort.thenComparing(secondarySort) : primarySort);

        attendeesTable.setItems(FXCollections.observableArrayList(filtered));
        if (attendeesCountLabel != null) {
            attendeesCountLabel.setText(filtered.size() + " résultat(s)");
        }
    }

    @FXML
    private void handleAttendeesSearch() {
        applyAttendeesFilter();
    }

    @FXML
    private void handleAttendeesReset() {
        if (attendeesSearchField != null) attendeesSearchField.clear();
        if (attendeesEventFilter != null) attendeesEventFilter.setValue("Tous les événements");
        if (attendeesStatusFilter != null) attendeesStatusFilter.setValue("Tous les statuts");
        if (attendeesSortCombo != null) attendeesSortCombo.setValue(null);
        applyAttendeesFilter();
    }

    /** Ordering: PENDING=0, CONFIRMED=1, REJECTED=2, CANCELLED=3 */
    private int statusOrder(AttendanceStatusEnum s) {
        if (s == null) return 0;
        switch (s) {
            case PENDING: case REGISTERED: return 0;
            case CONFIRMED: return 1;
            case REJECTED:  return 2;
            case CANCELLED: return 3;
            default: return 0;
        }
    }

    private boolean statusMatchesFrench(AttendanceStatusEnum s, String french) {
        if ("En attente".equals(french))  return s == AttendanceStatusEnum.PENDING || s == AttendanceStatusEnum.REGISTERED;
        if ("Confirmé".equals(french))    return s == AttendanceStatusEnum.CONFIRMED;
        if ("Rejeté".equals(french))      return s == AttendanceStatusEnum.REJECTED;
        if ("Annulé".equals(french))      return s == AttendanceStatusEnum.CANCELLED;
        return true;
    }

    /** Enables/disables an Accept (green) or Reject (orange) button with matching style. */
    private void setButtonState(Button btn, boolean enabled, boolean isAccept) {
        if (btn == null) return;
        btn.setDisable(!enabled);
        if (isAccept) {
            btn.setStyle(enabled
                ? "-fx-background-color:#16A34A; -fx-text-fill:white; -fx-font-weight:700; -fx-background-radius:8; -fx-padding:9 22;"
                : "-fx-background-color:#D1FAE5; -fx-text-fill:#9CA3AF; -fx-font-weight:700; -fx-background-radius:8; -fx-padding:9 22;");
        } else {
            btn.setStyle(enabled
                ? "-fx-background-color:#C2410C; -fx-text-fill:white; -fx-font-weight:700; -fx-background-radius:8; -fx-padding:9 22;"
                : "-fx-background-color:#FEE2E2; -fx-text-fill:#9CA3AF; -fx-font-weight:700; -fx-background-radius:8; -fx-padding:9 22;");
        }
    }

    @FXML
    private void handleAcceptRegistration() {
        changeRegistrationStatus(AttendanceStatusEnum.CONFIRMED);
    }

    @FXML
    private void handleRejectRegistration() {
        changeRegistrationStatus(AttendanceStatusEnum.REJECTED);
    }

    private void changeRegistrationStatus(AttendanceStatusEnum newStatus) {
        if (attendeesTable == null) return;
        EventRegistration selected = attendeesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Avertissement", "Veuillez sélectionner une inscription dans la liste.");
            return;
        }
        // Cannot modify a row cancelled by the candidate
        if (selected.getAttendanceStatus() == AttendanceStatusEnum.CANCELLED) {
            showAlert("Action impossible", "Cette inscription a été annulée par le candidat et ne peut pas être modifiée.");
            return;
        }
        try {
            selected.setAttendanceStatus(newStatus);
            registrationService.update(selected);
            String msg = newStatus == AttendanceStatusEnum.CONFIRMED
                ? "Inscription confirmée avec succès."
                : newStatus == AttendanceStatusEnum.REJECTED
                    ? "Inscription refusée. Vous pouvez l'accepter si vous changez d'avis."
                    : "Statut mis à jour.";
            showAlert("Mise à jour", msg);

            // Send email notification
            if (selected.getEmail() != null && !selected.getEmail().isBlank()) {
                String email = selected.getEmail();
                String name = selected.getCandidateName() != null ? selected.getCandidateName() : "Candidat";
                RecruitmentEvent realEvent = null;
                try { realEvent = eventService.getById(selected.getEventId()); } catch (SQLException ignored) {}
                final RecruitmentEvent evToUse = realEvent != null ? realEvent : selectedEvent;
                String eventTitle = evToUse != null ? evToUse.getTitle() : "";
                String eventDate = evToUse != null && evToUse.getEventDate() != null
                        ? evToUse.getEventDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";
                String eventLocation = evToUse != null && evToUse.getLocation() != null ? evToUse.getLocation() : "";
                String eventType = evToUse != null && evToUse.getEventType() != null ? evToUse.getEventType() : "";
                String meetLink = evToUse != null && evToUse.getMeetLink() != null ? evToUse.getMeetLink() : "";
                new Thread(() -> {
                    try {
                        EmailService.sendEventStatusNotification(
                            email, name, eventTitle, eventDate, eventLocation,
                            newStatus.name(), null, eventType, meetLink);
                    } catch (Exception ex) {
                        System.err.println("[RecruiterDashboard] Status email error: " + ex.getMessage());
                    }
                }, "event-status-email").start();
            }

            if (selectedEvent != null) refreshAttendees(selectedEvent.getId());
            else refreshAllAttendees();
            updateStatistics();
            if (attendeesCountLabel != null && attendeesTable.getItems() != null)
                attendeesCountLabel.setText(attendeesTable.getItems().size() + " résultat(s)");
        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    private void switchView(String view) {
        if (eventsView     != null) { eventsView.setVisible("events".equals(view));     eventsView.setManaged("events".equals(view)); }
        if (interviewsView != null) { interviewsView.setVisible("interviews".equals(view)); interviewsView.setManaged("interviews".equals(view)); }
        // Update tab button styles
        String activeStyle  = "-fx-background-color:#1565C0; -fx-text-fill:white; -fx-font-weight:700; -fx-background-radius:8; -fx-padding:9 18;";
        String inactiveStyle= "-fx-background-color:#F1F5F9; -fx-text-fill:#475569; -fx-font-weight:600; -fx-background-radius:8; -fx-padding:9 18; -fx-border-color:#E4EBF5; -fx-border-width:1; -fx-border-radius:8;";
        if (tabEventsBtn        != null) tabEventsBtn.setStyle("events".equals(view) ? activeStyle : inactiveStyle);
        if (tabSubscriptionsBtn != null) tabSubscriptionsBtn.setStyle("interviews".equals(view) ? activeStyle : inactiveStyle);
    }

    @FXML
    public void goToInterviews() {
        switchView("interviews");
        if (selectedEvent != null) refreshAttendees(selectedEvent.getId());
        else refreshAllAttendees();
    }

    @FXML
    public void goToEvents() {
        switchView("events");
        refreshTable();
    }

    @FXML
    private void handleSendEmail() {
        EventRegistration selected = attendeesTable != null
                ? attendeesTable.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            showAlert("Avertissement", "Sélectionnez un candidat dans la liste.");
            return;
        }
        if (selected.getEmail() == null || selected.getEmail().isBlank()) {
            showAlert("Erreur", "Aucun email disponible pour ce candidat.");
            return;
        }
        String name = selected.getCandidateName() != null ? selected.getCandidateName() : "Candidat";
        String eventTitle = selectedEvent != null ? selectedEvent.getTitle() : "";
        String eventDate = selectedEvent != null && selectedEvent.getEventDate() != null
                ? selectedEvent.getEventDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";
        String eventLocation = selectedEvent != null && selectedEvent.getLocation() != null
                ? selectedEvent.getLocation() : "";
        String eventType = selectedEvent != null && selectedEvent.getEventType() != null
                ? selectedEvent.getEventType() : "";
        new Thread(() -> {
            boolean sent;
            try {
                EmailService.sendEventRegistrationConfirmation(
                        selected.getEmail(), name, eventTitle, eventDate, eventLocation, eventType);
                sent = true;
            } catch (Exception ex) {
                sent = false;
                System.err.println("[RecruiterDashboard] Email error: " + ex.getMessage());
            }
            final boolean finalSent = sent;
            javafx.application.Platform.runLater(() ->
                showAlert(finalSent ? "Email envoyé" : "Erreur",
                          finalSent ? "Email de confirmation envoyé à " + selected.getEmail()
                                    : "Échec de l'envoi de l'email."));
        }, "event-manual-email").start();
    }

    @FXML
    public void goToJobOffers() {
        showAlert("Navigation", "Navigation vers les Offres d'emploi (à implémenter).");
    }

    @FXML
    public void goToSettings() {
        showAlert("Navigation", "Navigation vers les Paramètres (à implémenter).");
    }

    private void updateEventFromForm(RecruitmentEvent event) {
        event.setDescription(descriptionField.getText());
        event.setEventType(typeCombo.getValue());
        event.setLocation(locationField.getText());
        if (dateField.getValue() != null) {
            event.setEventDate(dateField.getValue().atStartOfDay());
        }
        try {
            event.setCapacity(Integer.parseInt(capacityField.getText()));
        } catch (NumberFormatException e) {
            event.setCapacity(0);
        }
        if (meetLinkField != null) event.setMeetLink(meetLinkField.getText());
    }

    private void populateForm(RecruitmentEvent event) {
        titleField.setText(event.getTitle());
        descriptionField.setText(event.getDescription());
        typeCombo.setValue(event.getEventType());
        locationField.setText(event.getLocation());
        capacityField.setText(String.valueOf(event.getCapacity()));
        if (event.getEventDate() != null) dateField.setValue(event.getEventDate().toLocalDate());
        if (meetLinkField != null) {
            boolean isWebinaire = "WEBINAIRE".equals(event.getEventType());
            meetLinkField.setVisible(isWebinaire);
            meetLinkField.setManaged(isWebinaire);
            meetLinkField.setText(event.getMeetLink() != null ? event.getMeetLink() : "");
            if (meetLinkErrorLabel != null) {
                meetLinkErrorLabel.setVisible(isWebinaire);
                meetLinkErrorLabel.setManaged(isWebinaire);
            }
        }
    }

    private void clearForm() {
        titleField.clear();
        descriptionField.clear();
        typeCombo.getSelectionModel().clearSelection();
        typeCombo.setValue(null);
        locationField.clear();
        capacityField.clear();
        dateField.setValue(null);
        if (meetLinkField != null) {
            meetLinkField.clear();
            meetLinkField.setVisible(false);
            meetLinkField.setManaged(false);
        }
        if (titleErrorLabel != null) titleErrorLabel.setText("");
        if (typeErrorLabel != null) typeErrorLabel.setText("");
        if (locationErrorLabel != null) locationErrorLabel.setText("");
        if (dateErrorLabel != null) dateErrorLabel.setText("");
        if (capacityErrorLabel != null) capacityErrorLabel.setText("");
        if (descriptionErrorLabel != null) descriptionErrorLabel.setText("");
        if (meetLinkErrorLabel != null) { meetLinkErrorLabel.setText(""); meetLinkErrorLabel.setVisible(false); meetLinkErrorLabel.setManaged(false); }
    }

    private boolean validateForm() {
        boolean hasError = false;

        // Clear previous error messages
        titleErrorLabel.setText("");
        typeErrorLabel.setText("");
        locationErrorLabel.setText("");
        dateErrorLabel.setText("");
        capacityErrorLabel.setText("");
        descriptionErrorLabel.setText("");
        if (meetLinkErrorLabel != null) meetLinkErrorLabel.setText("");

        if (titleField.getText() == null || titleField.getText().trim().length() < 3) {
            titleErrorLabel.setText("Minimum 3 caractères requis.");
            hasError = true;
        }

        if (typeCombo.getValue() == null) {
            typeErrorLabel.setText("Veuillez sélectionner un type.");
            hasError = true;
        }

        if (locationField.getText() == null || locationField.getText().trim().isEmpty()) {
            locationErrorLabel.setText("Le lieu est obligatoire.");
            hasError = true;
        }

        if (dateField.getValue() == null) {
            dateErrorLabel.setText("La date est obligatoire.");
            hasError = true;
        } else if (dateField.getValue().isBefore(java.time.LocalDate.now())) {
            dateErrorLabel.setText("La date ne peut pas être passée.");
            hasError = true;
        }

        try {
            int capacity = Integer.parseInt(capacityField.getText());
            if (capacity <= 0) {
                capacityErrorLabel.setText("Doit être un nombre positif.");
                hasError = true;
            }
        } catch (NumberFormatException e) {
            capacityErrorLabel.setText("Nombre invalide.");
            hasError = true;
        }

        if (descriptionField.getText() == null || descriptionField.getText().trim().length() < 10) {
            descriptionErrorLabel.setText("Minimum 10 caractères requis.");
            hasError = true;
        }

        // Validate meet link when WEBINAIRE is selected
        if ("WEBINAIRE".equals(typeCombo.getValue())) {
            String link = meetLinkField != null ? meetLinkField.getText() : null;
            if (link == null || link.trim().isEmpty()) {
                if (meetLinkErrorLabel != null) meetLinkErrorLabel.setText("Le lien de réunion est obligatoire pour un webinaire.");
                hasError = true;
            }
        }

        return !hasError;
    }

    @FXML
    private void handleDateValidation() {
        if (dateField.getValue() != null && dateField.getValue().isBefore(java.time.LocalDate.now())) {
            if (dateErrorLabel != null) {
                dateErrorLabel.setText("La date ne peut pas être passée.");
            }
        } else {
            if (dateErrorLabel != null) {
                dateErrorLabel.setText("");
            }
        }
        // Show/hide meet link field based on type
        if (typeCombo != null && meetLinkField != null) {
            boolean isWebinaire = "WEBINAIRE".equals(typeCombo.getValue());
            meetLinkField.setVisible(isWebinaire);
            meetLinkField.setManaged(isWebinaire);
        }
    }

    @FXML
    private void handlePickLocation() {
        Controllers.joboffers.LocationPickerController.pickLocation((lat, lng, address) -> {
            javafx.application.Platform.runLater(() -> {
                locationField.setText(address);
            });
        });
    }

    @FXML
    private void handlePickLocationNew() {
        Controllers.joboffers.LocationPickerController.pickLocation((lat, lng, address) -> {
            javafx.application.Platform.runLater(() -> {
                if (newLocationField != null) newLocationField.setText(address);
            });
        });
    }

    @FXML
    private void handleGenerateDescriptionAINew() {
        if (newTitleField == null || newDescriptionField == null) return;
        String title = newTitleField.getText();
        if (title == null || title.trim().isEmpty()) {
            showAlert("Information manquante", "Veuillez remplir le titre pour générer une description.");
            return;
        }
        String type = newTypeCombo != null ? newTypeCombo.getValue() : null;
        String location = newLocationField != null ? newLocationField.getText() : null;
        String generated = generateLocalEventDescription(title, type, location);
        newDescriptionField.setText(generated);
    }

    @FXML
    private void handleGenerateDescriptionAI() {
        String title = titleField.getText();
        if (title == null || title.trim().isEmpty()) {
            showAlert("Information manquante", "Veuillez remplir le titre pour générer une description.");
            return;
        }

        String type = typeCombo.getValue();
        String location = locationField.getText();
        String generatedDescription = generateLocalEventDescription(title, type, location);
        descriptionField.setText(generatedDescription);
    }

    private String generateLocalEventDescription(String title, String type, String location) {
        String prompt = "Tu es un recruteur expert en ressources humaines.\n" +
               "Rédige une description professionnelle et attirante (3 à 5 phrases) pour un événement de recrutement de type : \"" + type + "\"\n" +
               "Titre de l'événement : \"" + title + "\"\n" +
               "Lieu de l'événement : \"" + (location != null && !location.trim().isEmpty() ? location : "À définir") + "\"\n\n" +
               "IMPORTANT :\n" +
               "- Ne génère QUE la description (sans titre, sans introduction, sans guillemets).\n" +
               "- Le ton doit être professionnel, accueillant et dynamique.\n" +
               "- Met en valeur l'intérêt pour les candidats de participer à cet événement.";
               
        try {
            String result = callGeminiAPI(prompt);
            if (result != null && !result.isBlank()) return result;
        } catch (Exception e) {
            System.err.println("Gemini failed for event description: " + e.getMessage());
        }
        
        try {
            String result = callGroqAPI(prompt);
            if (result != null && !result.isBlank()) return result;
        } catch (Exception e) {
            System.err.println("Groq failed for event description: " + e.getMessage());
        }

        // Fallback to static text if all APIs fail
        String locPhrase = (location != null && !location.trim().isEmpty()) ? " à " + location : "";
        if ("WEBINAIRE".equals(type)) {
            return "Rejoignez-nous pour notre webinaire interactif : " + title + ". Cet événement en ligne sera l'occasion d'échanger avec nos experts et de découvrir nos opportunités depuis le confort de votre domicile.";
        } else if ("Job_Faire".equals(type)) {
            return "Ne manquez pas notre prochain salon de l'emploi : " + title + locPhrase + " ! Venez découvrir notre culture d'entreprise et saisir les meilleures opportunités de carrière.";
        } else if ("Interview day".equals(type)) {
            return "Inscrivez-vous à notre journée d'entretiens : " + title + locPhrase + ". Démontrez vos compétences techniques et comportementales, et peut-être décrocherez-vous votre futur poste !";
        }
        return "Nous avons le plaisir de vous annoncer notre prochain événement : " + title + locPhrase + ". Venez nombreux découvrir nos opportunités.";
    }

    private String callGeminiAPI(String prompt) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=AIzaSyA40pYJkW9p7QYQerVUv_rmS4pNFo1T46o";
        java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        c.setRequestMethod("POST"); c.setRequestProperty("Content-Type", "application/json");
        c.setDoOutput(true); c.setConnectTimeout(15000); c.setReadTimeout(60000);
        String body = "{\"contents\":[{\"parts\":[{\"text\":\"" + prompt.replace("\"", "\\\"").replace("\n", "\\n") + "\"}]}]," +
                "\"generationConfig\":{\"maxOutputTokens\":800,\"temperature\":0.7}}";
        c.getOutputStream().write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (c.getResponseCode() != 200) throw new Exception("Gemini HTTP " + c.getResponseCode());
        StringBuilder sb = new StringBuilder();
        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(c.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            String line; while ((line = br.readLine()) != null) sb.append(line);
        }
        String json = sb.toString();
        int s = json.indexOf("\"text\":"); if (s < 0) return null;
        s = json.indexOf("\"", s + 7) + 1; int e = s;
        for (int i = s; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (ch == '\\' && i + 1 < json.length()) { i++; continue; }
            if (ch == '"') { e = i; break; }
        }
        return e > s ? json.substring(s, e).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\").trim() : null;
    }

    private String callGroqAPI(String prompt) throws Exception {
        String[] MODELS = {"llama-3.3-70b-versatile", "llama-3.1-8b-instant", "mixtral-8x7b-32768"};
        String GROQ_KEY = "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";
        String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
        for (String model : MODELS) {
            try {
                java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(GROQ_URL).openConnection();
                c.setRequestMethod("POST");
                c.setRequestProperty("Content-Type", "application/json");
                c.setRequestProperty("Authorization", "Bearer " + GROQ_KEY);
                c.setDoOutput(true); c.setConnectTimeout(15000); c.setReadTimeout(30000);
                String body = "{\"model\":\"" + model + "\",\"messages\":[" +
                        "{\"role\":\"system\",\"content\":\"You are an expert HR recruiter.\"}," +
                        "{\"role\":\"user\",\"content\":\"" + prompt.replace("\"", "\\\"").replace("\n", "\\n") + "\"}]," +
                        "\"max_tokens\":600,\"temperature\":0.7}";
                c.getOutputStream().write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                if (c.getResponseCode() != 200) continue;
                StringBuilder sb = new StringBuilder();
                try (java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(c.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                    String line; while ((line = br.readLine()) != null) sb.append(line);
                }
                String json = sb.toString();
                int s = json.indexOf("\"content\":"); if (s < 0) continue;
                s = json.indexOf("\"", s + 10) + 1;
                int e = json.indexOf("\"", s);
                while (e > 0 && json.charAt(e - 1) == '\\') e = json.indexOf("\"", e + 1);
                if (s > 0 && e > s) {
                    String content = json.substring(s, e)
                            .replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\").trim();
                    if (!content.isBlank()) return content;
                }
            } catch (Exception ex) {
                System.err.println("Groq model " + model + " failed: " + ex.getMessage());
            }
        }
        throw new Exception("All Groq models failed");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * JavaFX workaround: setStyle() on a TableRow is often overridden by the
     * table's own CSS. Applying the style to each TableCell inside the row
     * ensures the color actually shows up regardless of the stylesheet.
     */
    private void applyCellStyle(String style, TableRow<?> row) {
        for (javafx.scene.Node node : row.getChildrenUnmodifiable()) {
            if (node instanceof TableCell) {
                node.setStyle(style);
            }
        }
    }
}

