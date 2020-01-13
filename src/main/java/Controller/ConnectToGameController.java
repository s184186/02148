package Controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;
import org.jspace.Space;
import java.io.IOException;

public class ConnectToGameController {

    private LobbyModel lobbyModel = new LobbyModel();
    private Stage lobbyStage;

    public TextField usernameField, URIField;
    private MainMenuController mainMenuController;

    public void handlePlay() throws IOException, InterruptedException {
        String username = usernameField.getText();

        lobbyModel.setURI(URIField.getText());
        lobbyModel.setUsername(username);

        mainMenuController.getSetupGameStage().close();

        lobbyStage = new Stage();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/lobbyView.fxml"));
        Parent root = loader.load();

        LobbyController lobbyController = loader.getController();
        lobbyController.setConnectToGameController(this);
        lobbyController.setLobbyModel(lobbyModel);
        lobbyController.setHost(false);
        lobbyController.setFields();

        Scene lobbyScene = new Scene(root);

        lobbyStage.setScene(lobbyScene);
        lobbyStage.show();
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
