package Lobby;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class MainMenuController {

    public Button hostGameButton, connectToGameButton, exitGameButton;

    private MainMenuView mainMenuView;
    private Stage setupGameStage;

    public void initialize() {
        //Changes appearance of buttons on mouseover
        hostGameButton.setOnMouseEntered(e -> hostGameButton.setStyle("-fx-background-color: rgb(97,134,47,0.53)"));
        hostGameButton.setOnMouseExited(e -> hostGameButton.setStyle("-fx-background-color: transparent"));

        connectToGameButton.setOnMouseEntered(e -> connectToGameButton.setStyle("-fx-background-color: rgb(97,134,47,0.53)"));
        connectToGameButton.setOnMouseExited(e -> connectToGameButton.setStyle("-fx-background-color: transparent"));

        exitGameButton.setOnMouseEntered(e -> exitGameButton.setStyle("-fx-background-color: rgb(97,134,47,0.53)"));
        exitGameButton.setOnMouseExited(e -> exitGameButton.setStyle("-fx-background-color: transparent"));
    }

    public void handleHostGame() throws IOException {
        setupGameStage = new Stage();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/setupGame.fxml"));
        Parent root = loader.load();

        SetupGameController setupGameController = loader.getController();
        setupGameController.setMainMenuController(this);

        Scene lobbyScene = new Scene(root);

        setupGameStage.setScene(lobbyScene);
        setupGameStage.getIcons().add(new Image(getClass().getResource("/icon.png").toExternalForm()));
        setupGameStage.setResizable(false);
        mainMenuView.getMainMenuStage().close();
        setupGameStage.show();
    }

    public void handleConnectGame() throws IOException {
        setupGameStage = new Stage();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/connectToGame.fxml"));
        Parent root = loader.load();

        ConnectToGameController connectToGameController = loader.getController();
        connectToGameController.setMainMenuController(this);

        Scene lobbyScene = new Scene(root);

        setupGameStage.setScene(lobbyScene);
        setupGameStage.setResizable(false);
        setupGameStage.getIcons().add(new Image(getClass().getResource("/icon.png").toExternalForm()));
        mainMenuView.getMainMenuStage().close();
        setupGameStage.show();
    }

    public void handleExit() {
        mainMenuView.getMainMenuStage().close();
        //Need to exit since there is a possibility of dangling threads
        //System.exit(-1);
    }

    void setMainMenuView(MainMenuView mainMenuView) {
        this.mainMenuView = mainMenuView;
    }

    Stage getSetupGameStage() {
        return setupGameStage;
    }
}


