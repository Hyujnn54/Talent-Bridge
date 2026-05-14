package Controllers.joboffers;

import Models.joboffers.JobOffer;
import Models.joboffers.JobOfferWarning;
import Models.joboffers.WarningCorrection;
import Models.joboffers.OfferSkill;
import Models.joboffers.ContractType;
import Models.joboffers.Status;
import Models.joboffers.SkillLevel;
import Services.joboffers.JobOfferService;
import Services.joboffers.JobOfferWarningService;
import Services.joboffers.WarningCorrectionService;
import Services.joboffers.OfferSkillService;
import Services.joboffers.FuzzySearchService;
import Services.joboffers.RecommendationService;
import Services.joboffers.NotificationService;
import Utils.UserContext;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JobOffersController {

    @FXML private VBox mainContainer;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbSearchCriteria;
    @FXML private Button btnSearch;
    @FXML private Button btnClearSearch;

    private VBox jobListContainer;
    private VBox detailContainer;
    private JobOffer selectedJob;

    private ComboBox<String> cbFilterType;
    private ComboBox<String> cbFilterLocation;
    private ContractType selectedContractType = null;
    private String selectedLocation = null;

    private JobOfferService jobOfferService;
    private OfferSkillService offerSkillService;
    private JobOfferWarningService warningService;
    private WarningCorrectionService correctionService;
    private RecommendationService recommendationService;

    // Form fields
    private TextField formTitleField;
    private TextArea formDescription;
    private TextField formLocation;
    private ComboBox<ContractType> formContractType;
    private DatePicker formDeadline;
    private ComboBox<Status> formStatus;

    // Inline error labels
    private Label titleErrorLabel;
    private Label descriptionErrorLabel;
    private Label locationErrorLabel;
    private Label deadlineErrorLabel;
    private Label skillsErrorLabel;

    // Skills
    private VBox skillsContainer;
    private List<SkillRow> skillRows;
    private boolean isEditMode = false;
    private JobOffer editingJob = null;

    // =========================================================================
    // Init
    // =========================================================================

    @FXML
    public void initialize() {
        jobOfferService  = new JobOfferService();
        offerSkillService = new OfferSkillService();
        warningService   = new JobOfferWarningService();
        correctionService = new WarningCorrectionService();
        recommendationService = new RecommendationService();
        skillRows = new ArrayList<>();
        buildUI();
        loadJobOffers();
        checkForWarnings();
    }

    // =========================================================================
    // Warning check on startup
    // =========================================================================

    private void checkForWarnings() {
        try {
            Long recruiterId = UserContext.getRecruiterId();
            int count = warningService.countPendingWarningsForRecruiter(recruiterId);
            if (count > 0) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Attention");
                alert.setHeaderText("Vous avez " + count + " offre(s) signalee(s)");
                alert.setContentText("Un administrateur a signale un probleme avec certaines de vos offres.\n" +
                        "Veuillez consulter les offres marquees en jaune et les corriger ou supprimer.");
                alert.show();
            }
        } catch (SQLException e) {
            System.err.println("Erreur verification avertissements: " + e.getMessage());
        }
    }

    // =========================================================================
    // Build UI
    // =========================================================================

    private void buildUI() {
        if (mainContainer == null) return;
        mainContainer.getChildren().clear();
        mainContainer.setStyle("-fx-background-color: #EBF0F8; -fx-padding: 20; -fx-spacing: 14;");

        // ── Header ──
        HBox headerBox = new HBox(12);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        VBox titleBox = new VBox(3);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        Label pageTitle = new Label("Gestion de mes offres");
        pageTitle.setStyle("-fx-font-size:24px; -fx-font-weight:700; -fx-text-fill:#2c3e50;");
        Label pageSub = new Label("Créez, modifiez et suivez vos offres d'emploi");
        pageSub.setStyle("-fx-font-size:12px; -fx-text-fill:#8FA3B8;");
        titleBox.getChildren().addAll(pageTitle, pageSub);
        Button btnCreate = new Button("+ Nouvelle offre");
        btnCreate.setStyle("-fx-background-color:#28a745; -fx-text-fill:white; -fx-font-weight:700; " +
                "-fx-font-size:13px; -fx-padding:10 20; -fx-background-radius:9; -fx-cursor:hand;");
        btnCreate.setOnAction(e -> showCreateForm());
        headerBox.getChildren().addAll(titleBox, btnCreate);
        mainContainer.getChildren().add(headerBox);

        // ── Search/filter bar ──
        mainContainer.getChildren().add(createSearchFilterBox());

        // ── SplitPane ──
        javafx.scene.control.SplitPane split = new javafx.scene.control.SplitPane();
        split.setDividerPositions(0.40);
        VBox.setVgrow(split, Priority.ALWAYS);
        split.setStyle("-fx-background-color: transparent; -fx-box-border: transparent; -fx-padding:0;");

        // Left side
        javafx.scene.control.ScrollPane leftScroll = new javafx.scene.control.ScrollPane();
        leftScroll.setFitToWidth(true);
        leftScroll.setStyle("-fx-background: transparent; -fx-background-color: #EBF0F8;");
        leftScroll.getStyleClass().add("transparent-scroll");
        VBox leftContent = new VBox(10);
        leftContent.setStyle("-fx-background-color: #EBF0F8;");
        leftContent.setPadding(new Insets(4, 8, 16, 2));

        HBox leftHeader = new HBox(8);
        leftHeader.setAlignment(Pos.CENTER_LEFT);
        leftHeader.setStyle("-fx-padding: 0 0 4 2;");
        Label leftTitle = new Label("Mes offres d'emploi");
        leftTitle.setStyle("-fx-font-size:13px; -fx-font-weight:700; -fx-text-fill:#2c3e50;");
        HBox.setHgrow(leftTitle, Priority.ALWAYS);
        jobCountLabel = new Label("");
        jobCountLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#8FA3B8; " +
                "-fx-background-color:#D4DCE8; -fx-background-radius:10; -fx-padding:2 8;");
        leftHeader.getChildren().addAll(leftTitle, jobCountLabel);

        jobListContainer = new VBox(8);
        leftContent.getChildren().addAll(leftHeader, jobListContainer);
        leftScroll.setContent(leftContent);

        // Right side
        javafx.scene.control.ScrollPane rightScroll = new javafx.scene.control.ScrollPane();
        rightScroll.setFitToWidth(true);
        rightScroll.setStyle("-fx-background: transparent; -fx-background-color: #EBF0F8;");
        rightScroll.getStyleClass().add("transparent-scroll");
        VBox rightContent = new VBox(14);
        rightContent.setStyle("-fx-background-color: #EBF0F8;");
        rightContent.setPadding(new Insets(4, 4, 16, 8));

        detailContainer = new VBox(14);
        // Placeholder
        VBox ph = new VBox(16);
        ph.setAlignment(Pos.CENTER);
        ph.setStyle("-fx-background-color:white; -fx-background-radius:14; -fx-padding:50 24;" +
                "-fx-border-color:#E8EEF8; -fx-border-width:1; -fx-border-radius:14;" +
                "-fx-effect:dropshadow(gaussian,rgba(91,163,245,0.09),12,0,0,3);");
        Label phIcon = new Label("💼"); phIcon.setStyle("-fx-font-size:40px;");
        Label phTitle = new Label("Sélectionnez une offre");
        phTitle.setStyle("-fx-font-size:14px; -fx-font-weight:700; -fx-text-fill:#2c3e50;");
        Label phSub = new Label("Cliquez sur une offre pour voir les détails et la gérer");
        phSub.setStyle("-fx-font-size:12px; -fx-text-fill:#8FA3B8; -fx-text-alignment:center;");
        phSub.setWrapText(true);
        ph.getChildren().addAll(phIcon, phTitle, phSub);
        detailContainer.getChildren().add(ph);

        rightContent.getChildren().add(detailContainer);
        rightScroll.setContent(rightContent);

        split.getItems().addAll(leftScroll, rightScroll);
        mainContainer.getChildren().add(split);
    }

    // =========================================================================
    // Search & filter box
    // =========================================================================

    private VBox createSearchFilterBox() {
        VBox container = new VBox(0);
        container.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 3);");

        HBox searchRow = new HBox(12);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.setStyle("-fx-padding: 18 20 12 20;");

        txtSearch = new TextField();
        txtSearch.setPromptText("Rechercher dans mes offres...");
        txtSearch.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 10 15; " +
                "-fx-border-color: #e9ecef; -fx-border-radius: 8; -fx-font-size: 13px; -fx-pref-height: 38;");
        HBox.setHgrow(txtSearch, Priority.ALWAYS);
        txtSearch.setOnAction(e -> handleSearch());

        Button btnSearchAction = new Button("Rechercher");
        btnSearchAction.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-font-size: 13px; " +
                "-fx-font-weight: 600; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        btnSearchAction.setOnAction(e -> handleSearch());

        searchRow.getChildren().addAll(txtSearch, btnSearchAction);

        Separator separator = new Separator();

        HBox filterRow = new HBox(12);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        filterRow.setStyle("-fx-padding: 12 20 18 20;");

        Label filterLabel = new Label("Filtres:");
        filterLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #495057;");

        cbFilterType = new ComboBox<>();
        cbFilterType.setPromptText("Type");
        cbFilterType.getItems().add("Tous");
        for (ContractType t : ContractType.values()) cbFilterType.getItems().add(formatContractType(t));
        cbFilterType.setStyle("-fx-pref-width: 130; -fx-pref-height: 34;");
        cbFilterType.setOnAction(e -> applyFilters());

        cbFilterLocation = new ComboBox<>();
        cbFilterLocation.setPromptText("Lieu");
        cbFilterLocation.getItems().add("Tous");
        try {
            cbFilterLocation.getItems().addAll(jobOfferService.getAllLocations());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        cbFilterLocation.setStyle("-fx-pref-width: 130; -fx-pref-height: 34;");
        cbFilterLocation.setOnAction(e -> applyFilters());

        Button btnReset = new Button("Reinitialiser");
        btnReset.setStyle("-fx-background-color: #f8f9fa; -fx-text-fill: #6c757d; -fx-font-size: 12px; " +
                "-fx-padding: 8 14; -fx-background-radius: 6; -fx-cursor: hand; " +
                "-fx-border-color: #dee2e6; -fx-border-radius: 6;");
        btnReset.setOnAction(e -> resetFilters());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label resultCount = new Label("");
        resultCount.setId("resultCount");
        resultCount.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px;");

        filterRow.getChildren().addAll(filterLabel, cbFilterType, cbFilterLocation, btnReset, spacer, resultCount);
        container.getChildren().addAll(searchRow, separator, filterRow);
        return container;
    }

    private void applyFilters() {
        String tv = cbFilterType.getValue();
        String lv = cbFilterLocation.getValue();
        selectedContractType = (tv == null || tv.equals("Tous")) ? null : getContractTypeFromLabel(tv);
        selectedLocation     = (lv == null || lv.equals("Tous")) ? null : lv;
        loadFilteredJobOffers();
    }

    private void resetFilters() {
        selectedContractType = null;
        selectedLocation     = null;
        if (cbFilterType     != null) cbFilterType.setValue(null);
        if (cbFilterLocation != null) cbFilterLocation.setValue(null);
        if (txtSearch        != null) txtSearch.clear();
        loadJobOffers();
    }

    private void loadFilteredJobOffers() {
        if (jobListContainer == null) return;
        jobListContainer.getChildren().clear();
        try {
            Long recruiterId = UserContext.getRecruiterId();
            List<JobOffer> jobs = (recruiterId != null)
                    ? JobOfferService.getByRecruiterId(recruiterId)
                    : new java.util.ArrayList<>();
            // Apply contract type filter
            if (selectedContractType != null) {
                jobs = jobs.stream().filter(j -> j.getContractType() == selectedContractType).toList();
            }
            // Apply location filter
            if (selectedLocation != null && !selectedLocation.isBlank()) {
                jobs = jobs.stream().filter(j -> selectedLocation.equals(j.getLocation())).toList();
            }
            String kw = txtSearch != null ? txtSearch.getText().trim().toLowerCase() : "";
            if (!kw.isEmpty()) {
                jobs = jobs.stream()
                        .filter(j -> (j.getTitle()       != null && j.getTitle().toLowerCase().contains(kw)) ||
                                     (j.getDescription() != null && j.getDescription().toLowerCase().contains(kw)) ||
                                     (j.getLocation()    != null && j.getLocation().toLowerCase().contains(kw)))
                        .toList();
            }
            updateResultCount(jobs.size());
            if (jobCountLabel != null) jobCountLabel.setText(jobs.size() + " offre(s)");
            if (jobs.isEmpty()) { jobListContainer.getChildren().add(createEmptyState()); return; }
            boolean first = true;
            for (JobOffer job : jobs) {
                VBox card = createJobCard(job);
                jobListContainer.getChildren().add(card);
                if (first) { selectJob(job, card); first = false; }
            }
        } catch (Exception e) {
            showAlert("Erreur", "Impossible de charger: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void updateResultCount(int count) {
        if (mainContainer == null) return;
        Label lbl = (Label) mainContainer.lookup("#resultCount");
        if (lbl != null) lbl.setText(count == 0 ? "Aucun resultat" : count + " offre(s)");
    }

    private VBox createEmptyState() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-padding: 30;");
        Label icon = new Label("📋");
        icon.setStyle("-fx-font-size: 36px;");
        Label text = new Label("Aucune offre trouvee");
        text.setStyle("-fx-font-size: 14px; -fx-text-fill: #6c757d;");
        box.getChildren().addAll(icon, text);
        return box;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String formatContractType(ContractType type) {
        return switch (type) {
            case CDI       -> "CDI";
            case CDD       -> "CDD";
            case INTERNSHIP -> "Stage";
            case FREELANCE  -> "Freelance";
            case PART_TIME  -> "Temps Partiel";
            case FULL_TIME  -> "Temps Plein";
        };
    }

    private ContractType getContractTypeFromLabel(String label) {
        return switch (label) {
            case "CDI"          -> ContractType.CDI;
            case "CDD"          -> ContractType.CDD;
            case "Stage"        -> ContractType.INTERNSHIP;
            case "Freelance"    -> ContractType.FREELANCE;
            case "Temps Partiel" -> ContractType.PART_TIME;
            case "Temps Plein"  -> ContractType.FULL_TIME;
            default -> null;
        };
    }

    private String formatSkillLevel(SkillLevel level) {
        return switch (level) {
            case BEGINNER     -> "Debutant";
            case INTERMEDIATE -> "Intermediaire";
            case ADVANCED     -> "Avance";
        };
    }

    // =========================================================================
    // List panel
    // =========================================================================

    private Label jobCountLabel; // tracks how many offers shown

    private VBox createJobListPanel() {
        VBox panel = new VBox(12);
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 18; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 2);");

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Mes offres d'emploi");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
        HBox.setHgrow(title, Priority.ALWAYS);
        jobCountLabel = new Label("");
        jobCountLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8FA3B8; " +
                "-fx-background-color: #EBF0F8; -fx-background-radius: 10; -fx-padding: 2 8;");
        titleRow.getChildren().addAll(title, jobCountLabel);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        jobListContainer = new VBox(8);
        jobListContainer.setStyle("-fx-padding: 5 5 5 0;");
        scroll.setContent(jobListContainer);

        panel.getChildren().addAll(titleRow, scroll);
        return panel;
    }

    // =========================================================================
    // Detail panel
    // =========================================================================

    private VBox createDetailPanel() {
        VBox panel = new VBox(15);
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 2);");

        HBox topBar = new HBox(15);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Details");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
        HBox.setHgrow(title, Priority.ALWAYS);

        Button btnCreate = new Button("+ Nouvelle offre");
        btnCreate.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: 600; " +
                "-fx-font-size: 13px; -fx-padding: 10 18; -fx-background-radius: 8; -fx-cursor: hand;");
        btnCreate.setOnAction(e -> showCreateForm());

        topBar.getChildren().addAll(title, btnCreate);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        detailContainer = new VBox(20);
        detailContainer.setStyle("-fx-padding: 10 5 10 0;");
        scrollPane.setContent(detailContainer);

        // Placeholder
        VBox placeholder = new VBox(14);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setStyle("-fx-padding: 60 24;");
        Label phIcon = new Label("💼"); phIcon.setStyle("-fx-font-size:40px;");
        Label phTitle = new Label("Sélectionnez une offre");
        phTitle.setStyle("-fx-font-size:15px; -fx-font-weight:700; -fx-text-fill:#2c3e50;");
        Label phSub = new Label("Cliquez sur une offre à gauche pour voir les détails et gérer l'annonce");
        phSub.setStyle("-fx-font-size:12px; -fx-text-fill:#8FA3B8; -fx-text-alignment:center;");
        phSub.setWrapText(true);
        placeholder.getChildren().addAll(phIcon, phTitle, phSub);
        detailContainer.getChildren().add(placeholder);

        panel.getChildren().addAll(topBar, scrollPane);
        return panel;
    }

    // =========================================================================
    // Load all offers
    // =========================================================================

    private void loadJobOffers() {
        if (jobListContainer == null) return;
        jobListContainer.getChildren().clear();
        try {
            Long recruiterId = UserContext.getRecruiterId();
            List<JobOffer> jobs = (recruiterId != null)
                    ? JobOfferService.getByRecruiterId(recruiterId)
                    : new java.util.ArrayList<>();
            if (jobCountLabel != null) jobCountLabel.setText(jobs.size() + " offre(s)");
            if (jobs.isEmpty()) { jobListContainer.getChildren().add(createEmptyState()); return; }
            boolean first = true;
            for (JobOffer job : jobs) {
                VBox card = createJobCard(job);
                jobListContainer.getChildren().add(card);
                if (first) { selectJob(job, card); first = false; }
            }
        } catch (Exception e) {
            showAlert("Erreur", "Impossible de charger les offres : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // =========================================================================
    // Job card
    // =========================================================================

    private VBox createJobCard(JobOffer job) {
        VBox card = new VBox(8);
        boolean flagged = job.isFlagged() || job.getStatus() == Status.FLAGGED;
        String bgColor     = flagged ? "#fff3cd" : "#f8f9fa";
        String borderColor = flagged ? "#ffc107" : "#dee2e6";
        card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 8; -fx-padding: 14; " +
                "-fx-border-color: " + borderColor + "; -fx-border-radius: 8; -fx-border-width: " +
                (flagged ? "2" : "1") + "; -fx-cursor: hand;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label(job.getTitle());
        titleLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        titleLbl.setWrapText(true);
        HBox.setHgrow(titleLbl, Priority.ALWAYS);

        Label badge = new Label(formatContractType(job.getContractType()));
        badge.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-padding: 3 8; " +
                "-fx-background-radius: 4; -fx-font-size: 11px;");
        header.getChildren().addAll(titleLbl, badge);

        Label location = new Label("📍 " + (job.getLocation() != null ? job.getLocation() : "Non specifie"));
        location.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px;");

        // Status row
        HBox statusRow = new HBox(8);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        String statusColor, statusText;
        if (flagged) {
            statusColor = "#ffc107"; statusText = "⚠ Signale";
        } else if (job.getStatus() == Status.OPEN) {
            statusColor = "#28a745"; statusText = "Ouvert";
        } else {
            statusColor = "#dc3545"; statusText = "Ferme";
        }
        Label statusLbl = new Label(statusText);
        statusLbl.setStyle("-fx-background-color: " + statusColor + "; -fx-text-fill: " +
                (flagged ? "#212529" : "white") + "; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 10px;");
        statusRow.getChildren().add(statusLbl);
        if (flagged) {
            Label actionReq = new Label("Action requise");
            actionReq.setStyle("-fx-text-fill: #856404; -fx-font-size: 10px; -fx-font-weight: bold;");
            statusRow.getChildren().add(actionReq);
        }

        card.getChildren().addAll(header, location, statusRow);
        card.setOnMouseClicked(e -> selectJob(job, card));
        return card;
    }

    private void selectJob(JobOffer job, VBox card) {
        // Reset all cards
        jobListContainer.getChildren().forEach(node -> {
            if (node instanceof VBox) {
                node.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 8; -fx-padding: 14; " +
                        "-fx-border-color: #e9ecef; -fx-border-width: 1; -fx-border-radius: 8; -fx-cursor: hand;");
            }
        });
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 14; " +
                "-fx-border-color: #5BA3F5; -fx-border-width: 2; -fx-border-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(91,163,245,0.25), 10, 0, 0, 2); -fx-cursor: hand;");
        selectedJob = job;
        displayJobDetails(job);
    }

    // =========================================================================
    // Display job details (read-only view)
    // =========================================================================

    private void displayJobDetails(JobOffer job) {
        detailContainer.getChildren().clear();

        // Show warning section first if any warning exists
        displayWarningsForRecruiter(job);

        // ---- Header card ----
        VBox headerCard = new VBox(12);
        headerCard.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 10; -fx-padding: 22;");

        Label title = new Label(job.getTitle());
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
        title.setWrapText(true);

        FlowPane metaFlow = new FlowPane(12, 8);
        metaFlow.setAlignment(Pos.CENTER_LEFT);

        Label contractTypeLbl = new Label("💼 " + formatContractType(job.getContractType()));
        contractTypeLbl.setStyle("-fx-background-color: #e3f2fd; -fx-text-fill: #1976d2; -fx-padding: 5 12; " +
                "-fx-background-radius: 12; -fx-font-size: 12px; -fx-font-weight: 600;");

        Label locationLbl = new Label("📍 " + (job.getLocation() != null ? job.getLocation() : "Non specifie"));
        locationLbl.setStyle("-fx-background-color: #f3e5f5; -fx-text-fill: #7b1fa2; -fx-padding: 5 12; " +
                "-fx-background-radius: 12; -fx-font-size: 12px; -fx-font-weight: 600;");

        boolean flagged = job.isFlagged() || job.getStatus() == Status.FLAGGED;
        String statusColor = flagged ? "#ffc107" : job.getStatus() == Status.OPEN ? "#28a745" : "#dc3545";
        String statusText  = flagged ? "⚠ Signale" : job.getStatus() == Status.OPEN ? "Ouvert" : "Ferme";
        Label statusLbl = new Label("● " + statusText);
        statusLbl.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 13px; -fx-font-weight: 700;");

        metaFlow.getChildren().addAll(contractTypeLbl, locationLbl, statusLbl);

        if (job.getDeadline() != null) {
            Label deadline = new Label("⏰ Date limite: " + job.getDeadline()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            deadline.setStyle("-fx-background-color: #ffebee; -fx-text-fill: #c62828; -fx-padding: 5 12; " +
                    "-fx-background-radius: 12; -fx-font-size: 12px; -fx-font-weight: 600;");
            metaFlow.getChildren().add(deadline);
        }

        headerCard.getChildren().addAll(title, metaFlow);
        detailContainer.getChildren().add(headerCard);

        // ---- Description ----
        if (job.getDescription() != null && !job.getDescription().isBlank()) {
            VBox descSection = new VBox(10);
            descSection.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 10; -fx-padding: 20;");
            Label descTitle = new Label("Description du poste");
            descTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
            Label descText = new Label(job.getDescription());
            descText.setWrapText(true);
            descText.setStyle("-fx-text-fill: #495057; -fx-font-size: 14px; -fx-line-spacing: 3;");
            descSection.getChildren().addAll(descTitle, descText);
            detailContainer.getChildren().add(descSection);
        }

        // ---- Skills ----
        try {
            List<OfferSkill> skills = offerSkillService.getSkillsByOfferId(job.getId());
            if (!skills.isEmpty()) {
                VBox skillsSection = new VBox(10);
                skillsSection.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 10; -fx-padding: 20;");
                Label skillsTitle = new Label("Competences requises");
                skillsTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
                FlowPane skillsFlow = new FlowPane(8, 8);
                for (OfferSkill skill : skills) {
                    VBox skillBox = new VBox(3);
                    skillBox.setStyle("-fx-background-color: white; -fx-padding: 8 14; -fx-background-radius: 8; " +
                            "-fx-border-color: #dee2e6; -fx-border-radius: 8;");
                    Label sName = new Label(skill.getSkillName());
                    sName.setStyle("-fx-font-weight: 600; -fx-text-fill: #2c3e50; -fx-font-size: 13px;");
                    Label sLevel = new Label(formatSkillLevel(skill.getLevelRequired()));
                    sLevel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d;");
                    skillBox.getChildren().addAll(sName, sLevel);
                    skillsFlow.getChildren().add(skillBox);
                }
                skillsSection.getChildren().addAll(skillsTitle, skillsFlow);
                detailContainer.getChildren().add(skillsSection);
            }
        } catch (SQLException e) {
            System.err.println("Erreur chargement competences : " + e.getMessage());
        }

        // ---- Location info (no map) ----
        if (job.getLocation() != null && !job.getLocation().trim().isEmpty()) {
            VBox locationSection = new VBox(8);
            locationSection.setStyle("-fx-background-color: #e8f5e9; -fx-background-radius: 10; -fx-padding: 14;");
            Label locationLabel = new Label("📍 " + job.getLocation());
            locationLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2e7d32;");
            locationSection.getChildren().add(locationLabel);
            detailContainer.getChildren().add(locationSection);
        }

        // ---- Published date ----
        if (job.getCreatedAt() != null) {
            Label posted = new Label("Publie le: " + job.getCreatedAt()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            posted.setStyle("-fx-text-fill: #8e9ba8; -fx-font-size: 12px; -fx-padding: 5 0;");
            detailContainer.getChildren().add(posted);
        }

        // ---- Action buttons ----
        HBox actionButtons = new HBox(12);
        actionButtons.setAlignment(Pos.CENTER);
        actionButtons.setStyle("-fx-padding: 20 0 10 0;");

        Button btnEdit = new Button("✏ Modifier");
        btnEdit.setStyle("-fx-background-color: #ffc107; -fx-text-fill: #212529; -fx-font-weight: 600; " +
                "-fx-font-size: 13px; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        btnEdit.setOnAction(e -> showEditForm(job));

        Button btnDelete = new Button("🗑 Supprimer");
        btnDelete.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: 600; " +
                "-fx-font-size: 13px; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        btnDelete.setOnAction(e -> handleDeleteJobOffer(job));

        String toggleText = (job.getStatus() == Status.OPEN) ? "Fermer l'offre" : "Ouvrir l'offre";
        Button btnToggle = new Button(toggleText);
        btnToggle.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white; -fx-font-weight: 600; " +
                "-fx-font-size: 13px; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        btnToggle.setOnAction(e -> handleToggleStatus(job));

        Button btnAnalyze = new Button("💡 Analyser l'offre");
        btnAnalyze.setStyle("-fx-background-color: #6f42c1; -fx-text-fill: white; -fx-font-weight: 600; " +
                "-fx-font-size: 13px; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        btnAnalyze.setOnAction(e -> showRecommendationsDialog(job));

        actionButtons.getChildren().addAll(btnEdit, btnDelete, btnToggle, btnAnalyze);
        detailContainer.getChildren().add(actionButtons);
    }

    private void showRecommendationsDialog(JobOffer job) {
        Models.joboffers.JobOfferRecommendation recommendation = recommendationService.analyzeOffer(job);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Analyse de l'Offre");
        dialog.setHeaderText("Recommandations Dynamiques pour : " + job.getTitle());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(500);

        // 1. KPIs
        HBox statsBox = new HBox(20);
        statsBox.setAlignment(Pos.CENTER);
        statsBox.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 15;");

        VBox appStat = new VBox(5);
        appStat.setAlignment(Pos.CENTER);
        Label lblAppCount = new Label(String.valueOf(recommendation.getApplicationCount()));
        lblAppCount.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #5BA3F5;");
        Label lblAppText = new Label("Candidatures");
        lblAppText.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d;");
        appStat.getChildren().addAll(lblAppCount, lblAppText);

        VBox scoreStat = new VBox(5);
        scoreStat.setAlignment(Pos.CENTER);
        Label lblScore = new Label(String.format("%.1f%%", recommendation.getAverageScore()));
        lblScore.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + (recommendation.getAverageScore() < 40 ? "#dc3545" : "#28a745") + ";");
        Label lblScoreText = new Label("Score Moyen");
        lblScoreText.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d;");
        scoreStat.getChildren().addAll(lblScore, lblScoreText);

        statsBox.getChildren().addAll(appStat, scoreStat);
        content.getChildren().add(statsBox);

        // 2. Unattractive Alert
        if (recommendation.isUnattractive()) {
            VBox alertBox = new VBox(5);
            alertBox.setStyle("-fx-background-color: #f8d7da; -fx-padding: 10; -fx-background-radius: 5; -fx-border-color: #f5c6cb; -fx-border-radius: 5;");
            Label alertTitle = new Label("⚠️ Offre Peu Attractrice");
            alertTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #721c24;");
            Label alertText = new Label("Cette offre génère peu d'intérêt ou attire des profils inadéquats. Suivez les recommandations ci-dessous.");
            alertText.setStyle("-fx-text-fill: #721c24; -fx-font-size: 12px;");
            alertText.setWrapText(true);
            alertBox.getChildren().addAll(alertTitle, alertText);
            content.getChildren().add(alertBox);
        }

        // 3. Recommendations List
        Label recTitle = new Label("💡 Conseils pour optimiser votre offre :");
        recTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        content.getChildren().add(recTitle);

        VBox recList = new VBox(10);
        for (String rec : recommendation.getRecommendations()) {
            HBox recRow = new HBox(8);
            recRow.setAlignment(Pos.TOP_LEFT);
            Label dot = new Label("•");
            dot.setStyle("-fx-text-fill: #6f42c1; -fx-font-weight: bold;");
            Label text = new Label(rec);
            text.setWrapText(true);
            text.setStyle("-fx-text-fill: #495057; -fx-font-size: 13px;");
            recRow.getChildren().addAll(dot, text);
            recList.getChildren().add(recRow);
        }
        content.getChildren().add(recList);

        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    // =========================================================================
    // Warnings section
    // =========================================================================

    private void displayWarningsForRecruiter(JobOffer job) {
        try {
            List<JobOfferWarning> warnings = warningService.getPendingWarningsByJobOfferId(job.getId());
            if (warnings.isEmpty()) return;

            for (JobOfferWarning w : warnings) {
                if (w.getStatus() == JobOfferWarning.WarningStatus.SENT) warningService.markAsSeen(w.getId());
            }

            VBox section = new VBox(14);
            section.setStyle("-fx-background-color: #f8d7da; -fx-background-radius: 10; -fx-padding: 20; " +
                    "-fx-border-color: #f5c6cb; -fx-border-radius: 10; -fx-border-width: 2;");

            Label alertTitle = new Label("⚠  Action requise - Offre signalee");
            alertTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #721c24;");
            Label alertSub = new Label("Un administrateur a signale un probleme. Corrigez ou supprimez l'offre.");
            alertSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #721c24;");
            alertSub.setWrapText(true);
            section.getChildren().addAll(alertTitle, alertSub);

            for (JobOfferWarning w : warnings) {
                VBox card = new VBox(8);
                card.setStyle("-fx-background-color: white; -fx-padding: 14; -fx-background-radius: 8;");

                Label reason = new Label("Raison : " + w.getReason());
                reason.setStyle("-fx-font-weight: 600; -fx-text-fill: #2c3e50;");
                reason.setWrapText(true);

                Label msgLbl = new Label("Message de l'admin :");
                msgLbl.setStyle("-fx-font-weight: 600; -fx-text-fill: #495057; -fx-font-size: 12px;");

                TextArea msgArea = new TextArea(w.getMessage());
                msgArea.setWrapText(true);
                msgArea.setEditable(false);
                msgArea.setPrefRowCount(3);
                msgArea.setStyle("-fx-control-inner-background: #f8f9fa; -fx-font-size: 13px;");

                Label dateLbl = new Label("Signale le " + w.getCreatedAt()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                dateLbl.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");

                card.getChildren().addAll(reason, msgLbl, msgArea, dateLbl);
                section.getChildren().add(card);
            }

            HBox btns = new HBox(12);
            btns.setAlignment(Pos.CENTER);
            btns.setStyle("-fx-padding: 8 0 0 0;");

            Button bEdit = new Button("✏ Modifier l'offre");
            bEdit.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: 600; " +
                    "-fx-padding: 9 18; -fx-background-radius: 6; -fx-cursor: hand;");
            bEdit.setOnAction(e -> showEditForm(job));

            Button bDel = new Button("🗑 Supprimer l'offre");
            bDel.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: 600; " +
                    "-fx-padding: 9 18; -fx-background-radius: 6; -fx-cursor: hand;");
            bDel.setOnAction(e -> handleDeleteJobOffer(job));

            Button bResolve = new Button("✔ J'ai corrige le probleme");
            bResolve.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: 600; " +
                    "-fx-padding: 9 18; -fx-background-radius: 6; -fx-cursor: hand;");
            bResolve.setOnAction(e -> handleMarkWarningsResolved(job, warnings));

            btns.getChildren().addAll(bEdit, bDel, bResolve);
            section.getChildren().add(btns);
            detailContainer.getChildren().add(section);

        } catch (SQLException e) {
            System.err.println("Erreur chargement avertissements: " + e.getMessage());
        }
    }

    private void handleMarkWarningsResolved(JobOffer job, List<JobOfferWarning> warnings) {
        Dialog<WarningCorrection> dialog = new Dialog<>();
        dialog.setTitle("Soumettre une correction");
        dialog.setHeaderText("Soumettre votre correction pour validation");

        ButtonType submitBT = new ButtonType("Soumettre", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitBT, ButtonType.CANCEL);

        VBox content = new VBox(14);
        content.setPadding(new Insets(20));
        content.setPrefWidth(560);

        Label offerLbl = new Label("Offre : " + job.getTitle());
        offerLbl.setStyle("-fx-font-weight: 600; -fx-font-size: 14px;");

        Label infoLbl = new Label("Votre correction sera envoyee a l'administrateur pour validation.");
        infoLbl.setWrapText(true);
        infoLbl.setStyle("-fx-text-fill: #856404; -fx-background-color: #fff3cd; " +
                "-fx-padding: 10; -fx-background-radius: 5;");

        HBox noteLabelRow = new HBox(10);
        noteLabelRow.setAlignment(Pos.CENTER_LEFT);
        Label noteLbl = new Label("Description des corrections :");
        noteLbl.setStyle("-fx-font-weight: 600;");

        Button btnGen = new Button("Generer automatiquement");
        btnGen.setStyle("-fx-background-color: #6f42c1; -fx-text-fill: white; -fx-font-size: 11px; " +
                "-fx-padding: 5 10; -fx-background-radius: 5; -fx-cursor: hand;");
        noteLabelRow.getChildren().addAll(noteLbl, btnGen);

        TextArea corrNote = new TextArea();
        corrNote.setPromptText("Decrivez les corrections apportees...");
        corrNote.setPrefRowCount(5);
        corrNote.setWrapText(true);

        Label loadingLbl = new Label("");
        loadingLbl.setStyle("-fx-text-fill: #6f42c1; -fx-font-size: 11px;");

        String wReason  = !warnings.isEmpty() ? warnings.get(0).getReason()  : "Non specifie";
        String wMessage = !warnings.isEmpty() ? warnings.get(0).getMessage() : "";

        btnGen.setOnAction(e -> {
            loadingLbl.setText("Generation en cours...");
            btnGen.setDisable(true);
            new Thread(() -> {
                String generated = generateCorrectionNote(wReason, wMessage, job.getTitle());
                javafx.application.Platform.runLater(() -> {
                    corrNote.setText(generated != null ? generated : "");
                    loadingLbl.setText(generated != null && !generated.isEmpty() ? "Description generee." : "Echec - ecrivez manuellement.");
                    btnGen.setDisable(false);
                });
            }).start();
        });

        content.getChildren().addAll(offerLbl, infoLbl, noteLabelRow, corrNote, loadingLbl);
        dialog.getDialogPane().setContent(content);

        Button submitBtn = (Button) dialog.getDialogPane().lookupButton(submitBT);
        submitBtn.setDisable(true);
        corrNote.textProperty().addListener((obs, o, n) -> submitBtn.setDisable(n.trim().length() < 10));

        dialog.setResultConverter(btn -> {
            if (btn != submitBT) return null;
            WarningCorrection c = new WarningCorrection();
            c.setJobOfferId(job.getId());
            c.setRecruiterId(UserContext.getRecruiterId());
            c.setCorrectionNote(corrNote.getText().trim());
            c.setNewTitle(job.getTitle());
            c.setNewDescription(job.getDescription());
            if (!warnings.isEmpty()) c.setWarningId(warnings.get(0).getId());
            return c;
        });

        dialog.showAndWait().ifPresent(correction -> {
            try {
                for (JobOfferWarning w : warnings) {
                    WarningCorrection c = new WarningCorrection();
                    c.setWarningId(w.getId());
                    c.setJobOfferId(job.getId());
                    c.setRecruiterId(UserContext.getRecruiterId());
                    c.setCorrectionNote(correction.getCorrectionNote());
                    c.setNewTitle(job.getTitle());
                    c.setNewDescription(job.getDescription());
                    correctionService.submitCorrection(c);
                }
                showAlert("Succes", "Correction soumise. Vous serez notifie une fois validee.",
                        Alert.AlertType.INFORMATION);
                loadJobOffers();
            } catch (SQLException ex) {
                showAlert("Erreur", "Erreur soumission: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    // =========================================================================
    // Create / Edit form  (NO map, NO autocomplete, plain text location)
    // =========================================================================

    private void showCreateForm() {
        isEditMode = false;
        editingJob = null;
        showJobForm("Creer une offre");
    }

    private void showEditForm(JobOffer job) {
        if (!job.getRecruiterId().equals(UserContext.getRecruiterId())) {
            showAlert("Permission refusee", "Vous ne pouvez modifier que vos propres offres.", Alert.AlertType.WARNING);
            return;
        }
        isEditMode = true;
        editingJob = job;
        showJobForm("Modifier l'offre");
    }

    private void showJobForm(String formTitle) {
        detailContainer.getChildren().clear();
        skillRows = new ArrayList<>();

        Button btnBack = new Button("← Retour");
        btnBack.setStyle("-fx-background-color: transparent; -fx-text-fill: #5BA3F5; " +
                "-fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 0 8 0;");
        btnBack.setOnAction(e -> { if (selectedJob != null) displayJobDetails(selectedJob); });

        Label formTitleLbl = new Label(formTitle);
        formTitleLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 0 0 10 0;");

        VBox form = new VBox(14);

        // --- Title ---
        Label titleLbl = new Label("Titre du poste *");
        titleLbl.setStyle("-fx-font-weight: bold;");
        formTitleField = new TextField();
        formTitleField.setPromptText("Ex: Developpeur Java Senior...");
        formTitleField.setStyle("-fx-padding: 10; -fx-font-size: 14px;");
        titleErrorLabel = new Label();
        titleErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");
        titleErrorLabel.setVisible(false); titleErrorLabel.setManaged(false);

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(formTitleField, Priority.ALWAYS);
        Button btnAI = new Button("AI Suggest");
        btnAI.setStyle("-fx-background-color: #6f42c1; -fx-text-fill: white; -fx-font-weight: 600; " +
                "-fx-padding: 10 14; -fx-background-radius: 6; -fx-cursor: hand;");
        Label aiStatus = new Label("");
        aiStatus.setStyle("-fx-text-fill: #6f42c1; -fx-font-size: 11px;");
        titleRow.getChildren().addAll(formTitleField, btnAI);

        // --- Description ---
        Label descLbl = new Label("Description du poste *");
        descLbl.setStyle("-fx-font-weight: bold;");
        formDescription = new TextArea();
        formDescription.setPromptText("Description detaillee du poste...");
        formDescription.setPrefRowCount(6);
        formDescription.setStyle("-fx-padding: 10; -fx-font-size: 14px;");
        descriptionErrorLabel = new Label();
        descriptionErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");
        descriptionErrorLabel.setVisible(false); descriptionErrorLabel.setManaged(false);

        // --- Location (plain text, no map) ---
        Label locLbl = new Label("Localisation *");
        locLbl.setStyle("-fx-font-weight: bold;");
        formLocation = new TextField();
        formLocation.setPromptText("Ex: Tunis, Paris, Remote...");
        formLocation.setStyle("-fx-padding: 10; -fx-font-size: 14px;");
        locationErrorLabel = new Label();
        locationErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");
        locationErrorLabel.setVisible(false); locationErrorLabel.setManaged(false);

        // --- Contract Type ---
        Label contractLbl = new Label("Type de contrat *");
        contractLbl.setStyle("-fx-font-weight: bold;");
        formContractType = new ComboBox<>();
        formContractType.getItems().addAll(ContractType.values());
        formContractType.setPromptText("Selectionner...");
        formContractType.setMaxWidth(Double.MAX_VALUE);
        formContractType.setStyle("-fx-font-size: 14px;");

        // --- Status ---
        Label statusLbl = new Label("Statut *");
        statusLbl.setStyle("-fx-font-weight: bold;");
        formStatus = new ComboBox<>();
        formStatus.getItems().addAll(Status.values());
        formStatus.setValue(Status.OPEN);
        formStatus.setMaxWidth(Double.MAX_VALUE);
        formStatus.setStyle("-fx-font-size: 14px;");

        // --- Deadline ---
        Label deadlineLbl = new Label("Date limite (Optionnel)");
        deadlineLbl.setStyle("-fx-font-weight: bold;");
        formDeadline = new DatePicker();
        formDeadline.setPromptText("Selectionner une date...");
        formDeadline.setMaxWidth(Double.MAX_VALUE);
        formDeadline.setStyle("-fx-font-size: 14px;");
        deadlineErrorLabel = new Label();
        deadlineErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");
        deadlineErrorLabel.setVisible(false); deadlineErrorLabel.setManaged(false);

        // --- Skills ---
        VBox skillsSection = new VBox(10);
        skillsSection.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 15; -fx-background-radius: 8;");
        Label skillsLbl = new Label("Competences requises *");
        skillsLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #2c3e50;");
        skillsErrorLabel = new Label();
        skillsErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");
        skillsErrorLabel.setVisible(false); skillsErrorLabel.setManaged(false);
        skillsContainer = new VBox(8);
        Button btnAddSkill = new Button("+ Ajouter une competence");
        btnAddSkill.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; " +
                "-fx-padding: 8 15; -fx-background-radius: 6; -fx-cursor: hand;");
        btnAddSkill.setOnAction(e -> addSkillRow(null));
        skillsSection.getChildren().addAll(skillsLbl, skillsErrorLabel, skillsContainer, btnAddSkill);

        // Populate if editing
        if (isEditMode && editingJob != null) {
            formTitleField.setText(editingJob.getTitle() != null ? editingJob.getTitle() : "");
            formDescription.setText(editingJob.getDescription() != null ? editingJob.getDescription() : "");
            formLocation.setText(editingJob.getLocation() != null ? editingJob.getLocation() : "");
            formContractType.setValue(editingJob.getContractType());
            formStatus.setValue(editingJob.getStatus() != null ? editingJob.getStatus() : Status.OPEN);
            if (editingJob.getDeadline() != null) formDeadline.setValue(editingJob.getDeadline().toLocalDate());
            try {
                for (OfferSkill sk : offerSkillService.getSkillsByOfferId(editingJob.getId())) addSkillRow(sk);
            } catch (SQLException ex) {
                showAlert("Erreur", "Impossible de charger les competences : " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        } else {
            addSkillRow(null);
        }

        // AI Suggest handler
        btnAI.setOnAction(e -> {
            String t = formTitleField.getText().trim();
            if (t.length() < 3) { showAlert("Attention", "Entrez un titre valide (min 3 car.)", Alert.AlertType.WARNING); return; }
            aiStatus.setText("Generation en cours...");
            btnAI.setDisable(true);
            new Thread(() -> {
                String sugg = generateJobSuggestions(t);
                javafx.application.Platform.runLater(() -> {
                    if (sugg != null && !sugg.isBlank()) { parseAndFillForm(sugg); aiStatus.setText("Formulaire rempli!"); }
                    else aiStatus.setText("Echec de la generation.");
                    btnAI.setDisable(false);
                });
            }).start();
        });

        addValidationListeners();

        Button btnSubmit = new Button(isEditMode ? "Mettre a jour l'offre" : "Creer l'offre");
        btnSubmit.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-font-weight: 700; " +
                "-fx-font-size: 15px; -fx-padding: 12 30; -fx-background-radius: 8; -fx-cursor: hand;");
        btnSubmit.setOnAction(e -> { if (isEditMode) handleUpdateJobOffer(); else handleCreateJobOffer(); });

        form.getChildren().addAll(
                titleLbl, titleRow, aiStatus, titleErrorLabel,
                descLbl, formDescription, descriptionErrorLabel,
                locLbl, formLocation, locationErrorLabel,
                contractLbl, formContractType,
                statusLbl, formStatus,
                deadlineLbl, formDeadline, deadlineErrorLabel,
                skillsSection, btnSubmit
        );

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        detailContainer.getChildren().addAll(btnBack, formTitleLbl, scroll);
    }

    // =========================================================================
    // Skill rows
    // =========================================================================

    private void addSkillRow(OfferSkill existing) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        TextField nameField = new TextField();
        nameField.setPromptText("Nom de la competence (ex: Java, Python)");
        nameField.setStyle("-fx-padding: 8; -fx-font-size: 13px;");
        HBox.setHgrow(nameField, Priority.ALWAYS);

        ComboBox<SkillLevel> levelCombo = new ComboBox<>();
        levelCombo.getItems().addAll(SkillLevel.values());
        levelCombo.setValue(SkillLevel.INTERMEDIATE);
        levelCombo.setStyle("-fx-font-size: 13px;");
        levelCombo.setPrefWidth(150);

        if (existing != null) {
            nameField.setText(existing.getSkillName());
            levelCombo.setValue(existing.getLevelRequired());
        }

        Button btnRemove = new Button("✕");
        btnRemove.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; " +
                "-fx-padding: 6 10; -fx-background-radius: 6; -fx-cursor: hand;");
        btnRemove.setOnAction(e -> {
            skillsContainer.getChildren().remove(row);
            skillRows.removeIf(r -> r.nameField == nameField);
        });

        row.getChildren().addAll(nameField, levelCombo, btnRemove);
        skillsContainer.getChildren().add(row);
        skillRows.add(new SkillRow(nameField, levelCombo));
    }

    // =========================================================================
    // CRUD handlers
    // =========================================================================

    private void handleCreateJobOffer() {
        if (!validateForm()) return;
        try {
            JobOffer newJob = new JobOffer();
            newJob.setRecruiterId(UserContext.getRecruiterId());
            newJob.setTitle(formTitleField.getText().trim());
            newJob.setDescription(formDescription.getText().trim());
            newJob.setLocation(formLocation.getText().trim());
            newJob.setContractType(formContractType.getValue());
            newJob.setStatus(formStatus.getValue());
            newJob.setCreatedAt(LocalDateTime.now());
            if (formDeadline.getValue() != null) newJob.setDeadline(formDeadline.getValue().atTime(23, 59));

            JobOffer saved = jobOfferService.createJobOffer(newJob);
            List<OfferSkill> skills = getSkillsFromForm(saved.getId());
            if (!skills.isEmpty()) offerSkillService.createOfferSkills(skills);

            showAlert("Succes", "Offre creee avec succes!", Alert.AlertType.INFORMATION);
            loadJobOffers();
            selectedJob = saved;
            displayJobDetails(saved);
        } catch (SQLException e) {
            showAlert("Erreur", "Echec creation: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void handleUpdateJobOffer() {
        if (!validateForm() || editingJob == null) return;
        try {
            editingJob.setTitle(formTitleField.getText().trim());
            editingJob.setDescription(formDescription.getText().trim());
            editingJob.setLocation(formLocation.getText().trim());
            editingJob.setContractType(formContractType.getValue());
            editingJob.setStatus(formStatus.getValue());
            editingJob.setDeadline(formDeadline.getValue() != null
                    ? formDeadline.getValue().atTime(23, 59) : null);

            if (jobOfferService.updateJobOffer(editingJob)) {
                offerSkillService.replaceOfferSkills(editingJob.getId(), getSkillsFromForm(editingJob.getId()));
                showAlert("Succes", "Offre mise a jour!", Alert.AlertType.INFORMATION);
                loadJobOffers();
                selectedJob = editingJob;
                displayJobDetails(editingJob);
            } else {
                showAlert("Erreur", "Echec de la mise a jour.", Alert.AlertType.ERROR);
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Echec: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void handleDeleteJobOffer(JobOffer job) {
        if (!job.getRecruiterId().equals(UserContext.getRecruiterId())) {
            showAlert("Permission refusee", "Vous ne pouvez supprimer que vos propres offres.", Alert.AlertType.WARNING);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText(null);
        confirm.setContentText("Etes-vous sur de vouloir supprimer \"" + job.getTitle() + "\" ? Cette action est irreversible.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    if (jobOfferService.deleteJobOffer(job.getId())) {
                        showAlert("Succes", "Offre supprimee.", Alert.AlertType.INFORMATION);
                        selectedJob = null;
                        detailContainer.getChildren().clear();
                        Label ph = new Label("Selectionnez une offre pour voir les details");
                        ph.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14px; -fx-padding: 40;");
                        detailContainer.getChildren().add(ph);
                        loadJobOffers();
                    } else {
                        showAlert("Erreur", "Impossible de supprimer.", Alert.AlertType.ERROR);
                    }
                } catch (SQLException e) {
                    showAlert("Erreur", "Erreur: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void handleToggleStatus(JobOffer job) {
        if (!job.getRecruiterId().equals(UserContext.getRecruiterId())) {
            showAlert("Permission refusee", "Vous ne pouvez changer le statut que de vos propres offres.", Alert.AlertType.WARNING);
            return;
        }
        try {
            Status newStatus = (job.getStatus() == Status.OPEN) ? Status.CLOSED : Status.OPEN;
            if (jobOfferService.updateJobOfferStatus(job.getId(), newStatus)) {
                job.setStatus(newStatus);
                showAlert("Succes", "Statut mis a jour : " + (newStatus == Status.OPEN ? "Ouvert" : "Ferme"),
                        Alert.AlertType.INFORMATION);
                loadJobOffers();
                displayJobDetails(job);
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de changer le statut: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // =========================================================================
    // Validation
    // =========================================================================

    private boolean validateForm() {
        String title = formTitleField.getText().trim();
        String desc  = formDescription.getText().trim();
        String loc   = formLocation.getText().trim();

        if (title.length() < 3) {
            showAlert("Validation", "Le titre doit contenir au moins 3 caracteres.", Alert.AlertType.WARNING);
            formTitleField.requestFocus(); return false;
        }
        if (title.length() > 100) {
            showAlert("Validation", "Le titre ne doit pas depasser 100 caracteres.", Alert.AlertType.WARNING);
            formTitleField.requestFocus(); return false;
        }
        if (desc.length() < 20) {
            showAlert("Validation", "La description doit contenir au moins 20 caracteres.", Alert.AlertType.WARNING);
            formDescription.requestFocus(); return false;
        }
        if (desc.length() > 2000) {
            showAlert("Validation", "La description ne doit pas depasser 2000 caracteres.", Alert.AlertType.WARNING);
            formDescription.requestFocus(); return false;
        }
        if (loc.length() < 2) {
            showAlert("Validation", "La localisation doit contenir au moins 2 caracteres.", Alert.AlertType.WARNING);
            formLocation.requestFocus(); return false;
        }
        if (formContractType.getValue() == null) {
            showAlert("Validation", "Veuillez selectionner un type de contrat.", Alert.AlertType.WARNING); return false;
        }
        if (formStatus.getValue() == null) {
            showAlert("Validation", "Veuillez selectionner un statut.", Alert.AlertType.WARNING); return false;
        }
        if (formDeadline.getValue() != null && formDeadline.getValue().isBefore(java.time.LocalDate.now())) {
            showAlert("Validation", "La date limite ne peut pas etre dans le passe.", Alert.AlertType.WARNING); return false;
        }
        boolean hasSkill = false;
        for (SkillRow r : skillRows) {
            String sn = r.nameField.getText().trim();
            if (!sn.isEmpty()) {
                if (sn.length() < 2) {
                    showAlert("Validation", "Chaque competence doit avoir au moins 2 caracteres.", Alert.AlertType.WARNING); return false;
                }
                if (r.levelCombo.getValue() == null) {
                    showAlert("Validation", "Selectionnez un niveau pour : " + sn, Alert.AlertType.WARNING); return false;
                }
                hasSkill = true;
            }
        }
        if (!hasSkill) {
            showAlert("Validation", "Ajoutez au moins une competence.", Alert.AlertType.WARNING); return false;
        }
        return true;
    }

    private void addValidationListeners() {
        formTitleField.textProperty().addListener((obs, o, n) ->
                formTitleField.setStyle("-fx-padding: 10; -fx-font-size: 14px; -fx-border-width: 2; -fx-border-color: " +
                        (n.length() >= 3 ? "#28a745" : n.isEmpty() ? "transparent" : "#ffc107") + ";"));
        formDescription.textProperty().addListener((obs, o, n) ->
                formDescription.setStyle("-fx-padding: 10; -fx-font-size: 14px; -fx-border-width: 2; -fx-border-color: " +
                        (n.length() >= 20 ? "#28a745" : n.isEmpty() ? "transparent" : "#ffc107") + ";"));
        formLocation.textProperty().addListener((obs, o, n) ->
                formLocation.setStyle("-fx-padding: 10; -fx-font-size: 14px; -fx-border-width: 2; -fx-border-color: " +
                        (n.length() >= 2 ? "#28a745" : n.isEmpty() ? "transparent" : "#ffc107") + ";"));
        formDeadline.valueProperty().addListener((obs, o, n) -> {
            boolean past = n != null && n.isBefore(java.time.LocalDate.now());
            formDeadline.setStyle("-fx-font-size: 14px;" + (past ? "-fx-border-color: #dc3545; -fx-border-width: 2;" : ""));
        });
    }

    private List<OfferSkill> getSkillsFromForm(Long offerId) {
        List<OfferSkill> list = new ArrayList<>();
        for (SkillRow r : skillRows) {
            String sn = r.nameField.getText().trim();
            if (!sn.isEmpty() && r.levelCombo.getValue() != null)
                list.add(new OfferSkill(offerId, sn, r.levelCombo.getValue()));
        }
        return list;
    }

    // =========================================================================
    // Search handler
    // =========================================================================

    @FXML
    private void handleSearch() {
        if (txtSearch == null || txtSearch.getText().trim().isEmpty()) { loadJobOffers(); return; }
        String keyword = txtSearch.getText().trim();
        NotificationService.showInfo("Recherche", "Recherche pour : \"" + keyword + "\"");
        try {
            FuzzySearchService fuzzy = FuzzySearchService.getInstance();
            final double THRESHOLD = 0.6;
            List<JobOffer> all = jobOfferService.getJobOffersByRecruiterId(UserContext.getRecruiterId());
            List<JobOffer> results = all.stream()
                    .filter(j -> {
                        String t = j.getTitle() != null ? j.getTitle() : "";
                        return t.toLowerCase().contains(keyword.toLowerCase()) ||
                               fuzzy.calculateBestScore(t, keyword) >= THRESHOLD;
                    })
                    .sorted((a, b) -> Double.compare(
                            fuzzy.calculateBestScore(b.getTitle() != null ? b.getTitle() : "", keyword),
                            fuzzy.calculateBestScore(a.getTitle() != null ? a.getTitle() : "", keyword)))
                    .toList();

            jobListContainer.getChildren().clear();
            if (results.isEmpty()) {
                Label empty = new Label("Aucun resultat pour \"" + keyword + "\"");
                empty.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px; -fx-padding: 20;");
                jobListContainer.getChildren().add(empty);
            } else {
                for (JobOffer j : results) jobListContainer.getChildren().add(createJobCard(j));
                NotificationService.showSuccess("Recherche", results.size() + " resultat(s)");
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Echec recherche: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleClearSearch() {
        if (txtSearch != null) txtSearch.clear();
        loadJobOffers();
    }

    // =========================================================================
    // AI helpers
    // =========================================================================

    private String generateJobSuggestions(String jobTitle) {
        String prompt = buildJobSuggestionsPrompt(jobTitle);
        // 1st try: Gemini
        try {
            String r = callGeminiAPI(prompt);
            if (r != null && r.contains("DESCRIPTION:") && r.contains("SKILLS:")) return r;
        } catch (Exception e) {
            System.err.println("Gemini failed: " + e.getMessage());
        }
        // 2nd try: Groq (same API used by GrokAIService)
        try {
            String r = callGroqAPI(prompt);
            if (r != null && r.contains("DESCRIPTION:") && r.contains("SKILLS:")) return r;
        } catch (Exception e) {
            System.err.println("Groq failed: " + e.getMessage());
        }
        // Fallback: smart domain-based defaults
        return getDefaultJobSuggestions(jobTitle);
    }

    private String buildJobSuggestionsPrompt(String jobTitle) {
        return "You are an expert HR recruiter with deep knowledge of all industries and technical domains.\n" +
               "Generate a realistic job offer for the position: \"" + jobTitle + "\"\n\n" +
               "IMPORTANT RULES:\n" +
               "- The SKILLS must be SPECIFIC and TECHNICAL to this exact role (e.g. for 'Cloud Security Engineer': AWS Security, IAM, Zero Trust, SIEM, Terraform)\n" +
               "- Do NOT use generic skills like 'Communication', 'Organisation', 'Travail en equipe' unless the role is purely managerial\n" +
               "- List 6 to 8 skills that a recruiter would actually search for in this domain\n" +
               "- The DESCRIPTION must be 4-5 sentences describing real responsibilities for this role in French\n" +
               "- Use professional French\n\n" +
               "Respond ONLY in this exact format (no extra text, no markdown, no asterisks):\n" +
               "DESCRIPTION: [4-5 sentences in French describing the role and responsibilities]\n" +
               "SKILLS: [skill1, skill2, skill3, skill4, skill5, skill6, skill7]\n\n" +
               "Example for 'Developpeur Web Full Stack':\n" +
               "DESCRIPTION: Nous recherchons un Developpeur Web Full Stack pour concevoir et developper des applications web modernes. Vous serez responsable du developpement front-end et back-end de nos plateformes. Vous travaillerez en collaboration etroite avec les equipes produit et design. Vous participerez aux revues de code et aux choix d'architecture technique.\n" +
               "SKILLS: React, Node.js, TypeScript, PostgreSQL, Docker, REST API, Git\n\n" +
               "Now generate for: \"" + jobTitle + "\"";
    }

    private String generateCorrectionNote(String reason, String message, String jobTitle) {
        try {
            String prompt = "Recruteur: correction (3-4 phrases FR) pour:\nRaison: " + reason +
                    "\nMessage: " + message + "\nOffre: " + jobTitle;
            String r = callGrokAPI(prompt);
            return r != null ? r : getDefaultCorrectionNote(reason);
        } catch (Exception e) { return getDefaultCorrectionNote(reason); }
    }

    private String callGeminiAPI(String prompt) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=AIzaSyA40pYJkW9p7QYQerVUv_rmS4pNFo1T46o";
        java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        c.setRequestMethod("POST"); c.setRequestProperty("Content-Type", "application/json");
        c.setDoOutput(true); c.setConnectTimeout(15000); c.setReadTimeout(60000);
        String body = "{\"contents\":[{\"parts\":[{\"text\":\"" + escapeJson(prompt) + "\"}]}]," +
                "\"generationConfig\":{\"maxOutputTokens\":800,\"temperature\":0.85}}";
        c.getOutputStream().write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (c.getResponseCode() != 200) throw new Exception("Gemini HTTP " + c.getResponseCode());
        StringBuilder sb = new StringBuilder();
        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(c.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            String line; while ((line = br.readLine()) != null) sb.append(line);
        }
        return extractGeminiContent(sb.toString());
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
                        "{\"role\":\"system\",\"content\":\"You are an expert HR recruiter. Follow the user format exactly.\"}," +
                        "{\"role\":\"user\",\"content\":\"" + escapeJson(prompt) + "\"}]," +
                        "\"max_tokens\":600,\"temperature\":0.85}";
                c.getOutputStream().write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                if (c.getResponseCode() != 200) continue;
                StringBuilder sb = new StringBuilder();
                try (java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(c.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                    String line; while ((line = br.readLine()) != null) sb.append(line);
                }
                // Extract content from Groq's OpenAI-compatible response
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

    private String callGrokAPI(String prompt) throws Exception {
        String url = "https://api.x.ai/v1/chat/completions";
        java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        c.setRequestMethod("POST"); c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("Authorization", "Bearer xai-BvO5mSs05cHXwQRM1qa8Z7lojgfAMS0I6Kc9Y1R5lQYSyHWO6eDq62ZZ0QsajWkyyyB6f41ZD4HmWOCU");
        c.setDoOutput(true); c.setConnectTimeout(15000); c.setReadTimeout(60000);
        String body = "{\"model\":\"grok-beta\",\"messages\":[{\"role\":\"user\",\"content\":\"" +
                escapeJson(prompt) + "\"}],\"max_tokens\":500,\"temperature\":0.7}";
        c.getOutputStream().write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (c.getResponseCode() != 200) throw new Exception("Grok HTTP " + c.getResponseCode());
        StringBuilder sb = new StringBuilder();
        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(c.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            String line; while ((line = br.readLine()) != null) sb.append(line);
        }
        return extractGrokContent(sb.toString());
    }

    private String extractGeminiContent(String json) {
        try {
            int s = json.indexOf("\"text\":"); if (s < 0) return null;
            s = json.indexOf("\"", s + 7) + 1; int e = s;
            for (int i = s; i < json.length(); i++) {
                char ch = json.charAt(i);
                if (ch == '\\' && i + 1 < json.length()) { i++; continue; }
                if (ch == '"') { e = i; break; }
            }
            return e > s ? json.substring(s, e).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\").trim() : null;
        } catch (Exception ex) { return null; }
    }

    private String extractGrokContent(String json) {
        try {
            int s = json.indexOf("\"content\":"); if (s < 0) return null;
            s = json.indexOf("\"", s + 10) + 1;
            int e = json.indexOf("\"", s);
            while (e > 0 && json.charAt(e - 1) == '\\') e = json.indexOf("\"", e + 1);
            return (s > 0 && e > s) ? json.substring(s, e).replace("\\n", "\n").replace("\\\"", "\"").trim() : null;
        } catch (Exception ex) { return null; }
    }

    private String escapeJson(String t) {
        if (t == null) return "";
        return t.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private void parseAndFillForm(String suggestions) {
        try {
            String d = extractField(suggestions, "DESCRIPTION:");
            if (d != null && !d.isBlank()) {
                // Strip any markdown artifacts
                d = d.replaceAll("\\*+", "").replaceAll("#+ ?", "").trim();
                formDescription.setText(d);
            }
            String sk = extractField(suggestions, "SKILLS:");
            if (sk != null && !sk.isBlank()) {
                // Strip brackets/markdown artifacts
                sk = sk.replaceAll("[\\[\\]\\*`]", "").trim();
                skillsContainer.getChildren().clear();
                skillRows.clear();
                for (String s : sk.split(",")) {
                    String raw = s.trim();
                    if (raw.isBlank()) continue;
                    // Try to extract a skill level hint from the name
                    SkillLevel level = SkillLevel.INTERMEDIATE;
                    String name = raw;
                    // e.g. "Python (Avance)" or "Python - Expert" or "Python (Beginner)"
                    String lower = raw.toLowerCase();
                    if (lower.contains("avance") || lower.contains("advanced") || lower.contains("expert") || lower.contains("senior")) {
                        level = SkillLevel.ADVANCED;
                        name = raw.replaceAll("(?i)\\s*[-–(]\\s*(avance|advanced|expert|senior)[)\\s]*", "").trim();
                    } else if (lower.contains("debut") || lower.contains("beginner") || lower.contains("junior") || lower.contains("notions")) {
                        level = SkillLevel.BEGINNER;
                        name = raw.replaceAll("(?i)\\s*[-–(]\\s*(debut\\w*|beginner|junior|notions)[)\\s]*", "").trim();
                    } else if (lower.contains("intermediaire") || lower.contains("intermediate")) {
                        level = SkillLevel.INTERMEDIATE;
                        name = raw.replaceAll("(?i)\\s*[-–(]\\s*(intermediaire|intermediate)[)\\s]*", "").trim();
                    }
                    if (name.length() >= 2) addSkillRow(new OfferSkill(null, name, level));
                }
                if (skillRows.isEmpty()) addSkillRow(null);
            }
        } catch (Exception e) {
            System.err.println("parseAndFillForm error: " + e.getMessage());
        }
    }

    private String extractField(String text, String fieldName) {
        if (text == null) return null;
        int s = text.indexOf(fieldName);
        if (s < 0) {
            // Try case-insensitive
            String lower = text.toLowerCase();
            s = lower.indexOf(fieldName.toLowerCase());
            if (s < 0) return null;
        }
        s += fieldName.length();
        // Skip any leading whitespace/colon
        while (s < text.length() && (text.charAt(s) == ' ' || text.charAt(s) == ':')) s++;
        // For SKILLS, stop at newline; for DESCRIPTION, grab until next labelled field
        int e;
        if (fieldName.equals("SKILLS:")) {
            e = text.indexOf("\n", s);
            if (e < 0) e = text.length();
        } else {
            // Stop at the next all-caps FIELD: pattern
            int nextField = text.indexOf("\nSKILLS:", s);
            if (nextField < 0) nextField = text.indexOf("\nSKILL:", s);
            e = nextField < 0 ? text.length() : nextField;
        }
        String result = text.substring(s, e).trim();
        // Remove surrounding quotes if any
        if (result.startsWith("\"") && result.endsWith("\"")) result = result.substring(1, result.length() - 1);
        return result.isBlank() ? null : result;
    }

    private String getDefaultJobSuggestions(String jobTitle) {
        String t = jobTitle.toLowerCase();

        // ── Web / Frontend ──────────────────────────────────────────────────
        if (contains(t, "frontend", "front-end", "react", "angular", "vue", "ui developer")) {
            return "DESCRIPTION: Nous recherchons un(e) Developpeur(se) Frontend passionné(e) pour creer des interfaces utilisateur modernes et performantes. " +
                   "Vous traduirez les maquettes UI/UX en composants riches et accessibles. " +
                   "Vous collaborerez avec les equipes backend pour integrer les APIs REST et GraphQL. " +
                   "Vous veillerez aux performances, a la compatibilite cross-browser et a l'accessibilite.\n" +
                   "SKILLS: React, TypeScript, HTML5, CSS3, Redux, REST API, Git";
        }
        // ── Backend ─────────────────────────────────────────────────────────
        if (contains(t, "backend", "back-end", "java developer", "spring", "node.js developer", "python developer", "django", "fastapi")) {
            return "DESCRIPTION: Nous recherchons un(e) Developpeur(se) Backend pour concevoir et maintenir des services robustes et scalables. " +
                   "Vous serez charge(e) de la conception des APIs, de l'architecture des bases de donnees et de l'optimisation des performances. " +
                   "Vous participerez aux revues de code et a la mise en place de bonnes pratiques DevOps. " +
                   "Une experience en environnements microservices est fortement appreciee.\n" +
                   "SKILLS: Java, Spring Boot, PostgreSQL, Docker, REST API, JUnit, Maven";
        }
        // ── Full Stack ───────────────────────────────────────────────────────
        if (contains(t, "full stack", "fullstack", "full-stack")) {
            return "DESCRIPTION: Nous recherchons un(e) Developpeur(se) Full Stack pour prendre en charge le cycle complet de developpement de nos applications. " +
                   "Vous interviendrez aussi bien sur la couche frontend que backend et contribuerez aux decisions d'architecture. " +
                   "Vous travaillerez en methode Agile au sein d'une equipe pluridisciplinaire. " +
                   "Une bonne maitrise des outils de CI/CD et du cloud est un atout majeur.\n" +
                   "SKILLS: React, Node.js, TypeScript, PostgreSQL, Docker, REST API, Git";
        }
        // ── Mobile ──────────────────────────────────────────────────────────
        if (contains(t, "mobile", "android", "ios", "flutter", "react native", "kotlin", "swift")) {
            return "DESCRIPTION: Nous recherchons un(e) Developpeur(se) Mobile pour concevoir des applications performantes sur iOS et/ou Android. " +
                   "Vous participerez a la conception, au developpement et au deploiement des features mobiles. " +
                   "Vous integrerez des APIs REST et veillerez a l'optimisation des performances et de l'UX. " +
                   "Vous contribuerez aux tests unitaires et aux revues de code.\n" +
                   "SKILLS: Flutter, Dart, Firebase, REST API, Android SDK, iOS Swift, Git";
        }
        // ── Cloud Security ───────────────────────────────────────────────────
        if (contains(t, "cloud security", "securite cloud", "security engineer", "ingenieur securite")) {
            return "DESCRIPTION: Nous recherchons un(e) Ingenieur(e) en Securite Cloud pour proteger nos infrastructures et donnees dans le cloud. " +
                   "Vous concevrez et implémenterez des architectures Zero Trust et des politiques IAM rigoureuses. " +
                   "Vous superviserez les incidents de securite via SIEM et coordonnerez la reponse aux menaces. " +
                   "Vous realiserez des audits de conformite (ISO 27001, SOC2) et des tests de penetration reguliers.\n" +
                   "SKILLS: AWS Security, IAM, Zero Trust, SIEM, Terraform, Kubernetes Security, Penetration Testing";
        }
        // ── Cloud / DevOps / Infrastructure ─────────────────────────────────
        if (contains(t, "cloud", "devops", "infrastructure", "sre", "platform engineer", "aws", "azure", "gcp", "kubernetes", "terraform")) {
            return "DESCRIPTION: Nous recherchons un(e) Ingenieur(e) Cloud/DevOps pour automatiser et fiabiliser nos infrastructures. " +
                   "Vous gererez le provisionning des ressources cloud et optimiserez les pipelines CI/CD. " +
                   "Vous mettrez en place la supervision, les alertes et les pratiques de reliability engineering. " +
                   "Vous travaillerez en etroite collaboration avec les equipes de developpement pour accelerer les livraisons.\n" +
                   "SKILLS: Kubernetes, Terraform, AWS, Docker, CI/CD, Prometheus, Ansible";
        }
        // ── Cybersecurity / Security Analyst ────────────────────────────────
        if (contains(t, "cybersecurite", "cybersecurity", "soc analyst", "analyste securite", "penetration", "pentest", "ethical hacker")) {
            return "DESCRIPTION: Nous recherchons un(e) Analyste en Cybersecurite pour surveiller, detecter et repondre aux menaces informatiques. " +
                   "Vous analyserez les vulnerabilites de nos systemes et proposerez des plans de remediation. " +
                   "Vous conduirez des tests d'intrusion et des audits de securite reguliers. " +
                   "Vous formerez les equipes internes aux bonnes pratiques de securite.\n" +
                   "SKILLS: SIEM, Penetration Testing, Wireshark, Metasploit, OWASP, Nmap, Python Scripting";
        }
        // ── Data Science / ML / AI ───────────────────────────────────────────
        if (contains(t, "data scientist", "machine learning", "deep learning", "ia ", "intelligence artificielle", "nlp", "computer vision", "ml engineer")) {
            return "DESCRIPTION: Nous recherchons un(e) Data Scientist / Ingenieur(e) ML pour concevoir des modeles predictifs a fort impact metier. " +
                   "Vous collecterez, nettoierez et analyserez de larges datasets pour en extraire des insights actionnables. " +
                   "Vous developperez et deploierez des modeles de machine learning en production. " +
                   "Vous collaborerez avec les equipes produit pour integrer l'IA dans nos solutions.\n" +
                   "SKILLS: Python, TensorFlow, PyTorch, Scikit-learn, SQL, MLflow, Pandas";
        }
        // ── Data Engineer ────────────────────────────────────────────────────
        if (contains(t, "data engineer", "ingenieur donnees", "big data", "spark", "kafka", "etl", "pipeline")) {
            return "DESCRIPTION: Nous recherchons un(e) Data Engineer pour construire et maintenir nos pipelines de donnees a grande echelle. " +
                   "Vous concevrez des architectures data robustes (data lake, data warehouse) et optimiserez les flux de donnees. " +
                   "Vous garantirez la qualite, la disponibilite et la securite des donnees. " +
                   "Vous travaillerez en collaboration avec les data scientists pour mettre en production leurs modeles.\n" +
                   "SKILLS: Apache Spark, Kafka, Airflow, Python, SQL, AWS S3, dbt";
        }
        // ── Data Analyst / BI ────────────────────────────────────────────────
        if (contains(t, "data analyst", "business intelligence", "bi developer", "analyste", "power bi", "tableau", "reporting")) {
            return "DESCRIPTION: Nous recherchons un(e) Data Analyst pour transformer les donnees en insights strategiques. " +
                   "Vous concevrez des tableaux de bord interactifs et produirez des rapports reguliers pour les parties prenantes. " +
                   "Vous collaborerez avec les metiers pour comprendre leurs besoins et traduire les KPIs en analyses. " +
                   "Vous assurerez la qualite et la fiabilite des donnees tout au long du cycle analytique.\n" +
                   "SKILLS: Power BI, SQL, Python, Excel, Tableau, DAX, Data Modeling";
        }
        // ── Network / Systems ────────────────────────────────────────────────
        if (contains(t, "network", "reseau", "cisco", "systeme", "sysadmin", "system admin", "linux admin", "infrastructure")) {
            return "DESCRIPTION: Nous recherchons un(e) Administrateur(trice) Systemes & Reseaux pour gerer et securiser notre infrastructure IT. " +
                   "Vous administrerez les serveurs, routeurs, switches et firewall de l'entreprise. " +
                   "Vous veillerez a la disponibilite des systemes et interviendrez en cas d'incidents. " +
                   "Vous documenterez les procedures et formerez les utilisateurs.\n" +
                   "SKILLS: Cisco, Linux, Active Directory, VMware, VPN, Firewalls, Bash Scripting";
        }
        // ── QA / Test ────────────────────────────────────────────────────────
        if (contains(t, "qa", "quality assurance", "test", "testeur", "automation test", "selenium")) {
            return "DESCRIPTION: Nous recherchons un(e) Ingenieur(e) QA pour garantir la qualite de nos applications logicielles. " +
                   "Vous concevrez et executerez des plans de tests fonctionnels, de regression et de performance. " +
                   "Vous developperez des tests automatises et integrerez les tests dans les pipelines CI/CD. " +
                   "Vous collaborerez etroitement avec les developpeurs pour identifier et corriger les defauts au plus tot.\n" +
                   "SKILLS: Selenium, Cypress, JUnit, Postman, JIRA, CI/CD, Python";
        }
        // ── Product / Project ────────────────────────────────────────────────
        if (contains(t, "product manager", "product owner", "chef de produit", "po ", "pm ")) {
            return "DESCRIPTION: Nous recherchons un(e) Product Manager pour piloter la vision et la roadmap de notre produit. " +
                   "Vous definirez les priorites du backlog en collaboration avec les parties prenantes. " +
                   "Vous animerez les rituels Agile et veillerez a la bonne execution des sprints. " +
                   "Vous analyserez les retours utilisateurs et les metriques pour orienter les decisions produit.\n" +
                   "SKILLS: Agile/Scrum, JIRA, User Story Mapping, A/B Testing, Analytics, Figma, OKRs";
        }
        // ── Project Manager ──────────────────────────────────────────────────
        if (contains(t, "project manager", "chef de projet", "gestionnaire de projet", "scrum master")) {
            return "DESCRIPTION: Nous recherchons un(e) Chef(fe) de Projet pour coordonner et piloter nos projets IT de bout en bout. " +
                   "Vous gererez les plannings, budgets et ressources tout en assurant la communication avec les parties prenantes. " +
                   "Vous identifierez et mitegerez les risques projet et veillerez au respect des delais. " +
                   "Vous animerez les equipes pluridisciplinaires en methode Agile ou Waterfall.\n" +
                   "SKILLS: Gestion de projet, Agile/Scrum, MS Project, JIRA, Risk Management, Leadership, PMP";
        }
        // ── UX/UI Designer ───────────────────────────────────────────────────
        if (contains(t, "ux", "ui ", "design", "graphi", "product designer", "figma")) {
            return "DESCRIPTION: Nous recherchons un(e) Designer UX/UI pour creer des experiences utilisateurs intuitives et esthetiques. " +
                   "Vous realiserez des recherches utilisateurs, wireframes et prototypes interactifs. " +
                   "Vous collaborerez avec les developpeurs pour garantir la fidelite du design en production. " +
                   "Vous contribuerez a l'evolution du Design System et des guidelines visuelles.\n" +
                   "SKILLS: Figma, Adobe XD, User Research, Prototyping, Design System, Accessibility, Sketch";
        }
        // ── Embedded / IoT ───────────────────────────────────────────────────
        if (contains(t, "embedded", "embarque", "iot", "firmware", "microcontroleur", "arduino", "raspberry")) {
            return "DESCRIPTION: Nous recherchons un(e) Ingenieur(e) Systemes Embarques pour developper des logiciels et firmwares pour nos dispositifs connectes. " +
                   "Vous concevrez et programmerez des microcontroleurs et cartes embarquees. " +
                   "Vous optimiserez les performances temps-reel et la consommation energetique. " +
                   "Vous travaillerez en collaboration avec les equipes hardware et cloud.\n" +
                   "SKILLS: C/C++, RTOS, Embedded Linux, I2C/SPI/UART, ARM Cortex, MQTT, Git";
        }
        // ── ERP / SAP ────────────────────────────────────────────────────────
        if (contains(t, "erp", "sap", "odoo", "oracle")) {
            return "DESCRIPTION: Nous recherchons un(e) Consultant(e) ERP pour implementer et optimiser nos solutions de gestion d'entreprise. " +
                   "Vous analyserez les processus metier et proposerez des configurations adaptees. " +
                   "Vous formerez les utilisateurs et assurerez le support post-implementation. " +
                   "Vous coordonnerez les migrations de donnees et les tests d'acceptation.\n" +
                   "SKILLS: SAP S/4HANA, ABAP, SAP FI/CO, SAP MM, BAPI/BADI, SQL, Project Management";
        }
        // ── Accounting / Finance ─────────────────────────────────────────────
        if (contains(t, "comptable", "accounting", "finance", "tresorier", "controleur", "auditeur", "audit")) {
            return "DESCRIPTION: Nous recherchons un(e) Comptable / Responsable Financier(e) rigoureux(se) pour gerer la comptabilite de l'entreprise. " +
                   "Vous serez en charge de la tenue des comptes, des declarations fiscales et des clotures mensuelles. " +
                   "Vous produirez les etats financiers et les reportings destines a la direction. " +
                   "Vous veillerez a la conformite avec les normes comptables en vigueur.\n" +
                   "SKILLS: Comptabilite generale, Excel, SAP, Fiscalite, IFRS, Analyse financiere, Sage";
        }
        // ── HR ───────────────────────────────────────────────────────────────
        if (contains(t, "rh", "ressources humaines", "human resources", "recrutement", "talent acquisition", "drh")) {
            return "DESCRIPTION: Nous recherchons un(e) Responsable RH pour accompagner la croissance de notre equipe. " +
                   "Vous piloterez les processus de recrutement, d'integration et de gestion des carrieres. " +
                   "Vous developperez la marque employeur et les strategies de fidelisation des talents. " +
                   "Vous assurerez la conformite avec la legislation du travail et gererez les relations sociales.\n" +
                   "SKILLS: Recrutement, ATS, Droit du travail, SIRH, Gestion de la paie, People Analytics, LinkedIn Recruiter";
        }
        // ── Marketing / Digital ──────────────────────────────────────────────
        if (contains(t, "marketing", "digital", "seo", "sem", "social media", "content", "growth")) {
            return "DESCRIPTION: Nous recherchons un(e) Responsable Marketing Digital pour accelerer notre croissance en ligne. " +
                   "Vous definirez et execterez la strategie d'acquisition et de fidelisation digitale. " +
                   "Vous gererez les campagnes payantes et le contenu organique sur tous les canaux. " +
                   "Vous analyserez les performances et optimiserez continuellement le ROI des actions marketing.\n" +
                   "SKILLS: SEO/SEM, Google Analytics, Meta Ads, HubSpot, Email Marketing, A/B Testing, Copywriting";
        }
        // ── Sales / Commercial ───────────────────────────────────────────────
        if (contains(t, "commercial", "sales", "business developer", "account manager", "ingenieur commercial")) {
            return "DESCRIPTION: Nous recherchons un(e) Commercial(e) / Business Developer dynamique pour developper notre portefeuille clients. " +
                   "Vous identifierez les opportunites, realiserez la prospection et menerez les cycles de vente de bout en bout. " +
                   "Vous construirez des relations durables avec les clients et detecterez les besoins d'upsell. " +
                   "Vous atteidrez et depasserez vos objectifs de CA trimestrels.\n" +
                   "SKILLS: Prospection B2B, CRM Salesforce, Negociation, Pipeline Management, Presentation Clients, Closing, HubSpot";
        }
        // ── Mechanical / Civil Engineering ───────────────────────────────────
        if (contains(t, "mecanique", "mechanical", "genie civil", "civil engineer", "btp", "structure")) {
            return "DESCRIPTION: Nous recherchons un(e) Ingenieur(e) Mecanique / Genie Civil pour concevoir et superviser nos projets de construction et d'equipements. " +
                   "Vous realiserez les etudes techniques, les plans et les calculs de dimensionnement. " +
                   "Vous superviserez l'execution des travaux et veillerez au respect des normes et de la securite. " +
                   "Vous coordonnerez les interventions des sous-traitants et gererez les plannings.\n" +
                   "SKILLS: AutoCAD, SolidWorks, Calcul de structure, Gestion de chantier, Normes Eurocode, MS Project, BIM";
        }
        // ── Healthcare / Pharma ──────────────────────────────────────────────
        if (contains(t, "medecin", "pharmacien", "infirmier", "sante", "health", "medical", "pharma", "biotech")) {
            return "DESCRIPTION: Nous recherchons un(e) professionnel(le) de sante pour renforcer notre equipe medicale. " +
                   "Vous assurerez la prise en charge des patients/projets dans le respect des protocoles cliniques. " +
                   "Vous collaborerez avec les equipes pluridisciplinaires pour optimiser les soins/produits. " +
                   "Vous contribuerez a la recherche et a l'amelioration continue des pratiques.\n" +
                   "SKILLS: Protocoles cliniques, BPF/BPC, Reglementation sante, Recherche clinique, Excel, Dossier patient electronique, Anglais scientifique";
        }
        // ── Generic Technical fallback ───────────────────────────────────────
        if (contains(t, "ingenieur", "engineer", "developpeur", "developer", "architecte", "architect", "tech")) {
            return "DESCRIPTION: Nous recherchons un(e) " + jobTitle + " talentueux(se) pour renforcer notre equipe technique. " +
                   "Vous participerez a la conception, au developpement et a l'amelioration de nos solutions. " +
                   "Vous travaillerez en methodologie Agile au sein d'une equipe dynamique et innovante. " +
                   "Vous contribuerez aux revues de code, aux choix techniques et a l'amelioration continue.\n" +
                   "SKILLS: Programmation, Architecture logicielle, Git, CI/CD, Tests automatises, Docker, Agile/Scrum";
        }
        // ── Generic catch-all ────────────────────────────────────────────────
        return "DESCRIPTION: Nous recherchons un(e) " + jobTitle + " motive(e) et experimente(e) pour rejoindre notre equipe. " +
               "Vous prendrez en charge les missions principales liees a ce poste avec autonomie et rigueur. " +
               "Vous collaborerez avec les differentes equipes pour atteindre les objectifs fixes. " +
               "Votre expertise contribuera directement a la croissance de l'entreprise.\n" +
               "SKILLS: " + getDomainSkillsFromTitle(jobTitle);
    }

    /** Extracts domain keywords from title for the catch-all fallback */
    private String getDomainSkillsFromTitle(String jobTitle) {
        String t = jobTitle.toLowerCase();
        if (contains(t, "logistique", "supply chain", "transport")) return "Supply Chain, ERP, Excel, Gestion des stocks, SAP, Negociation fournisseurs, Lean";
        if (contains(t, "juridique", "legal", "avocat", "juriste")) return "Droit des affaires, Redaction contractuelle, Veille juridique, RGPD, Negociation, LexisNexis";
        if (contains(t, "architecte", "architect")) return "Architecture systeme, Design Patterns, Microservices, Cloud, Documentation technique, Leadership technique";
        return "Expertise metier, Communication professionnelle, Gestion de projet, Analyse, Resolution de problemes, Reporting, Anglais professionnel";
    }

    private boolean contains(String text, String... keywords) {
        for (String kw : keywords) if (text.contains(kw)) return true;
        return false;
    }

    private String getDefaultCorrectionNote(String reason) {
        return "Nous avons procede aux corrections necessaires suite au signalement (" + reason + "). " +
               "L'offre a ete revue et mise a jour pour repondre aux exigences de la plateforme.";
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(message);
        a.showAndWait();
    }

    // =========================================================================
    // Inner class
    // =========================================================================

    private static class SkillRow {
        TextField nameField;
        ComboBox<SkillLevel> levelCombo;
        SkillRow(TextField n, ComboBox<SkillLevel> l) { this.nameField = n; this.levelCombo = l; }
    }
}










