package Lobby;

import com.google.gson.Gson;
import org.jspace.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;

import static Lobby.Templates.*;

public class Server implements Runnable {

    private Space gameSpace;
    private int numberOfTeams, version, maxNumberOfPlayers = 4;
    private String host;

    Server(int numberOfTeams, int version, String host, Space gameSpace) {
        this.numberOfTeams = numberOfTeams;
        this.version = version;
        this.host = host;
        this.gameSpace = gameSpace;
    }

    public void run() {
        try {
            //Create game serverSpace
            SpaceRepository gameRepository = new SpaceRepository();

            //Space where serverSpace threads communicate
            SequentialSpace serverSpace = new SequentialSpace();

            //Keep track of connected players
            serverSpace.put("numberOfPlayers", 0);
            serverSpace.put("teamPlayers", 1, 0);
            serverSpace.put("teamPlayers", 2, 0);

            if (version == 1) {
                maxNumberOfPlayers = 6;
                serverSpace.put("teamPlayers", 3, 0);
            }

            //Setting up URI
            //inetAddress = InetAddress.getLocalHost() will not always get the correct interface
            //So need to iterate over all interfaces to get the correct one
            //TODO: Find out how to automatically select the correct interface

            InetAddress inetAddress = InetAddress.getLocalHost();
            String ip = inetAddress.getHostAddress();
            int port = 11345;
            String gate = "tcp://" + ip + ":" + port + "?keep";
            gameSpace.put("IPPort", ip + ":" + port);
            System.out.println("A game is hosted on IP: " + ip + ":" + port);

            // Opening gate at given URI
            gameRepository.add("game", gameSpace);
            gameRepository.addGate(gate);

            //Look for players connecting
            LobbyRequestReceiver lobbyRequestReceiver = new LobbyRequestReceiver(this, gameSpace, serverSpace);
            startThread(lobbyRequestReceiver); //Don't need thread object since it closes automatically

            //Check players are still connected
            UserPinger userPinger = new UserPinger(gameSpace, serverSpace);
            Thread userPingerThread = startThread(userPinger);

            Object[] gameUpdate = serverSpace.get(new ActualField("gameUpdate"), new FormalField(String.class));
            if(((String) gameUpdate[1]).matches("startGame")){
                userPinger.stop(); //Stop while loop
                userPingerThread.interrupt(); //Interrupt blocking calls
                launchGameServer(serverSpace, gameSpace);
            } else {
                userPinger.stop(); //Stop while loop
                userPingerThread.interrupt(); //Interrupt blocking calls
                gameRepository.closeGate(gate);
                gameRepository.shutDown();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void launchGameServer(Space server, Space game) throws InterruptedException {
        System.out.println("Server: Game serverSpace has been launched");
        Object[][] regUsers = server.queryAll(connectedUser).toArray(new Object[0][]);
        String[] users = new String[regUsers.length];
        int[] teams = new int[regUsers.length];
        for (int i = 0; i < regUsers.length; i++) {
            users[i] = (String) regUsers[i][1];
            teams[i] = (Integer) regUsers[i][2];
        }
        MainGame mainGame = new MainGame(host, users, teams, version, game, numberOfTeams);
        mainGame.startGame();
    }

    private Thread startThread(Runnable object) {
        Thread objectThread = new Thread(object);
        objectThread.setDaemon(true);
        objectThread.start();
        return objectThread;
    }

    public int getMaxNumberOfPlayers() {
        return this.maxNumberOfPlayers;
    }

    public int getVersion(){
        return this.version;
    }

    public int getNumberOfTeams() {
        return numberOfTeams;
    }

    public String getHost() {
        return this.host;
    }
}

class LobbyRequestReceiver implements Runnable {

    private volatile boolean exit;
    private Server server;
    private final Space gameSpace;
    private final Space serverSpace;

    public LobbyRequestReceiver(Server server, Space gameSpace, Space serverSpace) {
        this.server = server;
        this.gameSpace = gameSpace;
        this.serverSpace = serverSpace;
    }

    public void run() {
        Gson gson = new Gson();
        try {
            while (!exit) {
                //Look for connection requests
                Object[] req = gameSpace.get(lobbyRequest);

                String type = (String) req[1];
                String username = (String) req[2];
                int team = (Integer) req[3];
                String info = (String) req[4];

                Object[] teamInfo;
                Object[][] usersConnected;

                switch (type){
                    case "sendMessage":
                        usersConnected = serverSpace.queryAll(connectedUser).toArray(new Object[0][]);
                        for (Object[] user : usersConnected) {
                            gameSpace.put("lobbyUpdate", "chatMessage", username, user[1], 0, info);
                        }
                        break;

                    case "leaveTeam":
                        //Leave team
                        teamInfo = serverSpace.get(teamPlayers(team));
                        serverSpace.put("teamPlayers", team, (Integer) teamInfo[2] - 1);
                        serverSpace.get(connectedUserSpecific(username));
                        serverSpace.put("connectedUserSpecific", username, 0);
                        gameSpace.put("leaveTeamAck", username, "ok");

                        //Inform users of team update
                        usersConnected = serverSpace.queryAll(connectedUser).toArray(new Object[0][]);
                        for (Object[] user : usersConnected) {
                            gameSpace.put("lobbyUpdate", "leftTeam", username, user[1], 0, "");
                        }
                        break;

                    case "joinTeam":
                        if (canJoinTeam(team)) {

                            //Can't join new team if already on team
                            Object[] userinfo = serverSpace.get(connectedUserSpecific(username));
                            if ((Integer) userinfo[2] != 0) {
                                gameSpace.put("joinTeamAck", username, "ko");
                                serverSpace.put("connectedUserSpecific", username, userinfo[2]);
                                return;
                            }

                            //Update teaminfo
                            teamInfo = serverSpace.get(teamPlayers(team));
                            serverSpace.put("teamPlayers", team, (Integer) teamInfo[2] + 1);
                            serverSpace.put("connectedUserSpecific", username, team);
                            gameSpace.put("joinTeamAck", username, "ok");

                            //Inform users of team update
                            usersConnected = serverSpace.queryAll(connectedUser).toArray(new Object[0][]);
                            for (Object[] user : usersConnected) {
                                gameSpace.put("lobbyUpdate", "joinedTeam", username, user[1], team, "");
                            }
                        } else {
                            gameSpace.put("joinTeamAck", username, "ko");
                        }
                        break;

                    case "connect":
                        //Check if user with that name already connected
                        if (serverSpace.queryp(connectedUserSpecific(username)) != null) {
                            gameSpace.put("connectToGameAck", username, "ko");
                            continue;
                        }

                        //Need to get disconnected token if user has previously been connected
                        gameSpace.getp(lobbyUpdateDisconnected(username, username));

                        //Max number of players in lobby?
                        int n = (Integer) serverSpace.get(numberOfPlayers)[1];
                        if (n != server.getMaxNumberOfPlayers()) {
                            //Wait for user to connect
                            gameSpace.put("connectToGameAck", username, "ok");

                            //Add user to game lobby and update other users
                            serverSpace.put("connectedUserSpecific", username, 0);
                            serverSpace.put("numberOfPlayers", n + 1);

                            //Inform connected users of newly connected user
                            usersConnected = serverSpace.queryAll(connectedUser).toArray(new Object[0][]);
                            String[] users = new String[usersConnected.length];
                            int[] teams = new int[usersConnected.length];

                            for (int i = 0; i < usersConnected.length; i++) {
                                users[i] = (String) usersConnected[i][1];
                                teams[i] = (Integer) usersConnected[i][2];
                                gameSpace.put("lobbyUpdate", "connected", username, usersConnected[i][1], 0, "");
                            }

                            //Give newly connected user lobby information
                            String usersJson = gson.toJson(users);
                            String teamsJson = gson.toJson(teams);
                            String[] userInfo = {usersJson, teamsJson, String.valueOf(server.getVersion()), String.valueOf(server.getNumberOfTeams()), server.getHost()};
                            String userInfoJson = gson.toJson(userInfo);

                            gameSpace.put("lobbyInfo", username, userInfoJson);

                        } else {
                            //Server is full
                            serverSpace.put("numberOfPlayers", n);
                            gameSpace.put("connectToGameAck", username, "ko");
                            System.out.println("Server: Lobby full");

                        }
                        break;

                    case "lobbyDisband":
                        usersConnected = serverSpace.queryAll(connectedUser).toArray(new Object[0][]);
                        for (Object[] user : usersConnected) {
                            if(!((String)user[1]).matches(server.getHost())) {
                                gameSpace.put("lobbyUpdate", "disconnected", user[1], user[1], 0, "");
                            }
                        }
                        serverSpace.put("gameUpdate","closeServer");
                        exit = true;
                        break;

                    case "startGame":
                        int numberOfPlayers = (int) serverSpace.query(Templates.numberOfPlayers)[1];
                        if(numberOfPlayers % 2 != 0 || numberOfPlayers <=2 || !username.matches(server.getHost())){
                            gameSpace.put("lobbyUpdate", "startGameAck", "", username, "ko");
                            break;
                        }
                        gameSpace.put("lobbyUpdate", "startGameAck", "", server.getHost(), "ko");

                        usersConnected = serverSpace.queryAll(connectedUser).toArray(new Object[0][]);
                        String[] users = new String[usersConnected.length];
                        int[] teams = new int[usersConnected.length];
                        for (int i = 0; i < usersConnected.length; i++) {
                            users[i] = (String) usersConnected[i][1];
                            teams[i] = (Integer) usersConnected[i][2];
                            gameSpace.put("lobbyUpdate", "connected", username, usersConnected[i][1], 0, "");
                        }
                        String usersJson = gson.toJson(users);
                        String teamsJson = gson.toJson(teams);
                        for (Object[] user : usersConnected) {
                            String[] userInfo = {usersJson, teamsJson, String.valueOf(server.getVersion()), String.valueOf(server.getNumberOfTeams()), server.getHost()};
                            String userInfoJson = gson.toJson(userInfo);
                            gameSpace.put("lobbyInfo", user[1], userInfoJson);
                            gameSpace.put("lobbyUpdate", "gameStart", "", user[1], 0, "");
                        }
                        serverSpace.put("gameUpdate","startGame");
                        exit = true;
                        break;

                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean canJoinTeam(int team) {
        int team1 = 0, team2 = 0, team3 = 0;
        try {
            team1 = (Integer) serverSpace.query(teamPlayers(1))[2];
            team2 = (Integer) serverSpace.query(teamPlayers(2))[2];
            if (server.getNumberOfTeams() == 3) {
                team3 = (Integer) serverSpace.query(teamPlayers(3))[2];
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Could be turned into one large return statement but would most likely be confusing
        if (server.getVersion() == 0) {
            return (team == 1 && team1 < 2) || (team == 2 && team2 < 2);
        } else {
            if(server.getNumberOfTeams() == 2){
                return (team == 1 && team1 < 3) || (team == 2 && team2 < 3);
            } else {
                return (team == 1 && team1 < 2) || (team == 2 && team2 < 2) || (team == 3 && team3 < 2);
            }

        }
    }
}

class UserPinger implements Runnable {

    private Space serverSpace;
    private Space gameSpace;
    private volatile boolean exit;

    public UserPinger(Space serverSpace, Space gameSpace) {
        this.serverSpace = serverSpace;
        this.gameSpace = gameSpace;
    }

    public void run() {
        try {
            while (!exit) {

                //Ping all users
                Object[][] regUsers = serverSpace.queryAll(connectedUser).toArray(new Object[0][]);

                for (Object[] regUser : regUsers) {

                    String username = (String) regUser[1];
                    gameSpace.put("lobbyUpdate", "ping", "", username, 0, "");
                    Thread.sleep(300);
                    Object[] ack = gameSpace.getp(lobbyUpdatePing(username));

                    //If a user doesn't respond within 0.3 seconds they have been disconnected
                    if (ack != null) {
                        int n = (Integer) serverSpace.get(numberOfPlayers)[1];
                        serverSpace.put("numberOfPlayers", n - 1);
                        //Inform all users of disconnected user
                        Object[][] users = serverSpace.queryAll(connectedUser).toArray(new Object[0][]);

                        for (Object[] user : users) {
                            gameSpace.put("lobbyUpdate", "disconnected", username, user[1], 0, "");
                        }

                        //Update serverSpace information
                        Object[] user = serverSpace.get(connectedUserSpecific(username));
                        int team = (Integer) user[2];
                        if (team != 0) {
                            Object[] teamInfo = serverSpace.get(teamPlayers(team));
                            serverSpace.put("teamPlayers", team, (Integer) teamInfo[2] - 1);
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
