package world.cals.supercollidersnippetmanager;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.stage.FileChooser;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.transport.OSCPortOut;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// ... existing imports ...

public class MainController {

    @FXML private TextField searchField;

    @FXML private TreeView<String> folderTree;
    @FXML private ListView<String> tagList;

    @FXML private ListView<Snippet> snippetList;
    @FXML private TextArea codeArea;

    @FXML private Label snippetTitle;
    @FXML private Label snippetMeta;

    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button copyButton;
    @FXML private Button duplicateButton;
    @FXML private Button exportButton;
    @FXML private Button clearFiltersButton;
    @FXML private Label filterLabel;
    @FXML private Button playButton;
    @FXML private Button stopButton;

    private final SnippetStore store = new JsonFileSnippetStore(AppPaths.dataDir());

    private static final DateTimeFormatter META_DT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    private String selectedTag = null;
    private String selectedFolder = null;
    private String searchText = "";

    @FXML
    private void initialize() {
        // Search field listener
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            searchText = newVal == null ? "" : newVal.trim();
            refreshSnippets();
        });
        snippetList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Snippet item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        snippetList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, snip) -> {
            boolean hasSelection = snip != null;
            editButton.setDisable(!hasSelection);
            deleteButton.setDisable(!hasSelection);
            copyButton.setDisable(!hasSelection);
            duplicateButton.setDisable(!hasSelection);
            exportButton.setDisable(!hasSelection);
            playButton.setDisable(!hasSelection);
            stopButton.setDisable(!hasSelection);

            if (!hasSelection) {
                snippetTitle.setText("No snippet selected");
                snippetMeta.setText("");
                codeArea.clear();
                return;
            }

            snippetTitle.setText(snip.getName());
            codeArea.setText(snip.getCode());

            String tags = snip.getTags().isEmpty() ? "(none)" : String.join(", ", snip.getTags());
            snippetMeta.setText(
                    "Folder: " + snip.getFolder()
                            + " | Tags: " + tags
                            + " | Created: " + META_DT.format(snip.getCreatedDate())
                            + " | Modified: " + META_DT.format(snip.getModifiedDate())
            );
        });

        folderTree.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                selectedFolder = buildFolderPath(newV);
            } else {
                selectedFolder = null;
            }
            refreshSnippets();
        });

        // Add context menu for folders
        folderTree.setContextMenu(createFolderContextMenu());
        folderTree.setOnContextMenuRequested(event -> {
            TreeItem<String> selected = folderTree.getSelectionModel().getSelectedItem();
            if (selected != null) {
                folderTree.getContextMenu().show(folderTree, event.getScreenX(), event.getScreenY());
            }
        });

        // Toggle tag selection on click
        tagList.setOnMouseClicked(event -> {
            String clickedTag = tagList.getSelectionModel().getSelectedItem();
            if (clickedTag != null) {
                if (clickedTag.equals(selectedTag)) {
                    // Clicking same tag - deselect
                    selectedTag = null;
                    tagList.getSelectionModel().clearSelection();
                } else {
                    // Clicking different tag - select it and clear folder selection
                    selectedTag = clickedTag;
                    selectedFolder = null;
                    folderTree.getSelectionModel().clearSelection();
                }
                refreshSnippets();
            }
        });

        refreshFolderTree();
        refreshSnippets();
        refreshTags();
    }

    @FXML
    private void onNewFolder() {
        TextInputDialog dialog = new TextInputDialog("synthesis");
        dialog.setTitle("New Folder");
        dialog.setHeaderText("Create a new folder under your snippets data directory");
        dialog.setContentText("Folder path (e.g. synthesis/effects):");

        Optional<String> folder = dialog.showAndWait()
                .map(String::trim)
                .filter(s -> !s.isBlank());

        if (folder.isEmpty()) return;

        try {
            store.createFolder(folder.get());
            refreshFolderTree();

            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Folder Created");
            ok.setHeaderText("Created folder");
            ok.setContentText(AppPaths.dataDir().resolve(folder.get()).toString());
            ok.showAndWait();
        } catch (IOException | RuntimeException e) {
            showError("Failed to create folder", e);
        }
    }

    private ContextMenu createFolderContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem createSubfolder = new MenuItem("New Subfolder...");
        createSubfolder.setOnAction(e -> onCreateSubfolder());

        MenuItem renameFolder = new MenuItem("Rename...");
        renameFolder.setOnAction(e -> onRenameFolder());

        MenuItem deleteFolder = new MenuItem("Delete Folder...");
        deleteFolder.setOnAction(e -> onDeleteFolder());

        MenuItem refreshItem = new MenuItem("Refresh");
        refreshItem.setOnAction(e -> refreshFolderTree());

        contextMenu.getItems().addAll(createSubfolder, renameFolder, new SeparatorMenuItem(), deleteFolder, new SeparatorMenuItem(), refreshItem);

        return contextMenu;
    }

    private void onCreateSubfolder() {
        TreeItem<String> selected = folderTree.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        String parentPath = buildFolderPath(selected);

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Subfolder");
        dialog.setHeaderText("Create subfolder in: " + parentPath);
        dialog.setContentText("Subfolder name:");

        Optional<String> result = dialog.showAndWait()
                .map(String::trim)
                .filter(s -> !s.isBlank());

        if (result.isEmpty()) return;

        try {
            String newPath = parentPath + "/" + result.get();
            store.createFolder(newPath);
            refreshFolderTree();

            // Expand parent to show new folder
            selected.setExpanded(true);
        } catch (IOException | RuntimeException e) {
            showError("Failed to create subfolder", e);
        }
    }

    private void onRenameFolder() {
        TreeItem<String> selected = folderTree.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        String oldPath = buildFolderPath(selected);

        TextInputDialog dialog = new TextInputDialog(selected.getValue());
        dialog.setTitle("Rename Folder");
        dialog.setHeaderText("Rename: " + oldPath);
        dialog.setContentText("New name:");

        Optional<String> result = dialog.showAndWait()
                .map(String::trim)
                .filter(s -> !s.isBlank());

        if (result.isEmpty()) return;

        try {
            // Build new path
            Path oldFullPath = AppPaths.dataDir().resolve(oldPath);
            Path parent = oldFullPath.getParent();
            Path newFullPath = parent.resolve(result.get());

            // Move folder
            Files.move(oldFullPath, newFullPath);

            // Update all snippets in this folder
            updateSnippetFolderPaths(oldPath, oldPath.replace(selected.getValue(), result.get()));

            refreshFolderTree();
            refreshSnippets();
        } catch (IOException e) {
            showError("Failed to rename folder", e);
        }
    }

    private void onDeleteFolder() {
        TreeItem<String> selected = folderTree.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        String folderPath = buildFolderPath(selected);

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Folder");
        confirm.setHeaderText("Delete folder: " + folderPath);
        confirm.setContentText("This will delete the folder and all snippets inside it. Are you sure?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            Path fullPath = AppPaths.dataDir().resolve(folderPath);
            deleteDirectory(fullPath);
            refreshFolderTree();
            refreshSnippets();
            refreshTags();
        } catch (IOException e) {
            showError("Failed to delete folder", e);
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var stream = Files.list(path)) {
                stream.forEach(child -> {
                    try {
                        deleteDirectory(child);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
        Files.delete(path);
    }

    private void updateSnippetFolderPaths(String oldFolder, String newFolder) throws IOException {
        List<Snippet> allSnippets = store.loadAll();
        for (Snippet snippet : allSnippets) {
            if (snippet.getFolder().startsWith(oldFolder)) {
                String newPath = snippet.getFolder().replace(oldFolder, newFolder);
                Snippet updated = new Snippet(
                        snippet.getId(),
                        snippet.getName(),
                        snippet.getDescription(),
                        snippet.getCode(),
                        snippet.getTags(),
                        newPath,
                        snippet.getCreatedDate(),
                        snippet.getModifiedDate()
                );
                store.save(updated);
            }
        }
    }

    private void refreshFolderTree() {
        try {
            Path dataDir = AppPaths.dataDir();
            Files.createDirectories(dataDir);

            TreeItem<String> root = new TreeItem<>("data");
            root.setExpanded(true);

            try (var stream = Files.walk(dataDir)) {
                stream
                        .filter(Files::isDirectory)
                        .filter(p -> !p.equals(dataDir))
                        .sorted(Comparator.comparing(Path::toString))
                        .forEach(dir -> addFolderPath(root, dataDir, dir));
            }

            folderTree.setRoot(root);
            folderTree.setShowRoot(false);
        } catch (IOException e) {
            showError("Failed to load folders", e);
        }
    }

    private void addFolderPath(TreeItem<String> root, Path base, Path dir) {
        Path rel = base.relativize(dir);
        TreeItem<String> current = root;

        for (Path part : rel) {
            String name = part.toString();

            TreeItem<String> next = null;
            for (TreeItem<String> child : current.getChildren()) {
                if (name.equals(child.getValue())) {
                    next = child;
                    break;
                }
            }

            if (next == null) {
                next = new TreeItem<>(name);
                current.getChildren().add(next);
                current.getChildren().sort(Comparator.comparing(TreeItem::getValue));
            }

            current = next;
        }
    }

    private void refreshSnippets() {
        try {
            List<Snippet> snippets = store.loadAll();

            // Apply search filter first (global)
            if (!searchText.isEmpty()) {
                String search = searchText.toLowerCase();
                snippets = snippets.stream()
                        .filter(s ->
                            s.getName().toLowerCase().contains(search) ||
                            s.getCode().toLowerCase().contains(search) ||
                            s.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(search)) ||
                            (s.getDescription() != null && s.getDescription().toLowerCase().contains(search))
                        )
                        .toList();
            }

            // Tag filter is global - search across all snippets
            if (selectedTag != null) {
                snippets = snippets.stream()
                        .filter(s -> s.getTags().contains(selectedTag))
                        .toList();
            } else if (selectedFolder != null) {
                // Only filter by folder if no tag is selected
                snippets = snippets.stream()
                        .filter(s -> s.getFolder().equals(selectedFolder))
                        .toList();
            }

            snippetList.getItems().setAll(snippets);
            updateFilterLabel();
        } catch (IOException e) {
            showError("Failed to load snippets", e);
        }
    }

    private void updateFilterLabel() {
        List<String> filters = new ArrayList<>();
        if (selectedFolder != null) {
            filters.add("Folder: " + selectedFolder);
        }
        if (selectedTag != null) {
            filters.add("Tag: " + selectedTag);
        }

        if (filters.isEmpty()) {
            filterLabel.setText("");
            clearFiltersButton.setVisible(false);
            clearFiltersButton.setManaged(false);
        } else {
            filterLabel.setText("Filtering by: " + String.join(" & ", filters));
            clearFiltersButton.setVisible(true);
            clearFiltersButton.setManaged(true);
        }
    }

    @FXML
    private void onClearFilters() {
        selectedFolder = null;
        selectedTag = null;
        folderTree.getSelectionModel().clearSelection();
        tagList.getSelectionModel().clearSelection();
        refreshSnippets();
    }

    private void refreshTags() {
        try {
            List<Snippet> snippets = store.loadAll();
            List<String> allTags = snippets.stream()
                    .flatMap(s -> s.getTags().stream())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            tagList.getItems().setAll(allTags);
        } catch (IOException e) {
            showError("Failed to load tags", e);
        }
    }

    private String buildFolderPath(TreeItem<String> item) {
        if (item == null || item.getParent() == null) return "";

        StringBuilder path = new StringBuilder(item.getValue());
        TreeItem<String> parent = item.getParent();

        while (parent != null && parent.getParent() != null) {
            path.insert(0, parent.getValue() + "/");
            parent = parent.getParent();
        }

        return path.toString();
    }

    @FXML
    private void onNewSnippet() {
        Optional<Snippet> created = showSnippetDialog(
                "New Snippet",
                "",
                "synthesis",
                "",
                List.of()
        ).map(draft -> Snippet.createNew(
                draft.name(),
                null,
                draft.code(),
                draft.tags(),
                draft.folder()
        ));

        if (created.isEmpty()) return;

        try {
            store.save(created.get());
            refreshSnippets();
            refreshTags();
            snippetList.getSelectionModel().select(created.get());
        } catch (IOException e) {
            showError("Failed to save snippet", e);
        }
    }

    @FXML
    private void onEditSnippet() {
        Snippet selected = snippetList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Optional<SnippetDraft> edited = showSnippetDialog(
                "Edit Snippet",
                selected.getName(),
                selected.getFolder(),
                selected.getCode(),
                selected.getTags()
        );

        if (edited.isEmpty()) return;

        Snippet updated = selected.withUpdatedContent(
                edited.get().name(),
                selected.getDescription(),
                edited.get().code(),
                edited.get().tags(),
                edited.get().folder()
        );

        try {
            store.save(updated);
            refreshSnippets();
            refreshTags();
            snippetList.getSelectionModel().select(updated);
        } catch (IOException e) {
            showError("Failed to update snippet", e);
        }
    }

    @FXML
    private void onDeleteSnippet() {
        Snippet selected = snippetList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Snippet");
        confirm.setHeaderText("Delete \"" + selected.getName() + "\"?");
        confirm.setContentText("This will remove the snippet JSON file from disk.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            store.delete(selected.getId(), selected.getFolder());
            refreshSnippets();
        } catch (IOException e) {
            showError("Failed to delete snippet", e);
        }
    }

    @FXML
    private void onCopySnippet() {
        Snippet snip = snippetList.getSelectionModel().getSelectedItem();
        if (snip == null) return;

        ClipboardContent content = new ClipboardContent();
        content.putString(snip.getCode());
        Clipboard.getSystemClipboard().setContent(content);
    }

    @FXML
    private void onDuplicateSnippet() {
        Snippet selected = snippetList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Snippet duplicate = Snippet.createNew(
                selected.getName() + " (Copy)",
                selected.getDescription(),
                selected.getCode(),
                selected.getTags(),
                selected.getFolder()
        );

        try {
            store.save(duplicate);
            refreshSnippets();
            refreshTags();
            snippetList.getSelectionModel().select(duplicate);
        } catch (IOException e) {
            showError("Failed to duplicate snippet", e);
        }
    }

    @FXML
    private void onExportSnippet() {
        Snippet selected = snippetList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Snippet");
        fileChooser.setInitialFileName(selected.getName() + ".json");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );

        java.io.File file = fileChooser.showSaveDialog(snippetList.getScene().getWindow());
        if (file == null) return;

        try {
            Path snippetPath = AppPaths.dataDir()
                    .resolve(selected.getFolder())
                    .resolve(selected.getId() + ".json");
            Files.copy(snippetPath, file.toPath(), StandardCopyOption.REPLACE_EXISTING);

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Export Successful");
            success.setHeaderText("Snippet exported");
            success.setContentText("Saved to: " + file.getAbsolutePath());
            success.showAndWait();
        } catch (IOException e) {
            showError("Failed to export snippet", e);
        }
    }

    @FXML
    private void onImportSnippet() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Snippet");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );

        java.io.File file = fileChooser.showOpenDialog(snippetList.getScene().getWindow());
        if (file == null) return;

        try {
            // Ask for folder to import into
            TextInputDialog folderDialog = new TextInputDialog("synthesis");
            folderDialog.setTitle("Import Snippet");
            folderDialog.setHeaderText("Choose destination folder");
            folderDialog.setContentText("Folder:");

            Optional<String> folder = folderDialog.showAndWait();
            if (folder.isEmpty()) return;

            // Create folder if it doesn't exist
            Path destFolder = AppPaths.dataDir().resolve(folder.get());
            Files.createDirectories(destFolder);

            // Read the snippet to get its ID
            String content = Files.readString(file.toPath());
            // Extract ID from JSON (simple approach)
            String id = content.substring(content.indexOf("\"id\":") + 7);
            id = id.substring(0, id.indexOf("\""));

            // Copy file to destination
            Path destPath = destFolder.resolve(id + ".json");
            Files.copy(file.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);

            refreshSnippets();
            refreshTags();
            refreshFolderTree();

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Import Successful");
            success.setHeaderText("Snippet imported");
            success.setContentText("Imported to folder: " + folder.get());
            success.showAndWait();
        } catch (IOException e) {
            showError("Failed to import snippet", e);
        }
    }

    @FXML
    private void onPlaySnippet() {
        Snippet selected = snippetList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        OSCPortOut oscSender = null;
        try {
            String code = selected.getCode();

            // Smart wrapper: detect if code defines SynthDef or Pdef and wrap appropriately
            String wrappedCode;
            if (code.contains("SynthDef(") || code.contains("SynthDef.new(")) {
                // If it's a SynthDef, wrap in fork, add/sync, then play
                String synthName = extractSynthDefName(code);

                // Check if code already has .add
                String defCode = code;
                if (code.contains(".add")) {
                    // Remove existing .add since we'll do it in the fork
                    defCode = code.replaceAll("\\.add;?", "");
                }

                wrappedCode = "fork {\n" +
                             "    " + defCode.trim() + ".add;\n" +
                             "    s.sync;\n" +
                             "    Synth(\\" + synthName + ");\n" +
                             "};";
            } else if (code.contains("Pdef(") || code.contains("Pbind(")) {
                // If it's a pattern, just add .play
                wrappedCode = "(\n" + code + "\n).play;";
            } else if (code.contains(".play")) {
                // Already has .play, use as-is
                wrappedCode = code;
            } else {
                // Default: wrap in parentheses and add .play
                wrappedCode = "(\n" + code + "\n).play;";
            }

            // Send OSC message to SuperCollider on port 57120 (NetAddr receiver)
            oscSender = new OSCPortOut(InetAddress.getByName("127.0.0.1"), 57120);
            OSCMessage message = new OSCMessage("/snippet/play", List.of(wrappedCode));

            oscSender.send(message);

            // Show feedback
            playButton.setText("▶ Playing...");
            playButton.setDisable(true);
            stopButton.setDisable(false);

            // Re-enable after a short delay
            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    javafx.application.Platform.runLater(() -> {
                        playButton.setText("▶ Play");
                        playButton.setDisable(false);
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            // Re-enable button on error
            playButton.setText("▶ Play");
            playButton.setDisable(false);

            e.printStackTrace(); // Log the actual error

            // Show setup instructions if connection fails
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Connection Error");
            alert.setHeaderText("Could not send to SuperCollider");
            alert.setContentText(
                "Error: " + e.getMessage() + "\n\n" +
                "Make sure you've run the setup code in SuperCollider.\n" +
                "Click 'Setup SC' button for instructions."
            );
            alert.getDialogPane().setPrefWidth(500);
            alert.showAndWait();
        } finally {
            if (oscSender != null) {
                try {
                    oscSender.close();
                } catch (Exception e) {
                    // Ignore close errors
                }
            }
        }
    }

    private String extractSynthDefName(String code) {
        // Extract SynthDef name from code like: SynthDef(\name, { ... })
        int start = code.indexOf("SynthDef(");
        if (start == -1) start = code.indexOf("SynthDef.new(");
        if (start == -1) return "default";

        start = code.indexOf("\\", start);
        if (start == -1) return "default";

        int end = code.indexOf(",", start);
        if (end == -1) end = code.indexOf(")", start);
        if (end == -1) return "default";

        return code.substring(start + 1, end).trim();
    }

    @FXML
    private void onSetupSuperCollider() {
        String setupCode =
            "(\n" +
            "// Make OSCdefs permanent (survive CmdPeriod)\n" +
            "OSCdef(\\snippetPlayer, { |msg|\n" +
            "    var code = msg[1].asString;\n" +
            "    code.interpret;\n" +
            "}, '/snippet/play').permanent_(true);\n" +
            "\n" +
            "OSCdef(\\snippetStop, {\n" +
            "    // Stop all patterns\n" +
            "    Pdef.all.do(_.stop);\n" +
            "    TempoClock.default.clear;\n" +
            "    \n" +
            "    // Free all synths\n" +
            "    Server.default.freeAll;\n" +
            "    \n" +
            "    \"All sounds stopped\".postln;\n" +
            "}, '/snippet/stop').permanent_(true);\n" +
            "\n" +
            "\"Snippet player ready! OSCdefs are permanent.\".postln;\n" +
            ")";

        // Copy to clipboard
        ClipboardContent content = new ClipboardContent();
        content.putString(setupCode);
        Clipboard.getSystemClipboard().setContent(content);

        // Show instructions
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("SuperCollider Setup");
        alert.setHeaderText("Setup code copied to clipboard!");
        alert.setContentText(
            "The setup code has been copied to your clipboard.\n\n" +
            "Steps:\n" +
            "1. Switch to SuperCollider IDE\n" +
            "2. Paste the code (Cmd+V)\n" +
            "3. Execute it (Cmd+Return)\n" +
            "4. Boot the server (Cmd+B) if not already running\n" +
            "5. Come back here and click Play on any snippet!\n\n" +
            "You only need to do this once per SuperCollider session."
        );
        alert.getDialogPane().setPrefWidth(500);
        alert.showAndWait();
    }

    @FXML
    private void onStopAll() {
        try {
            // Send stop command to SuperCollider via OSC
            OSCPortOut oscSender = new OSCPortOut(InetAddress.getByName("127.0.0.1"), 57120);
            OSCMessage message = new OSCMessage("/snippet/stop", List.of());

            oscSender.send(message);
            oscSender.close();

            // Visual feedback
            stopButton.setText("⏹ Stopped");
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    javafx.application.Platform.runLater(() -> {
                        stopButton.setText("⏹ Stop All");
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            showError("Failed to stop sounds", e);
        }
    }

    @FXML
    private void onSettings() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Settings");
        dialog.setHeaderText("Application Settings");

        // Create the settings UI
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // OSC Port setting
        Label oscPortLabel = new Label("OSC Port:");
        TextField oscPortField = new TextField("57120");
        oscPortField.setPromptText("Default: 57120");
        grid.add(oscPortLabel, 0, 0);
        grid.add(oscPortField, 1, 0);

        // SuperCollider path
        Label scPathLabel = new Label("SuperCollider Path:");
        TextField scPathField = new TextField("/Applications/SuperCollider.app/Contents/MacOS/sclang");
        scPathField.setPrefWidth(300);
        Button browseButton = new Button("Browse...");
        browseButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select SuperCollider Executable");
            fileChooser.setInitialDirectory(new java.io.File("/Applications"));
            java.io.File file = fileChooser.showOpenDialog(dialog.getOwner());
            if (file != null) {
                scPathField.setText(file.getAbsolutePath());
            }
        });
        HBox pathBox = new HBox(5, scPathField, browseButton);
        grid.add(scPathLabel, 0, 1);
        grid.add(pathBox, 1, 1);

        // Data directory
        Label dataDirLabel = new Label("Data Directory:");
        TextField dataDirField = new TextField(AppPaths.dataDir().toString());
        dataDirField.setPrefWidth(300);
        dataDirField.setEditable(false);
        Button openDataDirButton = new Button("Open Folder");
        openDataDirButton.setOnAction(e -> {
            try {
                java.awt.Desktop.getDesktop().open(AppPaths.dataDir().toFile());
            } catch (Exception ex) {
                showError("Failed to open folder", ex);
            }
        });
        HBox dataDirBox = new HBox(5, dataDirField, openDataDirButton);
        grid.add(dataDirLabel, 0, 2);
        grid.add(dataDirBox, 1, 2);

        // Auto-play option
        Label autoPlayLabel = new Label("Auto-play on select:");
        CheckBox autoPlayCheck = new CheckBox();
        autoPlayCheck.setSelected(false);
        grid.add(autoPlayLabel, 0, 3);
        grid.add(autoPlayCheck, 1, 3);

        // Max recent snippets
        Label maxRecentLabel = new Label("Max recent snippets:");
        Spinner<Integer> maxRecentSpinner = new Spinner<>(5, 50, 10, 5);
        maxRecentSpinner.setEditable(true);
        grid.add(maxRecentLabel, 0, 4);
        grid.add(maxRecentSpinner, 1, 4);

        // Add info section
        Label infoLabel = new Label("About:");
        TextArea infoArea = new TextArea();
        infoArea.setEditable(false);
        infoArea.setPrefRowCount(3);
        infoArea.setWrapText(true);
        infoArea.setText("SuperCollider Snippet Manager v1.0\n" +
                        "Data location: " + AppPaths.dataDir() + "\n" +
                        "Created with JavaFX and SuperCollider");
        grid.add(infoLabel, 0, 5);
        grid.add(infoArea, 1, 5);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Here you would save settings to preferences
                // For now, just show confirmation
                Alert confirm = new Alert(Alert.AlertType.INFORMATION);
                confirm.setTitle("Settings Saved");
                confirm.setHeaderText("Settings have been saved");
                confirm.setContentText("Note: Some settings may require restarting the application.");
                confirm.showAndWait();
            }
        });
    }

    private record SnippetDraft(String name, String folder, String code, List<String> tags) {
    }

    private Optional<SnippetDraft> showSnippetDialog(String title, String name, String folder, String code, List<String> tags) {
        Dialog<SnippetDraft> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField = new TextField(name);
        ComboBox<String> folderCombo = new ComboBox<>();
        folderCombo.setEditable(true);
        folderCombo.setValue(folder);

        // Populate with existing folders
        List<String> existingFolders = getAllFolders();
        folderCombo.getItems().addAll(existingFolders);

        TextField tagsField = new TextField(String.join(", ", tags));
        tagsField.setPromptText("tag1, tag2, tag3");

        TextArea codeField = new TextArea(code);
        codeField.setPrefRowCount(12);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        grid.addRow(0, new Label("Name:"), nameField);
        grid.addRow(1, new Label("Folder:"), folderCombo);
        grid.addRow(2, new Label("Tags:"), tagsField);
        grid.add(new Label("Code:"), 0, 3);
        grid.add(codeField, 0, 4, 2, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            String n = nameField.getText() == null ? "" : nameField.getText().trim();
            String f = folderCombo.getValue() == null ? "" : folderCombo.getValue().trim();
            String c = codeField.getText() == null ? "" : codeField.getText();
            String t = tagsField.getText() == null ? "" : tagsField.getText().trim();

            if (n.isBlank() || f.isBlank() || c.isBlank()) return null;

            List<String> tagList = t.isBlank() ? List.of() :
                    List.of(t.split(",")).stream()
                            .map(String::trim)
                            .filter(s -> !s.isBlank())
                            .collect(Collectors.toList());

            return new SnippetDraft(n, f, c, tagList);
        });

        return dialog.showAndWait();
    }

    private List<String> getAllFolders() {
        try {
            Path dataDir = AppPaths.dataDir();
            if (!Files.exists(dataDir)) {
                return new ArrayList<>();
            }

            try (var stream = Files.walk(dataDir)) {
                return stream
                        .filter(Files::isDirectory)
                        .filter(p -> !p.equals(dataDir))
                        .map(p -> dataDir.relativize(p).toString())
                        .sorted()
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private void showError(String title, Exception e) {
        e.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(title);
        a.setContentText(e.toString());
        a.showAndWait();
    }

    private void showError(String title, RuntimeException e) {
        e.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(title);
        a.setContentText(e.toString());
        a.showAndWait();
    }
}
