package Controllers.user;

import Models.user.Candidate;
import Models.user.Recruiter;
import Models.user.User;
import Services.user.ProfileService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import Utils.InputValidator;
import Utils.Session;
import Services.user.UserService;
import Services.user.LuxandFaceService;
import javafx.stage.FileChooser;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;


public class ProfileController {

    @FXML private TextField tfFirstName;
    @FXML private TextField tfEmail;
    @FXML private TextField tfPhone;
    @FXML private PasswordField pfPassword;
    @FXML private Label lblStatus;

    // recruiter
    @FXML private VBox recruiterBlock;
    @FXML private TextField tfCompanyName;
    @FXML private TextField tfCompanyLocation;

    // candidate
    @FXML private VBox candidateBlock;
    @FXML private TextField tfCandidateLocation;
    @FXML private TextField tfEducationLevel;
    @FXML private TextField tfExperienceYears;
    @FXML private TextField tfCvPath;
    @FXML private Button btnEnableFace;
    @FXML private Button btnDisableFace;
    @FXML private Button btnBrowseCv;
    @FXML private Button btnViewCv;
    @FXML private Button btnRemoveCv;
    @FXML private Label lblCvStatus;

    private static final String CV_UPLOAD_DIR = "uploads/cvs/";
    private File selectedCvFile = null; // Holds the new file picked by the user (not yet saved)

    private final ProfileService service = new ProfileService();

    @FXML
    public void initialize() {
        // Ensure CV upload directory exists
        try {
            Files.createDirectories(Paths.get(CV_UPLOAD_DIR));
        } catch (Exception e) {
            System.err.println("Could not create CV upload dir: " + e.getMessage());
        }
        loadMyProfile();
    }

