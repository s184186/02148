package Controller;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainMenuView extends Application {

    private Stage mainMenuStage;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {

        mainMenuStage = stage;

        FXMLLoader fxmlLoader = new FXMLLoader(MainMenuView.class.getResource("/MainMenu.fxml"));
        Parent root = fxmlLoader.load();

        MainMenuController mainMenuController = fxmlLoader.getController();
        mainMenuController.setMainMenuView(this);

        Scene scene = new Scene(root);
        mainMenuStage.setResizable(false);
        mainMenuStage.setTitle("Partners");
        mainMenuStage.setScene(scene);
        mainMenuStage.show();
    }

    Stage getMainMenuStage() {
        return mainMenuStage;
    }
}
