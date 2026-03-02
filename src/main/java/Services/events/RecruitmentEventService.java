package Services.events;

import Models.events.RecruitmentEvent;
import Models.events.EventRecruiter;
import Utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RecruitmentEventService {

    private Connection conn() { return MyDatabase.getInstance().getConnection(); }

    public RecruitmentEventService() {}

    public void add(RecruitmentEvent event) throws SQLException {
        String query = "INSERT INTO recruitment_event (recruiter_id, title, description, event_type, location, event_date, capacity, meet_link, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = conn().prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        ps.setLong(1, event.getRecruiterId());
        ps.setString(2, event.getTitle());
        ps.setString(3, event.getDescription());
        ps.setString(4, event.getEventType());
        ps.setString(5, event.getLocation());
        ps.setTimestamp(6, Timestamp.valueOf(event.getEventDate()));
        ps.setInt(7, event.getCapacity());
        ps.setString(8, event.getMeetLink());
        ps.setTimestamp(9, Timestamp.valueOf(java.time.LocalDateTime.now()));
        ps.executeUpdate();
        ResultSet keys = ps.getGeneratedKeys();
        if (keys.next()) event.setId(keys.getLong(1));
    }

    public void update(RecruitmentEvent event) throws SQLException {
        String query = "UPDATE recruitment_event SET title=?, description=?, event_type=?, location=?, event_date=?, capacity=?, meet_link=? WHERE id=?";
        PreparedStatement ps = conn().prepareStatement(query);
        ps.setString(1, event.getTitle());
        ps.setString(2, event.getDescription());
        ps.setString(3, event.getEventType());
        ps.setString(4, event.getLocation());
        ps.setTimestamp(5, Timestamp.valueOf(event.getEventDate()));
        ps.setInt(6, event.getCapacity());
        ps.setString(7, event.getMeetLink());
        ps.setLong(8, event.getId());
        ps.executeUpdate();
    }

    public void delete(long id) throws SQLException {
        String query = "DELETE FROM recruitment_event WHERE id=?";
        PreparedStatement ps = conn().prepareStatement(query);
        ps.setLong(1, id);
        ps.executeUpdate();
    }

    public RecruitmentEvent getById(long id) throws SQLException {
        String query = "SELECT * FROM recruitment_event WHERE id = ?";
        PreparedStatement ps = conn().prepareStatement(query);
        ps.setLong(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            RecruitmentEvent event = new RecruitmentEvent();
            event.setId(rs.getLong("id"));
            event.setRecruiterId(rs.getLong("recruiter_id"));
            event.setTitle(rs.getString("title"));
            event.setDescription(rs.getString("description"));
            event.setEventType(rs.getString("event_type"));
            event.setLocation(rs.getString("location"));
            event.setEventDate(rs.getTimestamp("event_date").toLocalDateTime());
            event.setCapacity(rs.getInt("capacity"));
            try { event.setMeetLink(rs.getString("meet_link")); } catch (SQLException ignored) {}
            event.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            return event;
        }
        return null;
    }

    public List<RecruitmentEvent> getAll() throws SQLException {
        List<RecruitmentEvent> events = new ArrayList<>();
        String query = "SELECT e.*, r.company_name, r.company_location, r.company_description " +
                       "FROM recruitment_event e LEFT JOIN recruiter r ON e.recruiter_id = r.id";
        Statement st = conn().createStatement();
        ResultSet rs = st.executeQuery(query);
        while (rs.next()) {
            RecruitmentEvent event = mapRow(rs);
            EventRecruiter recruiter = new EventRecruiter();
            recruiter.setId(rs.getLong("recruiter_id"));
            recruiter.setCompanyName(rs.getString("company_name"));
            recruiter.setCompanyLocation(rs.getString("company_location"));
            event.setRecruiter(recruiter);
            events.add(event);
        }
        return events;
    }

    public List<RecruitmentEvent> getByRecruiter(long recruiterId) throws SQLException {
        List<RecruitmentEvent> events = new ArrayList<>();
        String query = "SELECT * FROM recruitment_event WHERE recruiter_id = ?";
        PreparedStatement ps = conn().prepareStatement(query);
        ps.setLong(1, recruiterId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) events.add(mapRow(rs));
        return events;
    }

    /**
     * An event is considered popular if:
     *  - It has >= 50% fill rate, OR
     *  - It has the highest number of active registrations among all events (with at least 1)
     */
    public boolean isEventPopular(long eventId) throws SQLException {
        EventRegistrationService regService = new EventRegistrationService();
        int activeCount = regService.getActiveRegistrationCount(eventId);

        if (activeCount <= 0) return false;

        // Check fill rate
        String query = "SELECT capacity FROM recruitment_event WHERE id = ?";
        PreparedStatement ps = conn().prepareStatement(query);
        ps.setLong(1, eventId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            int capacity = rs.getInt("capacity");
            if (capacity > 0) {
                double fillRate = ((double) activeCount / capacity) * 100;
                if (fillRate >= 50) return true;
            }
        }

        // Mark the event with the most active registrations as popular
        String maxQuery = "SELECT event_id, COUNT(*) as cnt FROM event_registration " +
                "WHERE attendance_status NOT IN ('CANCELLED','REJECTED','ABSENT') " +
                "GROUP BY event_id ORDER BY cnt DESC LIMIT 1";
        Statement st = conn().createStatement();
        ResultSet maxRs = st.executeQuery(maxQuery);
        if (maxRs.next()) {
            long topEventId = maxRs.getLong("event_id");
            return topEventId == eventId;
        }
        return false;
    }

    private RecruitmentEvent mapRow(ResultSet rs) throws SQLException {
        RecruitmentEvent event = new RecruitmentEvent();
        event.setId(rs.getLong("id"));
        event.setRecruiterId(rs.getLong("recruiter_id"));
        event.setTitle(rs.getString("title"));
        event.setDescription(rs.getString("description"));
        event.setEventType(rs.getString("event_type"));
        event.setLocation(rs.getString("location"));
        event.setEventDate(rs.getTimestamp("event_date").toLocalDateTime());
        event.setCapacity(rs.getInt("capacity"));
        try { event.setMeetLink(rs.getString("meet_link")); } catch (SQLException ignored) {}
        try { event.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime()); } catch (SQLException ignored) {}
        return event;
    }
}
