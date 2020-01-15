package Controller;

import org.jspace.*;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;

import static Controller.Templates.*;


public class Server implements Runnable {

    private Space game;
    private int numberOfTeams, version, maxNumberOfPlayers = 4;
    private String host, gate;

    Server(int numberOfTeams, int version, String host, Space game) {
        this.numberOfTeams = numberOfTeams;
        this.version = version;
        this.host = host;
        this.game = game;
    }

    public void run() {
        Thread lobbyRequestReceiverThread = null, playersConnectedThread = null;
        LobbyRequestReceiver lobbyRequestReceiver = null;
        UserPinger userPinger = null;
        SpaceRepository gameRepository = null;

        SequentialSpace server = null;
        try {
            //Create game server
            gameRepository = new SpaceRepository();

            //Space where server threads communicate
            server = new SequentialSpace();

            //Keep track of connected players
            server.put("numberOfPlayers", 0);
            server.put("teamPlayers", 1, 0);
            server.put("teamPlayers", 2, 0);

            if (version == 1) {
                maxNumberOfPlayers = 6;
                server.put("teamPlayers", 3, 0);
            }

            server.put("maxNumberOfPlayers", maxNumberOfPlayers);

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
                    inetAddress = inetAddresses.nextElement();
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

            //Look for players connecting
            lobbyRequestReceiver = new LobbyRequestReceiver(game, server, host, version, numberOfTeams);
            lobbyRequestReceiverThread = startThread(lobbyRequestReceiver);

            //Check players are still connected
            userPinger = new UserPinger(server, game);
            playersConnectedThread = startThread(userPinger);

            Object[] gameUpdate = server.get(new ActualField("gameUpdate"), new FormalField(String.class));
            if(((String) gameUpdate[1]).matches("startGame")){
                launchGameServer(server, game);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            userPinger.stop(); //Stop while loop
            playersConnectedThread.interrupt(); //Interrupt blocking calls

            gameRepository.closeGate(gate);
            gameRepository.shutDown();
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

class LobbyRequestReceiver implements Runnable {

    private final int version;
    private final int numberOfTeams;
    private Space game, server;
    private volatile boolean exit;
    private String host;

    public LobbyRequestReceiver(Space game, Space server, String host, int version, int numberOfTeams) {
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
                Object[] req = game.get(lobbyRequest);

                String type = (String) req[1];
                String username = (String) req[2];
                int team = (Integer) req[3];

                Object[] teamInfo;
                Object[][] users;

                switch (type){
                    case "leaveTeam":
                        //Leave team
                        teamInfo = server.get(teamPlayers(team));
                        server.put("teamPlayers", team, (Integer) teamInfo[2] - 1);
                        server.get(connectedUserSpecific(username));
                        server.put("connectedUserSpecific", username, 0);
                        game.put("leaveTeamAck", username, "ok");

                        //Inform users of team update
                        users = server.queryAll(connectedUser).toArray(new Object[0][]);
                        for (Object[] user : users) {
                            game.put("lobbyUpdate", "leftTeam", username, user[1], 0);
                        }
                        break;

                    case "joinTeam":
                        if (canJoinTeam(team)) {

                            //Can't join new team if already on team
                            Object[] userinfo = server.get(connectedUserSpecific(username));
                            if ((Integer) userinfo[2] != 0) {
                                game.put("joinTeamAck", username, "ko");
                                server.put("connectedUserSpecific", username, userinfo[2]);
                                return;
                            }

                            //Update teaminfo
                            teamInfo = server.get(teamPlayers(team));
                            server.put("teamPlayers", team, (Integer) teamInfo[2] + 1);
                            server.put("connectedUserSpecific", username, team);
                            game.put("joinTeamAck", username, "ok");

                            //Inform users of team update
                            users = server.queryAll(connectedUser).toArray(new Object[0][]);
                            for (Object[] user : users) {
                                game.put("lobbyUpdate", "joinedTeam", username, user[1], team);
                            }
                        } else {
                            game.put("joinTeamAck", username, "ko");
                        }
                        break;

                    case "connect":
                        //Check if user with that name already connected
                        if (server.queryp(connectedUserSpecific(username)) != null) {
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
                            server.put("numberOfPlayers", n + 1);

                            StringBuilder connectedUsers = new StringBuilder();
                            StringBuilder userTeams = new StringBuilder();

                            //Inform connected users of newly connected user
                            users = server.queryAll(connectedUser).toArray(new Object[0][]);
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
                        break;

                    case "lobbyDisband":
                        users = server.queryAll(connectedUser).toArray(new Object[0][]);
                        for (Object[] user : users) {
                            if(!((String)user[1]).matches(host)) {
                                game.put("lobbyUpdate", "disconnected", user[1], user[1], 0);
                            }
                        }
                        server.put("gameUpdate","closeServer");
                        exit = true;
                        break;

                    case "startGame":
                        users = server.queryAll(connectedUser).toArray(new Object[0][]);
                        server.put("gameUpdate","startGame");
                        for (Object[] user : users) {
                            game.put("lobbyUpdate", "gameStart", "", user[1], 0);
                        }
                        break;

                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        exit = true;
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
}

class UserPinger implements Runnable {

    private Space server, game;
    private volatile boolean exit;

    public UserPinger(Space server, Space game) {
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
                    Object[] ack = game.getp(lobbyUpdatePing(username));

                    //If a user doesn't respond within 0.3 seconds they have been disconnected
                    if (ack != null) {
                        int n = (Integer) server.get(numberOfPlayers)[1];
                        server.put("numberOfPlayers", n-1);
                        //Inform all users of disconnected user
                        Object[][] users = server.queryAll(connectedUser).toArray(new Object[0][]);

                        for (Object[] user : users) {
                            game.put("lobbyUpdate", "disconnected", username, user[1], 0);
                        }

                        //Update server information
                        System.out.println("User: " + username + " disconnected from the lobby");
                        Object[] user = server.get(connectedUserSpecific(username));
                        int team = (Integer) user[2];
                        if(team != 0){
                            Object[] teamInfo = server.get(teamPlayers(team));
                            server.put("teamPlayers", user[2], (Integer) teamInfo[2] - 1);
                        }
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        exit = true;
    }
}