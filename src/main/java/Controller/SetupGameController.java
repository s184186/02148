package Controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import org.jspace.*;

import java.io.IOException;
import java.util.Scanner;

public class SetupGameController {

    private MainMenuController mainMenuController;
    private LobbyModel lobbyModel = new LobbyModel();
    private static Scanner in = new Scanner(System.in);
    private Stage lobbyStage;

    public ToggleGroup versionToggleGroup, teamNumberToggleGroup;
    public TextField usernameField;

    public void handlePlay() throws IOException, InterruptedException {
        String username = usernameField.getText();
        int version = Integer.valueOf(((RadioButton)versionToggleGroup.getSelectedToggle()).getId());
        int numberOfTeams = Integer.valueOf(((RadioButton)teamNumberToggleGroup.getSelectedToggle()).getId());

        SequentialSpace space = new SequentialSpace();
        Server server = new Server(numberOfTeams, version, username, space);
        Thread serverThread = new Thread(server);
        serverThread.setDaemon(true); //Thread should close when main thread closes
        serverThread.start();



        String URI = (String) space.get(new ActualField("URI"), new FormalField(String.class))[1];
        lobbyModel.setURI(URI);
        lobbyModel.setUsername(username);

        mainMenuController.getSetupGameStage().close();

        lobbyStage = new Stage();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/lobbyView.fxml"));
        Parent root = loader.load();

        LobbyController lobbyController = loader.getController();
        lobbyController.setSetupGameController(this);
        lobbyController.setServerThread(serverThread);
        lobbyController.setLobbyModel(lobbyModel);
        lobbyController.setHost(true);
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

    public Stage getLobbyStage(){
        return lobbyStage;
    }
}

