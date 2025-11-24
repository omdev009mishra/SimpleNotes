
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.text.SimpleDateFormat;
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

public class NotesApp extends JFrame {

    private JList<Note> noteList;
    private DefaultListModel<Note> noteListModel;
    private JTextArea textArea;
    private JTextField titleField;
    private DrawingPanel drawingPanel;
    private JTabbedPane tabbedPane;
    private enum Tool {PEN, BRUSH, ERASER, FILL}
    private Tool currentTool = Tool.BRUSH;
    private int brushSize = 6;
    private NoteDAO noteDAO;
    private Note currentNote;
    private boolean isNewNote = false;

    public NotesApp() {
        // --- Window Setup ---
        setTitle("Simple Notes App");
        setSize(600, 400);
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

        // --- Note List Panel ---
        noteListModel = new DefaultListModel<>();
        noteList = new JList<>(noteListModel);
        noteList.addListSelectionListener(this::noteSelectionChanged);
        JScrollPane listScrollPane = new JScrollPane(noteList);
        listScrollPane.setPreferredSize(new Dimension(150, 0));


        // --- Tabbed Pane Setup (Notes + Drawing) ---
        tabbedPane = new JTabbedPane();

        // Notes tab (rich dark UI similar to screenshot)
        textArea = new JTextArea();
        textArea.setFont(new Font("Arial", Font.PLAIN, 14));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        // Notes panel container
        JPanel notesPanel = new JPanel(new BorderLayout());
        Color bg = new Color(18, 18, 18);
        Color fg = new Color(230, 230, 230);
        Color muted = new Color(150, 150, 150);
        notesPanel.setBackground(bg);

        // Top bar: back (left) and undo/redo/check (right)
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(bg);
        JButton backBtn = new JButton("←");
        backBtn.setFocusable(false);
        backBtn.setBackground(bg);
        backBtn.setForeground(fg);
        backBtn.setBorderPainted(false);
        topBar.add(backBtn, BorderLayout.WEST);
        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        topRight.setBackground(bg);
        JButton undoBtn = new JButton("⤺"); undoBtn.setFocusable(false); undoBtn.setBackground(bg); undoBtn.setForeground(fg); undoBtn.setBorderPainted(false);
        JButton redoBtn = new JButton("⤻"); redoBtn.setFocusable(false); redoBtn.setBackground(bg); redoBtn.setForeground(fg); redoBtn.setBorderPainted(false);
        JButton doneBtn = new JButton("✓"); doneBtn.setFocusable(false); doneBtn.setBackground(bg); doneBtn.setForeground(fg); doneBtn.setBorderPainted(false);
        topRight.add(undoBtn); topRight.add(redoBtn); topRight.add(doneBtn);
        topBar.add(topRight, BorderLayout.EAST);
        notesPanel.add(topBar, BorderLayout.NORTH);

        // Title and meta
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(bg);
        titleField = new JTextField();
        titleField.setBorder(null);
        titleField.setFont(new Font("Arial", Font.BOLD, 20));
        titleField.setBackground(bg);
        titleField.setForeground(fg);
        titleField.setCaretColor(fg);
        titleField.setText("");
        titlePanel.add(titleField, BorderLayout.NORTH);
        JLabel metaLabel = new JLabel("");
        metaLabel.setForeground(muted);
        metaLabel.setBorder(BorderFactory.createEmptyBorder(4,0,8,0));
        titlePanel.add(metaLabel, BorderLayout.SOUTH);
        notesPanel.add(titlePanel, BorderLayout.BEFORE_FIRST_LINE);

        // Text area with placeholder behavior
        textArea.setBackground(bg);
        textArea.setForeground(fg);
        textArea.setCaretColor(fg);
        textArea.setBorder(null);
        String placeholder = "Start typing";
        // initialize placeholder
        if (textArea.getText().isEmpty()) {
            textArea.setText(placeholder);
            textArea.setForeground(muted);
        }
        textArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (textArea.getText().equals(placeholder)) {
                    textArea.setText("");
                    textArea.setForeground(fg);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (textArea.getText().isEmpty()) {
                    textArea.setText(placeholder);
                    textArea.setForeground(muted);
                }
            }
        });

        // update meta label with date and char count
        DocumentListener docListener = new DocumentListener() {
            void update() {
                String text = textArea.getText();
                if (text.equals(placeholder)) text = "";
                String date = (currentNote != null)
                        ? new SimpleDateFormat("dd MMM yyyy HH:mm").format(new Date()) // This should be the note's date
                        : new SimpleDateFormat("dd MMM yyyy HH:mm").format(new Date());
                metaLabel.setText(date + "  |  " + text.length() + " characters");
            }
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
        };
        textArea.getDocument().addDocumentListener(docListener);

        JScrollPane notesScroll = new JScrollPane(textArea);
        notesScroll.setBorder(null);
        notesScroll.getViewport().setBackground(bg);
        notesPanel.add(notesScroll, BorderLayout.CENTER);

        // Bottom toolbar (explicit buttons with behavior)
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        bottomBar.setBackground(bg);
        bottomBar.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
        JButton voiceBtn = new JButton("Voice");
        JButton imageBtn = new JButton("Image");
        JButton drawBtn = new JButton("Draw");
        JButton checklistBtn = new JButton("Checklist");
        JButton styleBtn = new JButton("T");
        JButton[] barButtons = {voiceBtn, imageBtn, drawBtn, checklistBtn, styleBtn};
        for (JButton b : barButtons) {
            b.setFocusable(false);
            b.setBackground(new Color(28,28,28));
            b.setForeground(fg);
            b.setBorderPainted(false);
            bottomBar.add(b);
        }

        // Voice: placeholder action
        voiceBtn.addActionListener(ae -> JOptionPane.showMessageDialog(this, "Voice feature not implemented yet.", "Voice", JOptionPane.INFORMATION_MESSAGE));

        // Image: choose an image and draw it onto the drawing canvas
        imageBtn.addActionListener(ae -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Insert Image");
            chooser.setFileFilter(new FileNameExtensionFilter("Image files", ImageIO.getReaderFileSuffixes()));
            int res = chooser.showOpenDialog(this);
            if (res == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                try {
                    BufferedImage img = ImageIO.read(f);
                    if (img != null) {
                        // ensure drawing panel exists
                        if (drawingPanel != null) {
                            BufferedImage canvas = drawingPanel.getCanvasImage();
                            Graphics2D g = canvas.createGraphics();
                            // draw image centered
                            int x = Math.max(0, (canvas.getWidth() - img.getWidth())/2);
                            int y = Math.max(0, (canvas.getHeight() - img.getHeight())/2);
                            g.drawImage(img, x, y, null);
                            g.dispose();
                            drawingPanel.repaint();
                            // switch to draw tab
                            tabbedPane.setSelectedComponent(drawingPanel);
                        } else {
                            JOptionPane.showMessageDialog(this, "Drawing panel not ready.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        JOptionPane.showMessageDialog(this, "Selected file is not a supported image.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Failed to load image: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Draw: switch to the Draw tab
        drawBtn.addActionListener(ae -> {
            if (drawingPanel != null) tabbedPane.setSelectedComponent(drawingPanel);
        });

        // Checklist: simple input dialog to insert bullet list
        checklistBtn.addActionListener(ae -> {
            String items = JOptionPane.showInputDialog(this, "Enter checklist items separated by commas:", "Checklist", JOptionPane.PLAIN_MESSAGE);
            if (items != null) {
                String[] parts = items.split(",");
                StringBuilder sb = new StringBuilder();
                for (String p : parts) {
                    sb.append("- [ ] ").append(p.trim()).append(System.lineSeparator());
                }
                // insert at caret
                int pos = textArea.getCaretPosition();
                try {
                    textArea.getDocument().insertString(pos, sb.toString(), null);
                } catch (Exception ex) {
                    textArea.append(sb.toString());
                }
            }
        });

        // T: toggle bold for the entire text area font
        styleBtn.addActionListener(ae -> {
            Font fnt = textArea.getFont();
            if (fnt.isBold()) textArea.setFont(fnt.deriveFont(Font.PLAIN));
            else textArea.setFont(fnt.deriveFont(Font.BOLD));
        });

        notesPanel.add(bottomBar, BorderLayout.SOUTH);

        // Floating color droplet on the left
        JButton colorDrop = new JButton();
        colorDrop.setBackground(new Color(255, 176, 0));
        colorDrop.setPreferredSize(new Dimension(28, 28));
        colorDrop.setBorderPainted(false);
        colorDrop.setFocusPainted(false);
        colorDrop.setOpaque(true);
        colorDrop.setToolTipText("Brush color");
        // position via absolute panel
        JLayeredPane layered = new JLayeredPane();
        layered.setLayout(new BorderLayout());
        layered.add(notesPanel, BorderLayout.CENTER);
        JPanel floatHolder = new JPanel(null);
        floatHolder.setOpaque(false);
        colorDrop.setBounds(12, 120, 28, 28);
        floatHolder.add(colorDrop);
        layered.add(floatHolder, BorderLayout.WEST);

        tabbedPane.addTab("Notes", layered);

        // Drawing tab
        drawingPanel = new DrawingPanel();
        tabbedPane.addTab("Draw", drawingPanel);

        add(tabbedPane, BorderLayout.CENTER);
        add(listScrollPane, BorderLayout.WEST);

        // Load notes from DB
        loadNotes();


        // --- Menu Bar Setup ---
        createMenuBar();
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // --- File Menu ---
        JMenu fileMenu = new JMenu("File");

        JMenuItem newItem = new JMenuItem("New");
        newItem.addActionListener(e -> createNewNote());
        fileMenu.add(newItem);

        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(e -> deleteCurrentNote());
        fileMenu.add(deleteItem);

        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.addActionListener(e -> saveCurrentNote());
        fileMenu.add(saveItem);

        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(ae -> System.exit(0));
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);

        // --- Draw Menu ---
        JMenu drawMenu = new JMenu("Draw");

        // Tool selection submenu
        JMenu toolMenu = new JMenu("Tool");
        ButtonGroup toolGroup = new ButtonGroup();
        JRadioButtonMenuItem penTool = new JRadioButtonMenuItem("Pen");
        JRadioButtonMenuItem brushTool = new JRadioButtonMenuItem("Brush", true);
        JRadioButtonMenuItem eraserTool = new JRadioButtonMenuItem("Eraser");
        JRadioButtonMenuItem fillTool = new JRadioButtonMenuItem("Fill");
        toolGroup.add(penTool); toolGroup.add(brushTool); toolGroup.add(eraserTool); toolGroup.add(fillTool);
        toolMenu.add(penTool); toolMenu.add(brushTool); toolMenu.add(eraserTool); toolMenu.add(fillTool);
        penTool.addActionListener(ae -> currentTool = Tool.PEN);
        brushTool.addActionListener(ae -> currentTool = Tool.BRUSH);
        eraserTool.addActionListener(ae -> currentTool = Tool.ERASER);
        fillTool.addActionListener(ae -> currentTool = Tool.FILL);
        drawMenu.add(toolMenu);

        // Brush size submenu
        JMenu sizeMenu = new JMenu("Brush Size");
        ButtonGroup sizeGroup = new ButtonGroup();
        for (int s : new int[]{2, 6, 12, 24}) {
            JRadioButtonMenuItem sizeItem = new JRadioButtonMenuItem(s + " px", s==brushSize);
            sizeItem.addActionListener(ae -> {
                String label = ((JRadioButtonMenuItem)ae.getSource()).getText();
                brushSize = Integer.parseInt(label.split(" ")[0]);
            });
            sizeGroup.add(sizeItem);
            sizeMenu.add(sizeItem);
        }
        drawMenu.add(sizeMenu);

        JMenuItem colorItem = new JMenuItem("Choose Color");
        colorItem.addActionListener(ae -> {
            Color chosen = JColorChooser.showDialog(this, "Choose Draw Color", drawingPanel.getCurrentColor());
            if (chosen != null) drawingPanel.setCurrentColor(chosen);
        });
        drawMenu.add(colorItem);

        JMenuItem clearItem = new JMenuItem("Clear Drawing");
        clearItem.addActionListener(ae -> drawingPanel.clear());
        drawMenu.add(clearItem);

        JMenuItem saveDrawingItem = new JMenuItem("Save Drawing");
        saveDrawingItem.addActionListener(ae -> saveDrawing());
        drawMenu.add(saveDrawingItem);

        menuBar.add(drawMenu);

        setJMenuBar(menuBar);
    }

    // Save the current drawing to an image file (PNG)
    private void saveDrawing() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Drawing");
        chooser.setFileFilter(new FileNameExtensionFilter("PNG Images", "png"));
        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            if (!f.getName().toLowerCase().endsWith(".png")) {
                f = new File(f.getParentFile(), f.getName() + ".png");
            }
            try {
                ImageIO.write(drawingPanel.getCanvasImage(), "png", f);
                JOptionPane.showMessageDialog(this, "Saved drawing to " + f.getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Failed to save drawing: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void noteSelectionChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            currentNote = noteList.getSelectedValue();
            if (currentNote != null) {
                isNewNote = false;
                titleField.setText(currentNote.getTitle());
                textArea.setText(currentNote.getContent());
                textArea.setForeground(new Color(230, 230, 230));
            }
        }
    }

    private void createNewNote() {
        isNewNote = true;
        currentNote = null;
        noteList.clearSelection();
        titleField.setText("New Note");
        textArea.setText("");
        titleField.requestFocus();
    }

    private void saveCurrentNote() {
        String title = titleField.getText();
        String content = textArea.getText();

        if (title.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Title cannot be empty.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Use SwingWorker for database operations
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                if (isNewNote || currentNote == null) {
                    Note newNote = new Note(0, title, content, new Date());
                    noteDAO.addNote(newNote);
                } else {
                    currentNote.setTitle(title);
                    currentNote.setContent(content);
                    noteDAO.updateNote(currentNote);
                }
                return null;
            }

            @Override
            protected void done() {
                // Refresh the notes list on the EDT
                loadNotes();
                JOptionPane.showMessageDialog(NotesApp.this, "Note saved!");
            }
        }.execute();
    }

    private void deleteCurrentNote() {
        if (currentNote != null) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete '" + currentNote.getTitle() + "'?",
                    "Confirm Deletion", JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        noteDAO.deleteNote(currentNote.getId());
                        return null;
                    }

                    @Override
                    protected void done() {
                        createNewNote();
                        loadNotes();
                    }
                }.execute();
            }
        }
    }

    private void loadNotes() {
        new SwingWorker<List<Note>, Void>() {
            @Override
            protected List<Note> doInBackground() throws Exception {
                return noteDAO.getAllNotes();
            }

            @Override
            protected void done() {
                try {
                    List<Note> notes = get();
                    noteListModel.clear();
                    for (Note note : notes) {
                        noteListModel.addElement(note);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    // --- DrawingPanel inner class ---
    private static class DrawingPanel extends JPanel {
        private BufferedImage canvas;
        private Color currentColor = Color.BLACK;
        private int prevX = -1, prevY = -1;

        // We will reference the outer class's tool and size via helper methods

        public DrawingPanel() {
            setBackground(Color.WHITE);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    ensureCanvas();
                    int x = e.getX();
                    int y = e.getY();
                    if (getOuterTool() == Tool.FILL) {
                        floodFill(x, y, currentColor);
                    } else {
                        prevX = x; prevY = y;
                        Graphics2D g = canvas.createGraphics();
                        if (getOuterTool() == Tool.ERASER) g.setColor(Color.WHITE);
                        else g.setColor(currentColor);
                        int s = getOuterBrushSize();
                        g.fillOval(x - s/2, y - s/2, s, s);
                        g.dispose();
                    }
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    prevX = -1;
                    prevY = -1;
                }
            });

            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    ensureCanvas();
                    int x = e.getX();
                    int y = e.getY();
                    Graphics2D g = canvas.createGraphics();
                    int s = getOuterBrushSize();
                    if (getOuterTool() == Tool.ERASER) {
                        g.setColor(Color.WHITE);
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
                    repaint();
                }
            });
        }

        private void ensureCanvas() {
            if (canvas == null || canvas.getWidth() != getWidth() || canvas.getHeight() != getHeight()) {
                BufferedImage newCanvas = new BufferedImage(Math.max(1, getWidth()), Math.max(1, getHeight()), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = newCanvas.createGraphics();
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, newCanvas.getWidth(), newCanvas.getHeight());
                if (canvas != null) g.drawImage(canvas, 0, 0, null);
                g.dispose();
                canvas = newCanvas;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (canvas == null) ensureCanvas();
            g.drawImage(canvas, 0, 0, null);
        }

        public void clear() {
            ensureCanvas();
            Graphics2D g = canvas.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            g.dispose();
            repaint();
        }

        public BufferedImage getCanvasImage() {
            ensureCanvas();
            return canvas;
        }

        public Color getCurrentColor() {
            return currentColor;
        }

        public void setCurrentColor(Color c) {
            currentColor = c;
        }

        // Helpers to access outer class fields
        private Tool getOuterTool() {
            Container p = getParent();
            while (p != null && !(p instanceof NotesApp)) p = p.getParent();
            if (p instanceof NotesApp) return ((NotesApp)p).currentTool;
            return Tool.BRUSH;
        }

        private int getOuterBrushSize() {
            Container p = getParent();
            while (p != null && !(p instanceof NotesApp)) p = p.getParent();
            if (p instanceof NotesApp) return ((NotesApp)p).brushSize;
            return 6;
        }

        // Simple iterative flood fill (BFS) on canvas image
        private void floodFill(int x, int y, Color fillColor) {
            ensureCanvas();
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

    public static void main(String[] args) {
        // Run the GUI on the Event Dispatch Thread for thread safety
        SwingUtilities.invokeLater(() -> new NotesApp().setVisible(true));
    }
}
