package minilinker.gui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import minilinker.linker.MiniLinker;
import minilinker.model.MiniObjectModule;
import minilinker.model.ObjectSection;
import minilinker.model.SymbolDefinition;
import minilinker.parser.ObjectFileParser;
import minilinker.util.HexUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MainApp extends Application {

    private final ObjectFileParser parser = new ObjectFileParser();
    private final List<MiniObjectModule> loadedModules = new ArrayList<>();

    private ListView<String> moduleListView;
    private TextArea detailsArea;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Mini Linker GUI");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Top Toolbar
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(0, 0, 10, 0));
        Button btnAdd = new Button("➕ Add Module");
        Button btnRemove = new Button("➖ Remove Selected");
        Button btnLink = new Button("🔗 Link Modules!");
        toolbar.getChildren().addAll(btnAdd, btnRemove, btnLink);
        root.setTop(toolbar);

        // Left Panel - Module List
        VBox leftPanel = new VBox(5);
        leftPanel.getChildren().add(new Label("Loaded Modules:"));
        moduleListView = new ListView<>();
        moduleListView.setPrefWidth(200);
        leftPanel.getChildren().add(moduleListView);
        root.setLeft(leftPanel);

        // Center Panel - Details
        VBox centerPanel = new VBox(5);
        centerPanel.setPadding(new Insets(0, 0, 0, 10));
        centerPanel.getChildren().add(new Label("Module Details / Logs:"));
        detailsArea = new TextArea();
        detailsArea.setEditable(false);
        detailsArea.setStyle("-fx-font-family: 'Consolas', monospace;");
        centerPanel.getChildren().add(detailsArea);
        root.setCenter(centerPanel);

        // Actions
        btnAdd.setOnAction(e -> addModule(primaryStage));
        btnRemove.setOnAction(e -> removeSelectedModule());
        btnLink.setOnAction(e -> linkModules(primaryStage));

        moduleListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> showModuleDetails(newVal)
        );

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();

        log("Mini Linker GUI started.");
    }

    private void addModule(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open MINIOBJ File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Object Files", "*.obj")
        );
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            try {
                MiniObjectModule module = parser.parse(file.toPath());
                
                // Check if already loaded
                for (MiniObjectModule m : loadedModules) {
                    if (m.getModuleName().equals(module.getModuleName())) {
                        log("Error: Module '" + module.getModuleName() + "' is already loaded.");
                        return;
                    }
                }
                
                loadedModules.add(module);
                moduleListView.getItems().add(module.getModuleName());
                log("Successfully loaded: " + module.getModuleName());
            } catch (Exception ex) {
                log("Error loading file: " + ex.getMessage());
            }
        }
    }

    private void removeSelectedModule() {
        int index = moduleListView.getSelectionModel().getSelectedIndex();
        if (index >= 0) {
            String name = moduleListView.getItems().remove(index);
            loadedModules.remove(index);
            log("Removed module: " + name);
            detailsArea.clear();
        }
    }

    private void showModuleDetails(String moduleName) {
        if (moduleName == null) return;
        
        MiniObjectModule selectedModule = null;
        for (MiniObjectModule m : loadedModules) {
            if (m.getModuleName().equals(moduleName)) {
                selectedModule = m;
                break;
            }
        }
        
        if (selectedModule == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("========== MODULE DETAILS ==========\n");
        sb.append("Name: ").append(selectedModule.getModuleName()).append("\n\n");
        
        sb.append("--- Sections ---\n");
        for (ObjectSection sec : selectedModule.getSections().values()) {
            sb.append("  ").append(sec.getType()).append(" (Size: ")
              .append(sec.getWords().size()).append(" words)\n");
        }
        
        sb.append("\n--- Global Symbols ---\n");
        for (SymbolDefinition def : selectedModule.getDefinitions().values()) {
            sb.append("  ").append(def.getName())
              .append(" = ").append(def.getSection()).append(":")
              .append(def.getWordOffset()).append("\n");
        }
        
        detailsArea.setText(sb.toString());
    }

    private void linkModules(Stage stage) {
        if (loadedModules.size() < 2) {
            log("Error: At least 2 modules are required to link.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save memory.hex");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Hex File", "*.hex"));
        fileChooser.setInitialFileName("memory.hex");
        File hexFile = fileChooser.showSaveDialog(stage);
        if (hexFile == null) return;

        fileChooser.setTitle("Save link.map");
        fileChooser.getExtensionFilters().clear();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Map File", "*.map"));
        fileChooser.setInitialFileName("link.map");
        File mapFile = fileChooser.showSaveDialog(stage);
        if (mapFile == null) return;

        try {
            MiniLinker linker = new MiniLinker();
            MiniLinker.LinkResult result = linker.linkModules(loadedModules);
            linker.writeOutputs(result, hexFile.toPath(), mapFile.toPath());
            
            log("LINK SUCCESSFUL!");
            log("Generated Hex: " + hexFile.getAbsolutePath());
            log("Generated Map: " + mapFile.getAbsolutePath());
            
            log("\n========== MEMORY.HEX ==========");
            log(Files.readString(hexFile.toPath()));
            
            log("\n========== LINK.MAP ==========");
            log(Files.readString(mapFile.toPath()));
            
        } catch (Exception ex) {
            log("LINK ERROR: " + ex.getMessage());
        }
    }

    private void log(String message) {
        detailsArea.appendText(message + "\n");
    }
}
