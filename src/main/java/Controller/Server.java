package Controller;

import Protocol.MainGame;
import org.jspace.*;

import java.net.InetAddress;

import static Protocol.Templates.*;


public class Server implements Runnable {

    private Space game;
    private int numberOfTeams;
    private int version;
    private String host;
    private String URI;
    private String gate;

    public Server(int numberOfTeams, int version, String host, Space game){
        this.numberOfTeams = numberOfTeams;
        this.version = version;
        this.host = host;
        this.game = game;
    }

    public void run() {
        PlayerConnector playerConnector = null;
        Thread playerConnectorThread = null;
        PlayersConnected playersConnected = null;
        Thread playersConnectedThread = null;
        TeamDistributor teamDistributor = null;
        Thread teamDistributorThread = null;
        SpaceRepository gameRepository = null;
        try {
            System.out.println("Starting server");
            //Create game server
            gameRepository = new SpaceRepository();

            //Space where users and server communicate

            //Space where server threads communicate
            SequentialSpace server = new SequentialSpace();

            //Keep track of connected players

            server.put("numberOfPlayers", 0);
            server.put("maxNumberOfPlayers", 6);

            // Setting up URI
            InetAddress inetAddress = null;
            inetAddress = InetAddress.getLocalHost();
            String ip = inetAddress.getHostAddress();
            int port = 11345;
            gate = "tcp://" + ip + ":" + port + "?keep";
            URI = "tcp://" + ip + ":" + port + "/game?keep";
            game.put("IPPORT", ip + ":" + port);
            System.out.println("A game is hosted on URI: " + ip + ":" + port);
            // Opening gate at given URI
            gameRepository.add("game", game);
            gameRepository.addGate(gate);

            //Look for players connecting
            playerConnector = new PlayerConnector(game, server, host, version, numberOfTeams);
            playerConnectorThread = new Thread(playerConnector);
            playerConnectorThread.setDaemon(true);
            playerConnectorThread.start();

            //Check players are still connected
            playersConnected = new PlayersConnected(server, game);
            playersConnectedThread = new Thread(playersConnected);
            playersConnectedThread.setDaemon(true);
            playersConnectedThread.start();

            //Look for joinTeam requests
            teamDistributor = new TeamDistributor(server, game, numberOfTeams);
            teamDistributorThread = new Thread(teamDistributor);
            teamDistributorThread.setDaemon(true);
            teamDistributorThread.start();

            //Wait for host to start game
            game.query(new ActualField("startGame"));

            System.out.println("Server: host has started the game");

            playerConnector.stop(); //Stop while loop
            playerConnectorThread.interrupt(); //Interrupt blocking calls

            playersConnected.stop();
            playersConnectedThread.interrupt();

            teamDistributor.stop();
            teamDistributorThread.interrupt();

            launchGameServer(server, game);

        } catch (Exception e) {
            gameRepository.closeGate(gate);
            gameRepository.shutDown();

            playerConnector.stop(); //Stop while loop
            playerConnectorThread.interrupt(); //Interrupt blocking calls

            playersConnected.stop();
            playersConnectedThread.interrupt();

            teamDistributor.stop();
            teamDistributorThread.interrupt();
        }
    }

    private void launchGameServer(Space server, Space game) throws InterruptedException {
        System.out.println("Server: Game server has been launched");
        Object[][] regUsers = server.queryAll(connectedUser.getFields()).toArray(new Object[0][]);
        String[] users = new String[regUsers.length];
        int[] teams = new int[regUsers.length];
        for(int i = 0; i < regUsers.length; i++){
            users[i] = (String) regUsers[i][1];
            teams[i] = (Integer) regUsers[i][2];
        }
        MainGame mainGame = new MainGame(host, users, teams, version, game);
        mainGame.startGame();
    }
}

class PlayerConnector implements Runnable{

    private Space game, server;
    private volatile boolean exit;
    private String host;
    private final int version;
    private final int numberOfTeams;

    public PlayerConnector(Space game, Space server, String host, int version, int numberOfTeams){
        this.game = game;
        this.server = server;
        this.host = host;
        this.version = version;
        this.numberOfTeams = numberOfTeams;
    }

