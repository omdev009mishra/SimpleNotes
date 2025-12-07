import java.util.Date;
//hey this is my project

public class Note {
    private int id;
    private String title;
    private String content;
    private Date lastModified;
    private String backgroundColor; // Hex color code
    private String fontFamily;

    public Note(int id, String title, String content, Date lastModified) {
        this(id, title, content, lastModified, "#121212", "Arial");
    }

    public Note(int id, String title, String content, Date lastModified, String backgroundColor, String fontFamily) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.lastModified = lastModified;
        this.backgroundColor = backgroundColor;
        this.fontFamily = fontFamily;
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

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    // This is used by the JList to display the note title.
    @Override
    public String toString() {
        return title;
    }
}