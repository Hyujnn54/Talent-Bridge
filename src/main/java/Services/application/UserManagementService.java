package Services.application;

import Utils.MyDatabase;
import Utils.UserContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing all users in the system.
 * NOTE: The 'users' table has NO 'role' column.
 * Role is derived by checking which child table (admin/candidate/recruiter)
 * the user belongs to via CASE expression.
 */
public class UserManagementService {

    public record UserRow(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String role,
        Boolean isActive,
        String additionalInfo
    ) {}

    private static Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    // Shared SELECT fragment — derives role from child tables
    private static final String SELECT_USERS =
        "SELECT u.id, u.first_name, u.last_name, u.email, u.phone, u.is_active, " +
        "  CASE " +
        "    WHEN a.id  IS NOT NULL THEN 'ADMIN' " +
        "    WHEN r.user_id IS NOT NULL THEN 'RECRUITER' " +
        "    WHEN c.user_id IS NOT NULL THEN 'CANDIDATE' " +
        "    ELSE 'UNKNOWN' " +
        "  END AS role, " +
        "  COALESCE(r.company_name, c.location, '') AS additional_info " +
        "FROM users u " +
        "LEFT JOIN admin     a ON u.id = a.id " +
        "LEFT JOIN recruiter r ON u.id = r.user_id " +
        "LEFT JOIN candidate c ON u.id = c.user_id ";

    private static UserRow mapRow(ResultSet rs) throws SQLException {
        return new UserRow(
            rs.getLong("id"),
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getString("email"),
            rs.getString("phone"),
            rs.getString("role"),
            rs.getBoolean("is_active"),
            rs.getString("additional_info")
        );
    }

    /**
     * Get all users in the system
     */
    public static List<UserRow> getAllUsers() {
        List<UserRow> users = new ArrayList<>();
        String sql = SELECT_USERS + "ORDER BY u.first_name, u.last_name";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) users.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("Error getting all users: " + e.getMessage());
            e.printStackTrace();
        }
        return users;
    }

    /**
     * Get users filtered by role
     */
    public static List<UserRow> getUsersByRole(UserContext.Role role) {
        List<UserRow> users = new ArrayList<>();
        // We filter AFTER deriving role via HAVING on the derived column
        String sql = SELECT_USERS +
            "HAVING role = ? " +
            "ORDER BY u.first_name, u.last_name";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, role.name());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) users.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("Error getting users by role: " + e.getMessage());
            e.printStackTrace();
        }
        return users;
    }

    /**
     * Get a specific user by ID
     */
    public static UserRow getUserById(Long userId) {
        String sql = SELECT_USERS + "WHERE u.id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("Error getting user by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Search users by email or name
     */
    public static List<UserRow> searchUsers(String query, UserContext.Role roleFilter) {
        List<UserRow> users = new ArrayList<>();
        String searchTerm = "%" + query + "%";

        String sql = SELECT_USERS +
            "WHERE (u.first_name LIKE ? OR u.last_name LIKE ? OR u.email LIKE ?) ";

        if (roleFilter != null) {
            // Use HAVING to filter on the derived role column
            sql += "HAVING role = ? ";
        }
        sql += "ORDER BY u.first_name, u.last_name";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, searchTerm);
            pstmt.setString(2, searchTerm);
            pstmt.setString(3, searchTerm);
            if (roleFilter != null) pstmt.setString(4, roleFilter.name());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) users.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("Error searching users: " + e.getMessage());
            e.printStackTrace();
        }
        return users;
    }

    /**
     * Create a new user
     */
    public static Long createUser(String firstName, String lastName, String email, String phone,
                                   String password, String role, Boolean isActive) {
        // users table has no role column — just insert the base user
        String sql = "INSERT INTO users (first_name, last_name, email, phone, password, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, firstName);
            pstmt.setString(2, lastName);
            pstmt.setString(3, email);
            pstmt.setString(4, phone);
            pstmt.setString(5, password != null ? password : "default123");
            pstmt.setBoolean(6, isActive != null ? isActive : true);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        Long userId = generatedKeys.getLong(1);
                        if ("RECRUITER".equals(role)) createRecruiterProfile(userId);
                        else if ("CANDIDATE".equals(role)) createCandidateProfile(userId);
                        return userId;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating user: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Update an existing user
     */
    public static boolean updateUser(Long userId, String firstName, String lastName, String email,
                                     String phone, String role, Boolean isActive) {
        // users table has no role column — only update the columns that exist
        String sql = "UPDATE users SET first_name=?, last_name=?, email=?, phone=?, is_active=? WHERE id=?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, firstName);
            pstmt.setString(2, lastName);
            pstmt.setString(3, email);
            pstmt.setString(4, phone);
            pstmt.setBoolean(5, isActive != null ? isActive : true);
            pstmt.setLong(6, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating user: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Delete a user (soft delete by setting is_active to false)
     */
    public static boolean deleteUser(Long userId) {
        String sql = "UPDATE users SET is_active = false WHERE id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
        }
        return false;
    }

    /**
     * Permanently delete a user (hard delete)
     */
    public static boolean permanentlyDeleteUser(Long userId) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error permanently deleting user: " + e.getMessage());
        }
        return false;
    }

    /**
     * Create recruiter profile for a new recruiter user
     */
    private static void createRecruiterProfile(Long userId) {
        String sql = "INSERT IGNORE INTO recruiter (user_id, company_name, company_location) VALUES (?, '', '')";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error creating recruiter profile: " + e.getMessage());
        }
    }

    /**
     * Create candidate profile for a new candidate user
     */
    private static void createCandidateProfile(Long userId) {
        String sql = "INSERT IGNORE INTO candidate (user_id, location, education_level, experience_years, cv_path) " +
                     "VALUES (?, '', '', 0, '')";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error creating candidate profile: " + e.getMessage());
        }
    }
}

