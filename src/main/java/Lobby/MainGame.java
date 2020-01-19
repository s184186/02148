package Lobby;

import com.google.gson.Gson;
import org.jspace.Space;

import java.util.Arrays;
import java.util.Scanner;

public class MainGame {

    private String[] players;
    private int[] teams;
    private int version;
    private Space game;
    private String host;
    private int numberOfTeams;

    public MainGame(String host, String[] players, int[] teams, int version, Space game, int numberOfTeams) {
        this.players = players;
        this.teams = teams;
        this.version = version;
        this.game = game;
        this.host = host;
        this.numberOfTeams = numberOfTeams;
    }

    public void startGame() {
        System.out.println("Players: " + Arrays.toString(players));
        System.out.println("Teams: " + Arrays.toString(teams));
        System.out.println("Version: " + version);
        System.out.println("Host: " + host);
        System.out.println("Number of teams: " + numberOfTeams);
        Gson gson = new Gson();
        int[] pieceIndexes = {1};
        int[] positions = {1};

        String pieceIndexesJson = gson.toJson(pieceIndexes);
        String positionsJson = gson.toJson(positions);

        try {
            game.put("gameUpdate", "playerMove", host, host, "", pieceIndexesJson, positionsJson);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Scanner in = new Scanner(System.in);
        in.next();
        try {
            game.put("gameUpdate", "resetBoard", "", host, "", "", "");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
