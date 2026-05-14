package Services.user;

import Models.user.User;
import Utils.MyDatabase;
import Utils.PasswordUtil;


import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {
    private Connection cnx() { return MyDatabase.getInstance().getConnection(); }

    // ===== CREATE =====
    public long addUser(User u) throws SQLException {
        String sql = """
            INSERT INTO users (email, password, first_name, last_name, phone, is_active, roles, discr)
            VALUES (?,?,?,?,?,1,?,?)
        """;

        String hashed = PasswordUtil.hash(u.getPassword());
        String roles = u.getRoles() != null ? u.getRoles() : "[\"ROLE_USER\"]";
        String discr = u.getDiscr() != null ? u.getDiscr() : "user";

        try (PreparedStatement ps = cnx().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getEmail());
            ps.setString(2, hashed);
            ps.setString(3, u.getFirstName());
            ps.setString(4, u.getLastName());
            ps.setString(5, u.getPhoneNumber());
            ps.setString(6, roles);
            ps.setString(7, discr);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("Failed to get generated user id.");
    }

    // ===== READ =====
    public User getById(long id) throws SQLException {
        String sql = "SELECT id, email, password, first_name, last_name, phone, roles, discr FROM users WHERE id=?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                User u = new User(
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("phone")
                );
                u.setId(rs.getLong("id"));
                u.setRoles(rs.getString("roles"));
                u.setDiscr(rs.getString("discr"));
                return u;
            }
        }
    }

    public List<User> getAll() throws SQLException {
        String sql = "SELECT id, email, password, first_name, last_name, phone, roles, discr FROM users ORDER BY id DESC";
        List<User> list = new ArrayList<>();
        try (PreparedStatement ps = cnx().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                User u = new User(
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("phone")
                );
                u.setId(rs.getLong("id"));
                u.setRoles(rs.getString("roles"));
                u.setDiscr(rs.getString("discr"));
                list.add(u);
            }
        }
        return list;
    }

    // ===== UPDATE =====
    public void updateUser(User u) throws SQLException {
        String sql = """
            UPDATE users
            SET email=?, password=?, first_name=?, last_name=?, phone=?
            WHERE id=?
        """;

        // ✅ prevent double-hash if password already stored-hash
        String passToStore = u.getPassword();
        if (!PasswordUtil.looksLikeBCryptHash(passToStore)) {
            passToStore = PasswordUtil.hash(passToStore);
        }

        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setString(1, u.getEmail());
            ps.setString(2, passToStore);
            ps.setString(3, u.getFirstName());
            ps.setString(4, u.getLastName());
            ps.setString(5, u.getPhoneNumber());
            ps.setLong(6, u.getId());
            ps.executeUpdate();
        }
    }

    // ===== DELETE =====
    public void deleteUser(long id) throws SQLException {
        try (PreparedStatement ps = cnx().prepareStatement("DELETE FROM users WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    // ===== VALIDATION =====
    public boolean emailExists(String email, Long excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE email=? " + (excludeId != null ? "AND id<>?" : "");
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setString(1, email);
            if (excludeId != null) ps.setLong(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }
    public void enableFaceLogin(long userId, String uuid) throws SQLException {
        // Try with face_enabled column first, fall back if column doesn't exist
        try {
            String sql = "UPDATE users SET face_person_id=?, face_enabled=1 WHERE id=?";
            try (PreparedStatement ps = cnx().prepareStatement(sql)) {
                ps.setString(1, uuid);
                ps.setLong(2, userId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            // face_enabled column may not exist — just update face_person_id
            String sql = "UPDATE users SET face_person_id=? WHERE id=?";
            try (PreparedStatement ps = cnx().prepareStatement(sql)) {
                ps.setString(1, uuid);
                ps.setLong(2, userId);
                ps.executeUpdate();
            }
        }
    }

    public void disableFaceLogin(long userId) throws SQLException {
        try {
            String sql = "UPDATE users SET face_person_id=NULL, face_enabled=0 WHERE id=?";
            try (PreparedStatement ps = cnx().prepareStatement(sql)) {
                ps.setLong(1, userId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            String sql = "UPDATE users SET face_person_id=NULL WHERE id=?";
            try (PreparedStatement ps = cnx().prepareStatement(sql)) {
                ps.setLong(1, userId);
                ps.executeUpdate();
            }
        }
    }

    public String getFacePersonId(long userId) throws SQLException {
        String sql = "SELECT face_person_id FROM users WHERE id=?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                String v = rs.getString("face_person_id");
                if (v == null) return null;
                v = v.trim();
                return v.isEmpty() ? null : v;
            }
        }
    }

    public User getByFacePersonId(String facePersonId) throws SQLException {
        String sql = "SELECT id, email, password, first_name, last_name, phone, roles, discr " +
                "FROM users WHERE face_person_id=? AND is_active=1";

        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setString(1, facePersonId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                User u = new User(
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("phone")
                );

                u.setId(rs.getLong("id"));
                u.setRoles(rs.getString("roles"));
                u.setDiscr(rs.getString("discr"));
                return u;
            }
        }
    }
}