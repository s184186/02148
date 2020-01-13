package Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.jspace.*;
import java.io.IOException;
import static Protocol.Templates.*;

public class MainMenuController {

    private MainMenuView mainMenuView;
    private Stage setupGameStage;
    @FXML private Button hostGame, connectGame, exitGame;

    public void initialize() {
        //Changes appearance of buttons on mouseover
        hostGame.setOnMouseEntered(e -> hostGame.setStyle("-fx-background-color: rgb(97,134,47,0.53)"));
        hostGame.setOnMouseExited(e -> hostGame.setStyle("-fx-background-color: transparent"));

        connectGame.setOnMouseEntered(e -> connectGame.setStyle("-fx-background-color: rgb(97,134,47,0.53)"));
        connectGame.setOnMouseExited(e -> connectGame.setStyle("-fx-background-color: transparent"));

        exitGame.setOnMouseEntered(e -> exitGame.setStyle("-fx-background-color: rgb(97,134,47,0.53)"));
        exitGame.setOnMouseExited(e -> exitGame.setStyle("-fx-background-color: transparent"));
    }

    public void handleHostGame() throws IOException{
        setupGameStage = new Stage();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/setupGame.fxml"));
        Parent root = loader.load();

        SetupGameController setupGameController = loader.getController();
        setupGameController.setMainMenuController(this);

        Scene lobbyScene = new Scene(root);

        setupGameStage.setScene(lobbyScene);
        setupGameStage.show();

        /*TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("Text Input Dialog");

        dialog.setContentText("Please enter your name:");
        String username = "";
        // Traditional way to get the response value.
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()){
            username = result.get();
        } else {
            return;
        }


        System.out.println("Plus version?(Y/N)");
        int version = 0;
        String answer = in.next();

        int teams = 2;

        if(answer.matches("Y")){
            version = 1;
            System.out.println("How many teams?(2/3)");
            teams = in.nextInt();
        }

        new Thread(new Server(teams, version, username)).start();

        Space game = null;

        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        String ip = inetAddress.getHostAddress();
        int port = 11345;

        String URI = "tcp://" + ip + ":" + port + "/game?keep";
        game = new RemoteSpace(URI);
        connectUser(username, game);

        System.out.println("Press any key to start game");
        in.next();
        game.put("startGame");
        System.out.println("Starting game");*/

    }
    public void handlePlayBots() throws IOException{
        setupGameStage = new Stage();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/connectToGame.fxml"));
        Parent root = loader.load();

        ConnectToGameController connectToGameController = loader.getController();
        connectToGameController.setMainMenuController(this);

        Scene lobbyScene = new Scene(root);

        setupGameStage.setScene(lobbyScene);
        setupGameStage.show();

        /*connectUser(username, game);

        game.query(new ActualField("startGame"));
        System.out.println("Host has started game");*/
    }

    public void handleExit(){
        mainMenuView.getMainMenuStage().close();
    }

    public void setMainMenuView(MainMenuView mainMenuView){
        this.mainMenuView = mainMenuView;
    }

    public Stage getSetupGameStage() {
        return setupGameStage;
    }
}


