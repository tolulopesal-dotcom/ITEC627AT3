// src/main/java/org/example/MainController.java
package org.example;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MainController
 *
 * Handles:
 * - Library list + CRUD (New, Import, Undo)
 * - Edit mode / Save Changes for selected item
 * - Search
 * - Recently viewed stack
 * - Task queue (PriorityQueue)
 * - Media preview/play/pause
 * - Persistence load/save
 * - Small animations (fade on refresh)
 *
 * UI expectations:
 * - main-view.fxml references fx:controller="org.example.MainController"
 * - FXML defines buttons with ids and hooks matching these @FXML methods
 */
public class MainController {

    // ====== FXML: Details / form ======
    @FXML private TextField titleField;
    @FXML private ComboBox<ItemCategory> categoryBox;
    @FXML private TextField tagsField;
    @FXML private Slider ratingSlider;
    @FXML private Label ratingValueLabel;
    @FXML private TextField pathField;
    @FXML private TextArea descField;
    @FXML private MediaView mediaView;

    // ====== FXML: Lists / status / tasks / controls ======
    @FXML private TextField searchField;
    @FXML private ListView<Item> libraryList;
    @FXML private ListView<String> recentList;
    @FXML private ListView<String> taskList;
    @FXML private DatePicker taskDueDatePicker;
    @FXML private Label statusLabel;

    @FXML private Button editButton;
    @FXML private Button saveChangesButton;
    @FXML private Button browseButton;
    @FXML private Button playButton;
    @FXML private Button pauseButton;

    // ====== Services / state ======
    private final LibraryRepository repo = new LibraryRepository();
    private final IndexService index = new IndexService();
    private final SearchService searcher = new SearchService();
    private final PersistenceService persist = new PersistenceService();
    private final MediaService media = new MediaService();

    private Item editingOriginal = null;
    private boolean editMode = false;

    /**
     * So App.stop() can get repo to save backup.
     */
    public LibraryRepository getRepository() {
        return repo;
    }

