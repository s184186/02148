package Model;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

import static javafx.application.Application.*;

public class MainMenu extends Application {
    private static Scene scene;
/*
    private Stage MainMenuStage;
*/

    public static void main(String[] args) {
        launch();
    }
    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("/MainMenu"));
        stage.setTitle("Partners");
        stage.setScene(scene);
        stage.show();
    }
    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }
    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainMenu.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }
}
