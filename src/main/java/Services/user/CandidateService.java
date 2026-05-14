package Services.user;

import Models.user.Candidate;
import Utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CandidateService {
    private Connection cnx() { return MyDatabase.getInstance().getConnection(); }
    private final UserService userService = new UserService();

    public void addCandidate(Candidate c) throws SQLException {
        String sql = """
            INSERT INTO candidate (id, location, education_level, experience_years, cv_path)
            VALUES (?,?,?,?,?)
        """;
        Connection conn = cnx();
        try {
            conn.setAutoCommit(false);
            if (c.getRoles() == null) c.setRoles("[\"ROLE_CANDIDATE\"]");
            if (c.getDiscr() == null) c.setDiscr("candidate");
            long userId = userService.addUser(c);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, userId);
                ps.setString(2, c.getLocation());
                ps.setString(3, c.getEducationLevel());
                if (c.getExperienceYears() == null) ps.setNull(4, Types.INTEGER);
                else ps.setInt(4, c.getExperienceYears());
                ps.setString(5, c.getCvPath());
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

    public Candidate getById(long id) throws SQLException {
        String sql = """
            SELECT u.id, u.email, u.password, u.first_name, u.last_name, u.phone,
                   c.location, c.education_level, c.experience_years, c.cv_path
            FROM users u
            JOIN candidate c ON c.id = u.id
            WHERE u.id=?
        """;
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Candidate c = new Candidate(
                        rs.getString("email"), rs.getString("password"),
                        rs.getString("first_name"), rs.getString("last_name"),
                        rs.getString("phone"), rs.getString("location"),
                        rs.getString("education_level"),
                        (Integer) rs.getObject("experience_years"), rs.getString("cv_path"));
                c.setId(rs.getLong("id"));
                return c;
            }
        }
    }

    public List<Candidate> getAll() throws SQLException {
        String sql = """
            SELECT u.id, u.email, u.password, u.first_name, u.last_name, u.phone,
                   c.location, c.education_level, c.experience_years, c.cv_path
            FROM users u
            JOIN candidate c ON c.id = u.id
            ORDER BY u.id DESC
        """;
        List<Candidate> list = new ArrayList<>();
        try (PreparedStatement ps = cnx().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Candidate c = new Candidate(
                        rs.getString("email"), rs.getString("password"),
                        rs.getString("first_name"), rs.getString("last_name"),
                        rs.getString("phone"), rs.getString("location"),
                        rs.getString("education_level"),
                        (Integer) rs.getObject("experience_years"), rs.getString("cv_path"));
                c.setId(rs.getLong("id"));
                list.add(c);
            }
        }
        return list;
    }

    public void updateCandidate(Candidate c) throws SQLException {
        Connection conn = cnx();
        try {
            conn.setAutoCommit(false);
            userService.updateUser(c);
            String sql = """
                UPDATE candidate
                SET location=?, education_level=?, experience_years=?, cv_path=?
                WHERE id=?
            """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, c.getLocation());
                ps.setString(2, c.getEducationLevel());
                if (c.getExperienceYears() == null) ps.setNull(3, Types.INTEGER);
                else ps.setInt(3, c.getExperienceYears());
                ps.setString(4, c.getCvPath());
                ps.setLong(5, c.getId());
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

    public void deleteCandidate(long id) throws SQLException {
        userService.deleteUser(id);
    }
}