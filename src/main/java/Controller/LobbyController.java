package Controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;
import org.jspace.Space;

import java.io.IOException;

import static Protocol.Templates.*;

public class LobbyController {


    public Label URIField;
    public Label team1Player1Field, team1Player2Field, team1Player3Field;
    public Label team2Player1Field, team2Player2Field, team2Player3Field;
    public Label team3Player1Field, team3Player2Field, team3Player3Field;
    public Label hostNameField, versionField, numberOfTeamsField;
    public Button playButton;

    public Button joinTeam1Button;
    public Button joinTeam2Button;
    public Button joinTeam3Button;
    public Button[] joinTeamButtons;

    public Label player1Field;
    public Label player4Field;
    public Label player5Field;
    public Label player2Field;
    public Label player3Field;
    public Label player6Field;
    private Label[] playerFields;

    private Label[] team1;
    private Label[] team2;
    private Label[] team3;
    public Label[][] teams;

    private boolean isHost;
    private SetupGameController setupGameController;
    private ConnectToGameController connectToGameController;
    private LobbyModel lobbyModel;
    private int numberOfTeams = 2, version = 0;
    private String host, username, URI;
    private Thread serverThread;
    private Thread lobbyUpdaterThread;
    private RemoteSpace game;


    public void setFields() throws IOException, InterruptedException {
        team1 = new Label[]{team1Player1Field, team1Player2Field, team1Player3Field};
        team2 = new Label[]{team2Player1Field, team2Player2Field, team2Player3Field};
        team3 = new Label[]{team3Player1Field, team3Player2Field, team3Player3Field};
        teams = new Label[][]{team1, team2, team3};

        joinTeamButtons = new Button[]{joinTeam1Button, joinTeam2Button, joinTeam3Button};

        playerFields = new Label[]{player1Field,player2Field,player3Field,player4Field,player5Field,player6Field};
        if(!isHost){
            playButton.setDisable(true);
        }

        URI = lobbyModel.getURI();
        username = lobbyModel.getUsername();

        game = new RemoteSpace(URI);
        lobbyUpdaterThread = connectUser(username, game, playerFields, teams, joinTeamButtons);

        version = (Integer) game.get(new ActualField("lobbyInfoVersion"), new ActualField(username), new FormalField(Integer.class))[2];
        numberOfTeams = (Integer) game.get(new ActualField("lobbyInfoNTeams"), new ActualField(username), new FormalField(Integer.class))[2];
        host = (String) game.get(new ActualField("lobbyInfoHost"), new ActualField(username), new FormalField(String.class))[2];

        hostNameField.setText(host);
        versionField.setText(String.valueOf(lobbyModel.getVersion()));
        numberOfTeamsField.setText(String.valueOf(lobbyModel.getNumberOfTeams()));
        URIField.setText(URI);
    }

    public void handlePlay() {
    }

