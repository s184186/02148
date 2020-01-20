package Lobby;

import Model.Game;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.jspace.RemoteSpace;
import org.jspace.SequentialSpace;
import org.jspace.Space;

import java.io.IOException;
import java.util.List;

import static Lobby.Templates.*;

public class LobbyController {

    public Label URIField;
    public Label team1Player1Field, team1Player2Field, team1Player3Field;
    public Label team2Player1Field, team2Player2Field, team2Player3Field;
    public Label team3Player1Field, team3Player2Field, team3Player3Field;
    public Label hostNameField, versionField, numberOfTeamsField;
    public Label player1Field, player2Field, player3Field, player4Field, player5Field, player6Field;
    public Button joinTeam1Button, joinTeam2Button, joinTeam3Button, playButton, cancelButton;
    public TextField textField;
    public VBox chatBox;
    public ScrollPane sp;

    private Label[] playerFields;
    private Button[] joinTeamButtons;
    private Label[][] teamLabels;

    private static LobbyUpdater lobbyUpdater;
    private SetupGameController setupGameController;
    private ConnectToGameController connectToGameController;
    private LobbyModel lobbyModel;
    private String username;
    private Thread lobbyUpdaterThread;
    private RemoteSpace game;
    private static Stage lobbyStage;
    private Gson gson = new Gson();

    private boolean isHost;
    private int numberOfTeams;

    public void initialize() {
        sp.setContent(chatBox);
        sp.vvalueProperty().bind(chatBox.heightProperty());

        Label[] team1 = new Label[]{team1Player1Field, team1Player2Field, team1Player3Field};
        Label[] team2 = new Label[]{team2Player1Field, team2Player2Field, team2Player3Field};
        Label[] team3 = new Label[]{team3Player1Field, team3Player2Field, team3Player3Field};
        teamLabels = new Label[][]{team1, team2, team3};

        joinTeamButtons = new Button[]{joinTeam1Button, joinTeam2Button, joinTeam3Button};

        playerFields = new Label[]{player1Field, player2Field, player3Field, player4Field, player5Field, player6Field};
    }

