import java.util.Date;
//hey this is my project

public class Note {
    private int id;
    private String title;
    private String content;
    private Date lastModified;

    public Note(int id, String title, String content, Date lastModified) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.lastModified = lastModified;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getLastModified() {
        return lastModified;
    }
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }
    // This is used by the JList to display the note title.
    @Override
    public String toString() {
        return title;
    }
}