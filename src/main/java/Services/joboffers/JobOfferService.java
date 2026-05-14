package Services.joboffers;

import Models.joboffers.JobOffer;
import Models.joboffers.ContractType;
import Models.joboffers.Status;
import Utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobOfferService {

    public JobOfferService() {
        // No cached connection - always use conn() to get a live connection
    }

    /** Always returns a live connection, reconnecting if needed. */
    private Connection conn() {
        return MyDatabase.getInstance().getConnection();
    }

    private static Connection getStaticConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    // ==================== STATIC METHODS (used by JobOffersBrowse / Applications) ====================

    public static List<JobOffer> getAll() {
        List<JobOffer> list = new ArrayList<>();
        String sql = "SELECT * FROM job_offer WHERE status = 'OPEN' ORDER BY created_at DESC";
        try (Statement st = getStaticConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("Error retrieving job offers: " + e.getMessage());
        }
        return list;
    }

    public static JobOffer getById(Long id) {
        String sql = "SELECT * FROM job_offer WHERE id = ?";
        try (PreparedStatement ps = getStaticConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving job offer by id: " + e.getMessage());
        }
        return null;
    }

    public static void addJobOffer(JobOffer jobOffer) {
        String sql = "INSERT INTO job_offer (recruiter_id, title, description, location, contract_type, deadline, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, 'OPEN')";
        try (PreparedStatement ps = getStaticConnection().prepareStatement(sql)) {
            ps.setLong(1, jobOffer.getRecruiterId());
            ps.setString(2, jobOffer.getTitle());
            ps.setString(3, jobOffer.getDescription());
            ps.setString(4, jobOffer.getLocation());
            ps.setString(5, jobOffer.getContractType() != null ? jobOffer.getContractType().name() : "CDI");
            ps.setTimestamp(6, jobOffer.getDeadline() != null ? Timestamp.valueOf(jobOffer.getDeadline()) : null);
            ps.executeUpdate();
            System.out.println("Job offer created successfully");
        } catch (SQLException e) {
            System.err.println("Error adding job offer: " + e.getMessage());
            throw new RuntimeException("Failed to create job offer: " + e.getMessage(), e);
        }
    }

    public static List<JobOffer> getByRecruiterId(Long recruiterId) {
        List<JobOffer> list = new ArrayList<>();
        String sql = "SELECT * FROM job_offer WHERE recruiter_id = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = getStaticConnection().prepareStatement(sql)) {
            ps.setLong(1, recruiterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving recruiter offers: " + e.getMessage());
        }
        return list;
    }

    public static List<JobOffer> searchByTitle(String keyword) {
        List<JobOffer> list = new ArrayList<>();
        String sql = "SELECT * FROM job_offer WHERE status = 'OPEN' AND title LIKE ? ORDER BY created_at DESC";
        try (PreparedStatement ps = getStaticConnection().prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error searching job offers by title: " + e.getMessage());
        }
        return list;
    }

    public static List<JobOffer> searchByLocation(String keyword) {
        List<JobOffer> list = new ArrayList<>();
        String sql = "SELECT * FROM job_offer WHERE status = 'OPEN' AND location LIKE ? ORDER BY created_at DESC";
        try (PreparedStatement ps = getStaticConnection().prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error searching job offers by location: " + e.getMessage());
        }
        return list;
    }

    public static List<JobOffer> searchByContractType(String contractType) {
        List<JobOffer> list = new ArrayList<>();
        String sql = "SELECT * FROM job_offer WHERE status = 'OPEN' AND contract_type = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = getStaticConnection().prepareStatement(sql)) {
            ps.setString(1, contractType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error searching job offers by contract type: " + e.getMessage());
        }
        return list;
    }

    public static List<String> getOfferSkills(Long offerId) {
        List<String> skills = new ArrayList<>();
        String sql = "SELECT skill_name FROM offer_skill WHERE offer_id = ? ORDER BY level_required DESC, skill_name ASC";
        try (PreparedStatement ps = getStaticConnection().prepareStatement(sql)) {
            ps.setLong(1, offerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) skills.add(rs.getString("skill_name"));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving offer skills: " + e.getMessage());
        }
        return skills;
    }

    // ==================== INSTANCE METHODS (used by JobOffersAdmin / offer branch) ====================

    // CREATE
    public JobOffer createJobOffer(JobOffer jobOffer) throws SQLException {
        String query = "INSERT INTO job_offer (recruiter_id, title, description, location, latitude, longitude, contract_type, created_at, deadline, status) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, jobOffer.getRecruiterId());
            ps.setString(2, jobOffer.getTitle());
            ps.setString(3, jobOffer.getDescription());
            ps.setString(4, jobOffer.getLocation());
            if (jobOffer.getLatitude() != null) ps.setDouble(5, jobOffer.getLatitude());
            else ps.setNull(5, Types.DOUBLE);
            if (jobOffer.getLongitude() != null) ps.setDouble(6, jobOffer.getLongitude());
            else ps.setNull(6, Types.DOUBLE);
            ps.setString(7, jobOffer.getContractType().name());
            ps.setTimestamp(8, Timestamp.valueOf(jobOffer.getCreatedAt()));
            ps.setTimestamp(9, jobOffer.getDeadline() != null ? Timestamp.valueOf(jobOffer.getDeadline()) : null);
            ps.setString(10, jobOffer.getStatus().name());

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) jobOffer.setId(generatedKeys.getLong(1));
                }
            }
            return jobOffer;
        }
    }

    // READ - Get by ID (instance)
    public JobOffer getJobOfferById(Long id) throws SQLException {
        String query = "SELECT * FROM job_offer WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(query)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResultSetToJobOffer(rs);
        }
        return null;
    }

    // READ - Get All (instance)
    public List<JobOffer> getAllJobOffers() throws SQLException {
        List<JobOffer> jobOffers = new ArrayList<>();
        String query = "SELECT * FROM job_offer ORDER BY created_at DESC";
        try (Statement stmt = conn().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) jobOffers.add(mapResultSetToJobOffer(rs));
        }
        return jobOffers;
    }

    public List<JobOffer> getAllOpenJobOffers() throws SQLException {
        return getJobOffersByStatus(Status.OPEN);
    }

    public List<JobOffer> getJobOffersByStatus(Status status) throws SQLException {
        List<JobOffer> jobOffers = new ArrayList<>();
        String query = "SELECT * FROM job_offer WHERE status = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = conn().prepareStatement(query)) {
            ps.setString(1, status.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) jobOffers.add(mapResultSetToJobOffer(rs));
        }
        return jobOffers;
    }

    public List<JobOffer> getJobOffersByRecruiterId(Long recruiterId) throws SQLException {
        List<JobOffer> jobOffers = new ArrayList<>();
        String query = "SELECT * FROM job_offer WHERE recruiter_id = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = conn().prepareStatement(query)) {
            ps.setLong(1, recruiterId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) jobOffers.add(mapResultSetToJobOffer(rs));
        }
        return jobOffers;
    }

    public List<JobOffer> searchJobOffers(String searchTerm, String searchField) throws SQLException {
        List<JobOffer> jobOffers = new ArrayList<>();
        String query = "SELECT * FROM job_offer WHERE " + searchField + " LIKE ? ORDER BY created_at DESC";
        try (PreparedStatement ps = conn().prepareStatement(query)) {
            ps.setString(1, "%" + searchTerm + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) jobOffers.add(mapResultSetToJobOffer(rs));
        }
        return jobOffers;
    }

    // UPDATE (instance)
    public boolean updateJobOffer(JobOffer jobOffer) throws SQLException {
        String query = "UPDATE job_offer SET recruiter_id = ?, title = ?, description = ?, location = ?, " +
                      "latitude = ?, longitude = ?, contract_type = ?, deadline = ?, status = ? WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(query)) {
            ps.setLong(1, jobOffer.getRecruiterId());
            ps.setString(2, jobOffer.getTitle());
            ps.setString(3, jobOffer.getDescription());
            ps.setString(4, jobOffer.getLocation());
            if (jobOffer.getLatitude() != null) ps.setDouble(5, jobOffer.getLatitude());
            else ps.setNull(5, Types.DOUBLE);
            if (jobOffer.getLongitude() != null) ps.setDouble(6, jobOffer.getLongitude());
            else ps.setNull(6, Types.DOUBLE);
            ps.setString(7, jobOffer.getContractType().name());
            ps.setTimestamp(8, jobOffer.getDeadline() != null ? Timestamp.valueOf(jobOffer.getDeadline()) : null);
            ps.setString(9, jobOffer.getStatus().name());
            ps.setLong(10, jobOffer.getId());
            return ps.executeUpdate() > 0;
        }
    }

    // DELETE (instance)
    public boolean deleteJobOffer(Long id) throws SQLException {
        Connection connection = conn();
        try {
            connection.setAutoCommit(false);

            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM interview_feedback WHERE interview_id IN (" +
                            "SELECT id FROM interview WHERE application_id IN (" +
                            "SELECT id FROM job_application WHERE offer_id = ?) )")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM interview WHERE application_id IN (" +
                            "SELECT id FROM job_application WHERE offer_id = ?)")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM application_status_history WHERE application_id IN (" +
                            "SELECT id FROM job_application WHERE offer_id = ?)")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM job_application WHERE offer_id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM warning_correction WHERE job_offer_id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM job_offer_warning WHERE job_offer_id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM offer_skill WHERE offer_id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM job_offer WHERE id = ?")) {
                ps.setLong(1, id);
                int rows = ps.executeUpdate();
                connection.commit();
                return rows > 0;
            }
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public boolean updateJobOfferStatus(Long id, Status status) throws SQLException {
        String query = "UPDATE job_offer SET status = ? WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(query)) {
            ps.setString(1, status.name());
            ps.setLong(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ==================== FILTERING ====================

    public List<JobOffer> filterJobOffers(String location, ContractType contractType, Status status) throws SQLException {
        List<JobOffer> jobOffers = new ArrayList<>();
        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM job_offer WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (location != null && !location.trim().isEmpty()) {
            queryBuilder.append(" AND location LIKE ?");
            params.add("%" + location.trim() + "%");
        }
        if (contractType != null) {
            queryBuilder.append(" AND contract_type = ?");
            params.add(contractType.name());
        }
        if (status != null) {
            queryBuilder.append(" AND status = ?");
            params.add(status.name());
        }
        queryBuilder.append(" ORDER BY created_at DESC");

        try (PreparedStatement ps = conn().prepareStatement(queryBuilder.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) jobOffers.add(mapResultSetToJobOffer(rs));
        }
        return jobOffers;
    }

    public List<JobOffer> filterJobOffersByRecruiter(Long recruiterId, String location, ContractType contractType, Status status) throws SQLException {
        List<JobOffer> jobOffers = new ArrayList<>();
        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM job_offer WHERE recruiter_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(recruiterId);

        if (location != null && !location.trim().isEmpty()) {
            queryBuilder.append(" AND location LIKE ?");
            params.add("%" + location.trim() + "%");
        }
        if (contractType != null) {
            queryBuilder.append(" AND contract_type = ?");
            params.add(contractType.name());
        }
        if (status != null) {
            queryBuilder.append(" AND status = ?");
            params.add(status.name());
        }
        queryBuilder.append(" ORDER BY created_at DESC");

        try (PreparedStatement ps = conn().prepareStatement(queryBuilder.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) jobOffers.add(mapResultSetToJobOffer(rs));
        }
        return jobOffers;
    }

    public List<String> getAllLocations() throws SQLException {
        List<String> locations = new ArrayList<>();
        String query = "SELECT DISTINCT location FROM job_offer WHERE location IS NOT NULL AND location != '' ORDER BY location";
        try (Statement stmt = conn().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) locations.add(rs.getString("location"));
        }
        return locations;
    }

    // ==================== STATISTICS ====================

    public Map<ContractType, Integer> statsGlobal() throws SQLException {
        Map<ContractType, Integer> stats = new HashMap<>();
        for (ContractType type : ContractType.values()) stats.put(type, 0);
        String query = "SELECT contract_type, COUNT(*) as total FROM job_offer GROUP BY contract_type";
        try (Statement stmt = conn().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                try {
                    ContractType type = ContractType.valueOf(rs.getString("contract_type"));
                    stats.put(type, rs.getInt("total"));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return stats;
    }

    public Map<ContractType, Integer> statsByRecruiter(Long recruiterId) throws SQLException {
        Map<ContractType, Integer> stats = new HashMap<>();
        for (ContractType type : ContractType.values()) stats.put(type, 0);
        String query = "SELECT contract_type, COUNT(*) as total FROM job_offer WHERE recruiter_id = ? GROUP BY contract_type";
        try (PreparedStatement ps = conn().prepareStatement(query)) {
            ps.setLong(1, recruiterId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    ContractType type = ContractType.valueOf(rs.getString("contract_type"));
                    stats.put(type, rs.getInt("total"));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return stats;
    }

    public int getTotalOffresGlobal() throws SQLException {
        String query = "SELECT COUNT(*) as total FROM job_offer";
        try (Statement stmt = conn().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) return rs.getInt("total");
        }
        return 0;
    }

    public int getTotalOffresByRecruiter(Long recruiterId) throws SQLException {
        String query = "SELECT COUNT(*) as total FROM job_offer WHERE recruiter_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(query)) {
            ps.setLong(1, recruiterId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("total");
        }
        return 0;
    }

    public int getExpiredOffresGlobal() throws SQLException {
        String query = "SELECT COUNT(*) as total FROM job_offer WHERE deadline < NOW()";
        try (Statement stmt = conn().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) return rs.getInt("total");
        }
        return 0;
    }

    public int getExpiredOffresByRecruiter(Long recruiterId) throws SQLException {
        String query = "SELECT COUNT(*) as total FROM job_offer WHERE recruiter_id = ? AND deadline < NOW()";
        try (PreparedStatement ps = conn().prepareStatement(query)) {
            ps.setLong(1, recruiterId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("total");
        }
        return 0;
    }

    // ==================== HELPERS ====================

    private static JobOffer mapRow(ResultSet rs) throws SQLException {
        return mapResultSetToJobOffer(rs, true);
    }

    private JobOffer mapResultSetToJobOffer(ResultSet rs) throws SQLException {
        return mapResultSetToJobOffer(rs, true);
    }

    private static JobOffer mapResultSetToJobOffer(ResultSet rs, boolean ignored) throws SQLException {
        JobOffer jobOffer = new JobOffer();
        jobOffer.setId(rs.getLong("id"));
        jobOffer.setRecruiterId(rs.getLong("recruiter_id"));
        jobOffer.setTitle(rs.getString("title"));
        jobOffer.setDescription(rs.getString("description"));
        jobOffer.setLocation(rs.getString("location"));

        try {
            double lat = rs.getDouble("latitude");
            if (!rs.wasNull()) jobOffer.setLatitude(lat);
            double lon = rs.getDouble("longitude");
            if (!rs.wasNull()) jobOffer.setLongitude(lon);
        } catch (SQLException ignored2) {}

        try {
            String ctStr = rs.getString("contract_type");
            if (ctStr != null) jobOffer.setContractType(ContractType.valueOf(ctStr));
        } catch (IllegalArgumentException e) {
            jobOffer.setContractType(ContractType.CDI);
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) jobOffer.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp deadline = rs.getTimestamp("deadline");
        if (deadline != null) jobOffer.setDeadline(deadline.toLocalDateTime());

        try {
            String statusStr = rs.getString("status");
            if (statusStr != null) jobOffer.setStatus(Status.valueOf(statusStr));
        } catch (IllegalArgumentException e) {
            jobOffer.setStatus(Status.OPEN);
        }

        try {
            jobOffer.setFlagged(rs.getBoolean("is_flagged"));
            Timestamp flaggedAt = rs.getTimestamp("flagged_at");
            if (flaggedAt != null) jobOffer.setFlaggedAt(flaggedAt.toLocalDateTime());
        } catch (SQLException ignored2) {
            jobOffer.setFlagged(false);
        }

        return jobOffer;
    }
}