    private void loadMyProfile() {
        try {
            Long userIdObj = Session.getUserId();
            if (userIdObj == null) {
                lblStatus.setText("Not logged in.");
                return;
            }
            long userId = userIdObj;

            User u = Session.getCurrentUser();
            boolean isRecruiter = u instanceof Recruiter;
            boolean isCandidate = u instanceof Candidate;

            ProfileService.UserProfile p = service.getUserProfile(userId);

            tfFirstName.setText(p.firstName());
            tfEmail.setText(p.email());
            tfPhone.setText(p.phone() == null ? "" : p.phone());

            recruiterBlock.setVisible(isRecruiter);
            recruiterBlock.setManaged(isRecruiter);

            candidateBlock.setVisible(isCandidate);
            candidateBlock.setManaged(isCandidate);

            String faceId = new UserService().getFacePersonId(userId);
            boolean enabled = (faceId != null && !faceId.isBlank());
            btnEnableFace.setVisible(!enabled);
            btnEnableFace.setManaged(!enabled);
            btnDisableFace.setVisible(enabled);
            btnDisableFace.setManaged(enabled);

            if (isRecruiter) {
                ProfileService.RecruiterInfo r = service.getRecruiterInfo(userId);
                tfCompanyName.setText(r.companyName());
                tfCompanyLocation.setText(r.companyLocation());
            }

            if (isCandidate) {
                ProfileService.CandidateInfo c = service.getCandidateInfo(userId);
                tfCandidateLocation.setText(c.location());
                tfEducationLevel.setText(c.educationLevel());
                tfExperienceYears.setText(c.experienceYears() == null ? "" : String.valueOf(c.experienceYears()));

                String cvPath = c.cvPath();
                selectedCvFile = null;
                if (cvPath != null && !cvPath.isBlank()) {
                    tfCvPath.setText(extractFileName(cvPath));
                    updateCvButtons(true);
                    lblCvStatus.setText("✅ CV uploaded");
                } else {
                    tfCvPath.setText("");
                    updateCvButtons(false);
                    lblCvStatus.setText("No CV uploaded yet");
                }
            }

            lblStatus.setText("Loaded ✅");

        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("Load failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleSave() {
        try {
            Long userIdObj = Session.getUserId();
            if (userIdObj == null) {
                showError("Not logged in.");
                return;
            }
            long userId = userIdObj;

            User u = Session.getCurrentUser();
            boolean isRecruiter = u instanceof Recruiter;
            boolean isCandidate = u instanceof Candidate;

            // ===== Validations =====
            String err;

            err = InputValidator.validateName(tfFirstName.getText().trim(), "Full name");
            if (err != null) { showError(err); return; }

            err = InputValidator.validatePhone8(tfPhone.getText().trim());
            if (err != null) { showError(err); return; }

            String newPass = pfPassword.getText();
            if (newPass != null && !newPass.isBlank()) {
                err = InputValidator.validateStrongPassword(newPass);
                if (err != null) { showError(err); return; }
            } else {
                newPass = null; // keep old password
            }

            // ===== Update users table =====
            service.updateUserCore(
                    userId,
                    tfFirstName.getText().trim(),
                    tfPhone.getText().trim(),
                    newPass
            );

            // ===== Type-specific update =====
            if (isRecruiter) {
                if (tfCompanyName.getText().trim().isEmpty()) { showError("Company name is required."); return; }

                service.updateRecruiterInfo(
                        userId,
                        tfCompanyName.getText().trim(),
                        tfCompanyLocation.getText().trim()
                );
            }

            if (isCandidate) {
                Integer years = null;
                String y = tfExperienceYears.getText().trim();
                if (!y.isEmpty()) {
                    try { years = Integer.parseInt(y); }
                    catch (NumberFormatException ex) { showError("Experience years must be a number."); return; }
                }

                // Handle CV file upload
                String cvPath = null;
                ProfileService.CandidateInfo existing = service.getCandidateInfo(userId);
                String existingCvPath = existing.cvPath();

                if (selectedCvFile != null) {
                    // A new file was selected — copy it to uploads/cvs/
                    try {
                        String fileName = UUID.randomUUID() + "_" + selectedCvFile.getName();
                        Path target = Paths.get(CV_UPLOAD_DIR, fileName);
                        Files.copy(selectedCvFile.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
                        cvPath = target.toString();
                        System.out.println("CV uploaded to: " + cvPath);

                        // Delete old CV file if it existed
                        if (existingCvPath != null && !existingCvPath.isBlank()) {
                            try { Files.deleteIfExists(Paths.get(existingCvPath)); } catch (Exception ignored) {}
                        }
                    } catch (Exception ex) {
                        showError("Failed to upload CV: " + ex.getMessage());
                        return;
                    }
                } else if (tfCvPath.getText() != null && !tfCvPath.getText().isBlank()) {
                    // Keep existing CV (no new file selected, but text field still shows file name)
                    cvPath = existingCvPath;
                }
                // If both are null/empty, cvPath stays null => CV removed
                // Delete old file from disk if CV was removed
                if (cvPath == null && existingCvPath != null && !existingCvPath.isBlank()) {
                    try { Files.deleteIfExists(Paths.get(existingCvPath)); } catch (Exception ignored) {}
                }

                service.updateCandidateInfo(
                        userId,
                        tfCandidateLocation.getText().trim(),
                        tfEducationLevel.getText().trim(),
                        years,
                        cvPath
                );

                // Update UI after save
                selectedCvFile = null;
                if (cvPath != null && !cvPath.isBlank()) {
                    tfCvPath.setText(extractFileName(cvPath));
                    updateCvButtons(true);
                    lblCvStatus.setText("✅ CV uploaded");
                } else {
                    tfCvPath.setText("");
                    updateCvButtons(false);
                    lblCvStatus.setText("No CV uploaded yet");
                }
            }

            pfPassword.clear();
            lblStatus.setText("Saved ✅");

        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("Save failed: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }

    @FXML
    private void handleEnableFaceLogin() {
        try {
            User me = Session.getCurrentUser();
            if (me == null) { showError("Not logged in."); return; }

            FileChooser fc = new FileChooser();
            fc.setTitle("Select 1–3 clear face photos (hold Ctrl for multiple)");
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
            );

            java.util.List<File> files = fc.showOpenMultipleDialog(tfEmail.getScene().getWindow());
            if (files == null || files.isEmpty()) return;
            if (files.size() > 3) files = files.subList(0, 3);

            UserService us = new UserService();
            LuxandFaceService lux = new LuxandFaceService();

            // Delete existing Luxand person (DB uuid) to avoid stale UUIDs
            String existingUuid = us.getFacePersonId(me.getId());
            System.out.println("existingUuid from DB = [" + existingUuid + "]");
            if (existingUuid != null && !existingUuid.isBlank()) {
                try { lux.deletePerson(existingUuid.trim()); System.out.println("Deleted old person: " + existingUuid); }
                catch (Exception ex) { System.err.println("Delete old person failed (ignored): " + ex.getMessage()); }
            }

            // Create fresh person with first photo
            byte[] firstBytes = Files.readAllBytes(files.get(0).toPath());
            String uuid = lux.addPerson(me.getEmail(), firstBytes, files.get(0).getName());
            System.out.println("Created Luxand person UUID=" + uuid);

            // Add remaining photos
            for (int i = 1; i < files.size(); i++) {
                byte[] b = Files.readAllBytes(files.get(i).toPath());
                lux.addFace(uuid, b, files.get(i).getName());
                System.out.println("Added extra face " + i + " UUID=" + uuid);
            }

            // Save UUID to DB
            us.enableFaceLogin(me.getId(), uuid);
            showAlert("Success", "Face login enabled ✅\n" + files.size() + " photo(s) registered.\nUUID: " + uuid);

            btnEnableFace.setVisible(false);
            btnEnableFace.setManaged(false);
            btnDisableFace.setVisible(true);
            btnDisableFace.setManaged(true);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Enable face failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleDisableFaceLogin() {
        try {
            User me = Session.getCurrentUser();
            if (me == null) return;

            UserService us = new UserService();
            LuxandFaceService lux = new LuxandFaceService();

            String existingUuid = us.getFacePersonId(me.getId());
            if (existingUuid != null && !existingUuid.isBlank()) {
                try { lux.deletePerson(existingUuid.trim()); System.out.println("Deleted Luxand person: " + existingUuid); }
                catch (Exception ex) { System.err.println("Delete failed (ignored): " + ex.getMessage()); }
            }

            us.disableFaceLogin(me.getId());
            showAlert("Done", "Face login disabled ✅");

            btnEnableFace.setVisible(true);
            btnEnableFace.setManaged(true);
            btnDisableFace.setVisible(false);
            btnDisableFace.setManaged(false);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Disable face failed: " + e.getMessage());
        }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    // ═══════════════════ CV Upload Methods ═══════════════════

    @FXML
    private void handleBrowseCv() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select your CV (PDF)");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                new FileChooser.ExtensionFilter("Word Documents", "*.doc", "*.docx")
        );
        File file = fc.showOpenDialog(tfCvPath.getScene().getWindow());
        if (file != null) {
            selectedCvFile = file;
            tfCvPath.setText(file.getName());
            lblCvStatus.setText("📎 New file selected — click Save to upload");
            updateCvButtons(true);
        }
    }

    @FXML
    private void handleViewCv() {
        try {
            Long userId = Session.getUserId();
            if (userId == null) return;

            // If a new file is selected but not yet saved, open the local file
            if (selectedCvFile != null && selectedCvFile.exists()) {
                java.awt.Desktop.getDesktop().open(selectedCvFile);
                return;
            }

            // Otherwise open the already-uploaded CV
            ProfileService.CandidateInfo c = service.getCandidateInfo(userId);
            String cvPath = c.cvPath();
            if (cvPath == null || cvPath.isBlank()) {
                showAlert("No CV", "No CV has been uploaded yet.");
                return;
            }
            File cvFile = new File(cvPath);
            if (!cvFile.exists()) {
                showError("CV file not found at: " + cvPath);
                return;
            }
            java.awt.Desktop.getDesktop().open(cvFile);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not open CV: " + e.getMessage());
        }
    }

    @FXML
    private void handleRemoveCv() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to remove your CV?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Remove CV");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                selectedCvFile = null;
                tfCvPath.setText("");
                lblCvStatus.setText("CV will be removed when you save");
                updateCvButtons(false);
            }
        });
    }

    private void updateCvButtons(boolean hasCv) {
        if (btnViewCv != null) {
            btnViewCv.setVisible(hasCv);
            btnViewCv.setManaged(hasCv);
        }
        if (btnRemoveCv != null) {
            btnRemoveCv.setVisible(hasCv);
            btnRemoveCv.setManaged(hasCv);
        }
    }

    private String extractFileName(String path) {
        if (path == null || path.isBlank()) return "";
        // Remove UUID prefix if present (format: uuid_originalname.pdf)
        String name = Paths.get(path).getFileName().toString();
        int underscoreIdx = name.indexOf('_');
        if (underscoreIdx > 0 && underscoreIdx < 40) {
            // Check if the part before _ looks like a UUID
            String prefix = name.substring(0, underscoreIdx);
            if (prefix.length() >= 32) {
                return name.substring(underscoreIdx + 1);
            }
        }
        return name;
    }


}