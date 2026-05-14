package Services.user;

import Models.user.Admin;
import Utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminService {
    private Connection cnx() { return MyDatabase.getInstance().getConnection(); }
    private final UserService userService = new UserService();

    // CREATE
    public void addAdmin(Admin a) throws SQLException {
        String sql = "INSERT INTO admin (id, assigned_area) VALUES (?,?)";
        Connection conn = cnx();
        try {
            conn.setAutoCommit(false);
            if (a.getRoles() == null) a.setRoles("[\"ROLE_ADMIN\"]");
            if (a.getDiscr() == null) a.setDiscr("admin");
            long userId = userService.addUser(a);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, userId);
                ps.setString(2, a.getAssignedArea());
                ps.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // READ ONE
    public Admin getById(long id) throws SQLException {
        String sql = """
            SELECT u.id, u.email, u.password, u.first_name, u.last_name, u.phone,
                   a.assigned_area
            FROM users u
            JOIN admin a ON a.id = u.id
            WHERE u.id=?
        """;
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                Admin a = new Admin(
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("phone"),
                        rs.getString("assigned_area")
                );
                a.setId(rs.getLong("id"));
                return a;
            }
        }
    }

    // READ ALL
    public List<Admin> getAll() throws SQLException {
        String sql = """
            SELECT u.id, u.email, u.password, u.first_name, u.last_name, u.phone,
                   a.assigned_area
            FROM users u
            JOIN admin a ON a.id = u.id
            ORDER BY u.id DESC
        """;
        List<Admin> list = new ArrayList<>();
        try (PreparedStatement ps = cnx().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Admin a = new Admin(
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("phone"),
                        rs.getString("assigned_area")
                );
                a.setId(rs.getLong("id"));
                list.add(a);
            }
        }
        return list;
    }

    // UPDATE
    public void updateAdmin(Admin a) throws SQLException {
        Connection conn = cnx();
        try {
            conn.setAutoCommit(false);

            userService.updateUser(a);

            try (PreparedStatement ps = conn.prepareStatement("UPDATE admin SET assigned_area=? WHERE id=?")) {
                ps.setString(1, a.getAssignedArea());
                ps.setLong(2, a.getId());
                ps.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // DELETE
    public void deleteAdmin(long id) throws SQLException {
        userService.deleteUser(id); // cascade deletes admin row
    }


}