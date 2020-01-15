package Controller;

import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class ConnectToGameController {

    public Label connectionFailedLabel;
    public Button playButton;
    public TextField usernameField, URIField;
    private LobbyModel lobbyModel = new LobbyModel();
    private Stage lobbyStage;
    private MainMenuController mainMenuController;

    public void initialize() {
        //Disable play button if either username or ip haven't been typed
        BooleanBinding bb = new BooleanBinding() {
            {
                super.bind(usernameField.textProperty(), URIField.textProperty());
            }

            @Override
            protected boolean computeValue() {
                return (usernameField.getText().isEmpty() || URIField.getText().isEmpty());
            }
        };
        playButton.disableProperty().bind(bb);
    }

    public void handlePlay() throws IOException, InterruptedException {
        String username = usernameField.getText();

        //Reset text if a previous connection attempt has failed
        connectionFailedLabel.setText("");

        lobbyModel.setIp(URIField.getText());
        lobbyModel.setUsername(username);

        lobbyStage = new Stage();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/lobbyView.fxml"));
        Parent root = loader.load();

        LobbyController lobbyController = loader.getController();
        lobbyController.setConnectToGameController(this);
        lobbyController.setLobbyModel(lobbyModel);
        lobbyController.setStage(lobbyStage);
        lobbyController.setHost(false);

        //Is connection refused?
        if (lobbyController.setFields()) {
            mainMenuController.getSetupGameStage().close();

            Scene lobbyScene = new Scene(root);

            lobbyStage.setScene(lobbyScene);
            lobbyStage.setResizable(false);
            lobbyStage.show();
        } else {
            connectionFailedLabel.setText("Unable to connect. Either lobby is full or username is in use");
        }
    }

    public void handleCancel() {
        mainMenuController.getSetupGameStage().close();
        MainMenuView mainMenuView = new MainMenuView();
        try{
            mainMenuView.start(new Stage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void setMainMenuController(MainMenuController mainMenuController) {
        this.mainMenuController = mainMenuController;
    }

    Stage getLobbyStage() {
        return lobbyStage;
    }
}