    public void handleCancel() {
        if(setupGameController != null){
            serverThread.interrupt();
            setupGameController.getLobbyStage().close();
        } else {
            connectToGameController.getLobbyStage().close();
        }

        lobbyUpdaterThread.interrupt();

        //JSpace prints the stacktrace of an IOException, nothing went wrong
        try {
            game.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleJoinTeam1() {
        if(joinTeam1Button.getText().matches("Leave")){
            try {
                game.put("team", "leaveTeam", username, 1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            try {
                game.put("team", "joinTeam", username, 1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void handleJoinTeam2() {
        if(joinTeam2Button.getText().matches("Leave")){
            try {
                game.put("team", "leaveTeam", username, 2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            try {
                game.put("team", "joinTeam", username, 2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void handleJoinTeam3() {
        if(joinTeam3Button.getText().matches("Leave")){
            try {
                game.put("team", "leaveTeam", username, 3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            try {
                game.put("team", "joinTeam", username, 3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static Thread connectUser(String username, Space game, Label[] playerFields, Label[][] teams, Button[] joinTeamButtons) throws InterruptedException {
        String info;
        game.put("connectToGameReq", username);

        Object[] ack = game.get(connectToGameAck(username).getFields());

        if(((String) ack[2]).matches("ok")) {

            game.put("connect", username, "yes");
            info = (String) game.get(lobbyInfo(username).getFields())[2];
            String[] names = info.split(" ");
            for(int i = 0; i < names.length; i++){
                playerFields[i].setText(names[i]);
            }
            System.out.println("Connected users: " + info);

        } else {
            System.out.println("Go back to menu");
            System.exit(0);
        }

        LobbyUpdater lobbyUpdater = new LobbyUpdater(game, username, playerFields, teams, joinTeamButtons);
        Thread lobbyUpdaterThread = new Thread(lobbyUpdater);
        lobbyUpdaterThread.setDaemon(true);
        lobbyUpdaterThread.start();

        return lobbyUpdaterThread;
    }

    public void setLobbyModel(LobbyModel lobbyModel) {
        this.lobbyModel = lobbyModel;
    }

    public void setSetupGameController(SetupGameController setupGameController) {
        this.setupGameController = setupGameController;
    }

    public void setConnectToGameController(ConnectToGameController connectToGameController) {
        this.connectToGameController = connectToGameController;
    }

    public boolean isHost() {
        return isHost;
    }

    public void setHost(boolean host) {
        isHost = host;
    }

    public void setServerThread(Thread serverThread) {
        this.serverThread = serverThread;
    }
}

class LobbyUpdater implements Runnable{

    private String username;
    private Label[] playerFields;
    private Label[][] teams;
    private Button[] joinTeamButtons;
    private Space space;
    private volatile boolean exit;

    public LobbyUpdater(Space space, String username, Label[] playerFields, Label[][] teams, Button[] joinTeamButtons){
        this.space = space;
        this.username = username;
        this.playerFields = playerFields;
        this.teams = teams;
        this.joinTeamButtons = joinTeamButtons;
    }

    public void run() {
        try {
            while(!exit){

                Object[] lobbyUpdate = space.get(lobbyUpdate(username).getFields());
                String type = (String) lobbyUpdate[1];
                String actor = (String) lobbyUpdate[2];

                switch(type){
                    case "disconnected":
                        if(actor.matches(username)){
                            Platform.runLater(
                                    () -> {

                                    }
                            );
                        } else {
                            Platform.runLater(
                                    () -> {
                                        for (Label[] team : teams) {
                                            for (Label playerField : team) {
                                                if (playerField.getText().matches(actor)) {
                                                    playerField.setText("");
                                                    if (username.matches(actor)) {
                                                        for (Button button : joinTeamButtons) {
                                                            button.setDisable(false);
                                                            button.setText("Join");
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
                    case "ping":
                        space.put("pingack", username);
                        break;
                    case "connected":
                        System.out.println(actor + " connected to the lobby");
                        Platform.runLater(
                                () -> {
                                    for(Label playerField: playerFields){
                                        if(playerField.getText().matches("") && !actor.matches(username)){
                                            playerField.setText(actor);
                                            break;
                                        }
                                    }
                                }
                        );
                        break;
                    case "joinedTeam":
                        int teamN = (Integer) lobbyUpdate[4]-1;
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
                        for(Label[] team: teams){
                            for(Label playerField: team){
                                if(playerField.getText().matches(actor)){
                                    Platform.runLater(
                                            () -> {
                                                playerField.setText("");
                                                if (username.matches(actor)) {
                                                    for (Button button : joinTeamButtons) {
                                                        button.setDisable(false);
                                                        button.setText("Join");
                                                    }
                                                }
                                            }
                                    );
                                    break;
                                }
                            }
                        }
                        break;
                }
            }
        } catch (InterruptedException e) {

        }
    }

    public void stop(){
        exit = true;
    }
}
