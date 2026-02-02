package world.cals.supercollidersnippetmanager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        var fxmlUrl = Objects.requireNonNull(
                HelloApplication.class.getResource("/world/cals/supercollidersnippetmanager/main-view.fxml"),
                "Cannot find main-view.fxml on classpath"
        );

        FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);
        Scene scene = new Scene(fxmlLoader.load(), 1200, 800);
        stage.setTitle("SuperCollider Snippet Manager");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }
}
