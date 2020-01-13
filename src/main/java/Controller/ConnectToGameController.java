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
    private LobbyModel lobbyModel = new LobbyModel();
    private Stage lobbyStage;

    public TextField usernameField, URIField;
    private MainMenuController mainMenuController;

    public void initialize(){
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
        connectionFailedLabel.setText("");

        lobbyModel.setIp(URIField.getText());
        lobbyModel.setUsername(username);

        lobbyStage = new Stage();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/lobbyView.fxml"));
        Parent root = loader.load();

        LobbyController lobbyController = loader.getController();
        lobbyController.setConnectToGameController(this);
        lobbyController.setLobbyModel(lobbyModel);
        lobbyController.setHost(false);
        if(lobbyController.setFields()){

            mainMenuController.getSetupGameStage().close();

            Scene lobbyScene = new Scene(root);

            lobbyStage.setScene(lobbyScene);
            lobbyStage.show();
        } else {
            connectionFailedLabel.setText("User with that name already exists");
        }
    }

    public void handleCancel() {
        mainMenuController.getSetupGameStage().close();
    }

    public void setMainMenuController(MainMenuController mainMenuController) {
        this.mainMenuController = mainMenuController;
    }

    public Stage getLobbyStage() {
        return lobbyStage;
    }
}
