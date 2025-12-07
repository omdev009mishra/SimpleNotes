# Simple Notes App

A modern, feature-rich desktop note-taking application built with Java Swing. This application allows users to create, edit, style, and organize notes, as well as create drawings.

## Features

*   **Create & Edit Notes**: Simple and intuitive text editor.
*   **Rich Styling**: Customize note background colors and font styles.
*   **Drawing Mode**: Switch to a drawing canvas to sketch ideas.
    *   **Tools**: Pen, Eraser, Fill Bucket.
    *   **Customization**: Adjustable brush size and color picker.
    *   **Save**: Export drawings as PNG images.
*   **Modern UI**: Dark theme with custom Swing components (ModernButton, ModernScrollBar).
*   **Persistence**: All notes are stored locally using SQLite.
*   **Grid View**: View all your notes in a responsive grid layout.

## Prerequisites

*   **Java Development Kit (JDK)**: Version 8 or higher.
*   **SQLite JDBC Driver**: Included in the `lib/` directory (`sqlite-jdbc-3.42.0.0.jar`).

## Project Structure

```
SimpleNotes/
├── bin/                # Compiled class files
├── lib/                # External libraries (SQLite JDBC)
├── src/                # Source code
│   ├── DatabaseManager.java
│   ├── Note.java
│   ├── NoteDAO.java
│   └── NotesApp.java
├── notes.db            # SQLite database file (created on first run)
└── README.md
```

## How to Run

1.  **Compile the project**:
    Open a terminal in the project root directory and run:
    ```powershell
    javac -cp "lib/sqlite-jdbc-3.42.0.0.jar" -d bin src/*.java
    ```

2.  **Run the application**:
    ```powershell
    java -cp "bin;lib/sqlite-jdbc-3.42.0.0.jar" NotesApp
    ```

## Usage

1.  **Home Screen**: Click the `+` button to create a new note.
2.  **New Note**: Select a background color and font style.
3.  **Editor**:
    *   Type your note in the text area.
    *   Click **Draw** to switch to the drawing canvas.
    *   Use the drawing tools to sketch. You can save your drawing or just use it for scratchpad.
    *   Click **Save** to persist your changes to the database.
    *   Click **Back** to return to the home screen (auto-saves).
4.  **Delete**: Click the `×` icon on a note card in the home screen to delete it.

## Technologies Used

*   Java Swing (GUI)
*   SQLite (Database)
*   JDBC (Database Connectivity)
