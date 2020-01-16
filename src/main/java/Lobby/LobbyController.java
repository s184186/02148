package Lobby;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.jspace.RemoteSpace;
import org.jspace.Space;

import java.io.IOException;

import static Lobby.Templates.*;

public class LobbyController {


    private static LobbyUpdater lobbyUpdater;
    public Label URIField;
    public Label team1Player1Field, team1Player2Field, team1Player3Field;
    public Label team2Player1Field, team2Player2Field, team2Player3Field;
    public Label team3Player1Field, team3Player2Field, team3Player3Field;
    public Label hostNameField, versionField, numberOfTeamsField;
    public Label player1Field, player2Field, player3Field, player4Field, player5Field, player6Field;
    public Button joinTeam1Button, joinTeam2Button, joinTeam3Button, playButton, cancelButton;

    private Label[] playerFields;
    private Button[] joinTeamButtons;
    private Label[][] teams;

    private boolean isHost;
    private SetupGameController setupGameController;
    private ConnectToGameController connectToGameController;
    private LobbyModel lobbyModel;
    private String username;
    private Thread lobbyUpdaterThread;
    private RemoteSpace game;
    private Thread serverThread;
    private static Stage lobbyStage;

    public void initialize() {
        Label[] team1 = new Label[]{team1Player1Field, team1Player2Field, team1Player3Field};
        Label[] team2 = new Label[]{team2Player1Field, team2Player2Field, team2Player3Field};
        Label[] team3 = new Label[]{team3Player1Field, team3Player2Field, team3Player3Field};
        teams = new Label[][]{team1, team2, team3};

        joinTeamButtons = new Button[]{joinTeam1Button, joinTeam2Button, joinTeam3Button};

        playerFields = new Label[]{player1Field, player2Field, player3Field, player4Field, player5Field, player6Field};
    }

    boolean setup() throws IOException, InterruptedException {
        if(setupGameController != null) {
            lobbyStage = setupGameController.getLobbyStage();
        } else {
            lobbyStage = connectToGameController.getLobbyStage();
        }

        if (!isHost) {
            playButton.setDisable(true);
        }

        //Make sure lobby closes properly when user closes window
        lobbyStage.setOnHiding(event -> shutdown());

        String ip = lobbyModel.getIp();
        String URI = "tcp://" + ip + "/game?keep";
        username = lobbyModel.getUsername();

        game = new RemoteSpace(URI);
        lobbyUpdaterThread = connectUser(username, game, playerFields, teams, joinTeamButtons, cancelButton);
        if (lobbyUpdaterThread == null) {
            return false;
        }

        int version = (Integer) game.get(lobbyInfoVersion(username))[2];
        int numberOfTeams = (Integer) game.get(lobbyInfoNTeams(username))[2];
        String host = (String) game.get(lobbyInfoHost(username))[2];

        hostNameField.setText(host);
        String versionText = "Normal";

        if (version == 1) {
            versionText = "Plus";
        }

        if (numberOfTeams == 2) {
            joinTeam3Button.setDisable(true);
        }

        versionField.setText(versionText);
        numberOfTeamsField.setText(String.valueOf(numberOfTeams));
        URIField.setText(ip);
        return true;
    }

    private static Thread connectUser(String username, Space game, Label[] playerFields, Label[][] teams, Button[] joinTeamButtons,
                                      Button cancelButton) throws InterruptedException {
        String infoUsers, infoTeams;
        game.put("lobbyRequest", "connect", username, 0);

        Object[] ack = game.get(connectToGameAck(username));

        if (((String) ack[2]).matches("ok")) {

            //Get lobbyinfo
            infoUsers = (String) game.get(lobbyInfoUsers(username))[2];
            infoTeams = (String) game.get(lobbyInfoTeams(username))[2];
            String[] namesinfo = infoUsers.split(" ");
            String[] teamsinfo = infoTeams.split(" ");

            for (int i = 0; i < namesinfo.length; i++) {

                playerFields[i].setText(namesinfo[i]);
                int teamN = Integer.valueOf(teamsinfo[i]);

                if (teamN != 0) {

                    Label[] team = teams[teamN - 1];

                    for (Label field : team) {

                        if (field.getText().matches("")) {
                            field.setText(namesinfo[i]);
                            break;
                        }
                    }
                }
            }

        } else {
            return null;
        }

        int numberOfTeams = (Integer) game.query(lobbyInfoNTeams(username))[2];


        lobbyUpdater = new LobbyUpdater(game, username, playerFields, teams, joinTeamButtons, cancelButton, numberOfTeams, lobbyStage);
        Thread lobbyUpdaterThread = new Thread(lobbyUpdater);
        lobbyUpdaterThread.setDaemon(true);
        lobbyUpdaterThread.start();

        return lobbyUpdaterThread;
    }


