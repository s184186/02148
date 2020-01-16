package Lobby;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

public class MainMenuController {

    private MainMenuView mainMenuView;
    private Stage setupGameStage;
    @FXML
    private Button hostGame, connectGame, exitGame;

    public void initialize() {
        //Changes appearance of buttons on mouseover
        hostGame.setOnMouseEntered(e -> hostGame.setStyle("-fx-background-color: rgb(97,134,47,0.53)"));
        hostGame.setOnMouseExited(e -> hostGame.setStyle("-fx-background-color: transparent"));

        connectGame.setOnMouseEntered(e -> connectGame.setStyle("-fx-background-color: rgb(97,134,47,0.53)"));
        connectGame.setOnMouseExited(e -> connectGame.setStyle("-fx-background-color: transparent"));

        exitGame.setOnMouseEntered(e -> exitGame.setStyle("-fx-background-color: rgb(97,134,47,0.53)"));
        exitGame.setOnMouseExited(e -> exitGame.setStyle("-fx-background-color: transparent"));
    }

    public void handleHostGame() throws IOException {
        setupGameStage = new Stage();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/setupGame.fxml"));
        Parent root = loader.load();

        SetupGameController setupGameController = loader.getController();
        setupGameController.setMainMenuController(this);

        Scene lobbyScene = new Scene(root);

        setupGameStage.setScene(lobbyScene);
        setupGameStage.setResizable(false);
        mainMenuView.getMainMenuStage().close();
        setupGameStage.show();
    }

    public void handlePlayBots() throws IOException {
        setupGameStage = new Stage();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/connectToGame.fxml"));
        Parent root = loader.load();

        ConnectToGameController connectToGameController = loader.getController();
        connectToGameController.setMainMenuController(this);

        Scene lobbyScene = new Scene(root);

        setupGameStage.setScene(lobbyScene);
        setupGameStage.setResizable(false);
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


