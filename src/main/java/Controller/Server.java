package Controller;

import Protocol.MainGame;
import org.jspace.*;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;

import static Protocol.Templates.*;


public class Server implements Runnable {

    private Space game;
    private int numberOfTeams, version;
    private String host, gate;

    public Server(int numberOfTeams, int version, String host, Space game) {
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
            if (version == 0) {
                server.put("maxNumberOfPlayers", 4);
            } else {
                server.put("teamPlayers", 3, 0);
                server.put("maxNumberOfPlayers", 6);
            }

            //Setting up URI
            //inetAddress = InetAddress.getLocalHost() will not always get the correct interface
            //So need to iterate over all interfaces to get the correct one
            //TODO: Find out how to automatically select the correct interface
            InetAddress inetAddress = null;
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(interfaces)) {
                if (netint.getName().matches("eth6")) {
                    //System.out.println(netint.getName());
                    Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
                    for (InetAddress inetAddresss : Collections.list(inetAddresses)) {
                        inetAddress = inetAddresss;
                        //System.out.printf("\tInetAddress: %s\n", inetAddresss);
                        break;
                    }
                }
            }

            String ip = inetAddress.getHostAddress();
            int port = 11345;
            gate = "tcp://" + ip + ":" + port + "?keep";
            game.put("IPPort", ip + ":" + port);
            System.out.println("A game is hosted on IP: " + ip + ":" + port);

            // Opening gate at given URI
            gameRepository.add("game", game);
            gameRepository.addGate(gate);

            //TODO: Find out if 3 threads are needed
            //Look for players connecting
            playerConnector = new PlayerConnector(game, server, host, version, numberOfTeams);
            playerConnectorThread = startThread(playerConnector);

            //Check players are still connected
            playersConnected = new PlayersConnected(server, game);
            playersConnectedThread = startThread(playersConnected);

            //Look for joinTeam requests
            teamDistributor = new TeamDistributor(server, game, numberOfTeams, version);
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

            //TODO: Make this do something
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

    private Thread startThread(Runnable object) {
        Thread objectThread = new Thread(object);
        objectThread.setDaemon(true);
        objectThread.start();
        return objectThread;
    }

    private void launchGameServer(Space server, Space game) throws InterruptedException {
        System.out.println("Server: Game server has been launched");
        Object[][] regUsers = server.queryAll(connectedUser).toArray(new Object[0][]);
        String[] users = new String[regUsers.length];
        int[] teams = new int[regUsers.length];
        for (int i = 0; i < regUsers.length; i++) {
            users[i] = (String) regUsers[i][1];
            teams[i] = (Integer) regUsers[i][2];
        }
        MainGame mainGame = new MainGame(host, users, teams, version, game);
        mainGame.startGame();
    }
}

class PlayerConnector implements Runnable {

    private final int version;
    private final int numberOfTeams;
    private Space game, server;
    private volatile boolean exit;
    private String host;

    public PlayerConnector(Space game, Space server, String host, int version, int numberOfTeams) {
        this.game = game;
        this.server = server;
        this.host = host;
        this.version = version;
        this.numberOfTeams = numberOfTeams;
    }

    public void run() {
        try {
            int maxPlayers = (Integer) server.query(maxNumberOfPlayers)[1];
            while (!exit) {
                //Look for connection requests
                Object[] req = game.get(connectToGameReq);
                String username = (String) req[1];
                System.out.println("Server: " + username + " is trying to connect");

                //Check if user with that name already connected
                if (server.queryp(connectedUserSpecific(username)) != null) {
                    System.out.println("Server: User with that username already connected");
                    game.put("connectToGameAck", username, "ko");
                    continue;
                }

                //Need to get disconnected token if user has previously been connected
                game.getp(lobbyUpdateDisconnected(username, username));

                //Max number of players in lobby?
                int n = (Integer) server.get(numberOfPlayers)[1];
                if (n != maxPlayers) {
                    //Wait for user to connect
                    game.put("connectToGameAck", username, "ok");

                    //Add user to game lobby and update other users
                    server.put("connectedUserSpecific", username, 0);
                    System.out.println("Server: " + username + " connected to lobby");
                    server.put("numberOfPlayers", n + 1);

                    StringBuilder connectedUsers = new StringBuilder();
                    StringBuilder userTeams = new StringBuilder();

                    //Inform connected users of newly connected user
                    Object[][] users = server.queryAll(connectedUser).toArray(new Object[0][]);
                    for (Object[] user : users) {
                        userTeams.append(user[2]).append(" ");
                        connectedUsers.append(user[1]).append(" ");
                        game.put("lobbyUpdate", "connected", username, user[1], 0);
                    }

                    //Give newly connected user lobby information
                    game.put("lobbyInfoUsers", username, connectedUsers.toString());
                    game.put("lobbyInfoTeams", username, userTeams.toString());
                    game.put("lobbyInfoVersion", username, version);
                    game.put("lobbyInfoNTeams", username, numberOfTeams);
                    game.put("lobbyInfoHost", username, host);
                } else {
                    //Server is full
                    server.put("numberOfPlayers", n);
                    game.put("connectToGameAck", username, "ko");
                    System.out.println("Server: Lobby full");

                }
            }
        } catch (InterruptedException e) {

        }
    }

