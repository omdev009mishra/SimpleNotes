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

    private CardLayout cardLayout;
    private JPanel mainPanel;
    private HomePanel homePanel;
    private EditorPanel editorPanel;
    private NoteDAO noteDAO;

    public NotesApp() {
        setTitle("Simple Notes App");
        setSize(900, 600);
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

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        homePanel = new HomePanel();
        editorPanel = new EditorPanel();

        mainPanel.add(homePanel, "HOME");
        mainPanel.add(editorPanel, "EDITOR");

        add(mainPanel);
        
        // Initial load
        homePanel.refreshNotes();
    }

    private void showHome() {
        homePanel.refreshNotes();
        cardLayout.show(mainPanel, "HOME");
    }

    private void showEditor(Note note) {
        editorPanel.setNote(note);
        cardLayout.show(mainPanel, "EDITOR");
    }

    // --- Home Panel ---
    private class HomePanel extends JPanel {
        private JPanel gridPanel;
        private List<Note> notes;

        public HomePanel() {
            setLayout(new BorderLayout());
            setBackground(new Color(18, 18, 18));

            // Header
            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(new Color(18, 18, 18));
            header.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            JLabel title = new JLabel("My Notes");
            title.setFont(new Font("Segoe UI", Font.BOLD, 28));
            title.setForeground(Color.WHITE);
            header.add(title, BorderLayout.WEST);
            add(header, BorderLayout.NORTH);

            // Grid of Notes
            gridPanel = new JPanel(new GridLayout(0, 3, 15, 15)); // 3 columns
            gridPanel.setBackground(new Color(18, 18, 18));
            gridPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            
            // Wrap grid in scroll pane
            // We need a wrapper panel to keep grid items from expanding too much vertically if few items
            JPanel gridWrapper = new JPanel(new BorderLayout());
            gridWrapper.setBackground(new Color(18, 18, 18));
            gridWrapper.add(gridPanel, BorderLayout.NORTH);
            
            JScrollPane scrollPane = new JScrollPane(gridWrapper);
            scrollPane.setBorder(null);
            scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
            add(scrollPane, BorderLayout.CENTER);

            // FAB (Floating Action Button)
            JButton fab = new ModernButton("+", new Color(100, 149, 237), new Color(120, 169, 255)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground()); // Use current color from ModernButton logic
                    // Hack: ModernButton uses 'currentColor' field which is private. 
                    // We can't easily access it unless we change ModernButton visibility or just rely on super.paintComponent
                    // but super draws a rounded rect. We want a circle.
                    // Let's just make a simple circle button here.
                    super.paintComponent(g); 
                    g2.dispose();
                }
            };
            fab.setFont(new Font("Segoe UI", Font.BOLD, 24));
            fab.setPreferredSize(new Dimension(60, 60));
            
            // Use a LayeredPane to float the FAB
            // Actually, let's just put it in the SOUTH of the main layout for simplicity or use a layered pane approach
            // For a true FAB, we need a LayeredPane.
            
            // Let's re-structure HomePanel to use LayeredPane
            removeAll();
            JLayeredPane layeredPane = new JLayeredPane();
            layeredPane.setLayout(new OverlayLayout(layeredPane)); // Overlay layout fills component
            
            JPanel contentPanel = new JPanel(new BorderLayout());
            contentPanel.setBackground(new Color(18, 18, 18));
            contentPanel.add(header, BorderLayout.NORTH);
            contentPanel.add(scrollPane, BorderLayout.CENTER);
            
            // FAB Panel (transparent, aligns button to bottom right)
            JPanel fabPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 30, 30));
            fabPanel.setOpaque(false);
            fabPanel.add(fab);
            
            // Add to layered pane (content at bottom, fab on top)
            // Note: OverlayLayout stacks last added on top? No, first added is on top usually or z-order.
            // Let's use absolute positioning for LayeredPane or just OverlayLayout
            // OverlayLayout puts components on top of each other.
            
            // We need to set alignment for OverlayLayout to work properly if we want them to stretch
            contentPanel.setAlignmentX(0.5f);
            contentPanel.setAlignmentY(0.5f);
            fabPanel.setAlignmentX(0.5f);
            fabPanel.setAlignmentY(0.5f); // This might center it.
            
            // Let's stick to a simpler approach: BorderLayout with FAB in South? No, that takes up space.
            // Let's use JLayeredPane with manual bounds in a resize listener?
            
            setLayout(new BorderLayout());
            JLayeredPane lp = new JLayeredPane();
            add(lp, BorderLayout.CENTER);
            
            lp.addComponentListener(new java.awt.event.ComponentAdapter() {
                public void componentResized(java.awt.event.ComponentEvent e) {
                    contentPanel.setBounds(0, 0, lp.getWidth(), lp.getHeight());
                    fab.setBounds(lp.getWidth() - 80, lp.getHeight() - 80, 60, 60);
                }
            });
            
            lp.add(contentPanel, JLayeredPane.DEFAULT_LAYER);
            lp.add(fab, JLayeredPane.PALETTE_LAYER);

            fab.addActionListener(e -> showNewNoteDialog());
        }

        public void refreshNotes() {
            gridPanel.removeAll();
            new SwingWorker<List<Note>, Void>() {
                @Override
                protected List<Note> doInBackground() throws Exception {
                    return noteDAO.getAllNotes();
                }

                @Override
                protected void done() {
                    try {
                        notes = get();
                        for (Note n : notes) {
                            gridPanel.add(createNoteCard(n));
                        }
                        gridPanel.revalidate();
                        gridPanel.repaint();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.execute();
        }

        private JPanel createNoteCard(Note note) {
            JPanel card = new JPanel(new BorderLayout());
            card.setPreferredSize(new Dimension(200, 150));
            try {
                card.setBackground(Color.decode(note.getBackgroundColor()));
            } catch (Exception e) {
                card.setBackground(new Color(30, 30, 30));
            }
            card.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            // Header panel for Title and Delete button
            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setOpaque(false);

            JLabel title = new JLabel(note.getTitle());
            title.setFont(new Font(note.getFontFamily(), Font.BOLD, 16));
            title.setForeground(getContrastColor(card.getBackground()));
            headerPanel.add(title, BorderLayout.CENTER);

            JButton deleteBtn = new JButton("×");
            deleteBtn.setMargin(new Insets(0,0,0,0));
            deleteBtn.setContentAreaFilled(false);
            deleteBtn.setBorderPainted(false);
            deleteBtn.setFocusPainted(false);
            deleteBtn.setForeground(getContrastColor(card.getBackground()));
            deleteBtn.setFont(new Font("Arial", Font.BOLD, 20));
            deleteBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            deleteBtn.setToolTipText("Delete Note");
            deleteBtn.addActionListener(e -> {
                int choice = JOptionPane.showConfirmDialog(NotesApp.this, "Delete this note?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() {
                            noteDAO.deleteNote(note.getId());
                            return null;
                        }
                        @Override
                        protected void done() {
                            refreshNotes();
                        }
                    }.execute();
                }
            });
            headerPanel.add(deleteBtn, BorderLayout.EAST);
            
            JTextArea preview = new JTextArea(note.getContent());
            preview.setFont(new Font(note.getFontFamily(), Font.PLAIN, 12));
            preview.setForeground(getContrastColor(card.getBackground()));
            preview.setOpaque(false);
            preview.setEditable(false);
            preview.setLineWrap(true);
            preview.setWrapStyleWord(true);
            if (preview.getText().length() > 100) preview.setText(preview.getText().substring(0, 100) + "...");

            card.add(headerPanel, BorderLayout.NORTH);
            card.add(preview, BorderLayout.CENTER);
            
            // Click listener to open note
            card.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showEditor(note);
                }
            });
            // Add to children too so clicking text works
            preview.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showEditor(note);
                }
            });

            return card;
        }
        
        private Color getContrastColor(Color color) {
            double y = (299 * color.getRed() + 587 * color.getGreen() + 114 * color.getBlue()) / 1000;
            return y >= 128 ? Color.BLACK : Color.WHITE;
        }
    }

    private void showNewNoteDialog() {
        JDialog dialog = new JDialog(this, "New Note", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(new Color(30, 30, 30));

        JPanel form = new JPanel(new GridLayout(0, 1, 10, 10));
        form.setBackground(new Color(30, 30, 30));
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel l1 = new JLabel("Choose Background:");
        l1.setForeground(Color.WHITE);
        JComboBox<String> colorCombo = new JComboBox<>(new String[]{"Dark (#121212)", "Red (#5c1a1a)", "Blue (#1a2f5c)", "Green (#1a5c2f)", "Purple (#4a1a5c)"});
        
        JLabel l2 = new JLabel("Choose Font Style:");
        l2.setForeground(Color.WHITE);
        JComboBox<String> fontCombo = new JComboBox<>(new String[]{"Arial", "Times New Roman", "Courier New", "Segoe UI", "Verdana"});

        form.add(l1);
        form.add(colorCombo);
        form.add(l2);
        form.add(fontCombo);

        JButton createBtn = new ModernButton("Create Note", new Color(100, 149, 237), new Color(120, 169, 255));
        createBtn.addActionListener(e -> {
            String bgSelection = (String) colorCombo.getSelectedItem();
            String bgHex = bgSelection.substring(bgSelection.indexOf("(") + 1, bgSelection.indexOf(")"));
            String font = (String) fontCombo.getSelectedItem();
            
            Note newNote = new Note(0, "New Note", "", new Date(), bgHex, font);
            // Save immediately to DB so it has an ID? Or just pass to editor and save later?
            // Let's pass to editor as a "new" note but with these presets.
            // Actually, Editor needs to know if it's new.
            
            dialog.dispose();
            showEditor(newNote);
        });

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(createBtn, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // --- Editor Panel ---
    private class EditorPanel extends JPanel {
        private JTextField titleField;
        private JTextArea textArea;
        private Note currentNote;
        private boolean isNew;
        
        // Drawing components
        private DrawingPanel drawingPanel;
        private JPanel contentContainer;
        private CardLayout contentLayout;
        private Tool currentTool = Tool.PEN;
        private int brushSize = 5;
        
        private enum Tool { PEN, ERASER, FILL }

        public EditorPanel() {
            setLayout(new BorderLayout());
            
            // Top Bar
            JPanel topBar = new JPanel(new BorderLayout());
            topBar.setOpaque(false);
            topBar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            JButton backBtn = new ModernButton("← Back", new Color(60, 60, 60), new Color(80, 80, 80));
            backBtn.addActionListener(e -> {
                saveNote(); // Auto-save on back
                showHome();
            });
            
            JButton saveBtn = new ModernButton("Save", new Color(60, 60, 60), new Color(80, 80, 80));
            saveBtn.addActionListener(e -> {
                saveNote();
                JOptionPane.showMessageDialog(this, "Saved!");
            });

            JButton drawToggleBtn = new ModernButton("Draw", new Color(60, 60, 60), new Color(80, 80, 80));
            drawToggleBtn.addActionListener(e -> {
                CardLayout cl = (CardLayout) contentContainer.getLayout();
                cl.next(contentContainer);
            });

            JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            rightButtons.setOpaque(false);
            rightButtons.add(drawToggleBtn);
            rightButtons.add(saveBtn);

            topBar.add(backBtn, BorderLayout.WEST);
            topBar.add(rightButtons, BorderLayout.EAST);
            
            add(topBar, BorderLayout.NORTH);

            // Content Container (CardLayout for Text vs Draw)
            contentLayout = new CardLayout();
            contentContainer = new JPanel(contentLayout);
            contentContainer.setOpaque(false);
            contentContainer.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

            // 1. Text View
            JPanel textPanel = new JPanel(new BorderLayout());
            textPanel.setOpaque(false);

            titleField = new JTextField();
            titleField.setBorder(null);
            titleField.setOpaque(false);
            titleField.setForeground(Color.WHITE);
            titleField.setFont(new Font("Segoe UI", Font.BOLD, 24));
            
            textArea = new JTextArea();
            textArea.setBorder(null);
            textArea.setOpaque(false);
            textArea.setForeground(Color.WHITE);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            
            JScrollPane scroll = new JScrollPane(textArea);
            scroll.setBorder(null);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.getVerticalScrollBar().setUI(new ModernScrollBarUI());

            textPanel.add(titleField, BorderLayout.NORTH);
            textPanel.add(scroll, BorderLayout.CENTER);
            
            // 2. Draw View
            drawingPanel = new DrawingPanel();
            
            contentContainer.add(textPanel, "TEXT");
            contentContainer.add(drawingPanel, "DRAW");
            
            add(contentContainer, BorderLayout.CENTER);
        }

        public void setNote(Note note) {
            this.currentNote = note;
            this.isNew = (note.getId() == 0);
            
            titleField.setText(note.getTitle());
            textArea.setText(note.getContent());
            
            // Apply styles
            try {
                Color bg = Color.decode(note.getBackgroundColor());
                setBackground(bg);
                // Adjust text color based on background
                Color fg = getContrastColor(bg);
                titleField.setForeground(fg);
                titleField.setCaretColor(fg);
                textArea.setForeground(fg);
                textArea.setCaretColor(fg);
            } catch (Exception e) {
                setBackground(new Color(18, 18, 18));
            }
            
            Font f = new Font(note.getFontFamily(), Font.PLAIN, 16);
            textArea.setFont(f);
            titleField.setFont(new Font(note.getFontFamily(), Font.BOLD, 24));
            
            // Reset drawing for new note (or load if we supported it)
            drawingPanel.clear();
        }
        
        private void saveNote() {
            if (currentNote == null) return;
            
            String title = titleField.getText();
            if (title.trim().isEmpty()) title = "Untitled";
            
            currentNote.setTitle(title);
            currentNote.setContent(textArea.getText());
            currentNote.setLastModified(new Date());
            
            if (isNew) {
                noteDAO.addNote(currentNote);
                isNew = false; 
            } else {
                noteDAO.updateNote(currentNote);
            }
        }
        
        private Color getContrastColor(Color color) {
            double y = (299 * color.getRed() + 587 * color.getGreen() + 114 * color.getBlue()) / 1000;
            return y >= 128 ? Color.BLACK : Color.WHITE;
        }

        // --- DrawingPanel inner class ---
        private class DrawingPanel extends JPanel {
            private BufferedImage canvas;
            private Color currentColor = Color.BLACK;
            private int prevX = -1, prevY = -1;

            public DrawingPanel() {
                setLayout(new BorderLayout());
                setBackground(Color.WHITE);
                
                // --- Top Control Bar (Close & Save) ---
                JPanel topControlBar = new JPanel(new BorderLayout());
                topControlBar.setOpaque(false);
                topControlBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

                JButton closeBtn = new JButton("Close");
                closeBtn.addActionListener(e -> {
                    CardLayout cl = (CardLayout) contentContainer.getLayout();
                    cl.show(contentContainer, "TEXT");
                });

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
                topRight.add(closeBtn);
                
                topControlBar.add(topRight, BorderLayout.EAST);
                add(topControlBar, BorderLayout.NORTH);

                // --- Bottom Toolbar (Tools) ---
                JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
                toolbar.setBackground(new Color(240, 240, 240));
                
                JButton penBtn = new JButton("Pen");
                JButton eraserBtn = new JButton("Eraser");
                JButton fillBtn = new JButton("Fill");
                JButton colorBtn = new JButton("Color");
                JButton clearBtn = new JButton("Clear");
                
                JSlider sizeSlider = new JSlider(1, 50, brushSize);
                sizeSlider.setPreferredSize(new Dimension(100, 20));
                sizeSlider.addChangeListener(e -> brushSize = sizeSlider.getValue());

                penBtn.addActionListener(e -> currentTool = Tool.PEN);
                eraserBtn.addActionListener(e -> currentTool = Tool.ERASER);
                fillBtn.addActionListener(e -> currentTool = Tool.FILL);
                
                colorBtn.addActionListener(e -> {
                    Color newColor = JColorChooser.showDialog(this, "Choose Color", currentColor);
                    if (newColor != null) {
                        currentColor = newColor;
                        // Update button color or icon to show selection
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
                canvasPanel.setBackground(Color.WHITE);
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
                            if (currentTool == Tool.ERASER) g.setColor(Color.WHITE);
                            else g.setColor(currentColor);
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
                        canvasPanel.repaint();
                    }
                });
            }

            private void ensureCanvas(int w, int h) {
                if (canvas == null || canvas.getWidth() != w || canvas.getHeight() != h) {
                    BufferedImage newCanvas = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = newCanvas.createGraphics();
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, newCanvas.getWidth(), newCanvas.getHeight());
                    if (canvas != null) g.drawImage(canvas, 0, 0, null);
                    g.dispose();
                    canvas = newCanvas;
                }
            }

            public void clear() {
                if (canvas != null) {
                    Graphics2D g = canvas.createGraphics();
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
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



    // --- Custom UI Components ---

    // 1. Modern Button with Hover Animation
    private static class ModernButton extends JButton {
        private Color normalColor;
        private Color hoverColor;
        private Color pressedColor;
        private Color currentColor;
        private Timer hoverTimer;
        private float animationProgress = 0f; // 0.0 to 1.0

        public ModernButton(String text, Color normal, Color hover) {
            super(text);
            this.normalColor = normal;
            this.hoverColor = hover;
            this.pressedColor = hover.darker();
            this.currentColor = normal;
            
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    startAnimation(true);
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    startAnimation(false);
                }
                @Override
                public void mousePressed(MouseEvent e) {
                    currentColor = pressedColor;
                    repaint();
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (getBounds().contains(e.getPoint())) {
                        currentColor = hoverColor;
                        animationProgress = 1f;
                    } else {
                        currentColor = normalColor;
                        animationProgress = 0f;
                    }
                    repaint();
                }
            });
        }

        private void startAnimation(boolean entering) {
            if (hoverTimer != null && hoverTimer.isRunning()) hoverTimer.stop();
            
            hoverTimer = new Timer(10, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (entering) {
                        animationProgress += 0.1f;
                        if (animationProgress >= 1f) {
                            animationProgress = 1f;
                            hoverTimer.stop();
                        }
                    } else {
                        animationProgress -= 0.1f;
                        if (animationProgress <= 0f) {
                            animationProgress = 0f;
                            hoverTimer.stop();
                        }
                    }
                    currentColor = interpolateColor(normalColor, hoverColor, animationProgress);
                    repaint();
                }
            });
            hoverTimer.start();
        }

        private Color interpolateColor(Color c1, Color c2, float fraction) {
            int r = (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * fraction);
            int g = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * fraction);
            int b = (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * fraction);
            return new Color(r, g, b);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2.setColor(currentColor);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
            
            super.paintComponent(g);
            g2.dispose();
        }
    }

    // 2. Modern ScrollBar UI
    private static class ModernScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = new Color(80, 80, 80);
            this.trackColor = new Color(30, 30, 30);
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
            g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2, thumbBounds.width - 4, thumbBounds.height - 4, 8, 8);
            g2.dispose();
        }
        
        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(trackColor);
            g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        // Run the GUI on the Event Dispatch Thread for thread safety
        SwingUtilities.invokeLater(() -> new NotesApp().setVisible(true));
    }
}
