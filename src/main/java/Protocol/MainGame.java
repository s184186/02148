package Protocol;

import org.jspace.Space;

import java.util.Arrays;

public class MainGame {

    private String[] players;
    private int[] teams;
    private int version;
    private Space game;
    private String host;

    public MainGame(String host, String[] players, int[] teams, int version, Space game) {
        this.players = players;
        this.teams = teams;
        this.version = version;
        this.game = game;
        this.host = host;
    }

    public void startGame() {
        System.out.println("Players: " + Arrays.toString(players));
        System.out.println("Teams: " + Arrays.toString(teams));
    }
}
