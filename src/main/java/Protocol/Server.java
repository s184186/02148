package Protocol;

import org.jspace.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static Protocol.Templates.*;


public class Server implements Runnable {

    private int numberOfTeams;
    private int version;
    private String host;

    public Server(int numberOfTeams, int version, String host){
        this.numberOfTeams = numberOfTeams;
        this.version = version;
        this.host = host;
    }

    public void run() {
        //Create game server
        SpaceRepository gameRepository = new SpaceRepository();

        //Space where users and server communicate
        SequentialSpace game = new SequentialSpace();

        //Space where server threads communicate
        SequentialSpace server = new SequentialSpace();

        //Keep track of connected players
        try {
            server.put("numberOfPlayers", 0);
            server.put("maxNumberOfPlayers", 6);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Setting up URI
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        String ip = inetAddress.getHostAddress();
        int port = 11345;

        String URI = "tcp://" + ip + ":" + port + "?keep";
        System.out.println("A game is hosted on URI: " + URI);

        // Opening gate at given URI
        gameRepository.addGate(URI);
        gameRepository.add("game", game);

        gameRepository.addGate("tcp://localhost:31415/?keep");

        //Look for players connecting
        PlayerConnector playerConnector = new PlayerConnector(game, server);
        Thread playerConnectorThread = new Thread(playerConnector);
        playerConnectorThread.start();

        //Check players are still connected
        PlayersConnected playersConnected = new PlayersConnected(server, game);
        Thread playersConnectedThread = new Thread(playersConnected);
        playersConnectedThread.start();

        //Look for joinTeam requests
        TeamDistributor teamDistributor = new TeamDistributor(server, game, numberOfTeams);
        Thread teamDistributorThread = new Thread(teamDistributor);
        teamDistributorThread.start();

        //Wait for host to start game
        try {
            game.query(new ActualField("startGame"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Server: host has started the game");

        playerConnector.stop(); //Stop while loop
        playerConnectorThread.interrupt(); //Interrupt blocking calls

        playersConnected.stop();
        playersConnectedThread.interrupt();

        teamDistributor.stop();
        teamDistributorThread.interrupt();

        try {
            launchGameServer(server, game);
        } catch (InterruptedException e) {
            e.printStackTrace();
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
    private int maxPlayers;
    private volatile boolean exit;

    public PlayerConnector(Space game, Space server){
        this.game = game;
        this.server = server;
    }

    public void run() {
        try {
            maxPlayers = (Integer) server.get(new ActualField("maxNumberOfPlayers"), new FormalField(Integer.class))[1];
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        while(!exit){
            try {

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
                    Object[] cnnct = game.get(connect(username).getFields());

                    if (((String) cnnct[2]).matches("yes")) {

                        //Add user to game lobby and update other users
                        server.put("connectedUser", username, 0);
                        System.out.println("Server: " + username + " connected to lobby");
                        server.put("numberOfPlayers", n+1);

                        String connectedUsers = "";

                        //Inform connected users of newly connected user
                        Object[][] users = server.queryAll(connectedUser.getFields()).toArray(new Object[0][]);
                        for (Object[] user: users) {
                            connectedUsers += user[1] + " ";
                            if(!username.matches((String) user[1])){
                                game.put("lobbyUpdate", user[1], username);
                            }
                        }
                        game.put("lobbyInfo", username, connectedUsers);
                    } else {

                        System.out.println(username + " refused connection");

                    }
                } else {

                    game.put("connectToGameAck", username, "ko");
                    System.out.println("Server: Lobby full");

                }
            } catch (Exception e) {

            }
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
                    game.put("lobbyUpdate", regUsers[i][1], "ping");
                    Thread.sleep(1000);
                    Object[] ack = game.getp(new ActualField("pingack"), new ActualField(regUsers[i][1]));
                    if(ack == null){
                        game.put("lobbyUpdate", regUsers[i][1], "disconnected");
                        System.out.println("User: " + regUsers[i][1] + " disconnected from the lobby");
                        server.get(new ActualField("connectedUser"), new ActualField(regUsers[i][1]));
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
                Object[] info = game.get(new ActualField("joinTeam"), new FormalField(String.class), new FormalField(Integer.class));
                int team = (Integer) info[2];
                System.out.println("Server: " + info[1] + " trying to join team " + team);
                if(canJoinTeam(team)){
                    teams[team - 1]++;
                    server.get(new ActualField("connectedUser"), new ActualField(info[1]), new FormalField(Integer.class));
                    server.put("connectedUser", info[1], info[2]);
                    game.put("joinTeamAck", info[1], "ok");
                    System.out.println("Server: " + info[1] + " joined team " + team);
                } else {
                    System.out.println("Server: team " + team + " full");
                    game.put("joinTeamAck", info[1], "ko");
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
