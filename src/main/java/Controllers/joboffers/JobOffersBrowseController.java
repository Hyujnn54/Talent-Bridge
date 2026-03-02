package Controllers.joboffers;

import Models.joboffers.JobOffer;
import Models.joboffers.OfferSkill;
import Models.joboffers.ContractType;
import Models.joboffers.Status;
import Models.joboffers.MatchingResult;
import Models.joboffers.SkillLevel;
import Services.joboffers.JobOfferService;
import Services.joboffers.OfferSkillService;
import Services.joboffers.FuzzySearchService;
import Services.joboffers.NotificationService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Contrôleur pour la vue Candidat - Consultation des offres d'emploi
 */
public class JobOffersBrowseController {

    @FXML private VBox mainContainer;
    @FXML private TextField txtSearch;

    private VBox jobListContainer;
    private VBox detailContainer;
    private JobOffer selectedJob;
    private ComboBox<String> cbFilterType;
    private ComboBox<String> cbFilterLocation;

    // Service de recherche floue
    private FuzzySearchService fuzzySearchService;

    private JobOfferService jobOfferService;
    private OfferSkillService offerSkillService;
    private MatchingWidgetController matchingWidget;

    private ContractType selectedContractType = null;
    private String selectedLocation = null;

    @FXML
    public void initialize() {
        jobOfferService = new JobOfferService();
        offerSkillService = new OfferSkillService();
        fuzzySearchService = FuzzySearchService.getInstance();
        matchingWidget = new MatchingWidgetController();
        matchingWidget.setOnProfileUpdated(() -> {
            if (selectedJob != null) {
                displayJobDetails(selectedJob);
            }
            loadJobOffers();
        });
        buildUI();
        loadJobOffers();
    }

    private void buildUI() {
        if (mainContainer == null) return;
        mainContainer.getChildren().clear();
        mainContainer.setStyle("-fx-background-color: #EBF0F8; -fx-padding: 20; -fx-spacing: 14;");

        // ── Header ──
        HBox headerBox = new HBox(12);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        VBox titleBox = new VBox(3);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        Label pageTitle = new Label("Offres d'emploi");
        pageTitle.setStyle("-fx-font-size:24px; -fx-font-weight:700; -fx-text-fill:#2c3e50;");
        Label pageSub = new Label("Parcourez les offres disponibles et postulez");
        pageSub.setStyle("-fx-font-size:12px; -fx-text-fill:#8FA3B8;");
        titleBox.getChildren().addAll(pageTitle, pageSub);
        headerBox.getChildren().add(titleBox);
        mainContainer.getChildren().add(headerBox);

        // ── Search/filter bar ──
        mainContainer.getChildren().add(createSearchFilterBox());

        // ── SplitPane ──
        javafx.scene.control.SplitPane split = new javafx.scene.control.SplitPane();
        split.setDividerPositions(0.42);
        VBox.setVgrow(split, Priority.ALWAYS);
        split.setStyle("-fx-background-color: transparent; -fx-box-border: transparent; -fx-padding:0;");

        // Left
        javafx.scene.control.ScrollPane leftScroll = new javafx.scene.control.ScrollPane();
        leftScroll.setFitToWidth(true);
        leftScroll.setStyle("-fx-background: transparent; -fx-background-color: #EBF0F8;");
        leftScroll.getStyleClass().add("transparent-scroll");
        VBox leftContent = new VBox(10);
        leftContent.setStyle("-fx-background-color: #EBF0F8;");
        leftContent.setPadding(new javafx.geometry.Insets(4, 8, 16, 2));

        HBox leftHeader = new HBox(8);
        leftHeader.setAlignment(Pos.CENTER_LEFT);
        leftHeader.setStyle("-fx-padding: 0 0 4 2;");
        Label leftTitle = new Label("Offres disponibles");
        leftTitle.setStyle("-fx-font-size:13px; -fx-font-weight:700; -fx-text-fill:#2c3e50;");
        HBox.setHgrow(leftTitle, Priority.ALWAYS);
        jobCountLabel = new Label("");
        jobCountLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#8FA3B8; " +
                "-fx-background-color:#D4DCE8; -fx-background-radius:10; -fx-padding:2 8;");
        leftHeader.getChildren().addAll(leftTitle, jobCountLabel);

        jobListContainer = new VBox(8);
        leftContent.getChildren().addAll(leftHeader, jobListContainer);
        leftScroll.setContent(leftContent);

        // Right
        javafx.scene.control.ScrollPane rightScroll = new javafx.scene.control.ScrollPane();
        rightScroll.setFitToWidth(true);
        rightScroll.setStyle("-fx-background: transparent; -fx-background-color: #EBF0F8;");
        rightScroll.getStyleClass().add("transparent-scroll");
        VBox rightContent = new VBox(14);
        rightContent.setStyle("-fx-background-color: #EBF0F8;");
        rightContent.setPadding(new javafx.geometry.Insets(4, 4, 16, 8));

