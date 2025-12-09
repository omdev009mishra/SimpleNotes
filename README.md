# NoteSphere

A modern, feature-rich note-taking application built with Java Swing. NoteSphere provides an elegant dark/light theme interface for organizing your thoughts, ideas, and work notes.

## ✨ Features

- 📝 **Rich Text Editing** - Create and edit notes with a clean, distraction-free interface
- 🎨 **Drawing Canvas** - Switch to drawing mode with pen, eraser, and fill tools
- 📂 **Organized Notebooks** - Categorize notes into Personal, Work, and Ideas
- 🌓 **Theme Switching** - Toggle between dark and light modes
- 🔍 **Search Functionality** - Quickly find notes by title or content
- 📊 **Live Statistics** - Real-time character count and last modified date
- 💾 **Auto-save** - Your notes are automatically saved to a local SQLite database

## 🚀 Quick Start

### Prerequisites
- Java Development Kit (JDK) 11 or higher
- PowerShell (Windows) or terminal access

### Installation & Running

#### Option 1: Using the Build Scripts (Recommended)

1. **Download SQLite JDBC Driver:**
```powershell
mkdir lib
Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.42.0.0/sqlite-jdbc-3.42.0.0.jar" -OutFile .\lib\sqlite-jdbc-3.42.0.0.jar
```

2. **Build the application:**
```powershell
.\build_app.bat
```

3. **Run the application:**
```powershell
.\run_app.bat
```

Or simply double-click `SimpleNotes.jar` after building.

#### Option 2: Manual Compilation

```powershell
# Navigate to the source directory
cd .\SimpleNotes\src

# Compile all Java files
javac -cp ".;..\..\lib\sqlite-jdbc-3.42.0.0.jar" *.java

# Run the application
java -cp ".;..\..\lib\sqlite-jdbc-3.42.0.0.jar" NotesApp
```

## 📖 Usage

### Creating Notes
- Click the **+** button in the middle panel or when no note is selected
- Choose a notebook category (Personal, Work, or Ideas)
- Start typing your note

### Organizing Notes
- Use the sidebar to switch between notebook categories
- Notes are automatically filtered by the selected category
- Use the search bar to find specific notes

### Drawing
- Click the **Draw** button to switch to canvas mode
- Use the toolbar to select pen, eraser, or fill tools
- Adjust brush size with the slider
- Save your drawing as a PNG image

### Settings
- Click **Settings** in the sidebar
- Toggle Dark Mode on/off
- Changes apply immediately

## 🏗️ Project Structure

```
SimpleNotes/
├── lib/                          # Dependencies
│   └── sqlite-jdbc-3.42.0.0.jar
├── SimpleNotes/
│   └── src/                      # Source code
│       ├── NotesApp.java         # Main application
│       ├── DatabaseManager.java  # SQLite database handler
│       ├── Note.java             # Note model
│       └── NoteDAO.java          # Database operations interface
├── build_app.bat                 # Build script
├── run_app.bat                   # Run script
└── README.md
```

## 🛠️ Technologies

- **Java Swing** - GUI framework
- **SQLite** - Local database storage
- **JDBC** - Database connectivity

## 📝 License

This project is open source and available for educational purposes.

## 🤝 Contributing

Feel free to fork this project and submit pull requests for any improvements!

---

Built by omdev009mishra and rituvashishth
