import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Date; // Import Date

public class DatabaseManager implements NoteDAO {

    // 1. Load the SQLite Driver
    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC Driver not found. Add the library to your project!");
            e.printStackTrace();
        }
    }

    // 2. Use the SQLite Connection String
    private static final String DB_URL = "jdbc:sqlite:notes.db";

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    @Override
    public void setup() throws SQLException {
        // SQLite syntax to create the table
        String sql = "CREATE TABLE IF NOT EXISTS notes ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " title TEXT NOT NULL,"
                + " content TEXT,"
                + " last_modified INTEGER NOT NULL," // We store dates as numbers (timestamps)
                + " background_color TEXT DEFAULT '#121212',"
                + " font_family TEXT DEFAULT 'Arial'"
                + ");";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            
            // Try to add columns if they don't exist (for existing DBs)
            try {
                stmt.execute("ALTER TABLE notes ADD COLUMN background_color TEXT DEFAULT '#121212'");
            } catch (SQLException ignored) {}
            try {
                stmt.execute("ALTER TABLE notes ADD COLUMN font_family TEXT DEFAULT 'Arial'");
            } catch (SQLException ignored) {}
            
            System.out.println("Database setup completed.");
        }
    }

    @Override
    public void addNote(Note note) {
        String sql = "INSERT INTO notes(title, content, last_modified, background_color, font_family) VALUES(?, ?, ?, ?, ?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, note.getTitle());
            pstmt.setString(2, note.getContent());
            // Convert Java Date to database number (long)
            pstmt.setLong(3, note.getLastModified().getTime());
            pstmt.setString(4, note.getBackgroundColor());
            pstmt.setString(5, note.getFontFamily());

            pstmt.executeUpdate();
            System.out.println("Note added: " + note.getTitle());

        } catch (SQLException e) {
            System.out.println("Error adding note: " + e.getMessage());
        }
    }

    @Override
    public Note getNote(int id) {
        String sql = "SELECT id, title, content, last_modified, background_color, font_family FROM notes WHERE id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Note(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("content"),
                        new Date(rs.getLong("last_modified")),
                        rs.getString("background_color"),
                        rs.getString("font_family")
                );
            }
        } catch (SQLException e) {
            System.out.println("Error getting note: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Note> getAllNotes() {
        List<Note> notes = new ArrayList<>();
        String sql = "SELECT * FROM notes ORDER BY last_modified DESC";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String bg = rs.getString("background_color");
                String font = rs.getString("font_family");
                if (bg == null) bg = "#121212";
                if (font == null) font = "Arial";

                Note note = new Note(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("content"),
                        new Date(rs.getLong("last_modified")),
                        bg,
                        font
                );
                notes.add(note);
            }
        } catch (SQLException e) {
            System.out.println("Error loading notes: " + e.getMessage());
        }
        return notes;
    }

    @Override
    public void updateNote(Note note) {
        String sql = "UPDATE notes SET title = ?, content = ?, last_modified = ?, background_color = ?, font_family = ? WHERE id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, note.getTitle());
            pstmt.setString(2, note.getContent());
            pstmt.setLong(3, new Date().getTime()); // Update time to now
            pstmt.setString(4, note.getBackgroundColor());
            pstmt.setString(5, note.getFontFamily());
            pstmt.setInt(6, note.getId());

            pstmt.executeUpdate();
            System.out.println("Note updated: " + note.getTitle());

        } catch (SQLException e) {
            System.out.println("Error updating note: " + e.getMessage());
        }
    }

    @Override
    public void deleteNote(int id) {
        String sql = "DELETE FROM notes WHERE id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            System.out.println("Note deleted with ID: " + id);

        } catch (SQLException e) {
            System.out.println("Error deleting note: " + e.getMessage());
        }
    }
}