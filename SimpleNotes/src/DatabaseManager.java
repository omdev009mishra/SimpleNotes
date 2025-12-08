import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Collections;
import java.util.Comparator;

public class DatabaseManager implements NoteDAO {

    private static boolean useInMemory = false;
    private static List<Note> memoryNotes = new ArrayList<>();
    private static int memoryIdCounter = 1;

    // 1. Load the SQLite Driver
    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC Driver not found. Using In-Memory mode.");
            useInMemory = true;
        }
    }

    // 2. Use the SQLite Connection String
    private static final String DB_URL = "jdbc:sqlite:notes.db";

    private Connection connect() throws SQLException {
        if (useInMemory) throw new SQLException("Running in In-Memory mode");
        return DriverManager.getConnection(DB_URL);
    }

    @Override
    public void setup() throws SQLException {
        if (useInMemory) {
            System.out.println("Database setup skipped (In-Memory mode).");
            return;
        }
        // SQLite syntax to create the table
        String sql = "CREATE TABLE IF NOT EXISTS notes ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " title TEXT NOT NULL,"
                + " content TEXT,"
                + " last_modified INTEGER NOT NULL,"
                + " background_color TEXT,"
                + " font_family TEXT,"
                + " category TEXT"
                + ");";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            
            // Check if columns exist (migration for old db)
            try {
                stmt.execute("ALTER TABLE notes ADD COLUMN background_color TEXT");
            } catch (SQLException ignored) {}
            try {
                stmt.execute("ALTER TABLE notes ADD COLUMN font_family TEXT");
            } catch (SQLException ignored) {}
            try {
                stmt.execute("ALTER TABLE notes ADD COLUMN category TEXT");
            } catch (SQLException ignored) {}
            
            System.out.println("Database setup completed.");
        }
    }

    @Override
    public void addNote(Note note) {
        if (useInMemory) {
            try {
                java.lang.reflect.Field idField = Note.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.setInt(note, memoryIdCounter++);
            } catch (Exception e) {
                e.printStackTrace();
            }
            memoryNotes.add(note);
            System.out.println("Note added (In-Memory): " + note.getTitle());
            return;
        }
        
        String sql = "INSERT INTO notes(title, content, last_modified, background_color, font_family, category) VALUES(?, ?, ?, ?, ?, ?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, note.getTitle());
            pstmt.setString(2, note.getContent());
            pstmt.setLong(3, note.getLastModified().getTime());
            pstmt.setString(4, note.getBackgroundColor());
            pstmt.setString(5, note.getFontFamily());
            pstmt.setString(6, note.getCategory());

            pstmt.executeUpdate();
            System.out.println("Note added: " + note.getTitle());

        } catch (SQLException e) {
            System.out.println("Error adding note: " + e.getMessage());
        }
    }

    @Override
    public Note getNote(int id) {
        if (useInMemory) {
            return memoryNotes.stream().filter(n -> n.getId() == id).findFirst().orElse(null);
        }
        
        String sql = "SELECT * FROM notes WHERE id = ?";

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
                        rs.getString("font_family"),
                        rs.getString("category")
                );
            }
        } catch (SQLException e) {
            System.out.println("Error getting note: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Note> getAllNotes() {
        if (useInMemory) {
            List<Note> sorted = new ArrayList<>(memoryNotes);
            sorted.sort((n1, n2) -> n2.getLastModified().compareTo(n1.getLastModified()));
            return sorted;
        }
        
        List<Note> notes = new ArrayList<>();
        String sql = "SELECT * FROM notes ORDER BY last_modified DESC";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Note note = new Note(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("content"),
                        new Date(rs.getLong("last_modified")),
                        rs.getString("background_color"),
                        rs.getString("font_family"),
                        rs.getString("category")
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
        if (useInMemory) {
            Note existing = getNote(note.getId());
            if (existing != null) {
                existing.setTitle(note.getTitle());
                existing.setContent(note.getContent());
                existing.setLastModified(new Date());
                existing.setCategory(note.getCategory());
                System.out.println("Note updated (In-Memory): " + note.getTitle());
            }
            return;
        }
        
        String sql = "UPDATE notes SET title = ?, content = ?, last_modified = ?, background_color = ?, font_family = ?, category = ? WHERE id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, note.getTitle());
            pstmt.setString(2, note.getContent());
            pstmt.setLong(3, new Date().getTime());
            pstmt.setString(4, note.getBackgroundColor());
            pstmt.setString(5, note.getFontFamily());
            pstmt.setString(6, note.getCategory());
            pstmt.setInt(7, note.getId());

            pstmt.executeUpdate();
            System.out.println("Note updated: " + note.getTitle());

        } catch (SQLException e) {
            System.out.println("Error updating note: " + e.getMessage());
        }
    }

    @Override
    public void deleteNote(int id) {
        if (useInMemory) {
            memoryNotes.removeIf(n -> n.getId() == id);
            System.out.println("Note deleted (In-Memory): " + id);
            return;
        }
        
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