    public void stop() {
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
        try {
            while (!exit) {
                //Ping all users
                Object[][] regUsers = server.queryAll(connectedUser).toArray(new Object[0][]);
                for (int i = 0; i < regUsers.length; i++) {
                    String username = (String) regUsers[i][1];
                    game.put("lobbyUpdate", "ping", "", username, 0);
                    Thread.sleep(300);
                    Object[] ack = game.getp(pingACK(username));
                    //If a user doesn't respond within 0.3 seconds they have been disconnected
                    if (ack == null) {
                        //Inform all users of disconnected user
                        Object[][] users = server.queryAll(connectedUser).toArray(new Object[0][]);
                        for (Object[] user : users) {
                            game.put("lobbyUpdate", "disconnected", user, user[1], 0);
                        }

                        //Update server information
                        System.out.println("User: " + username + " disconnected from the lobby");
                        Object[] user = server.get(connectedUserSpecific(username));
                        int team = (Integer) user[2];
                        Object[] teamInfo = server.get(teamPlayers(team));
                        server.put("teamPlayers", user[2], (Integer) teamInfo[2] - 1);
                    }
                }

            }
        } catch (Exception e) {

        }
    }

    public void stop() {
        exit = true;
    }
}

class TeamDistributor implements Runnable {

    private volatile boolean exit;
    private Space server, game;
    private int numberOfTeams;
    private int version;

    public TeamDistributor(Space server, Space game, int numberOfTeams, int version) {
        this.server = server;
        this.game = game;
        this.numberOfTeams = numberOfTeams;
        this.version = version;
    }

    public void run() {
        while (!exit) {
            try {
                Object[] info = game.get(teamReq);
                int team = (Integer) info[3];
                String name = (String) info[2];
                if (((String) info[1]).matches("joinTeam")) {
                    if (canJoinTeam(team)) {
                        //Can't join new team if already on team
                        Object[] userinfo = server.get(connectedUserSpecific(name));
                        if ((Integer) userinfo[2] != 0) {
                            game.put("joinTeamAck", name, "ko");
                            server.put("connectedUserSpecific", name, userinfo[2]);
                            return;
                        }

                        //Update teaminfo
                        Object[] teamInfo = server.get(teamPlayers(team));
                        server.put("teamPlayers", team, (Integer) teamInfo[2] + 1);
                        server.put("connectedUserSpecific", name, team);
                        game.put("joinTeamAck", name, "ok");

                        //Inform users of team update
                        Object[][] users = server.queryAll(connectedUser).toArray(new Object[0][]);
                        for (Object[] user : users) {
                            game.put("lobbyUpdate", "joinedTeam", name, user[1], team);
                        }
                    } else {
                        game.put("joinTeamAck", name, "ko");
                    }
                } else {
                    //Leave team
                    Object[] teamInfo = server.get(teamPlayers(team));
                    server.put("teamPlayers", team, (Integer) teamInfo[2] - 1);
                    server.get(connectedUserSpecific(name));
                    server.put("connectedUserSpecific", name, 0);
                    game.put("leaveTeamAck", name, "ok");

                    //Inform users of team update
                    Object[][] users = server.queryAll(connectedUser).toArray(new Object[0][]);
                    for (Object[] user : users) {
                        game.put("lobbyUpdate", "leftTeam", name, user[1], 0);
                    }
                }

            } catch (InterruptedException e) {

            }
        }
    }

    private boolean canJoinTeam(int team) {
        int team1 = 0, team2 = 0, team3 = 0;
        try {
            team1 = (Integer) server.query(teamPlayers(1))[2];
            team2 = (Integer) server.query(teamPlayers(2))[2];
            if (numberOfTeams == 3) {
                team3 = (Integer) server.query(teamPlayers(3))[2];
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //Could be turned into one large return statement but would most likely be confusing
        if (version == 0) {
            return (team == 1 && team1 < 2) || (team == 2 && team2 < 2);
        } else {
            if(numberOfTeams == 2){
                return (team == 1 && team1 < 3) || (team == 2 && team2 < 3);
            } else {
                return (team == 1 && team1 < 2) || (team == 2 && team2 < 2) || (team == 3 && team3 < 2);
            }

        }
    }

    public void stop() {
        exit = true;
    }
}
