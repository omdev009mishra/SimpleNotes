import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.Point;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.swing.event.ListSelectionEvent;
import javax.swing.JOptionPane;
import java.io.*;
import javax.imageio.ImageIO;
import java.awt.geom.RoundRectangle2D;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Main application class for Simple Notes.
 * Provides a GUI for creating, editing, and organizing notes,
 * as well as a drawing feature.
 */
public class NotesApp extends JFrame {

    private NoteDAO noteDAO;
    private NoteListPanel noteListPanel;
    private EditorPanel editorPanel;
    private SidebarPanel sidebar;
    private boolean isDarkMode = true;
    private String currentCategory = "Personal";

    public NotesApp() {
        setTitle("NoteSphere");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        noteDAO = new DatabaseManager();
        try {
            noteDAO.setup();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to set up database.", "Database Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        
        // 1. Sidebar (Left)
        sidebar = new SidebarPanel();
        
        // 2. Note List (Middle)
        noteListPanel = new NoteListPanel();
        
        // 3. Editor (Right)
        editorPanel = new EditorPanel();
        
        // Wire up selection
        noteListPanel.setSelectionListener(note -> editorPanel.setNote(note));
        
        // Create Split Panes
        // Inner split: List vs Editor
        JSplitPane innerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, noteListPanel, editorPanel);
        innerSplit.setDividerLocation(350); // Width of note list
        innerSplit.setDividerSize(1);
        innerSplit.setBorder(null);
        
        // Outer split: Sidebar vs Inner
        JSplitPane outerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, innerSplit);
        outerSplit.setDividerLocation(250); // Width of sidebar
        outerSplit.setDividerSize(1);
        outerSplit.setBorder(null);
        
        add(outerSplit, BorderLayout.CENTER);
        
