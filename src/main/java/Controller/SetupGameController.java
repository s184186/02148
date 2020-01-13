package Controller;

import javafx.beans.binding.BooleanBinding;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import org.jspace.*;

import java.io.IOException;
import java.util.Scanner;

public class SetupGameController {

    public Button playButton;
    public RadioButton toggle3;
    public RadioButton toggle2;
    private MainMenuController mainMenuController;
    private LobbyModel lobbyModel = new LobbyModel();
    private static Scanner in = new Scanner(System.in);
    private Stage lobbyStage;

    public ToggleGroup versionToggleGroup, teamNumberToggleGroup;
    public TextField usernameField;

    public void initialize(){
        BooleanBinding playBinding = new BooleanBinding() {
            {
                super.bind(usernameField.textProperty());
            }
            @Override
            protected boolean computeValue() {
                return (usernameField.getText().isEmpty());
            }
        };
        playButton.disableProperty().bind(playBinding);

        BooleanBinding teamBinding = new BooleanBinding() {
            {
                super.bind(versionToggleGroup.selectedToggleProperty());
            }
            @Override
            protected boolean computeValue() {
                return (Integer.valueOf(((RadioButton)versionToggleGroup.getSelectedToggle()).getId()) == 0);
            }
        };

        toggle3.disableProperty().bind(teamBinding);

    }

    public void handlePlay() throws IOException, InterruptedException {
        String username = usernameField.getText();
        int version = Integer.valueOf(((RadioButton)versionToggleGroup.getSelectedToggle()).getId());
        int numberOfTeams = Integer.valueOf(((RadioButton)teamNumberToggleGroup.getSelectedToggle()).getId());

        SequentialSpace space = new SequentialSpace();
        Server server = new Server(numberOfTeams, version, username, space);
        Thread serverThread = new Thread(server);
        serverThread.setDaemon(true); //Thread should close when main thread closes
        serverThread.start();

        String ip = (String) space.get(new ActualField("IPPORT"), new FormalField(String.class))[1];

        lobbyModel.setIp(ip);
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

    public void handleNormalToggle() {
        toggle2.setSelected(true);
        toggle3.setSelected(false);
    }

    public void setMainMenuController(MainMenuController mainMenuController) {
        this.mainMenuController = mainMenuController;
    }

    public Stage getLobbyStage(){
        return lobbyStage;
    }
}

