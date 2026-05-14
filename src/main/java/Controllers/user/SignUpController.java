package Controllers.user;

import Models.user.Candidate;
import Models.user.Recruiter;
import Services.user.CandidateService;
import Services.user.RecruiterService;
import Services.user.UserService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import Controllers.LocationPickerController;


public class SignUpController {

    @FXML private TextField txtFirstName;
    @FXML private TextField txtLastName;    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;

    @FXML private ListView<String> lvLocationSuggestions;

    // ✅ CHANGED: no Role enum
    @FXML private ComboBox<String> cbRole;

    @FXML private CheckBox cbTerms;

    // blocks
    @FXML private VBox recruiterBlock;
    @FXML private VBox candidateBlock;

    // recruiter fields
    @FXML private TextField txtCompanyName;
    @FXML private TextField txtCompanyLocation;

    // candidate fields
    @FXML private TextField txtCandidateLocation;
    @FXML private TextField txtEducationLevel;
    @FXML private TextField txtExperienceYears;
    @FXML private TextField txtCvPath;

    private final UserService userService = new UserService();
    private final RecruiterService recruiterService = new RecruiterService();
    private final CandidateService candidateService = new CandidateService();

    @FXML
    public void initialize() {
        javafx.animation.PauseTransition debounce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(350));

