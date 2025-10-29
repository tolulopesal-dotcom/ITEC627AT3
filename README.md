
This is a JavaFX-based desktop application designed to manage media files, notes, and study tasks. Its goal is to help users organize, search, and interact with their collections easily through a modern, responsive interface.

**Features Checklist**
Collection Management
    - Add new items (title, category, rating, tags, etc.).
    - Import folders with media files automatically.
    - Edit, rate, and tag items.
    - Undo functionality for recent changes.
    - Save/load library for persistence.
    - Auto-backup on exit.

Item Metadata
    - Title, category, tags, and star ratings.
    - File path or URL association.
    - Description field for detailed notes.
    - Media preview for audio/video.

Search & Filtering
    - Real-time search across titles, tags, and descriptions.
    - Clear/reset button for filters.
    - Keyword-based and tag-specific queries.

Recently Viewed
    - Tracks recently accessed items.
    - Quick access to important files.

Task Management
    - Task queue for study-related items.
    - Optional deadlines.
    - Process tasks sequentially with "Start Next" functionality.

Responsiveness
    - Adaptive layout for different screen sizes.
    - Minimum window width: 900px.
    - Auto-scroll for overflow content.

**Build and Run Steps**
**Prerequisites:**
Java Development Kit (JDK): Version 17 or higher.
Maven: Version 3.x or higher.

**How to Run**
------------------------------------------------------------
Option 1 — From Terminal:
    mvn clean javafx:run

Option 2 — From IntelliJ IDEA:
1. Open the project folder in IntelliJ.
2. Wait for Maven to index dependencies.
3. Run → Maven → Plugins → javafx → javafx:run
4. The application window will open.

**Maven Plugins**:
JavaFX Maven Plugin: Version 0.0.8

**Known Issues**
Limited Multi-User Support:
The application is designed for single-user environments.
Media Codec Compatibility:
Only supports media formats compatible with JavaFX MediaPlayer.
Backup Location:
Exits automatically save backups to the user's home directory without customization. .scol

**Future Enhancements**
Dark mode theme.
Cloud sync for collections and tasks.
Internationalization (multi-language support).
Advanced search filters.
Drag-and-drop imports.