    public void shutdown(){
        lobbyUpdater.stop();
        lobbyUpdaterThread.interrupt();

        if (setupGameController != null) {
            try {
                game.put("lobbyRequest","lobbyDisband", username, 0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            game.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handlePlay() throws InterruptedException {
        if (isHost) {
            game.put("lobbyRequest", "startGame", username, 0);
        }
    }

    public void handleCancel() {
        lobbyStage.close();

        MainMenuView mainMenuView = new MainMenuView();
        try{
            mainMenuView.start(new Stage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleJoinTeam1() {
        if (joinTeam1Button.getText().matches("Leave")) {
            try {
                game.put("lobbyRequest", "leaveTeam", username, 1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            try {
                game.put("lobbyRequest", "joinTeam", username, 1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void handleJoinTeam2() {
        if (joinTeam2Button.getText().matches("Leave")) {
            try {
                game.put("lobbyRequest", "leaveTeam", username, 2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            try {
                game.put("lobbyRequest", "joinTeam", username, 2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void handleJoinTeam3() {
        if (joinTeam3Button.getText().matches("Leave")) {
            try {
                game.put("lobbyRequest", "leaveTeam", username, 3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            try {
                game.put("lobbyRequest", "joinTeam", username, 3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    void setLobbyModel(LobbyModel lobbyModel) {
        this.lobbyModel = lobbyModel;
    }

    void setSetupGameController(SetupGameController setupGameController) {
        this.setupGameController = setupGameController;
    }

    void setConnectToGameController(ConnectToGameController connectToGameController) {
        this.connectToGameController = connectToGameController;
    }

    void setHost(boolean host) {
        isHost = host;
    }

    public void setServerThread(Thread serverThread) {
        this.serverThread = serverThread;
    }
}

class LobbyUpdater implements Runnable {

    private String username;
    private Label[] playerFields;
    private Label[][] teams;
    private Button[] joinTeamButtons;
    private Button cancelButton;
    private int numberOfTeams;
    private Stage lobbyStage;
    private Space space;
    private volatile boolean exit;

    LobbyUpdater(Space space, String username, Label[] playerFields, Label[][] teams,
                 Button[] joinTeamButtons, Button cancelButton, int numberOfTeams,
                 Stage lobbyStage) {
        this.space = space;
        this.username = username;
        this.playerFields = playerFields;
        this.teams = teams;
        this.joinTeamButtons = joinTeamButtons;
        this.cancelButton = cancelButton;
        this.numberOfTeams = numberOfTeams;
        this.lobbyStage = lobbyStage;
    }

    public void run() {
        try {
            while (!exit) {

                Object[] lobbyUpdate = space.get(lobbyUpdate(username));
                String type = (String) lobbyUpdate[1];
                String actor = (String) lobbyUpdate[2];

                switch (type) {
                    case "disconnected":
                        //This user has lost connection
                        if (actor.matches(username)) {
                            Platform.runLater(
                                    () -> { cancelButton.fire();
                            });
                        } else {

                            //Another user has lost connection
                            Platform.runLater(
                                    () -> {
                                        //Remove username from teams and connected user
                                        for (Label[] team : teams) {
                                            for (Label playerField : team) {
                                                if (playerField.getText().matches(actor)) {
                                                    playerField.setText("");
                                                    if (username.matches(actor)) {
                                                        for (int i = 0; i < numberOfTeams; i++) {
                                                            joinTeamButtons[i].setDisable(false);
                                                            joinTeamButtons[i].setText("Join");
                                                        }
                                                    }
                                                    break;
                                                }
                                            }
                                        }
                                        for (Label playerField : playerFields) {
                                            if (playerField.getText().matches(actor)) {
                                                playerField.setText("");
                                                break;
                                            }
                                        }
                                    }
                            );
                        }
                        break;
                    case "ping": //Don't need to do anything since server checks if the tuple has been gotten
                        break;
                    case "connected":
                        Platform.runLater(
                                () -> {
                                    //Update labels
                                    for (Label playerField : playerFields) {
                                        if (playerField.getText().matches("") && !actor.matches(username)) {
                                            playerField.setText(actor);
                                            break;
                                        }
                                    }
                                }
                        );
                        break;
                    case "joinedTeam":
                        int teamN = (Integer) lobbyUpdate[4] - 1;
                        Label[] teamJoin = teams[teamN];
                        for (int i = 0; i < 3; i++) {
                            if (teamJoin[i].getText().matches("")) {
                                //Can't update field from a non-javafx thread
                                int finalI = i;
                                Platform.runLater(
                                        () -> {
                                            teamJoin[finalI].setText(actor);
                                            if (username.matches(actor)) {
                                                for (int j = 0; j < 3; j++) {
                                                    if (j != teamN) {
                                                        joinTeamButtons[j].setDisable(true);
                                                    } else {
                                                        joinTeamButtons[j].setText("Leave");
                                                    }
                                                }
                                            }
                                        }
                                );
                                break;
                            }
                        }
                        break;
                    case "leftTeam":
                        for (Label[] team : teams) {
                            for (Label playerField : team) {
                                if (playerField.getText().matches(actor)) {
                                    Platform.runLater(
                                            () -> {
                                                playerField.setText("");
                                                if (username.matches(actor)) {
                                                    for (int i = 0; i < numberOfTeams; i++) {
                                                        joinTeamButtons[i].setDisable(false);
                                                        joinTeamButtons[i].setText("Join");
                                                    }
                                                }
                                            }
                                    );
                                    break;
                                }
                            }
                        }
                        break;

                    case "gameStart":
                        Platform.runLater(
                                () -> {
                                    lobbyStage.close();
                                }
                        );
                        System.out.println("game has started");
                        break;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void stop() {
        exit = true;
    }
}