    public void run() {
        try {
            int maxPlayers = (Integer) server.query(new ActualField("maxNumberOfPlayers"), new FormalField(Integer.class))[1];
            while(!exit){
                //Look for connection requests
                Object[] req = game.get(connectToGameReq.getFields());
                String username = (String) req[1];
                System.out.println("Server: " + username + " is trying to connect");

                //Check if user with that name already connected
                if(server.queryp(new ActualField("connectedUser"), new ActualField(username)) != null){
                    System.out.println("Server: User with that username already connected");
                    game.put("connectToGameAck", username, "ko");
                    continue;
                }

                int n = (Integer) server.get(numberOfPlayers.getFields())[1];

                //Max number of players in lobby?
                if (n != maxPlayers) {

                    //Wait for user to connect
                    System.out.println("Server: Connection approved");
                    game.put("connectToGameAck", username, "ok");

                    //Add user to game lobby and update other users
                    server.put("connectedUser", username, 0);
                    System.out.println("Server: " + username + " connected to lobby");
                    server.put("numberOfPlayers", n+1);

                    StringBuilder connectedUsers = new StringBuilder();

                    //Inform connected users of newly connected user
                    Object[][] users = server.queryAll(connectedUser.getFields()).toArray(new Object[0][]);
                    for (Object[] user: users) {
                        connectedUsers.append(user[1]).append(" ");
                        game.put("lobbyUpdate", "connected", username, user[1], 0);
                    }
                    game.put("lobbyInfo", username, connectedUsers.toString());
                    game.put("lobbyInfoVersion", username, version);
                    game.put("lobbyInfoNTeams", username, numberOfTeams);
                    game.put("lobbyInfoHost", username, host);
                } else {

                    game.put("connectToGameAck", username, "ko");
                    System.out.println("Server: Lobby full");

                }
            }
        } catch (InterruptedException e) {

        }
    }
    public void stop(){
        exit = true;
    }
}

class PlayersConnected implements Runnable {

    private Space server, game;
    private volatile boolean exit;


    public PlayersConnected(Space server, Space game) {
        this.server = server;
        this.game = game;
    }

    public void run() {
        try{
            while(!exit) {
                Object[][] regUsers = server.queryAll(connectedUser.getFields()).toArray(new Object[0][]);
                for(int i = 0; i < regUsers.length; i++){
                    game.put("lobbyUpdate", "ping", "", regUsers[i][1], 0);
                    Thread.sleep(1000);
                    Object[] ack = game.getp(new ActualField("pingack"), new ActualField(regUsers[i][1]));
                    if(ack == null){
                        Object[][] users = server.queryAll(connectedUser.getFields()).toArray(new Object[0][]);
                        for (Object[] user: users) {
                            game.put("lobbyUpdate", "disconnected", regUsers[i][1], user[1], 0);
                        }
                        System.out.println("User: " + regUsers[i][1] + " disconnected from the lobby");
                        server.get(new ActualField("connectedUser"), new ActualField(regUsers[i][1]), new FormalField(Integer.class));
                    }
                }

            }
        } catch(Exception e){

        }
    }

    public void stop(){
        exit = true;
    }
}

class TeamDistributor implements Runnable {

    private volatile boolean exit;
    private Space server, game;
    private int numberOfTeams;
    private int[] teams = new int[3];

    public TeamDistributor(Space server, Space game, int numberOfTeams) {
        this.server = server;
        this.game = game;
        this.numberOfTeams = numberOfTeams;
    }

    public void run(){

        while(!exit){
            try {
                Object[] info = game.get(new ActualField("team"), new FormalField(String.class), new FormalField(String.class), new FormalField(Integer.class));
                int team = (Integer) info[3];
                String name = (String) info[2];
                if(((String) info[1]).matches("joinTeam")) {
                    System.out.println("Server: " + name + " trying to join team " + team);
                    if (canJoinTeam(team)) {
                        teams[team - 1]++;
                        Object[] userinfo = server.get(new ActualField("connectedUser"), new ActualField(name), new FormalField(Integer.class));
                        if((Integer) userinfo[2] != 0){
                            System.out.println("Server: already on team " + team);
                            game.put("joinTeamAck", name, "ko");
                            return;
                        }
                        server.put("connectedUser", name, team);
                        game.put("joinTeamAck", name, "ok");
                        Object[][] users = server.queryAll(connectedUser.getFields()).toArray(new Object[0][]);
                        for (Object[] user : users) {
                            game.put("lobbyUpdate", "joinedTeam", name, user[1], team);
                        }
                        System.out.println("Server: " + name + " joined team " + team);
                    } else {
                        System.out.println("Server: team " + team + " full");
                        game.put("joinTeamAck", name, "ko");
                    }
                } else {
                    teams[team - 1]--;
                    server.get(new ActualField("connectedUser"), new ActualField(name), new FormalField(Integer.class));
                    server.put("connectedUser", name, 0);
                    game.put("leaveTeamAck", name, "ok");
                    Object[][] users = server.queryAll(connectedUser.getFields()).toArray(new Object[0][]);
                    for (Object[] user : users) {
                        game.put("lobbyUpdate", "leftTeam", name, user[1], 0);
                    }
                    System.out.println("Server: " + name + " joined team " + team);
                }

            } catch (InterruptedException e) {

            }
        }
    }

    private boolean canJoinTeam(int team){
        if(numberOfTeams == 2){
            return (team == 1 && teams[0] < 2) || (team == 2 && teams[1] < 2);
        } else {
            return (team == 1 && teams[0] < 2) || (team == 2 && teams[1] < 2)|| (team == 3 && teams[2] < 2);
        }
    }

    public void stop(){
        exit = true;
    }
}
