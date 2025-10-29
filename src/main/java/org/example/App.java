package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Path;

public class App extends Application {

    private MainController controller;

    @Override
    public void start(Stage stage) throws Exception {
        // load the UI layout
        FXMLLoader loader = new FXMLLoader(
                App.class.getResource("/main-view.fxml")
        );

        Scene scene = new Scene(loader.load(), 1280, 720);

        // apply custom styling
        String css = App.class.getResource("/styles.css").toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle("Smart Collections – Media & Notes Manager");
        stage.setScene(scene);
        stage.show();

        // grab controller reference for later
        controller = loader.getController();
    }

    @Override
    public void stop() {
        // save everything when app closes
        try {
            if (controller != null) {
                LibraryRepository repo = controller.getRepository();
                PersistenceService ps = new PersistenceService();

                // save to user's home directory
                Path backup = Path.of(
                        System.getProperty("user.home"),
                        "smart-collections-backup.scol"
                );

                ps.saveTo(backup.toFile(), repo);
            }
        } catch (Exception e) {
            System.err.println("Backup failed: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
