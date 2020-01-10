package Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.jspace.SequentialSpace;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import java.io.*;


import java.awt.*;
import java.io.IOException;

public class MainMenuController {
    private Stage lobbyStage = new Stage();
    @FXML private Button playOnline, playBots, exitGame;

    public void initialize() {
        //Changes appearance of buttons on mouseover
        playOnline.setOnMouseEntered(e -> playOnline.setStyle("-fx-background-color: rgb(97,134,47,0.53)"));
        playOnline.setOnMouseExited(e -> playOnline.setStyle("-fx-background-color: transparent"));

        playBots.setOnMouseEntered(e -> playBots.setStyle("-fx-background-color: rgb(97,134,47,0.53)"));
        playBots.setOnMouseExited(e -> playBots.setStyle("-fx-background-color: transparent"));

        exitGame.setOnMouseEntered(e -> exitGame.setStyle("-fx-background-color: rgb(97,134,47,0.53)"));
        exitGame.setOnMouseExited(e -> exitGame.setStyle("-fx-background-color: transparent"));
    }



    public void handlePlayOnline() throws IOException {
       /* FXMLLoader loader = new FXMLLoader(getClass().getResource("lobbyView.fxml"));
        Parent root = loader.load();
        SequentialSpace space = new SequentialSpace();

        Scene lobbyScene = new Scene(root);
        lobbyStage.setTitle("Lobby");
        lobbyStage.setScene(lobbyScene);
        lobbyStage.show();*/
        System.out.println("play online");
    }
    public void handlePlayBots() throws IOException {
     /*   FXMLLoader loader = new FXMLLoader(getClass().getResource("lobbyView.fxml"));
        Parent root = loader.load();

        Scene lobbyScene = new Scene(root);
        lobbyStage.setTitle("Create game");
        lobbyStage.setScene(lobbyScene);
        lobbyStage.show();*/
        System.out.println("play bots");
    }
    public void handleExit() throws IOException {
        System.out.println("Exit game");
    }
}

