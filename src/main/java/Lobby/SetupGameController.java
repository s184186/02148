package Lobby;

import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import org.jspace.SequentialSpace;
import java.io.IOException;
import static Lobby.Templates.IPPort;

public class SetupGameController {

    public Button playButton;
    public RadioButton toggle2,toggle3;
    public ToggleGroup versionToggleGroup, teamNumberToggleGroup;
    public TextField usernameField;

    private MainMenuController mainMenuController;
    private LobbyModel lobbyModel;
    private Stage lobbyStage;

    public void initialize() {
        //Play button is greyed out if username has not been entered
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

        //3 team radio button is greyed out if the normal version is selected
        BooleanBinding teamBinding = new BooleanBinding() {
            {
                super.bind(versionToggleGroup.selectedToggleProperty());
            }

            @Override
            protected boolean computeValue() {
                return (Integer.valueOf(((RadioButton) versionToggleGroup.getSelectedToggle()).getId()) == 0);
            }
        };

        toggle3.disableProperty().bind(teamBinding);

    }

    public void handlePlay() throws IOException, InterruptedException {
        String username = usernameField.getText();
        int version = Integer.valueOf(((RadioButton) versionToggleGroup.getSelectedToggle()).getId());
        int numberOfTeams = Integer.valueOf(((RadioButton) teamNumberToggleGroup.getSelectedToggle()).getId());

        SequentialSpace space = new SequentialSpace();
        Server server = new Server(numberOfTeams, version, username, space);
        Thread serverThread = new Thread(server);
        serverThread.setDaemon(true); //Thread should close when main thread closes
        serverThread.start();

        String ip = (String) space.get(IPPort)[1];

        lobbyModel = new LobbyModel();
        lobbyModel.setIp(ip);
        lobbyModel.setUsername(username);

        mainMenuController.getSetupGameStage().close();

        lobbyStage = new Stage();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/lobbyView.fxml"));
        Parent root = loader.load();

        LobbyController lobbyController = loader.getController();
        lobbyController.setSetupGameController(this);
        lobbyController.setLobbyModel(lobbyModel);
        lobbyController.setHost(true);
        lobbyController.setServer(server);
        lobbyController.setServerThread(serverThread);
        lobbyController.setup();

        Scene lobbyScene = new Scene(root);

        lobbyStage.setScene(lobbyScene);
        lobbyStage.setResizable(false);
        lobbyStage.show();

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

    public void handleNormalToggle() {
        //Only 2 teams available in normal version
        toggle2.setSelected(true);
        toggle3.setSelected(false);
    }

    void setMainMenuController(MainMenuController mainMenuController) {
        this.mainMenuController = mainMenuController;
    }

    Stage getLobbyStage() {
        return lobbyStage;
    }
}

