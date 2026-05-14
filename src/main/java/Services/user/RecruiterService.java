package Services.user;

import Models.user.Recruiter;
import Utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RecruiterService {
    private Connection cnx() { return MyDatabase.getInstance().getConnection(); }
    private final UserService userService = new UserService();

    public void addRecruiter(Recruiter r) throws SQLException {
        String sql = "INSERT INTO recruiter (id, company_name, company_location) VALUES (?,?,?)";
        Connection c = cnx();
        try {
            c.setAutoCommit(false);
            if (r.getRoles() == null) r.setRoles("[\"ROLE_RECRUITER\"]");
            if (r.getDiscr() == null) r.setDiscr("recruiter");
            long userId = userService.addUser(r);
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, userId);
                ps.setString(2, r.getCompanyName());
                ps.setString(3, r.getCompanyLocation());
                ps.executeUpdate();
            }
            c.commit();
        } catch (SQLException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(true);
        }
    }

    public Recruiter getById(long id) throws SQLException {
        String sql = """
            SELECT u.id, u.email, u.password, u.first_name, u.last_name, u.phone,
                   r.company_name, r.company_location
            FROM users u
            JOIN recruiter r ON r.id = u.id
            WHERE u.id=?
        """;
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Recruiter r = new Recruiter(
                        rs.getString("email"), rs.getString("password"),
                        rs.getString("first_name"), rs.getString("last_name"),
                        rs.getString("phone"), rs.getString("company_name"),
                        rs.getString("company_location"));
                r.setId(rs.getLong("id"));
                return r;
            }
        }
    }

    public List<Recruiter> getAll() throws SQLException {
        String sql = """
            SELECT u.id, u.email, u.password, u.first_name, u.last_name, u.phone,
                   r.company_name, r.company_location
            FROM users u
            JOIN recruiter r ON r.id = u.id
            ORDER BY u.id DESC
        """;
        List<Recruiter> list = new ArrayList<>();
        try (PreparedStatement ps = cnx().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Recruiter r = new Recruiter(
                        rs.getString("email"), rs.getString("password"),
                        rs.getString("first_name"), rs.getString("last_name"),
                        rs.getString("phone"), rs.getString("company_name"),
                        rs.getString("company_location"));
                r.setId(rs.getLong("id"));
                list.add(r);
            }
        }
        return list;
    }

    public void updateRecruiter(Recruiter r) throws SQLException {
        Connection c = cnx();
        try {
            c.setAutoCommit(false);
            userService.updateUser(r);
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE recruiter SET company_name=?, company_location=? WHERE id=?")) {
                ps.setString(1, r.getCompanyName());
                ps.setString(2, r.getCompanyLocation());
                ps.setLong(3, r.getId());
                ps.executeUpdate();
            }
            c.commit();
        } catch (SQLException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(true);
        }
    }

    public void deleteRecruiter(long id) throws SQLException {
        userService.deleteUser(id);
    }
}