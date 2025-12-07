import java.util.List;

public interface NoteDAO {
    void addNote(Note note);
    Note getNote(int id);
    List<Note> getAllNotes();
    void updateNote(Note note);
    void deleteNote(int id);
    void setup() throws Exception;
}