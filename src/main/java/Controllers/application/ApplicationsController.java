package Controllers.application;

import Models.interview.Interview;
import Services.application.ApplicationService;
import Services.application.ApplicationStatusHistoryService;
import Services.application.FileService;
import Services.application.GrokAIService;
import Services.interview.InterviewService;
import Services.joboffers.JobOfferService;
import Services.interview.MeetingService;
import Services.application.OllamaRankingService;
import Services.events.UserService;
import Utils.UserContext;
import Utils.ValidationUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ApplicationsController {

    @FXML private VBox mainContainer;
    @FXML private VBox candidateListContainer;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbSearchCriteria;
    @FXML private Label lblSubtitle;
    @FXML private Button btnSearch;
    @FXML private Button btnClear;

    private VBox detailContainer;
    private ApplicationService.ApplicationRow selectedApplication;
    private List<Long> selectedApplicationIds = new ArrayList<>();
    private VBox bulkActionPanel;
    private List<ApplicationService.ApplicationRow> currentApplications = new ArrayList<>();
    private final java.util.Map<Long, OllamaRankingService.RankResult> rankingCache = new java.util.HashMap<>();
    private boolean rankingActive = false;
    private Label rankingStatusLabel;
    private Label selectionTextLabel;

    @FXML
    public void initialize() {
        // mainContainer is injected from FXML - it's the right panel VBox
        if (mainContainer != null) {
            detailContainer = mainContainer;
        } else {
            detailContainer = new VBox(15);
        }

        // Initialize search UI
        initializeSearchUI();
        loadApplications();
    }

    private void initializeSearchUI() {
        UserContext.Role role = UserContext.getRole();

        // Set search criteria options based on role
        if (cbSearchCriteria != null) {
            if (role == UserContext.Role.RECRUITER || role == UserContext.Role.ADMIN) {
                cbSearchCriteria.getItems().addAll(
                        "Candidate Name",
                        "Candidate Email",
                        "Offer Title"
                );
            } else if (role == UserContext.Role.CANDIDATE) {
                cbSearchCriteria.getItems().addAll(
                        "Offer Title",
                        "Company Name",
                        "Status"
                );
            }
            // Ensure the prompt is visible even when ComboBox is non-editable by providing a custom button cell
            cbSearchCriteria.setPromptText("Search by...");
            cbSearchCriteria.setButtonCell(new ListCell<String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText("Search by...");
                    } else {
                        setText(item);
                    }
                }
            });
            // Keep default list cell rendering for dropdown
            cbSearchCriteria.setCellFactory(listView -> new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item);
                    }
                }
            });
        }

        // Setup search button
        if (btnSearch != null) {
            btnSearch.setOnAction(e -> performSearch());
        }

        // Setup clear button
        if (btnClear != null) {
            btnClear.setOnAction(e -> clearSearch());
        }

        // Allow search on Enter key
        if (txtSearch != null) {
            txtSearch.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER) {
                    performSearch();
                }
            });
        }
    }

    @FXML
    private void performSearch() {
        String searchCriteria = cbSearchCriteria.getValue();
        String searchText = txtSearch.getText();

        if (searchCriteria == null || searchCriteria.isEmpty()) {
            showAlert("Warning", "Please select a search criteria", Alert.AlertType.WARNING);
            return;
        }

        if (searchText == null || searchText.trim().isEmpty()) {
            showAlert("Warning", "Please enter search text", Alert.AlertType.WARNING);
            return;
        }

        UserContext.Role role = UserContext.getRole();
        List<Long> offerIds = null;

        // Get offer IDs based on role
        if (role == UserContext.Role.RECRUITER) {
            Long recruiterId = UserContext.getRecruiterId();
            List<Models.joboffers.JobOffer> recruiterOffers = JobOfferService.getByRecruiterId(recruiterId);
            offerIds = recruiterOffers.stream()
                    .map(Models.joboffers.JobOffer::getId)
                    .toList();
        }
        // For candidates and admins, pass null to search all offers

        // Perform search
        List<ApplicationService.ApplicationRow> searchResults = ApplicationService.searchApplications(
                offerIds,
                searchCriteria,
                searchText
        );

        // Filter by role if needed
        if (role == UserContext.Role.CANDIDATE) {
            Long candidateId = UserContext.getCandidateId();
            searchResults = searchResults.stream()
                    .filter(app -> app.candidateId().equals(candidateId))
                    .toList();
        }

        displaySearchResults(searchResults);
    }

    @FXML
    private void clearSearch() {
        if (cbSearchCriteria != null) {
            cbSearchCriteria.getSelectionModel().clearSelection();
            cbSearchCriteria.setValue(null);
            cbSearchCriteria.setPromptText("Search by...");
            // Force button cell text to show prompt for non-editable ComboBox
            try {
                if (cbSearchCriteria.getButtonCell() != null) {
                    cbSearchCriteria.getButtonCell().setText("Search by...");
                }
            } catch (Exception ignored) {
            }

            // If editable, clear its editor too
            try {
                if (cbSearchCriteria.isEditable() && cbSearchCriteria.getEditor() != null) {
                    cbSearchCriteria.getEditor().clear();
                }
            } catch (Exception ignored) {
            }
        }

        if (txtSearch != null) {
            txtSearch.clear();
        }

        loadApplications();
    }

    private void displaySearchResults(List<ApplicationService.ApplicationRow> results) {
        currentApplications = results;
        boolean showBulkPanel = UserContext.getRole() == UserContext.Role.RECRUITER;
        renderApplications(results, showBulkPanel);
    }

    private void loadApplications() {
        if (candidateListContainer == null) {
            System.err.println("[ApplicationsController] candidateListContainer is NULL!");
            return;
        }
        candidateListContainer.getChildren().clear();
        selectedApplicationIds.clear();

        List<ApplicationService.ApplicationRow> applications = ApplicationService.getAll();
        UserContext.Role role = UserContext.getRole();

        System.out.println("[ApplicationsController] Role: " + role + ", Total applications from DB: " + applications.size());

        // Filter by role
        if (role == UserContext.Role.CANDIDATE) {
            Long candidateId = UserContext.getCandidateId();
            System.out.println("[ApplicationsController] Candidate ID: " + candidateId);
            if (candidateId != null) {
                applications = applications.stream()
                        .filter(app -> app.candidateId().equals(candidateId))
                        .toList();
            }
            System.out.println("[ApplicationsController] After candidate filter: " + applications.size());
        } else if (role == UserContext.Role.RECRUITER) {
            Long recruiterId = UserContext.getRecruiterId();
            System.out.println("[ApplicationsController] Recruiter ID: " + recruiterId);
            List<Models.joboffers.JobOffer> recruiterOffers = JobOfferService.getByRecruiterId(recruiterId);
            System.out.println("[ApplicationsController] Recruiter job offers: " + recruiterOffers.size());

            // Always filter by recruiter's own job offers - empty list if no offers
            List<Long> offerIds = recruiterOffers.stream()
                    .map(Models.joboffers.JobOffer::getId)
                    .toList();
            applications = applications.stream()
                    .filter(app -> offerIds.contains(app.offerId()))
                    .toList();
            System.out.println("[ApplicationsController] After recruiter filter: " + applications.size());
        }

        // Hide archived applications for non-admins
        if (role != UserContext.Role.ADMIN) {
            applications = applications.stream()
                    .filter(app -> !app.isArchived())
                    .toList();
        }

        currentApplications = applications;
        renderApplications(applications, role == UserContext.Role.RECRUITER);
    }

    private void renderApplications(List<ApplicationService.ApplicationRow> applications, boolean showBulkPanel) {
        if (candidateListContainer == null) return;
        candidateListContainer.getChildren().clear();
        selectedApplicationIds.clear();

        // Update count badge
        if (lblSubtitle != null) {
            int cnt = applications == null ? 0 : applications.size();
            lblSubtitle.setText(cnt + " candidature(s)");
        }

        if (applications == null || applications.isEmpty()) {
            Label empty = new Label(showBulkPanel
                    ? "Aucune candidature trouvée."
                    : "Aucune candidature ne correspond à votre recherche.");
            empty.setStyle("-fx-text-fill: #999; -fx-font-size: 14px; -fx-padding: 30;");
            candidateListContainer.getChildren().add(empty);
            return;
        }

        if (showBulkPanel) {
            ensureBulkActionPanel(candidateListContainer);
            bulkActionPanel.setVisible(true);
            bulkActionPanel.setManaged(true);
        } else if (bulkActionPanel != null) {
            bulkActionPanel.setVisible(false);
            bulkActionPanel.setManaged(false);
        }

        List<ApplicationService.ApplicationRow> displayList = new ArrayList<>(applications);
        if (rankingActive) {
            displayList.sort((a, b) -> Integer.compare(getRankScore(b.id()), getRankScore(a.id())));
        }

        boolean first = true;
        for (ApplicationService.ApplicationRow app : displayList) {
            VBox card = createApplicationCard(app);
            candidateListContainer.getChildren().add(card);

            if (first) {
                selectApplication(app, card);
                first = false;
            }
        }
    }

    private int getRankScore(Long appId) {
        OllamaRankingService.RankResult result = rankingCache.get(appId);
        return result != null ? result.score() : -1;
    }

    private void createAndShowBulkActionPanel(VBox container) {
        bulkActionPanel = new VBox(14);
        bulkActionPanel.setStyle("-fx-background-color: #EBF3FE; -fx-background-radius: 12; "
                + "-fx-border-color: #5BA3F5; -fx-border-width: 1.5; -fx-border-radius: 12; "
                + "-fx-padding: 18;");
        bulkActionPanel.setVisible(false);
        bulkActionPanel.setManaged(false);

        Label titleLabel = new Label("Actions groupées");
        titleLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 15px; -fx-text-fill: #1565C0;");

        selectionTextLabel = new Label("Aucune candidature sélectionnée");
        selectionTextLabel.setStyle("-fx-text-fill: #1565C0; -fx-font-size: 13px;");

        // Status change row — stacked vertically so text never clips
        Label statusLabel = new Label("Changer le statut :");
        statusLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("SUBMITTED", "IN_REVIEW", "SHORTLISTED", "REJECTED", "INTERVIEW", "HIRED");
        statusCombo.setPrefWidth(220);
        statusCombo.setPromptText("Sélectionner un statut...");
        statusCombo.setStyle("-fx-font-size: 13px;");

        TextArea noteArea = new TextArea();
        noteArea.setPromptText("Ajouter une note (optionnel)");
        noteArea.setPrefRowCount(2);
        noteArea.setWrapText(true);
        noteArea.setStyle("-fx-font-size: 13px;");

        Button btnBulkUpdate = new Button("✔ Mettre à jour");
        btnBulkUpdate.setStyle("-fx-padding: 10 20; -fx-background-color: #5BA3F5; -fx-text-fill: white; "
                + "-fx-cursor: hand; -fx-font-weight: 700; -fx-font-size: 13px; -fx-background-radius: 8;");
        btnBulkUpdate.setOnAction(e -> {
            if (selectedApplicationIds.isEmpty()) {
                showAlert("Avertissement", "Aucune candidature sélectionnée", Alert.AlertType.WARNING);
                return;
            }
            if (statusCombo.getValue() == null) {
                showAlert("Avertissement", "Veuillez sélectionner un statut", Alert.AlertType.WARNING);
                return;
            }
            bulkUpdateStatus(new ArrayList<>(selectedApplicationIds), statusCombo.getValue(), noteArea.getText());
        });

        HBox statusRow = new HBox(10);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusRow.getChildren().addAll(statusCombo, btnBulkUpdate);

        // AI ranking row
        Button btnRank = new Button("🤖 Classer avec l'IA");
        btnRank.setStyle("-fx-padding: 10 18; -fx-background-color: #6f42c1; -fx-text-fill: white; "
                + "-fx-cursor: hand; -fx-font-weight: 700; -fx-font-size: 13px; -fx-background-radius: 8;");
        btnRank.setOnAction(e -> rankApplicationsWithAI());

        rankingStatusLabel = new Label("Classement: DÉSACTIVÉ");
        rankingStatusLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: 700; -fx-font-size: 13px;");

        HBox rankingRow = new HBox(12);
        rankingRow.setAlignment(Pos.CENTER_LEFT);
        rankingRow.getChildren().addAll(btnRank, rankingStatusLabel);

        bulkActionPanel.getChildren().addAll(titleLabel, selectionTextLabel,
                statusLabel, statusRow, noteArea, rankingRow);
        container.getChildren().add(0, bulkActionPanel);
    }

    private void ensureBulkActionPanel(VBox container) {
        if (bulkActionPanel == null) {
            createAndShowBulkActionPanel(container);
            return;
        }
        if (!container.getChildren().contains(bulkActionPanel)) {
            container.getChildren().add(0, bulkActionPanel);
        }
    }

    private void updateBulkActionPanelUI() {
        if (bulkActionPanel == null || selectionTextLabel == null) return;
        if (selectedApplicationIds.isEmpty()) {
            selectionTextLabel.setText("Aucune candidature sélectionnée");
        } else {
            selectionTextLabel.setText(selectedApplicationIds.size() + " candidature(s) sélectionnée(s)");
        }
    }

    private VBox createApplicationCard(ApplicationService.ApplicationRow app) {
        VBox card = new VBox(0);
        String normalStyle = "-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-border-color: #e9ecef; -fx-border-width: 1; -fx-border-radius: 12; "
                + "-fx-padding: 14 16; -fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.04),6,0,0,2);";
        card.setStyle(normalStyle);
        card.setUserData(app);

        // Hover animations
        card.setOnMouseEntered(e -> {
            if (!card.getStyleClass().contains("app-card-selected")) {
                card.setStyle("-fx-background-color: #F7FBFF; -fx-background-radius: 12; "
                        + "-fx-border-color: #5BA3F5; -fx-border-width: 1.5; -fx-border-radius: 12; "
                        + "-fx-padding: 14 16; -fx-cursor: hand; "
                        + "-fx-effect: dropshadow(gaussian,rgba(91,163,245,0.18),10,0,0,3);");
            }
        });
        card.setOnMouseExited(e -> {
            if (!card.getStyleClass().contains("app-card-selected")) {
                card.setStyle(normalStyle);
            }
        });

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        // Avatar circle with initials
        Label avatar = new Label(getInitials(app.candidateName()));
        avatar.setStyle("-fx-background-color: #EBF3FE; -fx-text-fill: #5BA3F5; "
                + "-fx-font-weight: 700; -fx-font-size: 13px; -fx-alignment: center; "
                + "-fx-min-width: 40; -fx-max-width: 40; -fx-min-height: 40; -fx-max-height: 40; "
                + "-fx-background-radius: 20;");

        // Checkbox for recruiter bulk select
        if (UserContext.getRole() == UserContext.Role.RECRUITER) {
            CheckBox selectCheckbox = new CheckBox();
            selectCheckbox.setStyle("-fx-font-size: 13px;");
            selectCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) { if (!selectedApplicationIds.contains(app.id())) selectedApplicationIds.add(app.id()); }
                else          selectedApplicationIds.remove(app.id());
                updateBulkActionPanelUI();
            });
            row.getChildren().add(selectCheckbox);
            card.setUserData(new Object[]{app, selectCheckbox});
        }

        VBox infoBox = new VBox(4);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label candidateName = new Label(app.candidateName() != null && !app.candidateName().trim().isEmpty()
                ? app.candidateName() : "Candidat #" + app.id());
        candidateName.setStyle("-fx-font-weight: 700; -fx-font-size: 13.5px; -fx-text-fill: #2c3e50;");

        Label jobTitle = new Label(app.jobTitle() != null ? app.jobTitle() : "Candidature");
        jobTitle.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

        HBox badgeRow = new HBox(6);
        badgeRow.setAlignment(Pos.CENTER_LEFT);

        // Color-coded status badge
        Label statusBadge = new Label(translateStatus(app.currentStatus()));
        statusBadge.setStyle("-fx-padding: 3 9; -fx-background-radius: 20; -fx-font-size: 11px; -fx-font-weight: 600; "
                + getStatusBadgeStyle(app.currentStatus()));

        if (UserContext.getRole() == UserContext.Role.RECRUITER && rankingActive) {
            OllamaRankingService.RankResult rankResult = rankingCache.get(app.id());
            if (rankResult != null) {
                Label rankBadge = new Label("IA " + rankResult.score());
                rankBadge.setStyle("-fx-padding: 3 9; -fx-background-color: #6f42c1; -fx-text-fill: white; "
                        + "-fx-background-radius: 20; -fx-font-size: 11px; -fx-font-weight: bold;");
                badgeRow.getChildren().add(rankBadge);
            }
        }

        if (app.isArchived()) {
            Label archivedBadge = new Label("ARCHIVÉ");
            archivedBadge.setStyle("-fx-padding: 3 9; -fx-background-color: #6c757d; -fx-text-fill: white; "
                    + "-fx-background-radius: 20; -fx-font-size: 11px;");
            badgeRow.getChildren().addAll(statusBadge, archivedBadge);
        } else {
            badgeRow.getChildren().add(statusBadge);
        }

        // Date label on the right
        Label dateLabel = new Label(app.appliedAt() != null
                ? app.appliedAt().format(DateTimeFormatter.ofPattern("dd/MM/yy")) : "");
        dateLabel.setStyle("-fx-text-fill: #adb5bd; -fx-font-size: 11px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox footerRow = new HBox(spacer, dateLabel);
        footerRow.setAlignment(Pos.CENTER_RIGHT);

        infoBox.getChildren().addAll(candidateName, jobTitle, badgeRow, footerRow);
        row.getChildren().addAll(avatar, infoBox);
        card.getChildren().add(row);

        card.setOnMouseClicked(e -> {
            if (!(e.getTarget() instanceof CheckBox)) selectApplication(app, card);
        });

        return card;
    }

    /** Returns initials from a full name */
    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    /** Color style for status badges */
    private String getStatusBadgeStyle(String status) {
        if (status == null) return "-fx-background-color: #e9ecef; -fx-text-fill: #6c757d;";
        return switch (status.toUpperCase()) {
            case "SUBMITTED"   -> "-fx-background-color: #E3F2FD; -fx-text-fill: #1565C0;";
            case "IN_REVIEW"   -> "-fx-background-color: #FFF8E1; -fx-text-fill: #E65100;";
            case "SHORTLISTED" -> "-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32;";
            case "INTERVIEW"   -> "-fx-background-color: #EDE7F6; -fx-text-fill: #4527A0;";
            case "HIRED"       -> "-fx-background-color: #E0F2F1; -fx-text-fill: #00695C;";
            case "REJECTED"    -> "-fx-background-color: #FFEBEE; -fx-text-fill: #B71C1C;";
            default            -> "-fx-background-color: #e9ecef; -fx-text-fill: #6c757d;";
        };
    }

    /** Translate status to French */
    private String translateStatus(String status) {
        if (status == null) return "Soumise";
        return switch (status.toUpperCase()) {
            case "SUBMITTED"   -> "Soumise";
            case "IN_REVIEW"   -> "En révision";
            case "SHORTLISTED" -> "Présélectionné(e)";
            case "INTERVIEW"   -> "Entretien planifié";
            case "HIRED"       -> "Embauché(e)";
            case "REJECTED"    -> "Rejetée";
            default -> status;
        };
    }

    /** Creates a small label+value box for the detail header grid */
    private VBox makeDetailItem(String labelText, String valueText) {
        VBox box = new VBox(3);
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d; -fx-font-weight: 600;");
        Label val = new Label(valueText);
        val.setStyle("-fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        val.setWrapText(true);
        box.getChildren().addAll(lbl, val);
        return box;
    }

    private void selectApplication(ApplicationService.ApplicationRow app, VBox card) {
        String normalStyle = "-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-border-color: #e9ecef; -fx-border-width: 1; -fx-border-radius: 12; "
                + "-fx-padding: 14 16; -fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.04),6,0,0,2);";
        String selectedStyle = "-fx-background-color: #EBF3FE; -fx-background-radius: 12; "
                + "-fx-border-color: #5BA3F5; -fx-border-width: 2; -fx-border-radius: 12; "
                + "-fx-padding: 14 16; -fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian,rgba(91,163,245,0.25),12,0,0,3);";

        candidateListContainer.getChildren().forEach(node -> {
            if (node instanceof VBox v) {
                v.getStyleClass().remove("app-card-selected");
                v.setStyle(normalStyle);
            }
        });

        card.getStyleClass().add("app-card-selected");
        card.setStyle(selectedStyle);
        selectedApplication = app;
        displayApplicationDetails(app);
    }

    private void displayApplicationDetails(ApplicationService.ApplicationRow app) {
        detailContainer.getChildren().clear();

        // Get role at the beginning so it's available throughout the method
        UserContext.Role role = UserContext.getRole();

        // Header section
        VBox headerBox = new VBox(10);
        headerBox.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-border-color: #e9ecef; -fx-border-width: 1; -fx-border-radius: 12; "
                + "-fx-padding: 20; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.05),8,0,0,2);");

        // Avatar + name row
        HBox nameRow = new HBox(14);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label avatarBig = new Label(getInitials(app.candidateName()));
        avatarBig.setStyle("-fx-background-color: #EBF3FE; -fx-text-fill: #5BA3F5; "
                + "-fx-font-weight: 700; -fx-font-size: 18px; -fx-alignment: center; "
                + "-fx-min-width: 54; -fx-max-width: 54; -fx-min-height: 54; -fx-max-height: 54; "
                + "-fx-background-radius: 27;");

        VBox nameBox = new VBox(4);
        Label candidateName = new Label(app.candidateName() != null && !app.candidateName().isBlank()
                ? app.candidateName() : "Candidat #" + app.id());
        candidateName.setStyle("-fx-font-weight: 700; -fx-font-size: 18px; -fx-text-fill: #2c3e50;");

        Label jobPosition = new Label(app.jobTitle() != null ? app.jobTitle() : "Candidature");
        jobPosition.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");
        nameBox.getChildren().addAll(candidateName, jobPosition);
        nameRow.getChildren().addAll(avatarBig, nameBox);

        // Info grid
        HBox infoGrid = new HBox(30);
        infoGrid.setStyle("-fx-padding: 10 0 0 0;");
        infoGrid.getChildren().addAll(
            makeDetailItem("📧 Email",  app.candidateEmail() != null ? app.candidateEmail() : "N/A"),
            makeDetailItem("📞 Tél.",   app.phone() != null ? app.phone() : "N/A"),
            makeDetailItem("📅 Posté",  app.appliedAt() != null
                    ? app.appliedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "N/A")
        );

        // Status badge
        Label currentStatus = new Label(translateStatus(app.currentStatus()));
        currentStatus.setStyle("-fx-padding: 5 14; -fx-background-radius: 20; -fx-font-weight: 700; -fx-font-size: 12px; "
                + getStatusBadgeStyle(app.currentStatus()));

        headerBox.getChildren().addAll(nameRow, infoGrid, currentStatus);
        detailContainer.getChildren().add(headerBox);

        if (role == UserContext.Role.RECRUITER && rankingActive) {
            VBox aiBox = new VBox(6);
            aiBox.setStyle("-fx-border-color: #e9ecef; -fx-border-radius: 4; -fx-padding: 12; -fx-background-color: #f8f0ff;");

            Label aiLabel = new Label("Classement IA");
            aiLabel.setStyle("-fx-font-weight: bold;");

            OllamaRankingService.RankResult rankResult = rankingCache.get(app.id());
            if (rankResult != null) {
                Label scoreLabel = new Label("Score : " + rankResult.score() + "/100");
                scoreLabel.setStyle("-fx-text-fill: #6f42c1; -fx-font-weight: bold;");
                Label rationaleLabel = new Label(rankResult.rationale());
                rationaleLabel.setWrapText(true);
                rationaleLabel.setStyle("-fx-text-fill: #555; -fx-font-size: 12;");
                aiBox.getChildren().addAll(aiLabel, scoreLabel, rationaleLabel);
            } else {
                Label noRankLabel = new Label("Pas encore classé.");
                noRankLabel.setStyle("-fx-text-fill: #999;");
                aiBox.getChildren().addAll(aiLabel, noRankLabel);
            }

            detailContainer.getChildren().add(aiBox);
        }

        // Cover Letter section
        if (app.coverLetter() != null && !app.coverLetter().isEmpty()) {
            VBox coverLetterBox = new VBox(5);
            coverLetterBox.setStyle("-fx-border-color: #e9ecef; -fx-border-radius: 4; -fx-padding: 15;");

            HBox coverHeaderBox = new HBox(10);
            coverHeaderBox.setAlignment(Pos.CENTER_LEFT);

            Label coverLabel = new Label("Lettre de motivation :");
            coverLabel.setStyle("-fx-font-weight: bold;");

            coverHeaderBox.getChildren().add(coverLabel);

            TextArea coverText = new TextArea(app.coverLetter());
            coverText.setEditable(false);
            coverText.setWrapText(true);
            coverText.setPrefRowCount(5);
            coverText.setStyle("-fx-control-inner-background: #f8f9fa; -fx-text-fill: #333;");

            if (role == UserContext.Role.ADMIN || role == UserContext.Role.RECRUITER) {
                final String originalText = app.coverLetter();

                Button btnTranslate = new Button("🌐 Traduire");
                btnTranslate.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white; -fx-font-size: 11; -fx-padding: 4 10; -fx-cursor: hand; -fx-background-radius: 4;");

                Button btnOriginal = new Button("🔄 Original");
                btnOriginal.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-size: 11; -fx-padding: 4 10; -fx-cursor: hand; -fx-background-radius: 4;");
                btnOriginal.setDisable(true);

                btnOriginal.setOnAction(e -> {
                    coverText.setText(originalText);
                    coverLabel.setText("Lettre de motivation :");
                    btnOriginal.setDisable(true);
                });

                btnTranslate.setOnAction(e -> {
                    ChoiceDialog<String> dialog = new ChoiceDialog<>("Français", "Français", "Anglais", "Arabe");
                    dialog.setTitle("Traduire la lettre de motivation");
                    dialog.setHeaderText("Sélectionnez la langue cible");
                    dialog.setContentText("Langue :");

                    dialog.showAndWait().ifPresent(language -> {
                        btnTranslate.setText("Traduction...");
                        btnTranslate.setDisable(true);
                        btnOriginal.setDisable(true);

                        // Map French display name to English for the API
                        String apiLang = switch (language) {
                            case "Français" -> "French";
                            case "Anglais"  -> "English";
                            case "Arabe"    -> "Arabic";
                            default         -> language;
                        };

                        javafx.concurrent.Task<String> task = new javafx.concurrent.Task<>() {
                            @Override
                            protected String call() {
                                return GrokAIService.translateCoverLetter(originalText, apiLang);
                            }
                        };

                        task.setOnSucceeded(ev -> {
                            String translated = task.getValue();
                            if (translated != null && !translated.isEmpty()) {
                                coverText.setText(translated);
                                coverLabel.setText("Lettre de motivation (" + language + ") :");
                                btnOriginal.setDisable(false);
                            }
                            btnTranslate.setText("🌐 Traduire");
                            btnTranslate.setDisable(false);
                        });

                        task.setOnFailed(ev -> {
                            btnTranslate.setText("🌐 Traduire");
                            btnTranslate.setDisable(false);
                            Alert alert = new Alert(Alert.AlertType.ERROR,
                                    "La traduction a échoué. Veuillez réessayer.", ButtonType.OK);
                            alert.showAndWait();
                        });

                        new Thread(task).start();
                    });
                });

                coverHeaderBox.getChildren().addAll(btnTranslate, btnOriginal);
            }

            coverLetterBox.getChildren().addAll(coverHeaderBox, coverText);
            detailContainer.getChildren().add(coverLetterBox);
        }

        // CV Path section
        if (app.cvPath() != null && !app.cvPath().isEmpty()) {
            VBox cvBox = new VBox(5);
            cvBox.setStyle("-fx-border-color: #e9ecef; -fx-border-radius: 4; -fx-padding: 15;");

            HBox cvLabelBox = new HBox(10);
            Label cvLabel = new Label("CV : " + app.cvPath());
            cvLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12;");

            if (role == UserContext.Role.RECRUITER || role == UserContext.Role.ADMIN) {
                Button btnDownload = new Button("📥 Télécharger CV");
                btnDownload.setStyle("-fx-padding: 4 10; -fx-font-size: 11; -fx-background-color: #28a745; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 4;");
                btnDownload.setOnAction(e -> downloadPDF(app));
                cvLabelBox.getChildren().addAll(cvLabel, btnDownload);
            } else {
                cvLabelBox.getChildren().add(cvLabel);
            }

            cvBox.getChildren().add(cvLabelBox);
            detailContainer.getChildren().add(cvBox);
        }

        // Status History section
        VBox historyBox = new VBox(8);
        historyBox.setStyle("-fx-border-color: #e9ecef; -fx-border-radius: 4; -fx-padding: 15;");

        Label historyLabel = new Label("Historique des statuts :");
        historyLabel.setStyle("-fx-font-weight: bold;");
        historyBox.getChildren().add(historyLabel);

        List<ApplicationStatusHistoryService.StatusHistoryRow> history =
                ApplicationStatusHistoryService.getByApplicationId(app.id());

        if (history.isEmpty()) {
            Label noHistory = new Label("Aucun historique disponible.");
            noHistory.setStyle("-fx-text-fill: #999;");
            historyBox.getChildren().add(noHistory);
        } else {
            for (ApplicationStatusHistoryService.StatusHistoryRow record : history) {
                VBox historyItem = new VBox(3);
                historyItem.setStyle("-fx-border-color: #dee2e6; -fx-border-radius: 3; -fx-padding: 8; -fx-background-color: white;");

                Label statusLabel = new Label(translateStatus(record.status()));
                statusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");

                Label dateLabel = new Label(record.changedAt() != null
                        ? record.changedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : "N/A");
                dateLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 11;");

                historyItem.getChildren().addAll(statusLabel, dateLabel);

                if (record.note() != null && !record.note().isEmpty()) {
                    Label noteLabel = new Label("Note : " + record.note());
                    noteLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11; -fx-wrap-text: true;");
                    noteLabel.setWrapText(true);
                    historyItem.getChildren().add(noteLabel);
                }

                boolean canEditHistory = false;
                boolean canDeleteHistory = false;

                if (role == UserContext.Role.ADMIN) {
                    canEditHistory = true;
                    canDeleteHistory = true;
                } else if (role == UserContext.Role.RECRUITER) {
                    try {
                        var offer = JobOfferService.getById(app.offerId());
                        if (offer != null && offer.getRecruiterId() != null && UserContext.getRecruiterId() != null
                                && offer.getRecruiterId().equals(UserContext.getRecruiterId())) {
                            canEditHistory = true;
                        }
                    } catch (Exception ignored) {}
                }

                if (canEditHistory || canDeleteHistory) {
                    HBox adminButtonBox = new HBox(10);
                    adminButtonBox.setAlignment(Pos.CENTER_RIGHT);

                    if (canEditHistory) {
                        Button btnEdit = new Button("✏ Modifier");
                        btnEdit.setStyle("-fx-padding: 4 8; -fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 4;");
                        btnEdit.setOnAction(e -> editStatusHistory(record));
                        adminButtonBox.getChildren().add(btnEdit);
                    }

                    if (canDeleteHistory) {
                        Button btnDelete = new Button("🗑 Supprimer");
                        btnDelete.setStyle("-fx-padding: 4 8; -fx-background-color: #dc3545; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 4;");
                        btnDelete.setOnAction(e -> deleteStatusHistory(record, app));
                        adminButtonBox.getChildren().add(btnDelete);
                    }

                    historyItem.getChildren().add(adminButtonBox);
                }

                historyBox.getChildren().add(historyItem);
            }
        }

        detailContainer.getChildren().add(historyBox);

        // Actions section

        VBox actionsBox = new VBox(10);
        actionsBox.setStyle("-fx-border-color: #e9ecef; -fx-border-radius: 4; -fx-padding: 15; -fx-background-color: #f8f9fa;");

        Label actionsLabel = new Label("Actions :");
        actionsLabel.setStyle("-fx-font-weight: bold;");
        actionsBox.getChildren().add(actionsLabel);

        if (role == UserContext.Role.CANDIDATE) {
            HBox buttonBox = new HBox(10);

            Button btnEdit = new Button("✏ Modifier");
            btnEdit.setStyle("-fx-padding: 6 12; -fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 6;");
            btnEdit.setOnAction(e -> showEditApplicationDialog(app));

            boolean canEdit = "SUBMITTED".equals(app.currentStatus());
            btnEdit.setDisable(!canEdit);
            if (!canEdit) {
                btnEdit.setStyle("-fx-padding: 6 12; -fx-background-color: #ccc; -fx-text-fill: #666; -fx-cursor: not-allowed; -fx-background-radius: 6;");
            }

            Button btnDelete = new Button("🗑 Supprimer");
            btnDelete.setStyle("-fx-padding: 6 12; -fx-background-color: #dc3545; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 6;");
            btnDelete.setOnAction(e -> deleteApplication(app));

            buttonBox.getChildren().addAll(btnEdit, btnDelete);
            actionsBox.getChildren().add(buttonBox);

        } else if (role == UserContext.Role.RECRUITER) {
            VBox statusUpdateBox = new VBox(8);

            Label statusLabel = new Label("Changer le statut :");
            statusLabel.setStyle("-fx-font-weight: bold;");

            ComboBox<String> statusCombo = new ComboBox<>();
            statusCombo.getItems().addAll("SUBMITTED", "IN_REVIEW", "SHORTLISTED", "REJECTED", "INTERVIEW", "HIRED");
            statusCombo.setValue(app.currentStatus());
            statusCombo.setPrefWidth(250);
            // Show French labels in the ComboBox
            statusCombo.setButtonCell(new javafx.scene.control.ListCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : translateStatus(item));
                }
            });
            statusCombo.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : translateStatus(item));
                }
            });

            TextArea noteArea = new TextArea();
            noteArea.setPromptText("Ajouter une note (optionnel)");
            noteArea.setPrefRowCount(3);
            noteArea.setWrapText(true);

            Button btnUpdate = new Button("✔ Mettre à jour");
            btnUpdate.setStyle("-fx-padding: 8 16; -fx-background-color: #28a745; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 6; -fx-font-weight: 600;");
            btnUpdate.setOnAction(e -> updateApplicationStatus(app, statusCombo.getValue(), noteArea.getText()));

            // Quick Action Buttons
            HBox quickActionsBox = new HBox(10);
            quickActionsBox.setStyle("-fx-padding: 10 0;");

            Button btnInReview = new Button("📋 En révision");
            btnInReview.setStyle("-fx-padding: 8 15; -fx-background-color: #17a2b8; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 6;");
            btnInReview.setOnAction(e -> startReviewApplication(app));

            Button btnAccept = new Button("✓ Présélectionner");
            btnAccept.setStyle("-fx-padding: 8 15; -fx-background-color: #28a745; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 6;");
            btnAccept.setOnAction(e -> acceptApplication(app));

            Button btnReject = new Button("✕ Rejeter");
            btnReject.setStyle("-fx-padding: 8 15; -fx-background-color: #dc3545; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 6;");
            btnReject.setOnAction(e -> rejectApplication(app));

            // Schedule button — show "Déjà planifié" if interview already scheduled
            boolean alreadyScheduled = "INTERVIEW".equals(app.currentStatus()) || "HIRED".equals(app.currentStatus());
            Button btnSchedule;
            if (alreadyScheduled) {
                btnSchedule = new Button("✅ Déjà planifié");
                btnSchedule.setStyle("-fx-padding: 8 15; -fx-background-color: #e9ecef; -fx-text-fill: #6c757d; -fx-cursor: default; -fx-font-weight: bold; -fx-background-radius: 6;");
                btnSchedule.setDisable(true);
                Tooltip tip = new Tooltip("Un entretien a déjà été planifié pour ce candidat.");
                Tooltip.install(btnSchedule, tip);
            } else {
                btnSchedule = new Button("📅 Planifier Entretien");
                btnSchedule.setStyle("-fx-padding: 8 15; -fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 6;");
                btnSchedule.setOnAction(e -> showInterviewScheduleDialog(app));
            }

            quickActionsBox.getChildren().addAll(btnInReview, btnAccept, btnReject, btnSchedule);

            statusUpdateBox.getChildren().addAll(statusLabel, statusCombo, new Label("Note :"), noteArea, btnUpdate, quickActionsBox);
            actionsBox.getChildren().add(statusUpdateBox);

        } else if (role == UserContext.Role.ADMIN) {
            HBox buttonBox = new HBox(10);

            Button btnDelete = new Button("🗑 Supprimer");
            btnDelete.setStyle("-fx-padding: 6 12; -fx-background-color: #dc3545; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 6;");
            btnDelete.setOnAction(e -> deleteApplication(app));

            Button btnArchive = new Button(app.isArchived() ? "📤 Désarchiver" : "📥 Archiver");
            btnArchive.setStyle("-fx-padding: 6 12; -fx-background-color: #6c757d; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 6;");
            btnArchive.setOnAction(e -> {
                boolean toArchive = !app.isArchived();
                ApplicationService.setArchived(app.id(), toArchive, UserContext.getAdminId());
                loadApplications();
                showAlert("Succès", toArchive ? "Candidature archivée." : "Candidature désarchivée.", Alert.AlertType.INFORMATION);
            });

            buttonBox.getChildren().addAll(btnArchive, btnDelete);
            actionsBox.getChildren().add(buttonBox);
        }

        detailContainer.getChildren().add(actionsBox);
    }

    private void showEditApplicationDialog(ApplicationService.ApplicationRow app) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier la Candidature");
        dialog.setHeaderText("Candidature #" + app.id());

        ScrollPane scrollPane = new ScrollPane();
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-padding: 20;");
        scrollPane.setContent(content);
        scrollPane.setFitToWidth(true);

        // Phone field with country selection
        Label phoneLabel = new Label("Numéro de téléphone *");
        phoneLabel.setStyle("-fx-font-weight: bold;");

        HBox phoneContainer = new HBox(10);
        phoneContainer.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> countryCombo = new ComboBox<>();
        countryCombo.getItems().addAll("Tunisie (+216)", "France (+33)");
        countryCombo.setValue("Tunisie (+216)");
        countryCombo.setPrefWidth(150);
        countryCombo.setStyle("-fx-font-size: 13px;");

        TextField phoneField = new TextField(app.phone() != null ? app.phone() : "");
        phoneField.setPromptText("Entrez votre numéro de téléphone");
        phoneField.setPrefWidth(250);

        Label phoneErrorLabel = new Label();
        phoneErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");
        phoneErrorLabel.setVisible(false);

        phoneContainer.getChildren().addAll(countryCombo, phoneField);

        // Cover Letter field
        Label letterLabel = new Label("Lettre de motivation * (50-2000 caractères)");
        letterLabel.setStyle("-fx-font-weight: bold;");

        TextArea letterArea = new TextArea(app.coverLetter() != null ? app.coverLetter() : "");
        letterArea.setPromptText("Expliquez pourquoi vous êtes intéressé(e) par ce poste...");
        letterArea.setPrefRowCount(8);
        letterArea.setWrapText(true);
        letterArea.setStyle("-fx-font-size: 13px;");

        String initialCoverLetter = app.coverLetter() != null ? app.coverLetter() : "";
        Label letterCharCount = new Label(initialCoverLetter.length() + "/2000");
        letterCharCount.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");

        // Update character count in real-time
        letterArea.textProperty().addListener((obs, oldVal, newVal) -> {
            int length = newVal != null ? newVal.length() : 0;
            letterCharCount.setText(length + "/2000");

            if (length > 2000) {
                letterCharCount.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 11px; -fx-font-weight: bold;");
            } else if (length < 50 && length > 0) {
                letterCharCount.setStyle("-fx-text-fill: #fd7e14; -fx-font-size: 11px;");
            } else {
                letterCharCount.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");
            }
        });

        Label letterErrorLabel = new Label();
        letterErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");
        letterErrorLabel.setVisible(false);

        VBox letterBox = new VBox(8);
        letterBox.getChildren().addAll(letterLabel, letterArea, letterCharCount);

        // CV/PDF file selection
        Label pdfLabel = new Label("Télécharger CV (PDF) - Optionnel");
        pdfLabel.setStyle("-fx-font-weight: bold;");

        HBox cvBox = new HBox(10);
        cvBox.setAlignment(Pos.CENTER_LEFT);
        TextField cvPathField = new TextField();
        cvPathField.setPromptText(app.cvPath() != null && !app.cvPath().isEmpty()
                ? "Actuel : " + app.cvPath() : "Aucun fichier sélectionné");
        cvPathField.setEditable(false);
        cvPathField.setPrefWidth(280);

        Button btnBrowseCV = new Button("📂 Parcourir");
        btnBrowseCV.setStyle("-fx-padding: 6 12; -fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 6;");
        btnBrowseCV.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Sélectionner un fichier PDF");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf")
            );
            java.io.File selectedFile = fileChooser.showOpenDialog(null);
            if (selectedFile != null) {
                cvPathField.setText(selectedFile.getAbsolutePath());
            }
        });

        // "Use Profile CV" button
        Button btnUseProfileCv = new Button("📄 CV du profil");
        btnUseProfileCv.setStyle("-fx-padding: 6 12; -fx-background-color: #28a745; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 6; -fx-font-size: 12px;");
        btnUseProfileCv.setOnAction(e -> {
            try {
                Long candId = UserContext.getCandidateId();
                if (candId == null) return;
                Services.user.ProfileService profileSvc = new Services.user.ProfileService();
                Services.user.ProfileService.CandidateInfo cInfo = profileSvc.getCandidateInfo(candId);
                String profileCvPath = cInfo.cvPath();
                if (profileCvPath == null || profileCvPath.isBlank()) {
                    new Alert(Alert.AlertType.INFORMATION,
                            "Aucun CV n'est enregistré dans votre profil.\nVeuillez d'abord télécharger un CV dans votre profil ou utiliser le bouton Parcourir.")
                            .showAndWait();
                    return;
                }
                java.io.File profileCvFile = new java.io.File(profileCvPath);
                if (!profileCvFile.exists()) {
                    new Alert(Alert.AlertType.WARNING,
                            "Le fichier CV de votre profil est introuvable :\n" + profileCvPath)
                            .showAndWait();
                    return;
                }
                cvPathField.setText(profileCvFile.getAbsolutePath());
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR,
                        "Impossible de charger le CV du profil : " + ex.getMessage())
                        .showAndWait();
            }
        });

        cvBox.getChildren().addAll(cvPathField, btnBrowseCV, btnUseProfileCv);

        Button btnGenerateLetter = new Button("🤖 Générer avec l'IA");
        btnGenerateLetter.setStyle("-fx-padding: 6 12; -fx-background-color: #6f42c1; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 6;");
        btnGenerateLetter.setOnAction(e -> generateCoverLetterForEdit(app, letterArea, cvPathField));

        HBox generateRow = new HBox(btnGenerateLetter);
        generateRow.setAlignment(Pos.CENTER_LEFT);

        // Add validation on input change for real-time feedback
        phoneField.textProperty().addListener((obs, oldVal, newVal) -> {
            String country = countryCombo.getValue();
            boolean isValid = "Tunisie (+216)".equals(country)
                    ? ValidationUtils.isValidTunisianPhone(newVal)
                    : ValidationUtils.isValidFrenchPhone(newVal);
            if (newVal.isEmpty()) {
                phoneErrorLabel.setVisible(false);
            } else if (!isValid) {
                phoneErrorLabel.setText(ValidationUtils.getPhoneErrorMessage(
                        "Tunisie (+216)".equals(country) ? "TN" : "FR", newVal));
                phoneErrorLabel.setVisible(true);
            } else {
                phoneErrorLabel.setVisible(false);
            }
        });

        countryCombo.setOnAction(e -> {
            String newVal = phoneField.getText();
            String country = countryCombo.getValue();
            boolean isValid = "Tunisie (+216)".equals(country)
                    ? ValidationUtils.isValidTunisianPhone(newVal)
                    : ValidationUtils.isValidFrenchPhone(newVal);
            if (newVal.isEmpty()) {
                phoneErrorLabel.setVisible(false);
            } else if (!isValid) {
                phoneErrorLabel.setText(ValidationUtils.getPhoneErrorMessage(
                        "Tunisie (+216)".equals(country) ? "TN" : "FR", newVal));
                phoneErrorLabel.setVisible(true);
            } else {
                phoneErrorLabel.setVisible(false);
            }
        });

        content.getChildren().addAll(
                phoneLabel, phoneContainer, phoneErrorLabel,
                letterBox, generateRow, letterErrorLabel,
                pdfLabel, cvBox
        );

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                String country = countryCombo.getValue();
                String phone = phoneField.getText();
                String coverLetter = letterArea.getText();
                String cvPath = cvPathField.getText();

                boolean phoneValid = "Tunisie (+216)".equals(country)
                        ? ValidationUtils.isValidTunisianPhone(phone)
                        : ValidationUtils.isValidFrenchPhone(phone);

                if (!phoneValid) {
                    showAlert("Erreur de validation",
                            ValidationUtils.getPhoneErrorMessage(
                                    "Tunisie (+216)".equals(country) ? "TN" : "FR", phone),
                            Alert.AlertType.ERROR);
                    return;
                }

                if (!ValidationUtils.isValidCoverLetter(coverLetter)) {
                    showAlert("Erreur de validation",
                            ValidationUtils.getCoverLetterErrorMessage(coverLetter),
                            Alert.AlertType.ERROR);
                    return;
                }

                updateApplicationWithTracking(app, phone, coverLetter, cvPath);
            }
        });
    }

    private void generateCoverLetterForEdit(ApplicationService.ApplicationRow app, TextArea letterArea, TextField cvPathField) {
        Alert loadingAlert = new Alert(Alert.AlertType.INFORMATION);
        loadingAlert.setTitle("Generating Cover Letter");
        loadingAlert.setHeaderText(null);
        loadingAlert.setContentText("Generating your personalized cover letter...\nThis may take a moment.");
        loadingAlert.getButtonTypes().setAll(ButtonType.CANCEL);
        loadingAlert.initModality(javafx.stage.Modality.NONE);
        loadingAlert.show();

        new Thread(() -> {
            try {
                UserService.UserInfo candidateInfo = UserService.getUserInfo(app.candidateId());
                if (candidateInfo == null) {
                    javafx.application.Platform.runLater(() -> {
                        loadingAlert.close();
                        showAlert("Error", "Could not retrieve candidate information.", Alert.AlertType.ERROR);
                    });
                    return;
                }

                Models.joboffers.JobOffer offer = JobOfferService.getById(app.offerId());
                String jobTitle = offer != null ? offer.getTitle() : (app.jobTitle() != null ? app.jobTitle() : "Job Offer");
                String companyName = offer != null ? UserService.getRecruiterCompanyName(offer.getRecruiterId()) : null;
                if (companyName == null || companyName.isEmpty()) {
                    companyName = "Our Company";
                }

                String experience = candidateInfo.experienceYears() != null && candidateInfo.experienceYears() > 0
                        ? candidateInfo.experienceYears() + " years of experience"
                        : "No specific experience years provided";

                String education = candidateInfo.educationLevel() != null && !candidateInfo.educationLevel().isEmpty()
                        ? candidateInfo.educationLevel()
                        : "Not specified";

                java.util.List<String> candidateSkills = UserService.getCandidateSkills(app.candidateId());

                String cvContent = "";
                String cvPath = cvPathField.getText();
                if (cvPath == null || cvPath.isBlank()) {
                    cvPath = app.cvPath();
                }
                if (cvPath != null && cvPath.startsWith("Current: ")) {
                    cvPath = cvPath.substring("Current: ".length()).trim();
                }
                if (cvPath != null && !cvPath.isBlank()) {
                    try {
                        FileService fileService = new FileService();
                        cvContent = fileService.extractTextFromPDF(cvPath);
                        if (cvContent == null) {
                            cvContent = "";
                        }
                    } catch (Exception e) {
                        System.err.println("Could not extract CV text: " + e.getMessage());
                        cvContent = "";
                    }
                }

                String generatedCoverLetter = GrokAIService.generateCoverLetter(
                        candidateInfo.firstName() + " " + candidateInfo.lastName(),
                        candidateInfo.email(),
                        candidateInfo.phone(),
                        jobTitle,
                        companyName,
                        experience,
                        education,
                        candidateSkills,
                        cvContent
                );

                javafx.application.Platform.runLater(() -> {
                    loadingAlert.close();

                    if (generatedCoverLetter != null && !generatedCoverLetter.isEmpty()) {
                        Alert reviewAlert = new Alert(Alert.AlertType.INFORMATION);
                        reviewAlert.setTitle("Generated Cover Letter");
                        reviewAlert.setHeaderText("Review and edit as needed:");

                        TextArea textArea = new TextArea(generatedCoverLetter);
                        textArea.setWrapText(true);
                        textArea.setPrefRowCount(15);
                        textArea.setStyle("-fx-font-size: 12px;");

                        reviewAlert.getDialogPane().setContent(textArea);
                        reviewAlert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

                        var result = reviewAlert.showAndWait();
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            letterArea.setText(generatedCoverLetter);
                            showAlert("Success", "Cover letter inserted! You can still edit it.", Alert.AlertType.INFORMATION);
                        }
                    } else {
                        showAlert("Error", "Failed to generate cover letter. Please write one manually.", Alert.AlertType.ERROR);
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    loadingAlert.close();
                    showAlert("Error", "Error generating cover letter: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        }).start();
    }

    private void updateApplicationWithTracking(ApplicationService.ApplicationRow app, String newPhone, String newCoverLetter, String newCvPath) {
        // Track what changed
        List<String> changes = new ArrayList<>();
        String oldPhone = app.phone() != null ? app.phone() : "";
        String oldCoverLetter = app.coverLetter() != null ? app.coverLetter() : "";
        String oldCvPath = app.cvPath() != null ? app.cvPath() : "";

        if (!oldPhone.equals(newPhone)) {
            changes.add("phone number");
        }
        if (!oldCoverLetter.equals(newCoverLetter)) {
            changes.add("cover letter");
        }

        // Handle CV file upload if new file selected
        String finalCvPath = oldCvPath;
        if (newCvPath != null && !newCvPath.isEmpty() && !newCvPath.equals(oldCvPath)) {
            try {
                java.io.File newCvFile = new java.io.File(newCvPath);
                if (newCvFile.exists()) {
                    // Delete old CV if it exists
                    if (!oldCvPath.isEmpty()) {
                        try {
                            FileService fileService = new FileService();
                            fileService.deletePDF(oldCvPath);
                            System.out.println("Old PDF deleted: " + oldCvPath);
                        } catch (Exception e) {
                            System.err.println("Error deleting old PDF: " + e.getMessage());
                        }
                    }

                    // Upload new CV
                    FileService fileService = new FileService();
                    finalCvPath = fileService.uploadPDF(newCvFile);
                    System.out.println("New PDF uploaded: " + finalCvPath);
                    changes.add("CV");
                }
            } catch (Exception e) {
                showAlert("Error", "Failed to upload new CV: " + e.getMessage(), Alert.AlertType.ERROR);
                e.printStackTrace();
                return;
            }
        }

        if (changes.isEmpty()) {
            showAlert("Info", "No changes made", Alert.AlertType.INFORMATION);
            return;
        }

        // Generate note based on changes
        String note = "Candidate changed the " + String.join(" and ", changes);
        System.out.println("Change note: " + note);

        // Update application
        try {
            ApplicationService.update(app.id(), newPhone, newCoverLetter, finalCvPath);
            System.out.println("Application updated in database");

            // Add to status history
            Long candidateId = UserContext.getCandidateId();
            ApplicationStatusHistoryService.addStatusHistory(app.id(), app.currentStatus(), candidateId, note);
            System.out.println("Status history added for application: " + app.id());

            loadApplications();
            showAlert("Success", "Application updated!\n\n" + note, Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Error", "Failed to update application: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void updateApplicationStatus(ApplicationService.ApplicationRow app, String newStatus, String note) {
        try {
            Long recruiterId = UserContext.getRecruiterId();
            String finalNote = (note == null || note.trim().isEmpty())
                    ? generateStatusChangeNote(app.currentStatus(), newStatus) : note;
            ApplicationService.updateStatus(app.id(), newStatus, recruiterId, finalNote);
            loadApplications();
            showAlert("Succès", "Statut mis à jour : " + translateStatus(newStatus), Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Erreur", "Échec de la mise à jour du statut : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private String generateStatusChangeNote(String oldStatus, String newStatus) {
        return switch (newStatus) {
            case "SUBMITTED"   -> "Candidature soumise à nouveau pour examen";
            case "IN_REVIEW"   -> "Le recruteur examine cette candidature";
            case "SHORTLISTED" -> "Le candidat a été présélectionné";
            case "REJECTED"    -> "La candidature a été rejetée";
            case "INTERVIEW"   -> "Le candidat est convoqué à un entretien";
            case "HIRED"       -> "Le candidat a été embauché";
            default            -> "Statut mis à jour : " + newStatus;
        };
    }

    private void acceptApplication(ApplicationService.ApplicationRow app) {
        try {
            Long recruiterId = UserContext.getRecruiterId();
            String note = "Le recruteur apprécie ce profil et a présélectionné le candidat";
            ApplicationService.updateStatus(app.id(), "SHORTLISTED", recruiterId, note);
            loadApplications();
            showAlert("Succès", "Candidature acceptée !\nLe candidat a été présélectionné.", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Erreur", "Échec de l'acceptation : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void startReviewApplication(ApplicationService.ApplicationRow app) {
        try {
            Long recruiterId = UserContext.getRecruiterId();
            String note = "Le recruteur a commencé l'examen de cette candidature";
            ApplicationService.updateStatus(app.id(), "IN_REVIEW", recruiterId, note);
            loadApplications();
            showAlert("Succès", "Statut passé en révision.", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Erreur", "Échec du changement de statut : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void rejectApplication(ApplicationService.ApplicationRow app) {
        try {
            Long recruiterId = UserContext.getRecruiterId();
            String note = "Le recruteur a examiné le profil et décidé de ne pas poursuivre";
            ApplicationService.updateStatus(app.id(), "REJECTED", recruiterId, note);
            loadApplications();
            showAlert("Succès", "Candidature rejetée.", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Erreur", "Échec du rejet de la candidature : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void deleteApplication(ApplicationService.ApplicationRow app) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Supprimer la candidature");
        confirmation.setHeaderText("Êtes-vous sûr ?");
        confirmation.setContentText("Cette action est irréversible.");
        confirmation.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                ApplicationService.delete(app.id());
                loadApplications();
                showAlert("Succès", "Candidature supprimée.", Alert.AlertType.INFORMATION);
            }
        });
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void downloadPDF(ApplicationService.ApplicationRow app) {
        try {
            java.io.File pdfFile = ApplicationService.downloadPDF(app.id());
            if (javafx.application.HostServices.class != null) {
                Desktop.getDesktop().open(pdfFile);
                showAlert("Succès", "Ouverture du fichier PDF...", Alert.AlertType.INFORMATION);
            }
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir le PDF : " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void editStatusHistory(ApplicationStatusHistoryService.StatusHistoryRow record) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier l'historique");
        dialog.setHeaderText("Entrée #" + record.id());

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        Label statusLabel = new Label("Statut :");
        statusLabel.setStyle("-fx-font-weight: bold;");

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("SUBMITTED", "IN_REVIEW", "SHORTLISTED", "REJECTED", "INTERVIEW", "HIRED");
        statusCombo.setValue(record.status() != null ? record.status() : "SUBMITTED");
        statusCombo.setPrefWidth(240);
        statusCombo.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty); setText(empty || item == null ? null : translateStatus(item));
            }
        });
        statusCombo.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty); setText(empty || item == null ? null : translateStatus(item));
            }
        });

        Label dateLabel = new Label("Modifié le :");
        dateLabel.setStyle("-fx-font-weight: bold;");
        TextField dateField = new TextField(record.changedAt() != null ?
                record.changedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A");
        dateField.setEditable(false);
        dateField.setStyle("-fx-opacity: 0.7;");

        Label noteLabel = new Label("Note * (5-255 caractères)");
        noteLabel.setStyle("-fx-font-weight: bold;");
        TextArea noteArea = new TextArea(record.note() != null ? record.note() : "");
        noteArea.setPromptText("Modifier la note... (min 5, max 255 caractères)");
        noteArea.setPrefRowCount(6);
        noteArea.setWrapText(true);
        noteArea.setStyle("-fx-font-size: 13px;");

        String initialNote = record.note() != null ? record.note() : "";
        Label noteCharCount = new Label(initialNote.length() + "/255");
        noteCharCount.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");

        Label noteErrorLabel = new Label();
        noteErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");
        noteErrorLabel.setVisible(false);

        // Update character count in real-time
        noteArea.textProperty().addListener((obs, oldVal, newVal) -> {
            int length = newVal != null ? newVal.length() : 0;
            noteCharCount.setText(length + "/255");

            if (length > 255) {
                noteCharCount.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 11px; -fx-font-weight: bold;");
                noteErrorLabel.setText(ValidationUtils.getNoteErrorMessage(newVal));
                noteErrorLabel.setVisible(true);
            } else if (length > 0 && length < 5) {
                noteCharCount.setStyle("-fx-text-fill: #fd7e14; -fx-font-size: 11px;");
                noteErrorLabel.setText(ValidationUtils.getNoteErrorMessage(newVal));
                noteErrorLabel.setVisible(true);
            } else if (length == 0) {
                noteCharCount.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");
                noteErrorLabel.setText(ValidationUtils.getNoteErrorMessage(newVal));
                noteErrorLabel.setVisible(true);
            } else {
                noteCharCount.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");
                noteErrorLabel.setVisible(false);
            }
        });

        VBox noteBox = new VBox(8);
        noteBox.getChildren().addAll(noteLabel, noteArea, noteCharCount, noteErrorLabel);

        HBox statusRow = new HBox(12);
        statusRow.getChildren().addAll(statusLabel, statusCombo);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        content.getChildren().addAll(
                statusRow,
                dateLabel, dateField,
                noteBox
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                String noteText = noteArea.getText();
                String selectedStatus = statusCombo.getValue();

                // Validate note before saving
                if (!ValidationUtils.isValidNote(noteText)) {
                    showAlert("Erreur de validation",
                            ValidationUtils.getNoteErrorMessage(noteText),
                            Alert.AlertType.ERROR);
                    return;
                }
                try {
                    ApplicationStatusHistoryService.updateStatusHistory(record.id(), selectedStatus, noteText);
                    loadApplications();
                    showAlert("Succès", "Historique mis à jour.", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Erreur", "Échec de la mise à jour : " + e.getMessage(), Alert.AlertType.ERROR);
                    e.printStackTrace();
                }
            }
        });
    }

    private void deleteStatusHistory(ApplicationStatusHistoryService.StatusHistoryRow record, ApplicationService.ApplicationRow app) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Supprimer l'entrée");
        confirmation.setHeaderText("Êtes-vous sûr ?");
        confirmation.setContentText("Supprimer cette entrée de l'historique ?\nCette action est irréversible.");
        confirmation.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    ApplicationStatusHistoryService.deleteStatusHistory(record.id());
                    loadApplications();
                    showAlert("Succès", "Entrée supprimée.", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Erreur", "Échec de la suppression : " + e.getMessage(), Alert.AlertType.ERROR);
                    e.printStackTrace();
                }
            }
        });
    }

    private void bulkUpdateStatus(List<Long> applicationIds, String newStatus, String note) {
        try {
            Long recruiterId = UserContext.getRecruiterId();
            String finalNote = (note == null || note.trim().isEmpty())
                    ? generateStatusChangeNote("MULTIPLE", newStatus) : note;
            ApplicationService.bulkUpdateStatus(applicationIds, newStatus, recruiterId, finalNote);
            selectedApplicationIds.clear();
            loadApplications();
            showAlert("Succès", applicationIds.size() + " candidature(s) mise(s) à jour : "
                    + translateStatus(newStatus), Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Erreur", "Échec de la mise à jour groupée : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void updateRankingStatus(String text, boolean success) {
        if (rankingStatusLabel == null) return;
        rankingStatusLabel.setText(text);
        rankingStatusLabel.setStyle(success
                ? "-fx-text-fill: #28a745; -fx-font-weight: bold;"
                : "-fx-text-fill: #dc3545; -fx-font-weight: bold;");
    }

    private void rankApplicationsWithAI() {
        if (UserContext.getRole() != UserContext.Role.RECRUITER) {
            showAlert("Warning", "Only recruiters can rank applications", Alert.AlertType.WARNING);
            return;
        }
        if (currentApplications == null || currentApplications.isEmpty()) {
            showAlert("Info", "No applications to rank", Alert.AlertType.INFORMATION);
            return;
        }

        rankingCache.clear();
        rankingActive = true;
        updateRankingStatus("Ranking: 0/" + currentApplications.size(), false);

        new Thread(() -> {
            int total = currentApplications.size();
            int done = 0;

            for (ApplicationService.ApplicationRow app : currentApplications) {
                OllamaRankingService.RankResult result = buildRankForApplication(app);
                if (result != null) {
                    rankingCache.put(app.id(), result);
                }
                done++;
                int progress = done;

                javafx.application.Platform.runLater(() -> {
                    updateRankingStatus("Ranking: " + progress + "/" + total, false);
                    if (progress == total) {
                        updateRankingStatus("Ranking: COMPLETE", true);
                        renderApplications(currentApplications, true);
                    }
                });
            }
        }).start();
    }

    private OllamaRankingService.RankResult buildRankForApplication(ApplicationService.ApplicationRow app) {
        try {
            Models.joboffers.JobOffer offer = JobOfferService.getById(app.offerId());
            List<String> offerSkills = JobOfferService.getOfferSkills(app.offerId());

            UserService.UserInfo userInfo = UserService.getUserInfo(app.candidateId());
            String experience = userInfo != null && userInfo.experienceYears() != null && userInfo.experienceYears() > 0
                    ? userInfo.experienceYears() + " years of experience"
                    : "No specific experience years provided";
            String education = userInfo != null && userInfo.educationLevel() != null && !userInfo.educationLevel().isEmpty()
                    ? userInfo.educationLevel()
                    : "Not specified";

            List<String> candidateSkills = UserService.getCandidateSkills(app.candidateId());
            String cvContent = extractCvText(app.cvPath());

                String jobTitle = offer != null ? offer.getTitle() : (app.jobTitle() != null ? app.jobTitle() : "Job Offer");
                String jobDescription = offer != null ? offer.getDescription() : "";

            return OllamaRankingService.rankApplication(
                    jobTitle,
                    jobDescription,
                    offerSkills,
                    app.candidateName() != null ? app.candidateName() : "Candidate",
                    experience,
                    education,
                    candidateSkills,
                    app.coverLetter(),
                    cvContent
            );
        } catch (Exception e) {
            System.err.println("Ranking failed for application " + app.id() + ": " + e.getMessage());
            return null;
        }
    }

    private String extractCvText(String cvPath) {
        if (cvPath == null || cvPath.isEmpty()) {
            return "";
        }
        try {
            FileService fileService = new FileService();
            String text = fileService.extractTextFromPDF(cvPath);
            return text == null ? "" : text;
        } catch (Exception e) {
            System.err.println("Failed to extract CV text: " + e.getMessage());
            return "";
        }
    }

    private void showInterviewScheduleDialog(ApplicationService.ApplicationRow app) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Planifier un Entretien");
        dialog.setHeaderText("Pour : " + (app.candidateName() != null && !app.candidateName().isBlank()
                ? app.candidateName() : "Candidat #" + app.id()));

        VBox content = new VBox(14);
        content.setPadding(new Insets(20));

        // DatePicker — default tomorrow, block past dates
        DatePicker datePicker = new DatePicker(java.time.LocalDate.now().plusDays(1));
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(java.time.LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                // Disable days strictly before today
                if (item.isBefore(java.time.LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #f8d7da;");
                }
            }
        });
        datePicker.setEditable(false);
        datePicker.setPrefWidth(350);

        // Time field with hint label
        VBox timeBox = new VBox(4);
        TextField timeField = new TextField("14:00");
        timeField.setPrefWidth(350);
        Label timeHint = new Label("Format: HH:mm  (ex: 09:30, 14:00, 16:45)");
        timeHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
        // Live format feedback — accept H:mm or HH:mm
        timeField.textProperty().addListener((obs, old, val) -> {
            boolean valid = parseTime(val) != null;
            timeField.setStyle(valid
                    ? "-fx-border-color: #28a745; -fx-border-radius: 4;"
                    : "-fx-border-color: #dc3545; -fx-border-radius: 4;");
        });
        timeBox.getChildren().addAll(timeField, timeHint);

        TextField durationField = new TextField("60");
        ComboBox<String> modeCombo = new ComboBox<>();
        modeCombo.getItems().addAll("ONLINE", "ON_SITE");
        modeCombo.setValue("ONLINE");

        // Meeting link row (ONLINE)
        TextField linkField = new TextField();
        linkField.setEditable(false);
        linkField.setStyle("-fx-background-color: #f8f9fa;");
        linkField.setPromptText("Cliquez 'Générer' pour créer un lien automatique...");
        Button genBtn = new Button("🔗 Générer");
        genBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 8 14;");
        genBtn.setOnAction(ev -> {
            try {
                if (datePicker.getValue() == null) {
                    showAlert("Erreur", "Veuillez d'abord sélectionner une date.", Alert.AlertType.WARNING);
                    return;
                }
                java.time.LocalTime parsedTime = parseTime(timeField.getText());
                if (parsedTime == null) {
                    showAlert("Erreur", "Format d'heure invalide. Utilisez HH:mm (ex: 14:00).", Alert.AlertType.WARNING);
                    return;
                }
                LocalDateTime dt = LocalDateTime.of(datePicker.getValue(), parsedTime);
                int dur = 60;
                try { dur = Integer.parseInt(durationField.getText().trim()); } catch (Exception ignored) {}
                linkField.setText(MeetingService.generateMeetingLink(app.id(), dt, dur));
                linkField.setStyle("-fx-background-color: #d4edda;");
            } catch (Exception ex) {
                showAlert("Erreur", "Impossible de générer le lien: " + ex.getMessage(), Alert.AlertType.WARNING);
            }
        });
        HBox linkRow = new HBox(10, linkField, genBtn);
        HBox.setHgrow(linkField, Priority.ALWAYS);

        // Location field (ON_SITE)
        TextField locationField = new TextField();
        locationField.setPromptText("ex: Bâtiment A, Salle 301");

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Notes supplémentaires...");
        notesArea.setPrefRowCount(3);

        VBox linkBox     = buildFieldBox("🔗 Lien de réunion (ONLINE)", linkRow);
        VBox locationBox = buildFieldBox("📍 Lieu (ON_SITE)", locationField);

        Runnable toggleMode = () -> {
            boolean online = "ONLINE".equals(modeCombo.getValue());
            linkBox.setVisible(online);      linkBox.setManaged(online);
            locationBox.setVisible(!online); locationBox.setManaged(!online);
        };
        modeCombo.valueProperty().addListener((o, ov, nv) -> toggleMode.run());
        toggleMode.run();

        // Validation hint label
        Label validationHint = new Label("");
        validationHint.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px; -fx-font-weight: 600;");
        validationHint.setWrapText(true);

        content.getChildren().addAll(
                buildFieldBox("📅 Date *", datePicker),
                buildFieldBox("🕐 Heure *", timeBox),
                buildFieldBox("⏱ Durée (minutes) *", durationField),
                buildFieldBox("🎯 Mode *", modeCombo),
                linkBox, locationBox,
                buildFieldBox("📝 Notes", notesArea),
                validationHint
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(480);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(480);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("✔ Planifier");
        okBtn.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6;");

        // Prevent dialog from closing on invalid input
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String error = validateScheduleForm(
                    datePicker, timeField, durationField, modeCombo, linkField, locationField);
            if (error != null) {
                validationHint.setText("⚠ " + error);
                event.consume(); // don't close dialog
            }
        });

        dialog.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;
            try {
                java.time.LocalTime parsedTime = parseTime(timeField.getText());
                if (parsedTime == null) throw new Exception("Heure invalide.");
                LocalDateTime scheduledAt = LocalDateTime.of(datePicker.getValue(), parsedTime);
                int duration = Integer.parseInt(durationField.getText().trim());
                String mode = modeCombo.getValue();

                if ("ONLINE".equals(mode) && (linkField.getText() == null || linkField.getText().isBlank()))
                    linkField.setText(MeetingService.generateMeetingLink(app.id(), scheduledAt, duration));

                Models.interview.Interview interview = new Models.interview.Interview(
                        app.id(), UserContext.getRecruiterId(), scheduledAt, duration, mode);
                interview.setStatus("SCHEDULED");
                interview.setNotes(notesArea.getText());
                if ("ONLINE".equals(mode)) interview.setMeetingLink(linkField.getText().trim());
                else                       interview.setLocation(locationField.getText().trim());

                InterviewService.addInterview(interview);
                ApplicationService.updateStatus(app.id(), "INTERVIEW", UserContext.getRecruiterId(),
                        "Entretien planifié pour le " + scheduledAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

                // Send immediate interview-scheduled confirmation email to candidate
                sendInterviewScheduledEmail(app, interview);

                showAlert("Succès", "Entretien planifié avec succès !\nUn email de confirmation a été envoyé au candidat.", Alert.AlertType.INFORMATION);
                loadApplications();
            } catch (Exception e) {
                showAlert("Erreur", e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    /** Returns an error message string if invalid, null if valid */
    private String validateScheduleForm(DatePicker datePicker, TextField timeField,
                                        TextField durationField, ComboBox<String> modeCombo,
                                        TextField linkField, TextField locationField) {
        if (datePicker.getValue() == null)
            return "Veuillez sélectionner une date.";

        java.time.LocalTime time = parseTime(timeField.getText());
        if (time == null)
            return "Heure invalide. Utilisez le format HH:mm (ex: 14:00).";

        LocalDateTime scheduledAt = LocalDateTime.of(datePicker.getValue(), time);
        if (scheduledAt.isBefore(LocalDateTime.now()))
            return "La date et l'heure doivent être dans le futur.";

        try {
            int dur = Integer.parseInt(durationField.getText().trim());
            if (dur <= 0) return "La durée doit être supérieure à 0.";
            if (dur > 480) return "La durée ne peut pas dépasser 480 minutes (8h).";
        } catch (NumberFormatException e) {
            return "La durée doit être un nombre entier valide.";
        }

        if (modeCombo.getValue() == null) return "Veuillez sélectionner un mode.";

        if ("ON_SITE".equals(modeCombo.getValue()) &&
                (locationField.getText() == null || locationField.getText().isBlank()))
            return "Le lieu est requis pour les entretiens sur site.";

        return null; // valid
    }

    /**
     * Robustly parse a time string in H:mm or HH:mm format.
     * Returns null if the string cannot be parsed.
     */
    private java.time.LocalTime parseTime(String text) {
        if (text == null || text.isBlank()) return null;
        text = text.trim();
        for (java.time.format.DateTimeFormatter fmt : new java.time.format.DateTimeFormatter[]{
                java.time.format.DateTimeFormatter.ofPattern("H:mm"),
                java.time.format.DateTimeFormatter.ofPattern("HH:mm"),
                java.time.format.DateTimeFormatter.ofPattern("H:m"),
                java.time.format.DateTimeFormatter.ISO_LOCAL_TIME
        }) {
            try {
                return java.time.LocalTime.parse(text, fmt);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private VBox buildFieldBox(String label, javafx.scene.Node field) {
        VBox box = new VBox(6);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        if (field instanceof TextField tf) tf.setPrefWidth(350);
        if (field instanceof ComboBox<?> cb) cb.setPrefWidth(350);
        box.getChildren().addAll(lbl, field);
        return box;
    }

    /**
     * Sends an immediate interview-scheduled confirmation email to the candidate.
     * Runs in a background thread so it never blocks the UI.
     */
    private void sendInterviewScheduledEmail(ApplicationService.ApplicationRow app,
                                             Models.interview.Interview interview) {
        new Thread(() -> {
            try {
                // Look up candidate contact info
                UserService.UserInfo info = UserService.getUserInfo(app.candidateId());
                if (info == null || info.email() == null || info.email().isBlank()) {
                    System.err.println("[AppController] No email for candidate " + app.candidateId() + " — skipping interview confirmation.");
                    return;
                }

                String candidateName = ((info.firstName() != null ? info.firstName() : "") + " "
                        + (info.lastName() != null ? info.lastName() : "")).trim();
                if (candidateName.isEmpty()) candidateName = "Candidat";

                System.out.println("[AppController] Sending interview confirmation to: " + info.email());
                boolean sent = Services.application.EmailServiceApplication.sendInterviewScheduledConfirmation(
                        info.email(),
                        candidateName,
                        app.jobTitle() != null ? app.jobTitle() : "Offre d'emploi",
                        interview
                );
                System.out.println("[AppController] Interview confirmation email " + (sent ? "sent ✓" : "FAILED ✗"));
            } catch (Exception e) {
                System.err.println("[AppController] Failed to send interview confirmation: " + e.getMessage());
            }
        }, "InterviewConfirmEmail").start();
    }
}