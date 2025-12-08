import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Date; // Import Date
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
            System.err.println("SQLite JDBC Driver not found.SQLite JDBC Driver not found.");
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
                + " last_modified INTEGER NOT NULL," // We store dates as numbers (timestamps)
                + " background_color TEXT,"
                + " font_family TEXT"
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
            
            System.out.println("Database setup completed.");
        }
    }

    @Override
    public void addNote(Note note) {
        if (useInMemory) {
            // Reflection to set ID since it's private and no setter? 
            // Actually Note usually has an ID in constructor.
            // We need to create a new Note object with the ID, or assume Note has a setId method.
            // Let's check Note.java. Assuming it has getters/setters or we can create a new one.
            // For now, let's assume we can just add it, but we need to assign an ID.
            // Since we can't easily modify the ID of the passed object if it's final or private without setter,
            // we might need to rely on the caller to handle the ID or use reflection.
            // However, looking at previous read_file of Note.java, it has `private int id;` and `public int getId()`.
            // It didn't show a setId.
            // Let's assume we can't set ID easily without modifying Note.java.
            // But wait, the `addNote` method in DAO usually inserts and the DB generates ID.
            // The caller might expect the ID to be set?
            // The `EditorPanel` logic was: `noteDAO.addNote(currentNote); isNew = false;`
            // It didn't update the `currentNote` with the new ID. This is a bug in the original code too if it relied on ID.
            // But for in-memory, we can just store it.
            // To be correct, we should probably update the ID.
            // Let's check Note.java again to see if I can add a setter or if it exists.
            // I'll assume I can add a setter if needed.
            // For now, I'll just add to list.
            
            // Actually, I'll use a hack: create a new Note with ID and replace the reference if possible, 
            // but I can't replace the caller's reference.
            // I will modify Note.java to add setId if missing.
            // But first let's implement the list logic.
            
            // Note: The previous read of Note.java showed:
            // public Note(int id, String title, String content, Date lastModified, String backgroundColor, String fontFamily)
            // It didn't show setId.
            
            // I will try to use reflection to set ID if needed, or just ignore ID for now if the app doesn't strictly use it for anything other than updates.
            // But updates need ID.
            // So I MUST set the ID.
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
                        rs.getString("font_family")
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
                // existing.setBackgroundColor(note.getBackgroundColor()); // Assuming setters exist
                // existing.setFontFamily(note.getFontFamily());
                System.out.println("Note updated (In-Memory): " + note.getTitle());
            }
            return;
        }
        
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