        detailContainer = new VBox(14);
        // Placeholder
        VBox ph = new VBox(16);
        ph.setAlignment(Pos.CENTER);
        ph.setStyle("-fx-background-color:white; -fx-background-radius:14; -fx-padding:50 24;" +
                "-fx-border-color:#E8EEF8; -fx-border-width:1; -fx-border-radius:14;" +
                "-fx-effect:dropshadow(gaussian,rgba(91,163,245,0.09),12,0,0,3);");
        Label phIcon = new Label("🔍"); phIcon.setStyle("-fx-font-size:40px;");
        Label phTitle2 = new Label("Sélectionnez une offre");
        phTitle2.setStyle("-fx-font-size:14px; -fx-font-weight:700; -fx-text-fill:#2c3e50;");
        Label phSub = new Label("Cliquez sur une offre pour voir les détails et postuler");
        phSub.setStyle("-fx-font-size:12px; -fx-text-fill:#8FA3B8; -fx-text-alignment:center;");
        phSub.setWrapText(true);
        ph.getChildren().addAll(phIcon, phTitle2, phSub);
        detailContainer.getChildren().add(ph);

        rightContent.getChildren().add(detailContainer);
        rightScroll.setContent(rightContent);

        split.getItems().addAll(leftScroll, rightScroll);
        mainContainer.getChildren().add(split);
    }

    private VBox createSearchFilterBox() {
        VBox container = new VBox(0);
        container.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                          "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 3);");

        HBox searchRow = new HBox(12);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.setStyle("-fx-padding: 20 20 15 20;");

        txtSearch = new TextField();
        txtSearch.setPromptText("Rechercher par titre, description...");
        txtSearch.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 12 15; " +
                          "-fx-border-color: #e9ecef; -fx-border-radius: 8; -fx-font-size: 14px; -fx-pref-height: 42;");
        HBox.setHgrow(txtSearch, Priority.ALWAYS);
        txtSearch.setOnAction(e -> handleSearch());

        Button btnSearchAction = new Button("🔍 Rechercher");
        btnSearchAction.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-font-size: 14px; " +
                                "-fx-font-weight: 600; -fx-padding: 12 24; -fx-background-radius: 8; -fx-cursor: hand;");
        btnSearchAction.setOnAction(e -> handleSearch());

        searchRow.getChildren().addAll(txtSearch, btnSearchAction);

        Separator separator = new Separator();

        HBox filterRow = new HBox(15);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        filterRow.setStyle("-fx-padding: 15 20 20 20;");

        Label filterLabel = new Label("Filtrer par :");
        filterLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #495057;");

        cbFilterType = new ComboBox<>();
        cbFilterType.setPromptText("Type de contrat");
        cbFilterType.getItems().add("Tous les types");
        for (ContractType type : ContractType.values()) {
            cbFilterType.getItems().add(formatContractType(type));
        }
        cbFilterType.setStyle("-fx-pref-width: 160; -fx-pref-height: 38;");
        cbFilterType.setOnAction(e -> applyFilters());

        cbFilterLocation = new ComboBox<>();
        cbFilterLocation.setPromptText("Localisation");
        cbFilterLocation.getItems().add("Toutes les villes");
        try {
            List<String> locations = jobOfferService.getAllLocations();
            cbFilterLocation.getItems().addAll(locations);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        cbFilterLocation.setStyle("-fx-pref-width: 160; -fx-pref-height: 38;");
        cbFilterLocation.setOnAction(e -> applyFilters());

        Button btnReset = new Button("✕ Réinitialiser");
        btnReset.setStyle("-fx-background-color: #f8f9fa; -fx-text-fill: #6c757d; -fx-font-size: 13px; " +
                         "-fx-padding: 10 16; -fx-background-radius: 8; -fx-cursor: hand; " +
                         "-fx-border-color: #dee2e6; -fx-border-radius: 8;");
        btnReset.setOnAction(e -> resetFilters());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label resultCount = new Label("");
        resultCount.setId("resultCount");
        resultCount.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 13px;");

        filterRow.getChildren().addAll(filterLabel, cbFilterType, cbFilterLocation, btnReset, spacer, resultCount);
        container.getChildren().addAll(searchRow, separator, filterRow);
        return container;
    }

    private void applyFilters() {
        String typeValue = cbFilterType.getValue();
        String locationValue = cbFilterLocation.getValue();

        selectedContractType = (typeValue == null || typeValue.equals("Tous les types")) ? null : getContractTypeFromLabel(typeValue);
        selectedLocation = (locationValue == null || locationValue.equals("Toutes les villes")) ? null : locationValue;

        loadFilteredJobOffers();
    }

    private void resetFilters() {
        selectedContractType = null;
        selectedLocation = null;
        if (cbFilterType != null) cbFilterType.setValue(null);
        if (cbFilterLocation != null) cbFilterLocation.setValue(null);
        if (txtSearch != null) txtSearch.clear();
        loadJobOffers();
    }

    private void loadFilteredJobOffers() {
        if (jobListContainer == null) return;
        jobListContainer.getChildren().clear();

        try {
            List<JobOffer> jobs = jobOfferService.filterJobOffers(selectedLocation, selectedContractType, Status.OPEN);

            String keyword = txtSearch != null ? txtSearch.getText().trim() : "";
            if (!keyword.isEmpty()) {
                // Utilisation de la recherche floue (tolère les fautes de frappe)
                final double FUZZY_THRESHOLD = 0.6; // 60% de similarité minimum

                jobs = jobs.stream()
                    .filter(job -> {
                        // Recherche exacte (priorité)
                        String title = job.getTitle() != null ? job.getTitle().toLowerCase() : "";
                        String desc = job.getDescription() != null ? job.getDescription().toLowerCase() : "";
                        String loc = job.getLocation() != null ? job.getLocation().toLowerCase() : "";
                        String keywordLower = keyword.toLowerCase();

                        // Si correspondance exacte, on garde
                        if (title.contains(keywordLower) || desc.contains(keywordLower) || loc.contains(keywordLower)) {
                            return true;
                        }

                        // Sinon, recherche floue
                        return fuzzySearchService.matchesAny(keyword, FUZZY_THRESHOLD,
                            job.getTitle(), job.getDescription(), job.getLocation());
                    })
                    .sorted((j1, j2) -> {
                        // Trier par pertinence de la recherche floue
                        double score1 = fuzzySearchService.calculateBestScore(
                            (j1.getTitle() + " " + j1.getDescription()), keyword);
                        double score2 = fuzzySearchService.calculateBestScore(
                            (j2.getTitle() + " " + j2.getDescription()), keyword);
                        return Double.compare(score2, score1); // Tri décroissant
                    })
                    .toList();

                // Notification si recherche floue a trouvé des résultats
                if (!jobs.isEmpty() && !keyword.isEmpty()) {
                    boolean hasExactMatch = jobs.stream().anyMatch(job ->
                        (job.getTitle() != null && job.getTitle().toLowerCase().contains(keyword.toLowerCase())));

                    if (!hasExactMatch) {
                        NotificationService.showInfo("Recherche intelligente",
                            "Résultats approximatifs trouvés pour \"" + keyword + "\"");
                    }
                }
            }

            updateResultCount(jobs.size());
            if (jobCountLabel != null) jobCountLabel.setText(jobs.size() + " offre(s)");

            if (jobs.isEmpty()) {
                jobListContainer.getChildren().add(createEmptyState());

                // Proposer des suggestions si aucun résultat
                if (!keyword.isEmpty()) {
                    showSearchSuggestions(keyword);
                }
                return;
            }

            boolean first = true;
            for (JobOffer job : jobs) {
                VBox card = createJobCard(job);
                jobListContainer.getChildren().add(card);
                if (first) {
                    selectJob(job, card);
                    first = false;
                }
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les offres : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Affiche des suggestions de recherche si aucun résultat trouvé
     */
    private void showSearchSuggestions(String query) {
        try {
            List<JobOffer> allJobs = jobOfferService.getAllOpenJobOffers();
            List<String> titles = allJobs.stream()
                .map(JobOffer::getTitle)
                .filter(t -> t != null)
                .distinct()
                .toList();

            List<String> suggestions = fuzzySearchService.getSuggestions(query, titles, 3);

            if (!suggestions.isEmpty()) {
                VBox suggestionBox = new VBox(10);
                suggestionBox.setStyle("-fx-padding: 20; -fx-alignment: center;");

                Label suggestionLabel = new Label("💡 Vouliez-vous dire :");
                suggestionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6c757d;");
                suggestionBox.getChildren().add(suggestionLabel);

                for (String suggestion : suggestions) {
                    Button suggBtn = new Button(suggestion);
                    suggBtn.setStyle("-fx-background-color: #e9ecef; -fx-text-fill: #495057; " +
                                    "-fx-padding: 8 16; -fx-background-radius: 20; -fx-cursor: hand;");
                    suggBtn.setOnAction(e -> {
                        txtSearch.setText(suggestion);
                        handleSearch();
                    });
                    suggestionBox.getChildren().add(suggBtn);
                }

                jobListContainer.getChildren().add(suggestionBox);
            }
        } catch (SQLException e) {
            // Ignorer les erreurs de suggestion
        }
    }

    private void updateResultCount(int count) {
        Label resultCount = (Label) mainContainer.lookup("#resultCount");
        if (resultCount != null) {
            if (count == 0) {
                resultCount.setText("Aucun résultat");
            } else if (count == 1) {
                resultCount.setText("1 offre trouvée");
            } else {
                resultCount.setText(count + " offres trouvées");
            }
        }
    }

    private VBox createEmptyState() {
        VBox emptyBox = new VBox(15);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setStyle("-fx-padding: 40;");

        Label emptyIcon = new Label("🔭");
        emptyIcon.setStyle("-fx-font-size: 48px;");

        Label emptyText = new Label("Aucune offre trouvée");
        emptyText.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: #6c757d;");

        Label emptyHint = new Label("Essayez de modifier vos critères de recherche");
        emptyHint.setStyle("-fx-font-size: 13px; -fx-text-fill: #adb5bd;");

        emptyBox.getChildren().addAll(emptyIcon, emptyText, emptyHint);
        return emptyBox;
    }

    private String formatContractType(ContractType type) {
        return switch (type) {
            case CDI -> "CDI";
            case CDD -> "CDD";
            case INTERNSHIP -> "Stage";
            case FREELANCE -> "Freelance";
            case PART_TIME -> "Temps partiel";
            case FULL_TIME -> "Temps plein";
        };
    }

    private ContractType getContractTypeFromLabel(String label) {
        return switch (label) {
            case "CDI" -> ContractType.CDI;
            case "CDD" -> ContractType.CDD;
            case "Stage" -> ContractType.INTERNSHIP;
            case "Freelance" -> ContractType.FREELANCE;
            case "Temps partiel" -> ContractType.PART_TIME;
            case "Temps plein" -> ContractType.FULL_TIME;
            default -> null;
        };
    }

    private Label jobCountLabel;

    private VBox createJobListPanel() {
        VBox panel = new VBox(15);
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 2);");

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("📋 Offres disponibles");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
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

        jobListContainer = new VBox(10);
        jobListContainer.setStyle("-fx-padding: 5 5 5 0;");
        scroll.setContent(jobListContainer);

        panel.getChildren().addAll(titleRow, scroll);
        return panel;
    }

    private VBox createDetailPanel() {
        VBox panel = new VBox(20);
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 25; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 2);");

        Label title = new Label("📄 Détails de l'offre");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        detailContainer = new VBox(20);
        detailContainer.setStyle("-fx-padding: 10 5 10 0;");
        scrollPane.setContent(detailContainer);

        VBox selectMessage = new VBox(14);
        selectMessage.setAlignment(Pos.CENTER);
        selectMessage.setStyle("-fx-padding: 60 24;");

        Label icon = new Label("🔍");
        icon.setStyle("-fx-font-size: 40px;");

        Label phTitle = new Label("Sélectionnez une offre");
        phTitle.setStyle("-fx-font-size:15px; -fx-font-weight:700; -fx-text-fill:#2c3e50;");

        Label text = new Label("Cliquez sur une offre à gauche pour voir les détails et postuler");
        text.setStyle("-fx-font-size:12px; -fx-text-fill:#8FA3B8; -fx-text-alignment:center;");
        text.setWrapText(true);

        selectMessage.getChildren().addAll(icon, phTitle, text);
        detailContainer.getChildren().add(selectMessage);

        panel.getChildren().addAll(title, scrollPane);
        return panel;
    }

    private void loadJobOffers() {
        if (jobListContainer == null) return;
        jobListContainer.getChildren().clear();

        try {
            List<JobOffer> jobs = jobOfferService.getJobOffersByStatus(Status.OPEN);
            updateResultCount(jobs.size());
            if (jobCountLabel != null) jobCountLabel.setText(jobs.size() + " offre(s)");

            if (jobs.isEmpty()) {
                jobListContainer.getChildren().add(createEmptyState());
                return;
            }

            boolean first = true;
            for (JobOffer job : jobs) {
                VBox card = createJobCard(job);
                jobListContainer.getChildren().add(card);
                if (first) {
                    selectJob(job, card);
                    first = false;
                }
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les offres : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private VBox createJobCard(JobOffer job) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10; -fx-padding: 15; " +
                     "-fx-border-color: transparent; -fx-border-radius: 10; -fx-cursor: hand;");

        // Check if candidate already applied
        Long candidateId = Utils.UserContext.getCandidateId();
        boolean alreadyApplied = candidateId != null &&
                Services.application.ApplicationService.hasAlreadyApplied(job.getId(), candidateId);

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(job.getTitle());
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
        title.setWrapText(true);
        HBox.setHgrow(title, Priority.ALWAYS);
        titleRow.getChildren().add(title);

        if (alreadyApplied) {
            Label appliedBadge = new Label("✅ Postulé");
            appliedBadge.setStyle("-fx-background-color: #d4edda; -fx-text-fill: #155724; -fx-padding: 3 8; " +
                                  "-fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: 700;");
            titleRow.getChildren().add(appliedBadge);
        }

        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER_LEFT);

        Label typeBadge = new Label(formatContractType(job.getContractType()));
        typeBadge.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-padding: 3 10; " +
                          "-fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: 600;");

        Label locationBadge = new Label("📍 " + (job.getLocation() != null ? job.getLocation() : "Non spécifié"));
        locationBadge.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px;");

        badges.getChildren().addAll(typeBadge, locationBadge);

        if (job.getCreatedAt() != null) {
            Label date = new Label("Publié le " + job.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            date.setStyle("-fx-text-fill: #adb5bd; -fx-font-size: 11px;");
            card.getChildren().addAll(titleRow, badges, date);
        } else {
            card.getChildren().addAll(titleRow, badges);
        }

        card.setOnMouseEntered(e -> {
            if (selectedJob == null || !selectedJob.getId().equals(job.getId())) {
                card.setStyle("-fx-background-color: #e9ecef; -fx-background-radius: 10; -fx-padding: 15; " +
                             "-fx-border-color: transparent; -fx-border-radius: 10; -fx-cursor: hand;");
            }
        });
        card.setOnMouseExited(e -> {
            if (selectedJob == null || !selectedJob.getId().equals(job.getId())) {
                card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10; -fx-padding: 15; " +
                             "-fx-border-color: transparent; -fx-border-radius: 10; -fx-cursor: hand;");
            }
        });

        card.setOnMouseClicked(e -> selectJob(job, card));
        return card;
    }

    private void selectJob(JobOffer job, VBox card) {
        jobListContainer.getChildren().forEach(node -> {
            if (node instanceof VBox) {
                node.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10; -fx-padding: 15; " +
                             "-fx-border-color: transparent; -fx-border-radius: 10; -fx-cursor: hand;");
            }
        });

        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 15; " +
                     "-fx-border-color: #5BA3F5; -fx-border-width: 2; -fx-border-radius: 10; " +
                     "-fx-effect: dropshadow(gaussian, rgba(91,163,245,0.3), 8, 0, 0, 2); -fx-cursor: hand;");

        selectedJob = job;
        displayJobDetails(job);
    }

    private void displayJobDetails(JobOffer job) {
        detailContainer.getChildren().clear();

        VBox headerCard = new VBox(12);
        headerCard.setStyle("-fx-background-color: linear-gradient(to right, #f8f9fa, #fff); " +
                           "-fx-background-radius: 10; -fx-padding: 25;");

        Label title = new Label(job.getTitle());
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
        title.setWrapText(true);

        FlowPane metaFlow = new FlowPane(12, 8);
        metaFlow.setAlignment(Pos.CENTER_LEFT);

        Label contractType = new Label("💼 " + formatContractType(job.getContractType()));
        contractType.setStyle("-fx-background-color: #e3f2fd; -fx-text-fill: #1976d2; -fx-padding: 6 12; " +
                             "-fx-background-radius: 15; -fx-font-size: 13px; -fx-font-weight: 600;");

        Label location = new Label("📍 " + (job.getLocation() != null ? job.getLocation() : "Non spécifié"));
        location.setStyle("-fx-background-color: #f3e5f5; -fx-text-fill: #7b1fa2; -fx-padding: 6 12; " +
                         "-fx-background-radius: 15; -fx-font-size: 13px; -fx-font-weight: 600;");

        metaFlow.getChildren().addAll(contractType, location);

        if (job.getDeadline() != null) {
            Label deadline = new Label("⏰ Date limite : " + job.getDeadline().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            deadline.setStyle("-fx-background-color: #ffebee; -fx-text-fill: #c62828; -fx-padding: 6 12; " +
                             "-fx-background-radius: 15; -fx-font-size: 13px; -fx-font-weight: 600;");
            metaFlow.getChildren().add(deadline);
        }

        headerCard.getChildren().addAll(title, metaFlow);
        detailContainer.getChildren().add(headerCard);

        if (matchingWidget != null) {
            MatchingResult matchResult = matchingWidget.calculateMatch(job);
            VBox matchingSection = matchingWidget.createMatchingScoreWidget(matchResult);
            detailContainer.getChildren().add(matchingSection);
        }

        if (job.getDescription() != null && !job.getDescription().isBlank()) {
            VBox descSection = new VBox(10);
            descSection.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10; -fx-padding: 20;");

            Label descTitle = new Label("📝 Description du poste");
            descTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

            Label descText = new Label(job.getDescription());
            descText.setWrapText(true);
            descText.setStyle("-fx-text-fill: #495057; -fx-font-size: 14px; -fx-line-spacing: 4;");

            descSection.getChildren().addAll(descTitle, descText);
            detailContainer.getChildren().add(descSection);
        }

        try {
            List<OfferSkill> skills = offerSkillService.getSkillsByOfferId(job.getId());
            if (!skills.isEmpty()) {
                VBox skillsSection = new VBox(12);
                skillsSection.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10; -fx-padding: 20;");

                Label skillsTitle = new Label("🎯 Compétences requises");
                skillsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

                FlowPane skillsFlow = new FlowPane(8, 8);
                for (OfferSkill skill : skills) {
                    Label skillTag = new Label(skill.getSkillName() + " - " + formatSkillLevel(skill.getLevelRequired()));
                    skillTag.setStyle("-fx-background-color: white; -fx-padding: 8 14; -fx-background-radius: 8; " +
                                     "-fx-border-color: #dee2e6; -fx-border-radius: 8; -fx-font-size: 12px;");
                    skillsFlow.getChildren().add(skillTag);
                }

                skillsSection.getChildren().addAll(skillsTitle, skillsFlow);
                detailContainer.getChildren().add(skillsSection);
            }
        } catch (SQLException e) {
            System.err.println("Erreur chargement compétences : " + e.getMessage());
        }

        if (job.getLocation() != null && !job.getLocation().trim().isEmpty()) {
            VBox distanceSection = new VBox(10);
            distanceSection.setStyle("-fx-background-color: #e8f5e9; -fx-background-radius: 10; -fx-padding: 15;");

            Label locationLabel = new Label("📍 " + job.getLocation());
            locationLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2e7d32;");

            distanceSection.getChildren().add(locationLabel);
            detailContainer.getChildren().add(distanceSection);
        }

        if (job.getCreatedAt() != null) {
            Label posted = new Label("📅 Publié le " + job.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.FRENCH)));
            posted.setStyle("-fx-text-fill: #adb5bd; -fx-font-size: 12px; -fx-padding: 10 0;");
            detailContainer.getChildren().add(posted);
        }

        HBox actionBox = new HBox(12);
        actionBox.setAlignment(Pos.CENTER);
        actionBox.setStyle("-fx-padding: 20 0;");

        Long candidateId = Utils.UserContext.getCandidateId();
        boolean alreadyApplied = candidateId != null &&
                Services.application.ApplicationService.hasAlreadyApplied(job.getId(), candidateId);

        if (alreadyApplied) {
            Button btnAlreadyApplied = new Button("✅ Déjà postulé");
            btnAlreadyApplied.setStyle(
                    "-fx-background-color: #e9ecef; " +
                    "-fx-text-fill: #6c757d; -fx-font-weight: 700; -fx-font-size: 15px; " +
                    "-fx-padding: 14 40; -fx-background-radius: 25; -fx-cursor: default; " +
                    "-fx-border-color: #ced4da; -fx-border-width: 2; -fx-border-radius: 25;");
            btnAlreadyApplied.setDisable(true);
            btnAlreadyApplied.setMouseTransparent(true);

            Label alreadyAppliedNote = new Label("Vous avez déjà soumis une candidature pour cette offre.");
            alreadyAppliedNote.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 13px; -fx-font-style: italic;");

            VBox alreadyAppliedBox = new VBox(8);
            alreadyAppliedBox.setAlignment(Pos.CENTER);
            alreadyAppliedBox.getChildren().addAll(btnAlreadyApplied, alreadyAppliedNote);
            actionBox.getChildren().add(alreadyAppliedBox);
        } else {
            Button btnApply = new Button("📝 Postuler à cette offre");
            btnApply.setStyle("-fx-background-color: linear-gradient(to right, #28a745, #20c997); " +
                             "-fx-text-fill: white; -fx-font-weight: 700; -fx-font-size: 15px; " +
                             "-fx-padding: 14 40; -fx-background-radius: 25; -fx-cursor: hand;");
            btnApply.setOnAction(e -> handleApply(job));
            actionBox.getChildren().add(btnApply);
        }

        detailContainer.getChildren().add(actionBox);
    }

    private String formatSkillLevel(SkillLevel level) {
        return switch (level) {
            case BEGINNER -> "Débutant";
            case INTERMEDIATE -> "Intermédiaire";
            case ADVANCED -> "Avancé";
        };
    }

    private void handleApply(JobOffer job) {
        Long candidateId = Utils.UserContext.getCandidateId();

        if (candidateId == null) {
            showAlert("Erreur", "Vous devez être connecté en tant que candidat pour postuler.", Alert.AlertType.WARNING);
            return;
        }

        // Check if already applied
        if (Services.application.ApplicationService.hasAlreadyApplied(job.getId(), candidateId)) {
            showAlert("Information", "Vous avez déjà postulé à cette offre.", Alert.AlertType.INFORMATION);
            return;
        }

        showApplicationForm(job);
    }

    private void showApplicationForm(JobOffer job) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Postuler");
        dialog.setHeaderText("Postuler pour : " + job.getTitle());

        ScrollPane scrollPane = new ScrollPane();
        VBox content = new VBox(15);
        content.setPadding(new javafx.geometry.Insets(20));
        content.setStyle("-fx-padding: 20;");
        scrollPane.setContent(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(500);

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

        TextField phoneField = new TextField();
        phoneField.setPromptText("Entrez votre numéro");
        phoneField.setPrefWidth(250);

        Label phoneErrorLabel = new Label();
        phoneErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");
        phoneErrorLabel.setVisible(false);

        phoneContainer.getChildren().addAll(countryCombo, phoneField);

        // Cover Letter field
        Label letterLabel = new Label("Lettre de motivation * (50-2000 caractères)");
        letterLabel.setStyle("-fx-font-weight: bold;");

        TextArea letterArea = new TextArea();
        letterArea.setPromptText("Expliquez pourquoi vous êtes intéressé par ce poste...");
        letterArea.setPrefRowCount(8);
        letterArea.setWrapText(true);
        letterArea.setStyle("-fx-font-size: 13px;");

        Label letterCharCount = new Label("0/2000");
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
        Label pdfLabel = new Label("CV (PDF) - Optionnel");
        pdfLabel.setStyle("-fx-font-weight: bold;");

        HBox pdfBox = new HBox(10);
        pdfBox.setAlignment(Pos.CENTER_LEFT);
        TextField pdfPathField = new TextField();
        pdfPathField.setPromptText("Aucun fichier sélectionné");
        pdfPathField.setEditable(false);
        pdfPathField.setPrefWidth(280);

        Button btnBrowse = new Button("📂 Parcourir");
        btnBrowse.setStyle("-fx-padding: 6 12; -fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 6;");
        btnBrowse.setOnAction(e -> {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Sélectionner un fichier PDF");
            fileChooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf")
            );
            java.io.File selectedFile = fileChooser.showOpenDialog(null);
            if (selectedFile != null) {
                pdfPathField.setText(selectedFile.getAbsolutePath());
            }
        });

        // "Use Profile CV" button
        Button btnUseProfileCv = new Button("📄 Utiliser le CV du profil");
        btnUseProfileCv.setStyle("-fx-padding: 6 12; -fx-background-color: #28a745; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 6; -fx-font-size: 12px;");
        btnUseProfileCv.setOnAction(e -> {
            try {
                Long candId = Utils.UserContext.getCandidateId();
                if (candId == null) { showAlert("Erreur", "Candidat non trouvé.", Alert.AlertType.WARNING); return; }
                Services.user.ProfileService profileSvc = new Services.user.ProfileService();
                Services.user.ProfileService.CandidateInfo cInfo = profileSvc.getCandidateInfo(candId);
                String profileCvPath = cInfo.cvPath();
                if (profileCvPath == null || profileCvPath.isBlank()) {
                    showAlert("Pas de CV", "Aucun CV n'est enregistré dans votre profil.\nVeuillez d'abord télécharger un CV dans votre profil ou utiliser le bouton Parcourir.", Alert.AlertType.INFORMATION);
                    return;
                }
                java.io.File profileCvFile = new java.io.File(profileCvPath);
                if (!profileCvFile.exists()) {
                    showAlert("Fichier introuvable", "Le fichier CV de votre profil est introuvable :\n" + profileCvPath, Alert.AlertType.WARNING);
                    return;
                }
                pdfPathField.setText(profileCvFile.getAbsolutePath());
            } catch (Exception ex) {
                showAlert("Erreur", "Impossible de charger le CV du profil : " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        pdfBox.getChildren().addAll(pdfPathField, btnBrowse, btnUseProfileCv);

        // Phone validation on input change
        phoneField.textProperty().addListener((obs, oldVal, newVal) -> {
            String country = countryCombo.getValue();
            boolean isValid = validatePhone(country, newVal);

            if (newVal.isEmpty()) {
                phoneErrorLabel.setVisible(false);
            } else if (!isValid) {
                phoneErrorLabel.setText(getPhoneErrorMessage(country, newVal));
                phoneErrorLabel.setVisible(true);
            } else {
                phoneErrorLabel.setVisible(false);
            }
        });

        countryCombo.setOnAction(e -> {
            String newVal = phoneField.getText();
            String country = countryCombo.getValue();
            boolean isValid = validatePhone(country, newVal);

            if (newVal.isEmpty()) {
                phoneErrorLabel.setVisible(false);
            } else if (!isValid) {
                phoneErrorLabel.setText(getPhoneErrorMessage(country, newVal));
                phoneErrorLabel.setVisible(true);
            } else {
                phoneErrorLabel.setVisible(false);
            }
        });

        // Generate Cover Letter button (AI)
        Button btnGenerateLetter = new Button("🤖 Générer avec IA");
        btnGenerateLetter.setStyle("-fx-background-color: #9B59B6; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 12px;");
        btnGenerateLetter.setOnAction(e -> generateCoverLetterWithAI(job, letterArea, pdfPathField));

        HBox generateBox = new HBox(10);
        generateBox.setAlignment(Pos.CENTER_LEFT);
        generateBox.getChildren().add(btnGenerateLetter);

        content.getChildren().addAll(
                phoneLabel, phoneContainer, phoneErrorLabel,
                letterBox, letterErrorLabel,
                generateBox,
                pdfLabel, pdfBox
        );

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Style the OK button
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("Postuler");
        okButton.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white;");

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                String country = countryCombo.getValue();
                String phone = phoneField.getText();
                String coverLetter = letterArea.getText();
                String cvPath = pdfPathField.getText();

                // Validate phone based on selected country
                if (!validatePhone(country, phone)) {
                    showAlert("Erreur de validation", getPhoneErrorMessage(country, phone), Alert.AlertType.ERROR);
                    return;
                }

                // Validate cover letter
                if (coverLetter.length() < 50) {
                    showAlert("Erreur de validation", "La lettre de motivation doit contenir au moins 50 caractères.", Alert.AlertType.ERROR);
                    return;
                }
                if (coverLetter.length() > 2000) {
                    showAlert("Erreur de validation", "La lettre de motivation ne peut pas dépasser 2000 caractères.", Alert.AlertType.ERROR);
                    return;
                }

                submitApplication(job, phone, coverLetter, cvPath);
            }
        });
    }

    private boolean validatePhone(String country, String phone) {
        if (phone == null || phone.isEmpty()) return false;
        String digits = phone.replaceAll("[^0-9]", "");
        if ("Tunisie (+216)".equals(country)) {
            return digits.length() == 8 && (digits.startsWith("2") || digits.startsWith("5") || digits.startsWith("9"));
        } else if ("France (+33)".equals(country)) {
            return digits.length() == 9 || digits.length() == 10;
        }
        return digits.length() >= 8;
    }

    private String getPhoneErrorMessage(String country, String phone) {
        if (phone == null || phone.isEmpty()) return "Le numéro de téléphone est requis.";
        if ("Tunisie (+216)".equals(country)) {
            return "Format tunisien : 8 chiffres commençant par 2, 5 ou 9";
        } else if ("France (+33)".equals(country)) {
            return "Format français : 9-10 chiffres";
        }
        return "Numéro de téléphone invalide";
    }

    private void submitApplication(JobOffer job, String phone, String coverLetter, String cvPath) {
        try {
            Long candidateId = Utils.UserContext.getCandidateId();
            if (candidateId == null) {
                showAlert("Erreur", "ID candidat non trouvé. Veuillez vous reconnecter.", Alert.AlertType.ERROR);
                return;
            }

            // Check if candidate has already applied to this offer
            if (Services.application.ApplicationService.hasAlreadyApplied(job.getId(), candidateId)) {
                showAlert("Déjà postulé",
                        "Vous avez déjà postulé à cette offre.\n\nVous pouvez consulter votre candidature dans la section Candidatures.",
                        Alert.AlertType.WARNING);
                return;
            }

            // If cvPath is provided, it's a file path - upload the file
            java.io.File pdfFile = null;
            if (cvPath != null && !cvPath.isEmpty()) {
                pdfFile = new java.io.File(cvPath);
            }

            Long applicationId = Services.application.ApplicationService.createWithPDF(job.getId(), candidateId, phone, coverLetter, pdfFile);

            if (applicationId != null) {
                showAlert("Succès", "Candidature soumise avec succès!", Alert.AlertType.INFORMATION);
                sendApplicationConfirmationEmail(candidateId, job.getTitle());
                // Refresh job list cards (to show ✅ badge) and detail view (to disable apply button)
                loadJobOffers();
                if (selectedJob != null && selectedJob.getId().equals(job.getId())) {
                    displayJobDetails(job);
                }
            } else {
                showAlert("Erreur", "Échec de la soumission. Veuillez réessayer.", Alert.AlertType.ERROR);
            }

        } catch (Exception e) {
            showAlert("Erreur", "Échec de la soumission : " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void sendApplicationConfirmationEmail(Long candidateId, String offerTitle) {
        new Thread(() -> {
            try {
                Services.events.UserService.UserInfo info = Services.events.UserService.getUserInfo(candidateId);
                if (info == null || info.email() == null || info.email().isBlank()) {
                    System.err.println("[JobOffersBrowse] No email found for candidate " + candidateId + " — skipping confirmation.");
                    return;
                }

                String candidateName = ((info.firstName() != null ? info.firstName() : "") + " " +
                        (info.lastName() != null ? info.lastName() : "")).trim();
                String title = (offerTitle != null && !offerTitle.isBlank()) ? offerTitle : "Offre d'emploi";

                System.out.println("[JobOffersBrowse] Sending application confirmation to: " + info.email());
                boolean sent = Services.application.EmailServiceApplication.sendApplicationConfirmation(
                        info.email(),
                        candidateName.isEmpty() ? "Candidat" : candidateName,
                        title,
                        java.time.LocalDateTime.now()
                );
                System.out.println("[JobOffersBrowse] Application confirmation email " + (sent ? "sent ✓" : "FAILED ✗"));
            } catch (Exception e) {
                System.err.println("[JobOffersBrowse] Failed to send confirmation email: " + e.getMessage());
            }
        }, "AppConfirmEmail").start();
    }

    /**
     * Generate cover letter using AI based on candidate profile and CV
     */
    private void generateCoverLetterWithAI(JobOffer job, TextArea letterArea, TextField pdfPathField) {
        // Show loading dialog
        Alert loadingAlert = new Alert(Alert.AlertType.INFORMATION);
        loadingAlert.setTitle("Génération de lettre");
        loadingAlert.setHeaderText(null);
        loadingAlert.setContentText("Génération de votre lettre de motivation personnalisée...\nCela peut prendre un moment.");
        loadingAlert.getButtonTypes().setAll(ButtonType.CANCEL);
        loadingAlert.initModality(javafx.stage.Modality.NONE);
        loadingAlert.show();

        new Thread(() -> {
            try {
                Long candidateId = Utils.UserContext.getCandidateId();
                if (candidateId == null) {
                    javafx.application.Platform.runLater(() -> {
                        loadingAlert.close();
                        showAlert("Erreur", "ID candidat non trouvé. Veuillez vous reconnecter.", Alert.AlertType.ERROR);
                    });
                    return;
                }

                // Fetch candidate information
                Services.events.UserService.UserInfo candidateInfo = Services.events.UserService.getUserInfo(candidateId);
                if (candidateInfo == null) {
                    javafx.application.Platform.runLater(() -> {
                        loadingAlert.close();
                        showAlert("Erreur", "Impossible de récupérer les informations du candidat.", Alert.AlertType.ERROR);
                    });
                    return;
                }

                // Fetch candidate skills from database
                java.util.List<String> candidateSkills = Services.events.UserService.getCandidateSkills(candidateId);

                // Extract CV content if PDF is uploaded
                String cvContent = "";
                String pdfPath = pdfPathField.getText();
                if (pdfPath != null && !pdfPath.isEmpty()) {
                    try {
                        Services.application.FileService fileService = new Services.application.FileService();
                        cvContent = fileService.extractTextFromPDF(pdfPath);
                        if (cvContent == null) cvContent = "";
                    } catch (Exception e) {
                        System.err.println("Could not extract CV text: " + e.getMessage());
                        cvContent = "";
                    }
                }

                String experience = candidateInfo.experienceYears() != null && candidateInfo.experienceYears() > 0
                        ? candidateInfo.experienceYears() + " ans d'expérience"
                        : "Non spécifié";

                String education = candidateInfo.educationLevel() != null && !candidateInfo.educationLevel().isEmpty()
                        ? candidateInfo.educationLevel()
                        : "Non spécifié";

                // Get company name from recruiter
                String companyName = Services.events.UserService.getRecruiterCompanyName(job.getRecruiterId());
                if (companyName == null || companyName.isEmpty()) {
                    companyName = "Votre entreprise";
                }

                // Call Cover Letter generation service
                String generatedCoverLetter = Services.application.GrokAIService.generateCoverLetter(
                        candidateInfo.firstName() + " " + candidateInfo.lastName(),
                        candidateInfo.email(),
                        candidateInfo.phone(),
                        job.getTitle(),
                        companyName,
                        experience,
                        education,
                        candidateSkills,
                        cvContent
                );

                String finalLetter = generatedCoverLetter;
                javafx.application.Platform.runLater(() -> {
                    try {
                        loadingAlert.close();
                    } catch (Exception ex) {}

                    if (finalLetter != null && !finalLetter.isEmpty()) {
                        Alert reviewAlert = new Alert(Alert.AlertType.INFORMATION);
                        reviewAlert.setTitle("Lettre générée");
                        reviewAlert.setHeaderText("Voici votre lettre générée par IA. Vous pouvez la modifier:");

                        TextArea textArea = new TextArea(finalLetter);
                        textArea.setWrapText(true);
                        textArea.setPrefRowCount(15);
                        textArea.setStyle("-fx-font-size: 12px;");

                        reviewAlert.getDialogPane().setContent(textArea);
                        reviewAlert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

                        var reviewResult = reviewAlert.showAndWait();
                        if (reviewResult.isPresent() && reviewResult.get() == ButtonType.OK) {
                            letterArea.setText(textArea.getText());
                            showAlert("Succès", "Lettre insérée! Vous pouvez encore la modifier avant de soumettre.", Alert.AlertType.INFORMATION);
                        }
                    } else {
                        showAlert("Erreur", "Échec de la génération. Veuillez écrire manuellement ou réessayer.", Alert.AlertType.ERROR);
                    }
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    try {
                        loadingAlert.close();
                    } catch (Exception ex) {}
                    showAlert("Erreur", "Erreur lors de la génération : " + e.getMessage(), Alert.AlertType.ERROR);
                    e.printStackTrace();
                });
            }
        }).start();
    }

    @FXML
    private void handleSearch() {
        String query = txtSearch != null ? txtSearch.getText().trim() : "";
        if (!query.isEmpty()) {
            NotificationService.showInfo("🔍 Recherche", "Recherche en cours pour : \"" + query + "\"");
        }
        loadFilteredJobOffers();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}