        // Initial load
        noteListPanel.refreshNotes();
    }

    // --- Sidebar Panel ---
    private class SidebarPanel extends JPanel {
        private List<JPanel> categoryItems = new ArrayList<>();
        private List<String> categories = new ArrayList<>();

        private JLabel appTitle;
        private JLabel settings;
        private JPanel content;

        public SidebarPanel() {
            setLayout(new BorderLayout());
            
            content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setOpaque(false);
            content.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
            
            // App Title
            appTitle = new JLabel(" NoteSphere");
            appTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
            appTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(appTitle);
            content.add(Box.createVerticalStrut(30));
            
            // Notebooks Section
            addSectionTitle(content, "Notebooks");
            addCategoryItem(content, "Personal");
            addCategoryItem(content, "Work");
            addCategoryItem(content, "Ideas");
            content.add(Box.createVerticalStrut(20));
            
            // Recents Section
            addSectionTitle(content, "Recents");
            // addSidebarItem(content, "Diaries", false); // Placeholder
            // addSidebarItem(content, "TodoList", false); // Placeholder
            
            add(content, BorderLayout.NORTH);
            
            // Settings at bottom
            settings = new JLabel(" Settings");
            settings.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            settings.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            settings.setCursor(new Cursor(Cursor.HAND_CURSOR));
            settings.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showSettingsDialog();
                }
                @Override
                public void mouseEntered(MouseEvent e) {
                    settings.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    settings.setForeground(Color.GRAY);
                }
            });
            add(settings, BorderLayout.SOUTH);
            
            updateTheme(isDarkMode);
        }
        
        private void addSectionTitle(JPanel p, String text) {
            JLabel l = new JLabel(text);
            l.setFont(new Font("Segoe UI", Font.BOLD, 12));
            l.setForeground(Color.GRAY);
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(l);
            p.add(Box.createVerticalStrut(10));
        }
        
        private void addCategoryItem(JPanel p, String text) {
            JPanel item = new JPanel(new BorderLayout());
            item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
            boolean isSelected = text.equals(currentCategory);
            item.setOpaque(isSelected);
            
            // Initial color set by updateTheme or here if needed, but updateTheme is called at end of constructor
            // We can set a default based on isDarkMode
            if (isSelected) item.setBackground(isDarkMode ? new Color(60, 60, 60) : new Color(220, 220, 220));
            
            JLabel l = new JLabel(" " + text); // Icon placeholder
            l.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            l.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
            l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
            
            item.add(l, BorderLayout.CENTER);
            item.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            item.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectCategory(text);
                }
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!text.equals(currentCategory)) {
                        item.setOpaque(true);
                        item.setBackground(isDarkMode ? new Color(50, 50, 50) : new Color(230, 230, 230));
                        item.repaint();
                    }
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    if (!text.equals(currentCategory)) {
                        item.setOpaque(false);
                        item.repaint();
                    }
                }
            });

            categoryItems.add(item);
            categories.add(text);
            p.add(item);
            p.add(Box.createVerticalStrut(5));
        }

        private void selectCategory(String category) {
            currentCategory = category;
            for (int i = 0; i < categories.size(); i++) {
                JPanel item = categoryItems.get(i);
                String cat = categories.get(i);
                boolean isSelected = cat.equals(category);
                item.setOpaque(isSelected);
                if (isSelected) item.setBackground(isDarkMode ? new Color(60, 60, 60) : new Color(220, 220, 220));
                item.repaint();
            }
            noteListPanel.setCategory(category);
            noteListPanel.refreshNotes();
        }
        
        public void updateTheme(boolean dark) {
            Color bg = dark ? new Color(45, 45, 45) : new Color(240, 240, 240);
            Color fg = dark ? Color.WHITE : Color.BLACK;
            Color border = dark ? new Color(30, 30, 30) : new Color(200, 200, 200);
            
            setBackground(bg);
            setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, border));
            
            appTitle.setForeground(fg);
            settings.setForeground(Color.GRAY);
            
            // Update section titles
            for (Component c : content.getComponents()) {
                if (c instanceof JLabel && (((JLabel)c).getText().equals("Notebooks") || ((JLabel)c).getText().equals("Recents"))) {
                     ((JLabel)c).setForeground(Color.GRAY);
                }
            }

            // Update categories
            for (int i = 0; i < categoryItems.size(); i++) {
                JPanel item = categoryItems.get(i);
                String cat = categories.get(i);
                boolean isSelected = cat.equals(currentCategory);
                
                JLabel l = (JLabel) item.getComponent(0);
                l.setForeground(fg);
                
                if (isSelected) {
                    item.setBackground(dark ? new Color(60, 60, 60) : new Color(220, 220, 220));
                } else {
                    item.setBackground(bg);
                }
            }
            repaint();
        }
    }

    // --- Note List Panel ---
    private class NoteListPanel extends JPanel {
        private JPanel listContainer;
        private List<Note> notes;
        private JTextField searchField;
        private java.util.function.Consumer<Note> selectionListener;
        private String categoryFilter = "Personal";
        private JLabel titleLabel;

        public NoteListPanel() {
            setLayout(new BorderLayout());
            setBackground(new Color(30, 30, 30)); // Middle Pane Color
            setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(20, 20, 20)));

            // Header
            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(new Color(30, 30, 30));
            header.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
            
            titleLabel = new JLabel("Personal");
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
            titleLabel.setForeground(Color.WHITE);
            header.add(titleLabel, BorderLayout.WEST);
            
            // Add Button
            JButton addBtn = new JButton("+");
            addBtn.setForeground(Color.WHITE);
            addBtn.setBorderPainted(false);
            addBtn.setContentAreaFilled(false);
            addBtn.setFont(new Font("Segoe UI", Font.BOLD, 20));
            addBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            addBtn.addActionListener(e -> showNewNoteDialog());
            header.add(addBtn, BorderLayout.EAST);
            
            add(header, BorderLayout.NORTH);
            
            // Search & List
            JPanel centerPanel = new JPanel(new BorderLayout());
            centerPanel.setBackground(new Color(30, 30, 30));
            
            // Search Bar
            searchField = new JTextField();
            searchField.setBackground(new Color(45, 45, 45));
            searchField.setForeground(Color.WHITE);
            searchField.setCaretColor(Color.WHITE);
            searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(45, 45, 45), 5),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
            ));
            searchField.putClientProperty("JTextField.placeholderText", "Search");
            searchField.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { filterNotes(); }
                public void removeUpdate(DocumentEvent e) { filterNotes(); }
                public void changedUpdate(DocumentEvent e) { filterNotes(); }
            });
            
            JPanel searchWrapper = new JPanel(new BorderLayout());
            searchWrapper.setBackground(new Color(30, 30, 30));
            searchWrapper.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 20));
            searchWrapper.add(searchField, BorderLayout.CENTER);
            
            centerPanel.add(searchWrapper, BorderLayout.NORTH);
            
            // List
            listContainer = new JPanel();
            listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
            listContainer.setBackground(new Color(30, 30, 30));
            
            JScrollPane scroll = new JScrollPane(listContainer);
            scroll.setBorder(null);
            scroll.getVerticalScrollBar().setUI(new ModernScrollBarUI(isDarkMode));
            
            centerPanel.add(scroll, BorderLayout.CENTER);
            add(centerPanel, BorderLayout.CENTER);
        }
        
        public void setSelectionListener(java.util.function.Consumer<Note> listener) {
            this.selectionListener = listener;
        }
        
        public void setCategory(String category) {
            this.categoryFilter = category;
            titleLabel.setText(category);
        }

        public void refreshNotes() {
            listContainer.removeAll();
            new SwingWorker<List<Note>, Void>() {
                @Override
                protected List<Note> doInBackground() throws Exception {
                    return noteDAO.getAllNotes();
                }

                @Override
                protected void done() {
                    try {
                        notes = get();
                        filterNotes();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.execute();
        }

        private void filterNotes() {
            String query = (searchField != null) ? searchField.getText().toLowerCase() : "";
            List<Note> filtered = new ArrayList<>();
            if (notes != null) {
                for (Note n : notes) {
                    String noteCat = n.getCategory();
                    if (noteCat == null) noteCat = "Personal"; // Default
                    
                    if (noteCat.equals(categoryFilter)) {
                        if (n.getTitle().toLowerCase().contains(query) || n.getContent().toLowerCase().contains(query)) {
                            filtered.add(n);
                        }
                    }
                }
                updateList(filtered);
            }
        }

        private void updateList(List<Note> notesToShow) {
            listContainer.removeAll();
            if (notesToShow != null && !notesToShow.isEmpty()) {
                for (Note n : notesToShow) {
                    listContainer.add(createListItem(n));
                    listContainer.add(Box.createVerticalStrut(5)); // Spacing
                }
            }
            listContainer.revalidate();
            listContainer.repaint();
        }
        
        private JPanel createListItem(Note note) {
            JPanel item = new JPanel(new BorderLayout());
            item.setBackground(isDarkMode ? new Color(40, 40, 40) : new Color(245, 245, 245));
            item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
            item.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
            item.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            JLabel title = new JLabel(note.getTitle());
            title.setFont(new Font("Segoe UI", Font.BOLD, 14));
            title.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
            
            JLabel preview = new JLabel();
            String content = note.getContent().replace("\n", " ");
            if (content.length() > 30) content = content.substring(0, 30) + "...";
            preview.setText(content);
            preview.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            preview.setForeground(Color.GRAY);
            
            item.add(title, BorderLayout.NORTH);
            item.add(preview, BorderLayout.CENTER);
            
            item.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (selectionListener != null) selectionListener.accept(note);
                }
                @Override
                public void mouseEntered(MouseEvent e) {
                    item.setBackground(isDarkMode ? new Color(50, 50, 50) : new Color(235, 235, 235));
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    item.setBackground(isDarkMode ? new Color(40, 40, 40) : new Color(245, 245, 245));
                }
            });
            
            return item;
        }
        
        public void updateTheme(boolean dark) {
            Color bg = dark ? new Color(30, 30, 30) : new Color(255, 255, 255);
            Color fg = dark ? Color.WHITE : Color.BLACK;
            Color border = dark ? new Color(20, 20, 20) : new Color(220, 220, 220);
            Color itemBg = dark ? new Color(40, 40, 40) : new Color(245, 245, 245);
            
            setBackground(bg);
            setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, border));
            
            // Update Header (North)
            Component header = ((BorderLayout)getLayout()).getLayoutComponent(BorderLayout.NORTH);
            if (header instanceof JPanel) {
                header.setBackground(bg);
                titleLabel.setForeground(fg);
                Component addBtn = ((BorderLayout)((JPanel)header).getLayout()).getLayoutComponent(BorderLayout.EAST);
                if (addBtn instanceof JButton) {
                    addBtn.setForeground(fg);
                }
            }
            
            // Update Center Panel
            Component center = ((BorderLayout)getLayout()).getLayoutComponent(BorderLayout.CENTER);
            if (center instanceof JPanel) {
                center.setBackground(bg);
                
                Component searchWrapper = ((BorderLayout)((JPanel)center).getLayout()).getLayoutComponent(BorderLayout.NORTH);
                if (searchWrapper instanceof JPanel) {
                    searchWrapper.setBackground(bg);
                    searchField.setBackground(dark ? new Color(45, 45, 45) : new Color(240, 240, 240));
                    searchField.setForeground(fg);
                    searchField.setCaretColor(fg);
                    searchField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(dark ? new Color(45, 45, 45) : new Color(240, 240, 240), 5),
                        BorderFactory.createEmptyBorder(5, 10, 5, 10)
                    ));
                }
                
                listContainer.setBackground(bg);
                
                Component scroll = ((BorderLayout)((JPanel)center).getLayout()).getLayoutComponent(BorderLayout.CENTER);
                if (scroll instanceof JScrollPane) {
                    ((JScrollPane)scroll).getVerticalScrollBar().setUI(new ModernScrollBarUI(dark));
                }
                
                for (Component c : listContainer.getComponents()) {
                    if (c instanceof JPanel) {
                        c.setBackground(itemBg);
                        Component title = ((BorderLayout)((JPanel)c).getLayout()).getLayoutComponent(BorderLayout.NORTH);
                        if (title instanceof JLabel) title.setForeground(fg);
                    }
                }
            }
            repaint();
        }
    }

    private void showNewNoteDialog() {
        // Simplified creation for this UI
        Note newNote = new Note(0, "New Note", "", new Date(), "#121212", "Segoe UI", currentCategory);
        noteDAO.addNote(newNote);
        
        // Get the note with its ID from database
        try {
            List<Note> allNotes = noteDAO.getAllNotes();
            if (!allNotes.isEmpty()) {
                newNote = allNotes.get(allNotes.size() - 1); // Get the last added note
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        noteListPanel.refreshNotes();
        editorPanel.setNote(newNote);
    }

    private void showSettingsDialog() {
        JDialog settingsDialog = new JDialog(this, "Settings", true);
        settingsDialog.setSize(400, 300);
        settingsDialog.setLocationRelativeTo(this);
        settingsDialog.setLayout(new BorderLayout());
        
        Color bg = isDarkMode ? new Color(45, 45, 45) : new Color(240, 240, 240);
        Color fg = isDarkMode ? Color.WHITE : Color.BLACK;
        
        settingsDialog.getContentPane().setBackground(bg);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Settings");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(fg);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(title);
        content.add(Box.createVerticalStrut(20));

        JCheckBox darkMode = new JCheckBox("Dark Mode");
        darkMode.setSelected(isDarkMode);
        darkMode.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        darkMode.setForeground(fg);
        darkMode.setOpaque(false);
        darkMode.setAlignmentX(Component.LEFT_ALIGNMENT);
        darkMode.addActionListener(e -> {
            applyTheme(darkMode.isSelected());
            Color newBg = isDarkMode ? new Color(45, 45, 45) : new Color(240, 240, 240);
            Color newFg = isDarkMode ? Color.WHITE : Color.BLACK;
            settingsDialog.getContentPane().setBackground(newBg);
            title.setForeground(newFg);
            darkMode.setForeground(newFg);
        });
        content.add(darkMode);
        
        content.add(Box.createVerticalStrut(10));
        
        JLabel version = new JLabel("Version 1.0.0");
        version.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        version.setForeground(Color.GRAY);
        version.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(version);

        settingsDialog.add(content, BorderLayout.CENTER);
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> settingsDialog.dispose());
        JPanel btnPanel = new JPanel();
        btnPanel.setOpaque(false);
        btnPanel.add(closeBtn);
        settingsDialog.add(btnPanel, BorderLayout.SOUTH);

        settingsDialog.setVisible(true);
    }

    // --- Editor Panel ---
    private class EditorPanel extends JPanel {
        private JTextField titleField;
        private JTextArea textArea;
        private Note currentNote;
        private JLabel dateLabel;
        private JLabel charCountLabel;
        private JPanel emptyStatePanel;
        private JPanel editorContentPanel;
        
        // Drawing components
        private DrawingPanel drawingPanel;
        private JPanel contentContainer;
        private Tool currentTool = Tool.PEN;
        private int brushSize = 5;
        
        private enum Tool { PEN, ERASER, FILL }

        public EditorPanel() {
            setLayout(new BorderLayout());
            setBackground(new Color(18, 18, 18)); // Darkest for editor
            
            // Create empty state panel
            createEmptyStatePanel();
            
            // Create editor content panel
            createEditorContentPanel();
            
            // Start with empty state
            add(emptyStatePanel, BorderLayout.CENTER);
        }
        
        private void createEmptyStatePanel() {
            emptyStatePanel = new JPanel(new GridBagLayout());
            emptyStatePanel.setOpaque(false);
            
            JPanel centerPanel = new JPanel();
            centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
            centerPanel.setOpaque(false);
            
            JLabel emptyLabel = new JLabel("No note selected");
            emptyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
            emptyLabel.setForeground(Color.GRAY);
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            JLabel hintLabel = new JLabel("Create a new note to get started");
            hintLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            hintLabel.setForeground(Color.GRAY);
            hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            JButton createBtn = new JButton("+");
            createBtn.setFont(new Font("Segoe UI", Font.BOLD, 48));
            createBtn.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
            createBtn.setContentAreaFilled(false);
            createBtn.setBorderPainted(false);
            createBtn.setFocusPainted(false);
            createBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            createBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            createBtn.addActionListener(e -> showCategorySelectionDialog());
            createBtn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { 
                    createBtn.setForeground(new Color(100, 150, 255)); 
                }
                public void mouseExited(MouseEvent e) { 
                    createBtn.setForeground(isDarkMode ? Color.WHITE : Color.BLACK); 
                }
            });
            
            centerPanel.add(emptyLabel);
            centerPanel.add(Box.createVerticalStrut(10));
            centerPanel.add(hintLabel);
            centerPanel.add(Box.createVerticalStrut(30));
            centerPanel.add(createBtn);
            
            emptyStatePanel.add(centerPanel);
        }
        
        private void showCategorySelectionDialog() {
            JDialog categoryDialog = new JDialog(NotesApp.this, "Select Notebook", true);
            categoryDialog.setSize(300, 250);
            categoryDialog.setLocationRelativeTo(NotesApp.this);
            categoryDialog.setLayout(new BorderLayout());
            
            Color bg = isDarkMode ? new Color(45, 45, 45) : new Color(240, 240, 240);
            Color fg = isDarkMode ? Color.WHITE : Color.BLACK;
            
            categoryDialog.getContentPane().setBackground(bg);
            
            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setOpaque(false);
            content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            
            JLabel title = new JLabel("Choose a notebook for your note:");
            title.setFont(new Font("Segoe UI", Font.BOLD, 14));
            title.setForeground(fg);
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            content.add(title);
            content.add(Box.createVerticalStrut(20));
            
            String[] categories = {"Personal", "Work", "Ideas"};
            for (String cat : categories) {
                JButton catBtn = new JButton(cat);
                catBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
                catBtn.setMaximumSize(new Dimension(200, 40));
                catBtn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                catBtn.setBackground(isDarkMode ? new Color(60, 60, 60) : new Color(220, 220, 220));
                catBtn.setForeground(fg);
                catBtn.setFocusPainted(false);
                catBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                catBtn.addActionListener(e -> {
                    createNoteInCategory(cat);
                    categoryDialog.dispose();
                });
                content.add(catBtn);
                content.add(Box.createVerticalStrut(10));
            }
            
            categoryDialog.add(content, BorderLayout.CENTER);
            categoryDialog.setVisible(true);
        }
        
        private void createNoteInCategory(String category) {
            currentCategory = category;
            showNewNoteDialog();
        }
        
        private void createEditorContentPanel() {
            editorContentPanel = new JPanel(new BorderLayout());
            editorContentPanel.setOpaque(false);
            
            // Top Bar
            JPanel topBar = new JPanel(new BorderLayout());
            topBar.setOpaque(false);
            topBar.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            
            // Info Panel (Date & Char Count)
            JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
            infoPanel.setOpaque(false);
            
            dateLabel = new JLabel("Today");
            dateLabel.setForeground(Color.GRAY);
            dateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            
            charCountLabel = new JLabel("0 characters");
            charCountLabel.setForeground(Color.GRAY);
            charCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            
            infoPanel.add(dateLabel);
            infoPanel.add(charCountLabel);
            
            topBar.add(infoPanel, BorderLayout.WEST);
            
            // Tools
            JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            tools.setOpaque(false);
            
            JButton drawToggleBtn = new JButton("Draw");
            styleToolbarButton(drawToggleBtn);
            drawToggleBtn.addActionListener(e -> {
                boolean isDrawing = !drawingPanel.isVisible();
                drawingPanel.setVisible(isDrawing);
                drawToggleBtn.setText(isDrawing ? "Text" : "Draw");
                contentContainer.revalidate();
                contentContainer.repaint();
            });
            
            JButton saveBtn = new JButton("Save");
            styleToolbarButton(saveBtn);
            saveBtn.addActionListener(e -> {
                saveNote();
                noteListPanel.refreshNotes(); // Refresh list to show updates
            });
            
            JButton deleteBtn = new JButton("Delete");
            styleToolbarButton(deleteBtn);
            deleteBtn.setForeground(new Color(255, 100, 100));
            deleteBtn.addActionListener(e -> {
                if (currentNote != null) {
                    int choice = JOptionPane.showConfirmDialog(NotesApp.this, "Delete this note?", "Confirm", JOptionPane.YES_NO_OPTION);
                    if (choice == JOptionPane.YES_OPTION) {
                        noteDAO.deleteNote(currentNote.getId());
                        currentNote = null;
                        titleField.setText("");
                        textArea.setText("");
                        noteListPanel.refreshNotes();
                    }
                }
            });

            tools.add(drawToggleBtn);
            tools.add(saveBtn);
            tools.add(deleteBtn);
            topBar.add(tools, BorderLayout.EAST);
            
            editorContentPanel.add(topBar, BorderLayout.NORTH);

            // Content Container (OverlayLayout for Text vs Draw)
            contentContainer = new JPanel();
            contentContainer.setLayout(new OverlayLayout(contentContainer));
            contentContainer.setOpaque(false);
            contentContainer.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

            // 1. Text View
            JPanel textPanel = new JPanel(new BorderLayout());
            textPanel.setOpaque(false);

            titleField = new JTextField();
            titleField.setBorder(null);
            titleField.setOpaque(false);
            titleField.setForeground(Color.WHITE);
            titleField.setFont(new Font("Segoe UI", Font.BOLD, 32));
            titleField.setCaretColor(Color.WHITE);
            
            textArea = new JTextArea() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    if (getText().isEmpty()) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(Color.GRAY);
                        g2.setFont(getFont().deriveFont(Font.ITALIC));
                        g2.drawString("Type here...", getInsets().left, g.getFontMetrics().getAscent() + getInsets().top);
                        g2.dispose();
                    }
                }
            };
            textArea.setBorder(null);
            textArea.setOpaque(false);
            textArea.setForeground(new Color(220, 220, 220));
            textArea.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setCaretColor(Color.WHITE);
            textArea.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { updateStats(); }
                public void removeUpdate(DocumentEvent e) { updateStats(); }
                public void changedUpdate(DocumentEvent e) { updateStats(); }
            });
            
            JScrollPane scroll = new JScrollPane(textArea);
            scroll.setBorder(null);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.getVerticalScrollBar().setUI(new ModernScrollBarUI(isDarkMode));

            textPanel.add(titleField, BorderLayout.NORTH);
            textPanel.add(scroll, BorderLayout.CENTER);
            
            // 2. Draw View
            drawingPanel = new DrawingPanel();
            drawingPanel.setOpaque(false);
            drawingPanel.setVisible(false);
            
            // In OverlayLayout, the first component added is on top
            contentContainer.add(drawingPanel);
            contentContainer.add(textPanel);
            
            editorContentPanel.add(contentContainer, BorderLayout.CENTER);
        }
        
        private void styleToolbarButton(JButton btn) {
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setForeground(Color.GRAY);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { btn.setForeground(isDarkMode ? Color.WHITE : Color.BLACK); }
                public void mouseExited(MouseEvent e) { btn.setForeground(Color.GRAY); }
            });
        }

        private void updateStats() {
            // Update character count
            int charCount = textArea.getText().length();
            charCountLabel.setText(charCount + " character" + (charCount != 1 ? "s" : ""));
            
            // Update date
            if (currentNote != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
                dateLabel.setText(sdf.format(currentNote.getLastModified()));
            } else {
                dateLabel.setText("Today");
            }
        }

        public void setNote(Note note) {
            this.currentNote = note;
            
            if (note != null) {
                // Switch to editor view
                remove(emptyStatePanel);
                add(editorContentPanel, BorderLayout.CENTER);
                
                titleField.setText(note.getTitle());
                textArea.setText(note.getContent());
                
                // Update stats
                updateStats();
                
                // Reset drawing for new note (or load if we supported it)
                drawingPanel.clear();
                
                revalidate();
                repaint();
            } else {
                // Switch to empty state
                remove(editorContentPanel);
                add(emptyStatePanel, BorderLayout.CENTER);
                revalidate();
                repaint();
            }
        }
        
        private void saveNote() {
            if (currentNote == null) return;
            
            String title = titleField.getText();
            if (title.trim().isEmpty()) title = "Untitled";
            
            currentNote.setTitle(title);
            currentNote.setContent(textArea.getText());
            currentNote.setLastModified(new Date());
            
            noteDAO.updateNote(currentNote);
        }

        public void updateTheme(boolean dark) {
            Color bg = dark ? new Color(18, 18, 18) : new Color(255, 255, 255);
            Color fg = dark ? Color.WHITE : Color.BLACK;
            Color textFg = dark ? new Color(220, 220, 220) : Color.BLACK;
            
            setBackground(bg);
            titleField.setForeground(fg);
            titleField.setCaretColor(fg);
            textArea.setForeground(textFg);
            textArea.setCaretColor(fg);
            dateLabel.setForeground(Color.GRAY);
            charCountLabel.setForeground(Color.GRAY);
            
            // Update empty state panel colors
            for (Component c : emptyStatePanel.getComponents()) {
                if (c instanceof JPanel) {
                    for (Component inner : ((JPanel)c).getComponents()) {
                        if (inner instanceof JLabel) {
                            ((JLabel)inner).setForeground(Color.GRAY);
                        }
                    }
                }
            }
            
            repaint();
        }

        // --- DrawingPanel inner class ---
        private class DrawingPanel extends JPanel {
            private BufferedImage canvas;
            private Color currentColor = Color.WHITE; // Default white for dark mode
            private int prevX = -1, prevY = -1;

            public DrawingPanel() {
                setLayout(new BorderLayout());
                setOpaque(false);
                setBackground(new Color(0, 0, 0, 0));
                
                // --- Top Control Bar (Save) ---
                JPanel topControlBar = new JPanel(new BorderLayout());
                topControlBar.setOpaque(false);
                topControlBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

                JButton saveImgBtn = new JButton("Save Image");
                saveImgBtn.addActionListener(e -> {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle("Save Drawing");
                    fileChooser.setFileFilter(new FileNameExtensionFilter("PNG Images", "png"));
                    if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                        File file = fileChooser.getSelectedFile();
                        if (!file.getName().toLowerCase().endsWith(".png")) {
                            file = new File(file.getParentFile(), file.getName() + ".png");
                        }
                        try {
                            if (canvas != null) ImageIO.write(canvas, "png", file);
                            JOptionPane.showMessageDialog(this, "Image saved successfully!");
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(this, "Error saving image: " + ex.getMessage());
                        }
                    }
                });

                JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                topRight.setOpaque(false);
                topRight.add(saveImgBtn);
                
                topControlBar.add(topRight, BorderLayout.EAST);
                add(topControlBar, BorderLayout.NORTH);

                // --- Bottom Toolbar (Tools) ---
                JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
                toolbar.setBackground(new Color(40, 40, 40));
                
                JButton penBtn = new JButton("Pen");
                JButton eraserBtn = new JButton("Eraser");
                JButton fillBtn = new JButton("Fill");
                JButton colorBtn = new JButton("Color");
                JButton clearBtn = new JButton("Clear");
                
                JSlider sizeSlider = new JSlider(1, 50, brushSize);
                sizeSlider.setBackground(new Color(40, 40, 40));
                sizeSlider.setPreferredSize(new Dimension(100, 20));
                sizeSlider.addChangeListener(e -> brushSize = sizeSlider.getValue());

                penBtn.addActionListener(e -> currentTool = Tool.PEN);
                eraserBtn.addActionListener(e -> currentTool = Tool.ERASER);
                fillBtn.addActionListener(e -> currentTool = Tool.FILL);
                
                colorBtn.addActionListener(e -> {
                    Color newColor = JColorChooser.showDialog(this, "Choose Color", currentColor);
                    if (newColor != null) {
                        currentColor = newColor;
                    }
                });
                
                clearBtn.addActionListener(e -> clear());
                
                toolbar.add(new JLabel("Tools:"));
                toolbar.add(penBtn);
                toolbar.add(eraserBtn);
                toolbar.add(fillBtn);
                toolbar.add(new JSeparator(JSeparator.VERTICAL));
                toolbar.add(new JLabel("Size:"));
                toolbar.add(sizeSlider);
                toolbar.add(new JSeparator(JSeparator.VERTICAL));
                toolbar.add(colorBtn);
                toolbar.add(clearBtn);
                
                add(toolbar, BorderLayout.SOUTH);

                JPanel canvasPanel = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        if (canvas == null) ensureCanvas(getWidth(), getHeight());
                        g.drawImage(canvas, 0, 0, null);
                    }
                };
                canvasPanel.setOpaque(false);
                canvasPanel.setBackground(new Color(0, 0, 0, 0));
                canvasPanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                add(canvasPanel, BorderLayout.CENTER);

                canvasPanel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        ensureCanvas(canvasPanel.getWidth(), canvasPanel.getHeight());
                        int x = e.getX();
                        int y = e.getY();
                        if (currentTool == Tool.FILL) {
                            floodFill(x, y, currentColor);
                        } else {
                            prevX = x; prevY = y;
                            Graphics2D g = canvas.createGraphics();
                            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            if (currentTool == Tool.ERASER) {
                                g.setComposite(AlphaComposite.Clear);
                                g.setColor(new Color(0, 0, 0, 0));
                            } else {
                                g.setColor(currentColor);
                            }
                            int s = brushSize;
                            g.fillOval(x - s/2, y - s/2, s, s);
                            g.dispose();
                        }
                        canvasPanel.repaint();
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        prevX = -1;
                        prevY = -1;
                    }
                });

                canvasPanel.addMouseMotionListener(new MouseAdapter() {
                    @Override
                    public void mouseDragged(MouseEvent e) {
                        ensureCanvas(canvasPanel.getWidth(), canvasPanel.getHeight());
                        int x = e.getX();
                        int y = e.getY();
                        Graphics2D g = canvas.createGraphics();
                        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        int s = brushSize;
                        if (currentTool == Tool.ERASER) {
                            g.setComposite(AlphaComposite.Clear);
                            g.setColor(new Color(0, 0, 0, 0));
                            g.setStroke(new BasicStroke(s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                            if (prevX != -1) g.drawLine(prevX, prevY, x, y);
                        } else {
                            g.setColor(currentColor);
                            int stroke = Math.max(1, s);
                            g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                            if (prevX != -1) g.drawLine(prevX, prevY, x, y);
                        }
                        prevX = x;
                        prevY = y;
                        g.dispose();
                        canvasPanel.repaint();
                    }
                });
            }

            private void ensureCanvas(int w, int h) {
                if (canvas == null || canvas.getWidth() != w || canvas.getHeight() != h) {
                    BufferedImage newCanvas = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = newCanvas.createGraphics();
                    g.setBackground(new Color(0, 0, 0, 0));
                    g.clearRect(0, 0, newCanvas.getWidth(), newCanvas.getHeight());
                    if (canvas != null) g.drawImage(canvas, 0, 0, null);
                    g.dispose();
                    canvas = newCanvas;
                }
            }

            public void clear() {
                if (canvas != null) {
                    Graphics2D g = canvas.createGraphics();
                    g.setBackground(new Color(0, 0, 0, 0));
                    g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                    g.dispose();
                    repaint();
                }
            }

            // Simple iterative flood fill (BFS) on canvas image
            private void floodFill(int x, int y, Color fillColor) {
                if (canvas == null) return;
                int w = canvas.getWidth(), h = canvas.getHeight();
                if (x < 0 || x >= w || y < 0 || y >= h) return;
                int target = canvas.getRGB(x, y);
                int replacement = fillColor.getRGB();
                if (target == replacement) return;
                Deque<Point> stack = new ArrayDeque<>();
                stack.push(new Point(x, y));
                while (!stack.isEmpty()) {
                    Point p = stack.pop();
                    int px = p.x, py = p.y;
                    if (px < 0 || px >= w || py < 0 || py >= h) continue;
                    if (canvas.getRGB(px, py) != target) continue;
                    canvas.setRGB(px, py, replacement);
                    stack.push(new Point(px+1, py));
                    stack.push(new Point(px-1, py));
                    stack.push(new Point(px, py+1));
                    stack.push(new Point(px, py-1));
                }
            }
        }
    }

    // 2. Modern Scroll Bar UI
    private static class ModernScrollBarUI extends BasicScrollBarUI {
        private Color thumb;
        private Color track;

        public ModernScrollBarUI(boolean dark) {
            this.thumb = dark ? new Color(80, 80, 80) : new Color(200, 200, 200);
            this.track = dark ? new Color(30, 30, 30) : new Color(240, 240, 240);
        }

        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = thumb;
            this.trackColor = track;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton btn = new JButton();
            btn.setPreferredSize(new Dimension(0, 0));
            btn.setMinimumSize(new Dimension(0, 0));
            btn.setMaximumSize(new Dimension(0, 0));
            return btn;
        }
        
        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(thumbColor);
            g2.fillRoundRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height, 10, 10);
            g2.dispose();
        }
        
        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            g.setColor(trackColor);
            g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        }
    }

    private void applyTheme(boolean dark) {
        this.isDarkMode = dark;
        sidebar.updateTheme(dark);
        noteListPanel.updateTheme(dark);
        editorPanel.updateTheme(dark);
        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new NotesApp().setVisible(true);
        });
    }
}