    // ====== Init ======
    @FXML
    public void initialize() {
        // Try to load backup automatically so Collection isn't empty every run
        try {
            File backupFile = Path.of(
                    System.getProperty("user.home"),
                    "smart-collections-backup.scol"
            ).toFile();
            if (backupFile.exists()) {
                persist.loadFrom(backupFile, repo);
            }
        } catch (Exception e) {
            System.err.println("Startup load failed: " + e.getMessage());
            setStatus("Could not load previous library.");
        }

        categoryBox.getItems().setAll(ItemCategory.values());

        ratingSlider.valueProperty().addListener((obs, ov, nv) ->
                ratingValueLabel.setText(String.format("★ %.1f", nv.doubleValue()))
        );

        libraryList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Item it, boolean empty) {
                super.updateItem(it, empty);
                setText(empty || it == null ? null : it.toString());
            }
        });

        libraryList.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSel, newSel) -> {
                    if (newSel != null) {
                        showItemReadonly(newSel);
                        pushRecent(newSel);
                        setStatus("Selected: " + newSel.getTitle());
                    }
                }
        );

        setEditMode(false);

        index.reindex(repo);
        refreshAllListsAnimated();
        setStatus("Ready");
    }

    // ====== Mode helpers ======
    private void setEditMode(boolean enabled) {
        editMode = enabled;

        titleField.setDisable(!enabled);
        categoryBox.setDisable(!enabled);
        tagsField.setDisable(!enabled);
        ratingSlider.setDisable(!enabled);
        pathField.setDisable(!enabled);
        descField.setDisable(!enabled);
        browseButton.setDisable(!enabled);

        editButton.setDisable(enabled);
        saveChangesButton.setDisable(!enabled);
    }

    private void setStatus(String s) {
        statusLabel.setText(s);
    }

    // ====== Recently viewed stack ======
    private void pushRecent(Item it) {
        repo.recentlyViewed().push(it.getId());
        refreshRecentList();
    }

    // ====== Deep copy (for undo + cancel) ======
    private Item deepCopy(Item src) {
        Item c = new Item(src.getTitle());
        c.setId(src.getId());
        c.setCategory(src.getCategory());
        c.setTags(new ArrayList<>(src.getTags()));
        c.setRating(src.getRating());
        c.setPathOrUrl(src.getPathOrUrl());
        c.setDescription(src.getDescription());
        c.setCreatedAt(src.getCreatedAt());
        c.setMediaKind(src.getMediaKind());
        return c;
    }

    // ====== Display item in read-only mode ======
    private void showItemReadonly(Item it) {
        titleField.setText(it.getTitle());
        categoryBox.setValue(it.getCategory());
        tagsField.setText(String.join(", ", it.getTags()));
        ratingSlider.setValue(it.getRating());
        ratingValueLabel.setText("★ " + it.getRating() + ".0");
        pathField.setText(it.getPathOrUrl());
        descField.setText(it.getDescription());

        if (it.getMediaKind() == MediaKind.AUDIO || it.getMediaKind() == MediaKind.VIDEO) {
            bindMediaPreview(it.getPathOrUrl());
            playButton.setDisable(false);
            pauseButton.setDisable(false);
        } else {
            clearMediaPreview();
            playButton.setDisable(true);
            pauseButton.setDisable(true);
        }

        editingOriginal = null;
        setEditMode(false);
    }

    // ====== Edit button ======
    @FXML
    private void onEdit(ActionEvent e) {
        Item sel = libraryList.getSelectionModel().getSelectedItem();
        if (sel == null) {
            setStatus("Nothing selected to edit.");
            return;
        }

        editingOriginal = deepCopy(sel);
        setEditMode(true);
        setStatus("Editing: " + sel.getTitle());
    }

    // ====== Save Changes button ======
    @FXML
    private void onSaveChanges(ActionEvent e) {
        Item sel = libraryList.getSelectionModel().getSelectedItem();
        if (!editMode || sel == null || editingOriginal == null) {
            setStatus("No pending edits.");
            return;
        }

        // push undo snapshot BEFORE applying changes
        repo.undoStack().push(new Memento(editingOriginal, false));

        // commit UI -> model
        String newTitle = titleField.getText().trim();
        sel.setTitle(newTitle.isEmpty() ? "Untitled" : newTitle);

        sel.setCategory(categoryBox.getValue());
        sel.setTags(parseTags(tagsField.getText()));
        sel.setRating((int)Math.round(ratingSlider.getValue()));
        sel.setPathOrUrl(pathField.getText().trim());
        sel.setDescription(descField.getText().trim());

        index.reindex(repo);
        refreshAllListsAnimated();

        setEditMode(false);
        editingOriginal = null;
        setStatus("Saved.");
    }

    // ====== Browse path ======
    @FXML
    private void onBrowsePath(ActionEvent e) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose File or Media");
        File chosen = chooser.showOpenDialog(pathField.getScene().getWindow());
        if (chosen != null) {
            pathField.setText(chosen.getAbsolutePath());
            setStatus("Path selected.");
        }
    }

    // ====== Media preview ======
    private void bindMediaPreview(String pathOrUrl) {
        try {
            media.prepare(pathOrUrl);
            media.apply(mediaView);
        } catch (Exception ex) {
            clearMediaPreview();
            setStatus("Media error: " + ex.getMessage());
        }
    }

    private void clearMediaPreview() {
        MediaPlayer mp = mediaView.getMediaPlayer();
        if (mp != null) mp.stop();
        mediaView.setMediaPlayer(null);
    }

    @FXML
    private void onPlayMedia(ActionEvent e) {
        MediaPlayer mp = mediaView.getMediaPlayer();
        if (mp != null) mp.play();
    }

    @FXML
    private void onPauseMedia(ActionEvent e) {
        MediaPlayer mp = mediaView.getMediaPlayer();
        if (mp != null) mp.pause();
    }

    // ====== Task queue ======
    @FXML
    private void onAddTask(ActionEvent e) {
        Item sel = libraryList.getSelectionModel().getSelectedItem();
        String baseTitle = sel != null ? sel.getTitle() : "Study task";
        LocalDate due = taskDueDatePicker.getValue();

        TodoTask task = new TodoTask(baseTitle, Instant.now(), due);
        repo.taskQueue().offer(task);

        refreshTaskList();
        setStatus("Task added.");
    }

    @FXML
    private void onProcessNextTask(ActionEvent e) {
        TodoTask next = repo.taskQueue().poll();
        if (next == null) {
            setStatus("No tasks.");
        } else {
            setStatus("Next: " + next.formatForList());
        }
        refreshTaskList();
    }

    // ====== Search ======
    @FXML
    private void onSearchClick(ActionEvent e) {
        String q = searchField.getText();
        if (q == null || q.isBlank()) {
            refreshLibraryList();
            setStatus("Search cleared.");
            return;
        }

        List<ItemId> ranked = searcher.rankedSearch(repo, q);
        List<Item> hits = new ArrayList<>();
        for (ItemId id : ranked) {
            repo.find(id).ifPresent(hits::add);
        }

        libraryList.getItems().setAll(hits);
        fadeNode(libraryList);
        setStatus("Found " + hits.size() + " result(s).");
    }

    @FXML
    private void onClearClick(ActionEvent e) {
        searchField.clear();
        refreshLibraryList();
        setStatus("Ready");
    }

    // ====== New / Import / Undo ======
    @FXML
    private void onNewItem(ActionEvent e) {
        Item it = Item.newBlank();
        repo.add(it);

        index.reindex(repo);
        refreshLibraryList();

        libraryList.getSelectionModel().select(it);
        libraryList.scrollTo(it);
        showItemReadonly(it);

        setStatus("New item created.");
    }

    @FXML
    private void onImportFolder(ActionEvent e) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose Folder to Import");
        File dir = chooser.showDialog(searchField.getScene().getWindow());
        if (dir == null) {
            setStatus("Import cancelled.");
            return;
        }

        int added = importRecursive(dir);
        index.reindex(repo);
        refreshAllListsAnimated();
        setStatus("Imported " + added + " file(s).");
    }

    @FXML
    private void onUndo(ActionEvent e) {
        if (repo.undoStack().isEmpty()) {
            setStatus("Undo stack empty.");
            return;
        }

        Memento m = repo.undoStack().pop();
        repo.apply(m);

        index.reindex(repo);
        refreshAllListsAnimated();
        setStatus("Undo applied.");
    }

    // ====== Persistence menu ======
    @FXML
    private void onSaveLibrary(ActionEvent e) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Library");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Smart Collections (*.scol)", "*.scol"));
        File out = chooser.showSaveDialog(searchField.getScene().getWindow());
        if (out == null) return;

        try {
            persist.saveTo(out, repo);
            setStatus("Saved to " + out.getName());
        } catch (Exception ex) {
            setStatus("Save failed: " + ex.getMessage());
        }
    }

    @FXML
    private void onLoadLibrary(ActionEvent e) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load Library");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Smart Collections (*.scol)", "*.scol"));
        File in = chooser.showOpenDialog(searchField.getScene().getWindow());
        if (in == null) return;

        try {
            persist.loadFrom(in, repo);
            index.reindex(repo);
            refreshAllListsAnimated();
            setStatus("Loaded " + in.getName());
        } catch (Exception ex) {
            setStatus("Load failed: " + ex.getMessage());
        }
    }

    // ====== Recursive import helper ======
    private int importRecursive(File base) {
        Set<String> knownPaths = repo.all().stream()
                .map(Item::getPathOrUrl)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<String> allowed = List.of(".txt", ".md", ".pdf", ".mp3", ".mp4");
        int[] count = {0};

        Deque<File> stack = new ArrayDeque<>();
        stack.push(base);

        while (!stack.isEmpty()) {
            File f = stack.pop();
            if (f.isDirectory()) {
                File[] kids = f.listFiles();
                if (kids != null) {
                    for (File k : kids) stack.push(k);
                }
            } else {
                String nameLower = f.getName().toLowerCase(Locale.ROOT);
                boolean ok = allowed.stream().anyMatch(nameLower::endsWith);
                if (!ok) continue;

                String abs = f.getAbsolutePath();
                if (knownPaths.contains(abs)) continue;

                Item it = new Item(f.getName());
                it.setPathOrUrl(abs);
                it.setCreatedAt(Instant.now());

                if (nameLower.endsWith(".mp3")) {
                    it.setCategory(ItemCategory.AUDIO);
                    it.setMediaKind(MediaKind.AUDIO);
                } else if (nameLower.endsWith(".mp4")) {
                    it.setCategory(ItemCategory.VIDEO);
                    it.setMediaKind(MediaKind.VIDEO);
                } else if (nameLower.endsWith(".pdf")) {
                    it.setCategory(ItemCategory.DOCUMENT);
                    it.setMediaKind(MediaKind.OTHER);
                } else {
                    it.setCategory(ItemCategory.NOTE);
                    it.setMediaKind(MediaKind.OTHER);
                }

                repo.add(it);
                knownPaths.add(abs);
                count[0]++;
            }
        }
        return count[0];
    }

    // ====== Refresh helpers / animation ======
    private void refreshAllListsAnimated() {
        refreshLibraryList();
        refreshRecentList();
        refreshTaskList();
        fadeNode(libraryList);
    }

    private void refreshLibraryList() {
        libraryList.getItems().setAll(new ArrayList<>(repo.all()));
    }

    private void refreshRecentList() {
        List<String> lines = repo.recentlyViewed().stream()
                .map(id -> repo.find(id).orElse(null))
                .filter(Objects::nonNull)
                .map(Item::toString)
                .toList();
        recentList.getItems().setAll(lines);
    }

    private void refreshTaskList() {
        var snapshot = repo.viewAllTasksByPriority();
        var formatted = snapshot.stream().map(TodoTask::formatForList).toList();
        taskList.getItems().setAll(formatted);
    }

    private void fadeNode(javafx.scene.Node n) {
        FadeTransition ft = new FadeTransition(Duration.millis(200), n);
        ft.setFromValue(0.3);
        ft.setToValue(1.0);
        ft.play();
    }

    private List<String> parseTags(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
