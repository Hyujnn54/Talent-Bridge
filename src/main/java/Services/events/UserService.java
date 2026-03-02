package Services.events;

import Models.events.RoleEnum;
import Models.events.EventUser;
import Utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    public static class UserInfo {
        public final String firstName;
        public final String lastName;
        public final String email;
        public final String phone;
        public final String educationLevel;
        public final Integer experienceYears;

        public UserInfo(String firstName, String lastName, String email, String phone,
                        String educationLevel, Integer experienceYears) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.phone = phone;
            this.educationLevel = educationLevel;
            this.experienceYears = experienceYears;
        }

        public String firstName() { return firstName; }
        public String lastName() { return lastName; }
        public String email() { return email; }
        public String phone() { return phone; }
        public String educationLevel() { return educationLevel; }
        public Integer experienceYears() { return experienceYears; }
    }

    private Connection conn() { return MyDatabase.getInstance().getConnection(); }

    public UserService() {}

    // ── Events-module methods ──────────────────────────────────────────────

    public void add(EventUser user) throws SQLException {
        String query = "INSERT INTO users (email, password, first_name, last_name, phone, role, is_active, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = conn().prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, user.getEmail());
        ps.setString(2, user.getPassword());
        ps.setString(3, user.getFirstName());
        ps.setString(4, user.getLastName());
        ps.setString(5, user.getPhone());
        ps.setString(6, user.getRole() != null ? user.getRole().toString() : "CANDIDATE");
        ps.setBoolean(7, user.isActive());
        ps.setTimestamp(8, Timestamp.valueOf(java.time.LocalDateTime.now()));
        ps.executeUpdate();
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) { user.setId(rs.getLong(1)); }
    }

    public EventUser login(String email, String password) throws SQLException {
        String query = "SELECT * FROM users WHERE email = ? AND password = ? AND is_active = 1";
        PreparedStatement ps = conn().prepareStatement(query);
        ps.setString(1, email);
        ps.setString(2, password);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) { return mapUser(rs); }
        return null;
    }

    public EventUser getById(long id) throws SQLException {
        String query = "SELECT * FROM users WHERE id = ?";
        PreparedStatement ps = conn().prepareStatement(query);
        ps.setLong(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) { return mapUser(rs); }
        return null;
    }

    public List<EventUser> getAll() throws SQLException {
        List<EventUser> users = new ArrayList<>();
        Statement st = conn().createStatement();
        ResultSet rs = st.executeQuery("SELECT * FROM users");
        while (rs.next()) { users.add(mapUser(rs)); }
        return users;
    }

    public void update(EventUser user) throws SQLException {
        String query = "UPDATE users SET email=?, first_name=?, last_name=?, phone=?, role=?, is_active=? WHERE id=?";
        PreparedStatement ps = conn().prepareStatement(query);
        ps.setString(1, user.getEmail());
        ps.setString(2, user.getFirstName());
        ps.setString(3, user.getLastName());
        ps.setString(4, user.getPhone());
        ps.setString(5, user.getRole() != null ? user.getRole().toString() : "CANDIDATE");
        ps.setBoolean(6, user.isActive());
        ps.setLong(7, user.getId());
        ps.executeUpdate();
    }

    private EventUser mapUser(ResultSet rs) throws SQLException {
        EventUser user = new EventUser();
        user.setId(rs.getLong("id"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setPhone(rs.getString("phone"));
        try { user.setRole(RoleEnum.valueOf(rs.getString("role"))); } catch (Exception ignored) {}
        user.setActive(rs.getBoolean("is_active"));
        try { user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime()); } catch (Exception ignored) {}
        return user;
    }

    // ── Static helper methods ─────────────────────────────────────────────

    public static UserInfo getUserInfo(Long userId) {
        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            // users table has no 'role' column — role is determined by child tables
            // (candidate, recruiter, admin). We LEFT JOIN candidate for extra fields.
            String query = "SELECT u.first_name, u.last_name, u.email, u.phone, " +
                          "c.education_level, c.experience_years " +
                          "FROM users u LEFT JOIN candidate c ON u.id = c.user_id " +
                          "WHERE u.id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new UserInfo(
                    rs.getString("first_name"), rs.getString("last_name"),
                    rs.getString("email"), rs.getString("phone"),
                    rs.getString("education_level"),
                    rs.getObject("experience_years") != null ? rs.getInt("experience_years") : null);
            }
        } catch (SQLException e) { System.err.println("Error fetching user info: " + e.getMessage()); }
        return null;
    }

    public static String getRecruiterCompanyName(Long recruiterId) {
        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT company_name FROM recruiter WHERE user_id = ?");
            stmt.setLong(1, recruiterId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) { return rs.getString("company_name"); }
        } catch (SQLException e) { System.err.println("Error fetching recruiter info: " + e.getMessage()); }
        return null;
    }

    public static java.util.List<String> getCandidateSkills(Long candidateId) {
        java.util.List<String> skills = new java.util.ArrayList<>();
        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT skill_name, level FROM candidate_skill WHERE candidate_id = ? ORDER BY level DESC, skill_name ASC");
            stmt.setLong(1, candidateId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                skills.add(rs.getString("skill_name") + " (" + rs.getString("level") + ")");
            }
        } catch (SQLException e) { System.err.println("Error fetching candidate skills: " + e.getMessage()); }
        return skills;
    }
}
