package Services.user;

import Utils.MyDatabase;
import Utils.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthService {
    private Connection cnx() { return MyDatabase.getInstance().getConnection(); }

    public Models.user.User login(String email, String password) throws SQLException {
        String sql = """
                SELECT id, email, password, first_name, last_name, phone, roles, discr
                FROM users
                WHERE email=? AND is_active=1
                """;
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                long   id         = rs.getLong("id");
                String storedHash = rs.getString("password");
                String firstName  = rs.getString("first_name");
                String lastName   = rs.getString("last_name");
                String phone      = rs.getString("phone");
                String roles      = rs.getString("roles");
                String discr      = rs.getString("discr");

                if (!PasswordUtil.verify(password, storedHash)) return null;

                // 1) ADMIN?
                Models.user.Admin a = findAdmin(id, email, storedHash, firstName, lastName, phone);
                if (a != null) {
                    a.setRoles(roles);
                    a.setDiscr(discr);
                    return a;
                }

                // 2) RECRUITER?
                Models.user.Recruiter r = findRecruiter(id, email, storedHash, firstName, lastName, phone);
                if (r != null) {
                    r.setRoles(roles);
                    r.setDiscr(discr);
                    return r;
                }

                // 3) CANDIDATE?
                Models.user.Candidate c = findCandidate(id, email, storedHash, firstName, lastName, phone);
                if (c != null) {
                    c.setRoles(roles);
                    c.setDiscr(discr);
                    return c;
                }

                // 4) fallback base user
                Models.user.User u = new Models.user.User(email, storedHash, firstName, lastName, phone);
                u.setId(id);
                u.setRoles(roles);
                u.setDiscr(discr);
                return u;
            }
        }
    }

    private Models.user.Admin findAdmin(long id, String email, String password,
                                        String firstName, String lastName, String phone) throws SQLException {
        String sql = "SELECT assigned_area FROM admin WHERE id=?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Models.user.Admin a = new Models.user.Admin(email, password, firstName, lastName, phone,
                        rs.getString("assigned_area"));
                a.setId(id);
                return a;
            }
        }
    }

    private Models.user.Recruiter findRecruiter(long id, String email, String password,
                                                 String firstName, String lastName, String phone) throws SQLException {
        String sql = "SELECT company_name, company_location FROM recruiter WHERE id=?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Models.user.Recruiter r = new Models.user.Recruiter(
                        email, password, firstName, lastName, phone,
                        rs.getString("company_name"),
                        rs.getString("company_location"));
                r.setId(id);
                return r;
            }
        }
    }

    private Models.user.Candidate findCandidate(long id, String email, String password,
                                                 String firstName, String lastName, String phone) throws SQLException {
        String sql = "SELECT location, education_level, experience_years, cv_path FROM candidate WHERE id=?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Models.user.Candidate c = new Models.user.Candidate(
                        email, password, firstName, lastName, phone,
                        rs.getString("location"),
                        rs.getString("education_level"),
                        rs.getInt("experience_years"),
                        rs.getString("cv_path"));
                c.setId(id);
                return c;
            }
        }
    }

    public Models.user.User loginWithFacePersonId(String personId) throws SQLException {
        // Always get a fresh connection — this may be called from a background thread
        // after the main connection has been recycled
        Connection c = MyDatabase.getInstance().getConnection();

        String sql = "SELECT id, email, password, first_name, last_name, phone, roles, discr " +
                "FROM users WHERE face_person_id=? AND is_active=1";

        System.out.println("[AuthService] loginWithFacePersonId uuid=" + personId);

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, personId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("[AuthService] No user found for uuid=" + personId);
                    return null;
                }
                long   id         = rs.getLong("id");
                String storedHash = rs.getString("password");
                String firstName  = rs.getString("first_name");
                String lastName   = rs.getString("last_name");
                String phone      = rs.getString("phone");
                String email      = rs.getString("email");
                String roles      = rs.getString("roles");
                String discr      = rs.getString("discr");

                Models.user.Admin a = findAdmin(id, email, storedHash, firstName, lastName, phone);
                if (a != null) { a.setRoles(roles); a.setDiscr(discr); return a; }
                Models.user.Recruiter r = findRecruiter(id, email, storedHash, firstName, lastName, phone);
                if (r != null) { r.setRoles(roles); r.setDiscr(discr); return r; }
                Models.user.Candidate cc = findCandidate(id, email, storedHash, firstName, lastName, phone);
                if (cc != null) { cc.setRoles(roles); cc.setDiscr(discr); return cc; }

                Models.user.User u = new Models.user.User(email, storedHash, firstName, lastName, phone);
                u.setId(id);
                u.setRoles(roles);
                u.setDiscr(discr);
                return u;
            }
        }
    }
}