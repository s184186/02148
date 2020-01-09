package View;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

import static javafx.application.Application.*;

public class MainMenu {
    private Stage MainMenuStage;

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage stage) throws IOException {
        MainMenuStage=stage;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MainMenu.fxml"));
        Parent root = loader.load();
        MainMenuStage.setTitle("Partners");
        Scene scene = new Scene(root);
        MainMenuStage.setScene(scene);
        MainMenuStage.show();
    }
}
