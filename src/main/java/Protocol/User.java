package Protocol;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;
import org.jspace.Space;

import java.io.IOException;
import java.util.Scanner;

import static test.Templates.*;

public class User {

    private static boolean isHost = false;
    private static Scanner in = new Scanner(System.in);

    public static void main(String[] args) throws IOException, InterruptedException {

        System.out.println("Enter username:");
        String username = in.next();

        System.out.println("Create new game?(Y/N)");
        String answer = in.next();

        if(answer.matches("Y")){
            isHost = true;

            System.out.println("Plus version?(Y/N)");
            int version = 0;
            answer = in.next();

            int teams = 2;

            if(answer.matches("Y")){
                version = 1;
                System.out.println("How many teams?(2/3)");
                teams = in.nextInt();
            }

            new Thread(new Server(teams, version, username)).start();
        }

        String info;

        RemoteSpace game = new RemoteSpace("tcp://localhost:31415/game?keep");
        game.put("connectToGameReq", username);

        Object[] ack = game.get(connectToGameAck(username).getFields());

        if(((String) ack[2]).matches("ok")) {

            System.out.println("Connect to game?(Y/N)");
            String connect = in.next();

            if(connect.matches("Y")){

                game.put("connect", username, "yes");
                info = (String) game.get(lobbyInfo(username).getFields())[2];

                System.out.println("Connected users: " +info);

            }else{

                game.put("connect", username, "no");
                System.out.println("Go back to menu");
                System.exit(0);
            }
        } else {
            System.out.println("Go back to menu");
            System.exit(0);
        }

        LobbyUpdater lobbyUpdater = new LobbyUpdater(game, username);
        Thread lobbyUpdaterThread = new Thread(lobbyUpdater);
        lobbyUpdaterThread.start();

        System.out.println("Which team to join?");
        int team = in.nextInt();
        game.put("joinTeam", username, team);
        ack = game.get(new ActualField("joinTeamAck"), new ActualField(username), new FormalField(String.class));
        if(((String) ack[2]).matches("ok")){
            System.out.println("Joined team " + team);
        } else {
            System.out.println("Couldn't join team " + team);
        }

        if(isHost){
            System.out.println("Press any key to start game");
            in.next();
            game.put("startGame");
            System.out.println("Starting game");
        } else {
            game.query(new ActualField("startGame"));
            System.out.println("Host has started game");
        }

        lobbyUpdater.stop();

        launchGame();

    }

    private static void launchGame(){
        System.out.println("Game has been launched");
    }
}

class LobbyUpdater implements Runnable{

    private String username;
    private Space space;
    private volatile boolean exit;

    public LobbyUpdater(Space space, String username){
        this.space = space;
        this.username = username;
    }

    public void run() {
        String info = "";
        while(!exit){
            try {
                info = (String) space.get(lobbyUpdate(username).getFields())[2];
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(info.matches("disconnected")){

                System.out.println("Disconnected from lobby");
                System.exit(-1);

            } else if (info.matches("ping")){
                try {
                    space.put("pingack", username);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println(info + " connected to the lobby");
            }
        }
    }

    public void stop(){
        exit = true;
    }
}

