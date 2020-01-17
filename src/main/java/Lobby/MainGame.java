package Lobby;

import org.jspace.Space;

import java.util.Arrays;

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
    }
}
