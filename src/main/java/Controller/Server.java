package Controller;

import Protocol.MainGame;
import org.jspace.*;

import java.net.InetAddress;

import static Protocol.Templates.*;


public class Server implements Runnable {

    private Space game;
    private int numberOfTeams, version;
    private String host, gate;

    public Server(int numberOfTeams, int version, String host, Space game){
        this.numberOfTeams = numberOfTeams;
        this.version = version;
        this.host = host;
        this.game = game;
    }

    public void run() {
        Thread playerConnectorThread = null, playersConnectedThread = null, teamDistributorThread = null;
        PlayerConnector playerConnector = null;
        PlayersConnected playersConnected = null;
        TeamDistributor teamDistributor = null;
        SpaceRepository gameRepository = null;
        try {
            System.out.println("Starting server");

            //Create game server
            gameRepository = new SpaceRepository();

            //Space where server threads communicate
            SequentialSpace server = new SequentialSpace();

            //Keep track of connected players

            server.put("numberOfPlayers", 0);
            server.put("teamPlayers", 1, 0);
            server.put("teamPlayers", 2, 0);
            if(version==0){
                server.put("maxNumberOfPlayers", 4);
            } else {
                server.put("teamPlayers", 3, 0);
                server.put("maxNumberOfPlayers", 6);
            }

            // Setting up URI
            InetAddress inetAddress;
            inetAddress = InetAddress.getLocalHost();
            String ip = inetAddress.getHostAddress();
            int port = 11345;
            gate = "tcp://" + ip + ":" + port + "?keep";
            String URI = "tcp://" + ip + ":" + port + "/game?keep";
            game.put("IPPORT", ip + ":" + port);
            System.out.println("A game is hosted on IP: " + ip + ":" + port);
            // Opening gate at given URI
            gameRepository.add("game", game);
            gameRepository.addGate(gate);

            //Look for players connecting
            playerConnector = new PlayerConnector(game, server, host, version, numberOfTeams);
            playerConnectorThread = startThread(playerConnector);

            //Check players are still connected
            playersConnected = new PlayersConnected(server, game);
            playersConnectedThread = startThread(playersConnected);

            //Look for joinTeam requests
            teamDistributor = new TeamDistributor(server, game, numberOfTeams);
            teamDistributorThread = startThread(teamDistributor);

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

    private Thread startThread(Runnable object){
        Thread objectThread = new Thread(object);
        objectThread.setDaemon(true);
        objectThread.start();
        return objectThread;
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
                if(server.queryp(new ActualField("connectedUser"), new ActualField(username), new FormalField(Integer.class)) != null){
                    System.out.println("Server: User with that username already connected");
                    game.put("connectToGameAck", username, "ko");
                    continue;
                }

                int n = (Integer) server.get(numberOfPlayers.getFields())[1];
                game.getp(new ActualField("lobbyUpdate"), new ActualField("disconnected"), new ActualField(username), new ActualField(username), new ActualField(0));

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
                    StringBuilder userTeams = new StringBuilder();

                    //Inform connected users of newly connected user
                    Object[][] users = server.queryAll(connectedUser.getFields()).toArray(new Object[0][]);
                    for (Object[] user: users) {
                        userTeams.append(user[2]).append(" ");
                        connectedUsers.append(user[1]).append(" ");
                        game.put("lobbyUpdate", "connected", username, user[1], 0);
                    }
                    game.put("lobbyInfoUsers", username, connectedUsers.toString());
                    game.put("lobbyInfoTeams", username, userTeams.toString());
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
                    Thread.sleep(300);
                    Object[] ack = game.getp(new ActualField("pingack"), new ActualField(regUsers[i][1]));
                    if(ack == null){
                        Object[][] users = server.queryAll(connectedUser.getFields()).toArray(new Object[0][]);
                        for (Object[] user: users) {
                            game.put("lobbyUpdate", "disconnected", regUsers[i][1], user[1], 0);
                        }
                        System.out.println("User: " + regUsers[i][1] + " disconnected from the lobby");
                        Object[] user = server.get(new ActualField("connectedUser"), new ActualField(regUsers[i][1]), new FormalField(Integer.class));
                        Object[] teamInfo = server.get(new ActualField("teamPlayers"), new ActualField(user[2]), new FormalField(Integer.class));
                        server.put("teamPlayers", user[2], (Integer) teamInfo[2] - 1);
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
                    //System.out.println("Server: " + name + " trying to join team " + team);
                    if (canJoinTeam(team)) {
                        System.out.println("here1");
                        Object[] teamInfo = server.get(new ActualField("teamPlayers"), new ActualField(team), new FormalField(Integer.class));
                        System.out.println("here2");
                        server.put("teamPlayers", team, (Integer) teamInfo[2] + 1);
                        Object[] userinfo = server.get(new ActualField("connectedUser"), new ActualField(name), new FormalField(Integer.class));
                        if((Integer) userinfo[2] != 0){
                            //System.out.println("Server: already on team " + team);
                            game.put("joinTeamAck", name, "ko");
                            return;
                        }
                        server.put("connectedUser", name, team);
                        game.put("joinTeamAck", name, "ok");
                        Object[][] users = server.queryAll(connectedUser.getFields()).toArray(new Object[0][]);
                        for (Object[] user : users) {
                            game.put("lobbyUpdate", "joinedTeam", name, user[1], team);
                        }
                        //System.out.println("Server: " + name + " joined team " + team);
                    } else {
                        //System.out.println("Server: team " + team + " full");
                        game.put("joinTeamAck", name, "ko");
                    }
                } else {
                    Object[] teamInfo = server.get(new ActualField("teamPlayers"), new ActualField(team), new FormalField(Integer.class));
                    server.put("teamPlayers", team, (Integer) teamInfo[2] - 1);
                    server.get(new ActualField("connectedUser"), new ActualField(name), new FormalField(Integer.class));
                    server.put("connectedUser", name, 0);
                    game.put("leaveTeamAck", name, "ok");
                    Object[][] users = server.queryAll(connectedUser.getFields()).toArray(new Object[0][]);
                    for (Object[] user : users) {
                        game.put("lobbyUpdate", "leftTeam", name, user[1], 0);
                    }
                    //System.out.println("Server: " + name + " joined team " + team);
                }

            } catch (InterruptedException e) {

            }
        }
    }

    private boolean canJoinTeam(int team){
        int team1 = 0, team2 = 0, team3 = 0;
        try {
            team1 = (Integer) server.query(new ActualField("teamPlayers"), new ActualField(1), new FormalField(Integer.class))[2];
            team2 = (Integer) server.query(new ActualField("teamPlayers"), new ActualField(2), new FormalField(Integer.class))[2];
            if(numberOfTeams==3) {
                team3 = (Integer) server.query(new ActualField("teamPlayers"), new ActualField(3), new FormalField(Integer.class))[2];
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(numberOfTeams == 2){
            return (team == 1 && team1 < 2) || (team == 2 && team2 < 2);
        } else {
            return (team == 1 && team1 < 2) || (team == 2 && team2 < 2)|| (team == 3 && team3 < 2);
        }
    }

    public void stop(){
        exit = true;
    }
}