        txtCompanyLocation.textProperty().addListener((obs, oldV, newV) -> {
            debounce.stop();
            if (newV == null || newV.trim().length() < 3) {
                lvLocationSuggestions.setVisible(false);
                lvLocationSuggestions.setManaged(false);
                lvLocationSuggestions.getItems().clear();
                return;
            }

            debounce.setOnFinished(ev -> {
                new Thread(() -> {
                    try {
                        var list = searchLocations(newV);

                        javafx.application.Platform.runLater(() -> {
                            lvLocationSuggestions.getItems().setAll(list);
                            boolean show = !list.isEmpty();
                            lvLocationSuggestions.setVisible(show);
                            lvLocationSuggestions.setManaged(show);
                        });

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();
            });
            debounce.playFromStart();
        });

        lvLocationSuggestions.setOnMouseClicked(e -> {
            String selected = lvLocationSuggestions.getSelectionModel().getSelectedItem();
            if (selected != null) {
                txtCompanyLocation.setText(selected);
                lvLocationSuggestions.setVisible(false);
                lvLocationSuggestions.setManaged(false);
            }
        });
        // ✅ only these two types in signup
        cbRole.setItems(FXCollections.observableArrayList("CANDIDATE", "RECRUITER"));

        cbRole.valueProperty().addListener((obs, oldV, newV) -> {
            boolean isRecruiter = "RECRUITER".equals(newV);
            boolean isCandidate = "CANDIDATE".equals(newV);

            recruiterBlock.setVisible(isRecruiter);
            recruiterBlock.setManaged(isRecruiter);

            candidateBlock.setVisible(isCandidate);
            candidateBlock.setManaged(isCandidate);
        });

        // hide blocks initially
        recruiterBlock.setVisible(false);
        recruiterBlock.setManaged(false);
        candidateBlock.setVisible(false);
        candidateBlock.setManaged(false);
    }

    @FXML
    private void handleSignUp() {

        // Terms
        if (!cbTerms.isSelected()) {
            showAlert(Alert.AlertType.ERROR, "Error", "You must accept Terms and Conditions.");
            return;
        }

        // Type
        String type = cbRole.getValue();
        if (type == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please select account type.");
            return;
        }

// First name
        String firstName = txtFirstName.getText() == null ? "" : txtFirstName.getText().trim();
        if (firstName.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "First name is required.");
            return;
        }
        String err = Utils.InputValidator.validateName(firstName, "First name");
        if (err != null) { showAlert(Alert.AlertType.ERROR, "Error", err); return; }

// Last name
        String lastName = txtLastName.getText() == null ? "" : txtLastName.getText().trim();
        if (lastName.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Last name is required.");
            return;
        }
        err = Utils.InputValidator.validateName(lastName, "Last name");
        if (err != null) { showAlert(Alert.AlertType.ERROR, "Error", err); return; }

        // Email
        String email = txtEmail.getText() == null ? "" : txtEmail.getText().trim();
        err = Utils.InputValidator.validateEmail(email);
        if (err != null) { showAlert(Alert.AlertType.ERROR, "Error", err); return; }

        // Email unique (DB)
        try {
            if (userService.emailExists(email, null)) {
                showAlert(Alert.AlertType.ERROR, "Error", "Email already exists.");
                return;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Database error while checking email.");
            return;
        }

        // Phone (8 digits)
        String phone = txtPhone.getText() == null ? "" : txtPhone.getText().trim();
        err = Utils.InputValidator.validatePhone8(phone);
        if (err != null) { showAlert(Alert.AlertType.ERROR, "Error", err); return; }

        // Password
        String pass = txtPassword.getText();
        String confirm = txtConfirmPassword.getText();

        err = Utils.InputValidator.validateStrongPassword(pass);
        if (err != null) { showAlert(Alert.AlertType.ERROR, "Error", err); return; }

        if (!pass.equals(confirm)) {
            showAlert(Alert.AlertType.ERROR, "Error", "Passwords do not match.");
            return;
        }

        // Insert user (NO role column in DB)
        try {
            String rolesJson = "[\"ROLE_" + type + "\"]";
            String discr = type.toLowerCase();

            if ("RECRUITER".equals(type)) {
                String companyName = txtCompanyName.getText() == null ? "" : txtCompanyName.getText().trim();
                if (companyName.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Company name is required for recruiter.");
                    return;
                }

                Recruiter r = new Recruiter(
                        email, pass, firstName, lastName, phone,
                        companyName,
                        txtCompanyLocation.getText() == null ? "" : txtCompanyLocation.getText().trim()
                );
                r.setRoles(rolesJson);
                r.setDiscr(discr);

                recruiterService.addRecruiter(r);

            } else { // CANDIDATE
                Integer years = null;
                String yearsText = txtExperienceYears.getText() == null ? "" : txtExperienceYears.getText().trim();
                if (!yearsText.isEmpty()) {
                    try { years = Integer.parseInt(yearsText); }
                    catch (NumberFormatException nfe) {
                        showAlert(Alert.AlertType.ERROR, "Error", "Experience years must be a number.");
                        return;
                    }
                }

                Candidate c = new Candidate(
                        email, pass, firstName, lastName, phone,
                        txtCandidateLocation.getText() == null ? "" : txtCandidateLocation.getText().trim(),
                        txtEducationLevel.getText() == null ? "" : txtEducationLevel.getText().trim(),
                        years,
                        txtCvPath.getText() == null ? "" : txtCvPath.getText().trim()
                );
                c.setRoles(rolesJson);
                c.setDiscr(discr);

                candidateService.addCandidate(c);
            }

            showAlert(Alert.AlertType.INFORMATION, "Success", "Account created successfully ✅");

            // Send welcome email in background
            final String finalEmail = email;
            final String finalFirst = firstName;
            new Thread(() -> {
                try {
                    new Services.user.EmailService().sendWelcome(finalEmail, finalFirst);
                } catch (Exception ex) {
                    System.err.println("[SignUp] Welcome email failed: " + ex.getMessage());
                }
            }, "welcome-email").start();

            handleShowLogin();
            clearForm();

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Database error: " + ex.getMessage());
        }
    }

    @FXML
    private void handleShowLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/user/Login.fxml"));
            Stage stage = (Stage) txtEmail.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearForm() {
        txtFirstName.clear();
        txtLastName.clear();
        txtEmail.clear();
        txtPhone.clear();
        txtPassword.clear();
        txtConfirmPassword.clear();
        cbRole.setValue(null);
        cbTerms.setSelected(false);

        txtCompanyName.clear();
        txtCompanyLocation.clear();
        txtCandidateLocation.clear();
        txtEducationLevel.clear();
        txtExperienceYears.clear();
        txtCvPath.clear();

        recruiterBlock.setVisible(false);
        recruiterBlock.setManaged(false);
        candidateBlock.setVisible(false);
        candidateBlock.setManaged(false);
    }

    private java.util.List<String> searchLocations(String query) throws Exception {
        if (query == null || query.trim().length() < 3) {
            return java.util.Collections.emptyList();
        }

        String url = "https://nominatim.openstreetmap.org/search?format=json&limit=5&q="
                + java.net.URLEncoder.encode(query.trim(), java.nio.charset.StandardCharsets.UTF_8);

        java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .header("User-Agent", "TalentBridgeApp/1.0 (email: talentbridge.app@gmail.com)")
                .header("Accept", "application/json")
                .GET()
                .build();

        java.net.http.HttpResponse<String> resp = java.net.http.HttpClient.newHttpClient()
                .send(req, java.net.http.HttpResponse.BodyHandlers.ofString());

        // ✅ DEBUG if not ok
        if (resp.statusCode() != 200) {
            System.out.println("Nominatim status = " + resp.statusCode());
            System.out.println("Body (first 200 chars) = " +
                    resp.body().substring(0, Math.min(200, resp.body().length())));
            return java.util.Collections.emptyList();
        }

        // ✅ Sometimes they send HTML even with 200, so guard it
        String body = resp.body();
        if (body != null && body.trim().startsWith("<")) {
            System.out.println("Nominatim returned HTML instead of JSON:");
            System.out.println(body.substring(0, Math.min(200, body.length())));
            return java.util.Collections.emptyList();
        }

        org.json.JSONArray arr = new org.json.JSONArray(body);

        java.util.List<String> results = new java.util.ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            org.json.JSONObject n = arr.optJSONObject(i);
            if (n != null && n.has("display_name")) results.add(n.getString("display_name"));
        }
        return results;
    }

    private String reverseGeocode(double lat, double lng) throws Exception {
        String url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=" + lat + "&lon=" + lng;

        java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .header("User-Agent", "TalentBridgeApp/1.0 (email: your_email@gmail.com)")
                .header("Accept", "application/json")
                .GET()
                .build();

        java.net.http.HttpResponse<String> resp = java.net.http.HttpClient.newHttpClient()
                .send(req, java.net.http.HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200) return lat + ", " + lng;

        org.json.JSONObject json = new org.json.JSONObject(resp.body());
        if (json.has("display_name")) return json.getString("display_name");

        return lat + ", " + lng;
    }

    @FXML
    private void handlePickRecruiterLocation() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/user/LocationPicker.fxml"));
            javafx.scene.Parent root = loader.load();

            LocationPickerController ctrl = loader.getController();

            // ✅ set callback so Select returns coords
            ctrl.setOnPicked((lat, lng) -> {
                txtCompanyLocation.setText(String.format("%.6f, %.6f", lat, lng));
            });

            Stage popup = new Stage();
            popup.setTitle("Pick location");
            popup.initOwner(txtEmail.getScene().getWindow());
            popup.initModality(javafx.stage.Modality.WINDOW_MODAL);

            Scene scene = new Scene(root, 900, 650);
            popup.setScene(scene);
            popup.show();

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to open map: " + ex.getMessage()).showAndWait();
        }
    }
}