    boolean setup() throws IOException, InterruptedException {
        if(isHost) {
            lobbyStage = setupGameController.getLobbyStage();
        } else {
            lobbyStage = connectToGameController.getLobbyStage();
            playButton.setDisable(true);
        }

        //Make sure lobby closes properly when user closes window
//        lobbyStage.setOnHiding(event -> shutdown());

        String ip = lobbyModel.getIp();
        String URI = "tcp://" + ip + "/game?keep";
        username = lobbyModel.getUsername();

        int version;
        String host;

        //Connect to lobby
        game = new RemoteSpace(URI);
        game.put("lobbyRequest", "connect", username, 0, "");

        Object[] ack = game.get(connectToGameAck(username));

        if (((String) ack[2]).matches("ok")) {

            //Get lobbyinfo
            Object[] lobbyInfoJson = game.get(lobbyInfo(username));
            String[] lobbyInfo = gson.fromJson((String) lobbyInfoJson[2], String[].class);

            String[] users = gson.fromJson(lobbyInfo[0], String[].class);
            int[] teams = gson.fromJson(lobbyInfo[1], int[].class);
            version = gson.fromJson(lobbyInfo[2], int.class);
            numberOfTeams = gson.fromJson(lobbyInfo[3], int.class);
            host = gson.fromJson(lobbyInfo[4], String.class);

            for (int i = 0; i < users.length; i++) {

                playerFields[i].setText(users[i]);
                int teamN = teams[i];

                if (teamN != 0) {

                    Label[] team = teamLabels[teamN - 1];

                    for (Label field : team) {

                        if (field.getText().matches("")) {
                            field.setText(users[i]);
                            break;
                        }
                    }
                }
            }

            lobbyUpdater = new LobbyUpdater(this);
            lobbyUpdaterThread = new Thread(lobbyUpdater);
            lobbyUpdaterThread.setDaemon(true);
            lobbyUpdaterThread.start();

        } else {
            return false;
        }

        //Set lobby text
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

    public void startGame(){
        lobbyStage.close();

        Object[] lobbyInfoJson = new Object[0];
        try {
            lobbyInfoJson = game.get(lobbyInfo(username));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String[] lobbyInfo = gson.fromJson((String) lobbyInfoJson[2], String[].class);

        String[] users = gson.fromJson(lobbyInfo[0], String[].class);
        int[] teams = gson.fromJson(lobbyInfo[1], int[].class);
        int version = gson.fromJson(lobbyInfo[2], int.class);
        int numberOfTeams = gson.fromJson(lobbyInfo[3], int.class);
        String host = gson.fromJson(lobbyInfo[4], String.class);

        lobbyUpdater.stop();
        lobbyUpdaterThread.interrupt();

        Stage stage = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/game.fxml"));
        Parent root = null;

        try {
            root = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Space user = new SequentialSpace();
        GameView gameView = fxmlLoader.getController();
        gameView.setUserSpace(user);
        gameView.setHost(host);
        gameView.setUsers(users);
        gameView.setTeams(teams);
        gameView.setVersion(version);
        gameView.setNumberOfTeams(numberOfTeams);
        gameView.setSpace(game);
        gameView.setUsername(username);
        gameView.setup();

        Scene scene = new Scene(root);
        stage.setResizable(false);
        stage.setTitle("Partners");
        stage.getIcons().add(new Image(getClass().getResource("/icon.png").toExternalForm()));
        stage.setScene(scene);
        stage.show();

        GameUpdater gameUpdater = new GameUpdater(game, user, username, gameView);
        new Thread(gameUpdater).start();
    }

    public void shutdown(){
        lobbyUpdater.stop();
        lobbyUpdaterThread.interrupt();

        if (isHost) {
            try {
                game.put("lobbyRequest","lobbyDisband", username, 0, "");
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
            game.put("lobbyRequest", "startGame", username, 0, "");
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
                game.put("lobbyRequest", "leaveTeam", username, 1, "");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            try {
                game.put("lobbyRequest", "joinTeam", username, 1, "");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void handleJoinTeam2() {
        if (joinTeam2Button.getText().matches("Leave")) {
            try {
                game.put("lobbyRequest", "leaveTeam", username, 2, "");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            try {
                game.put("lobbyRequest", "joinTeam", username, 2, "");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void handleJoinTeam3() {
        if (joinTeam3Button.getText().matches("Leave")) {
            try {
                game.put("lobbyRequest", "leaveTeam", username, 3, "");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            try {
                game.put("lobbyRequest", "joinTeam", username, 3, "");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void handleTextBox(KeyEvent e){
        if(e.getCode() == KeyCode.ENTER) {
            handleChat();
        }
    }

    public void handleChat(){
        String text = textField.getText();
        try {
            game.put("lobbyRequest", "sendMessage", username, 0, text);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        textField.clear();
        textField.requestFocus();
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

    public Space getSpace() {
        return this.game;
    }

    public String getUsername() {
        return this.username;
    }

    public VBox getChatBox() {
        return this.chatBox;
    }

    public Button getCancelButton() {
        return this.cancelButton;
    }

    public Label[][] getTeams() {
        return teamLabels;
    }

    public int getNumberOfTeams() {
        return numberOfTeams;
    }

    public Button[] getJoinTeamButtons() {
        return this.joinTeamButtons;
    }

    public Label[] getPlayerFields() {
        return playerFields;
    }
}

class LobbyUpdater implements Runnable {

    private LobbyController lobbyController;
    private volatile boolean exit;

    LobbyUpdater(LobbyController lobbyController) {
        this.lobbyController = lobbyController;
    }

    public void run() {
        try {
            while (!exit) {

                Object[] lobbyUpdate = lobbyController.getSpace().get(lobbyUpdate(lobbyController.getUsername()));
                String type = (String) lobbyUpdate[1];
                String actor = (String) lobbyUpdate[2];
                String info = (String) lobbyUpdate[5];

                switch (type) {
                    case "chatMessage":
                        Platform.runLater(
                                    () -> {
                                        String infoF = actor + ": " + info;
                                        while(infoF.length()>35){
                                                Label label = new Label(infoF.substring(0, 35));
                                                label.setAlignment(Pos.CENTER_RIGHT);

                                                lobbyController.getChatBox().getChildren().add(label);
                                                infoF = infoF.substring(35);
                                            }

                                        Label label = new Label(infoF);
                                        label.setAlignment(Pos.CENTER_RIGHT);

                                        lobbyController.getChatBox().getChildren().add(label);
                                });

                        break;

                    case "disconnected":
                        //This user has lost connection
                        if (actor.matches(lobbyController.getUsername())) {
                            Platform.runLater(() -> lobbyController.getCancelButton().fire());
                        } else {

                            //Another user has lost connection
                            Platform.runLater(
                                    () -> {
                                        //Remove username from teams and connected user
                                        for (Label[] team : lobbyController.getTeams()) {
                                            for (Label playerField : team) {
                                                if (playerField.getText().matches(actor)) {
                                                    playerField.setText("");
                                                    if (lobbyController.getUsername().matches(actor)) {
                                                        for (int i = 0; i < lobbyController.getNumberOfTeams(); i++) {
                                                            lobbyController.getJoinTeamButtons()[i].setDisable(false);
                                                            lobbyController.getJoinTeamButtons()[i].setText("Join");
                                                        }
                                                    }
                                                    break;
                                                }
                                            }
                                        }
                                        for (Label playerField : lobbyController.getPlayerFields()) {
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
                                    for (Label playerField : lobbyController.getPlayerFields()) {
                                        if (playerField.getText().matches("") && !actor.matches(lobbyController.getUsername())) {
                                            playerField.setText(actor);
                                            break;
                                        }
                                    }
                                }
                        );
                        break;
                    case "joinedTeam":
                        int teamN = (Integer) lobbyUpdate[4] - 1;
                        Label[] teamJoin = lobbyController.getTeams()[teamN];
                        for (int i = 0; i < 3; i++) {
                            if (teamJoin[i].getText().matches("")) {
                                //Can't update field from a non-javafx thread
                                int finalI = i;
                                Platform.runLater(
                                        () -> {
                                            teamJoin[finalI].setText(actor);
                                            if (lobbyController.getUsername().matches(actor)) {
                                                for (int j = 0; j < 3; j++) {
                                                    if (j != teamN) {
                                                        lobbyController.getJoinTeamButtons()[j].setDisable(true);
                                                    } else {
                                                        lobbyController.getJoinTeamButtons()[j].setText("Leave");
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
                        for (Label[] team : lobbyController.getTeams()) {
                            for (Label playerField : team) {
                                if (playerField.getText().matches(actor)) {
                                    Platform.runLater(
                                            () -> {
                                                playerField.setText("");
                                                if (lobbyController.getUsername().matches(actor)) {
                                                    for (int i = 0; i < lobbyController.getNumberOfTeams(); i++) {
                                                        lobbyController.getJoinTeamButtons()[i].setDisable(false);
                                                        lobbyController.getJoinTeamButtons()[i].setText("Join");
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
                                    lobbyController.startGame();
                                }
                        );
                        exit = true;
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
