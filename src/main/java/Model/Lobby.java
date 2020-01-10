package Model;

import org.jspace.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Scanner;

public class Lobby {
    public static void main(String[] args) throws IOException, InterruptedException, IOException {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Choose 'HOST' or 'JOIN': ");
        String input = scanner.next();
        if (input.equals("HOST")) {
            SequentialSpace game = new SequentialSpace();
            SpaceRepository repository = new SpaceRepository();

            // Setting up URI
            InetAddress inetAddress = InetAddress.getLocalHost();
            String ip = inetAddress.getHostAddress();
            int port = 11345;

            String URI = "tcp://" + ip + ":" + port + "?keep";
            System.out.println("A game is hosted on URI: " + URI);

            // Opening gate at given URI
            repository.addGate(URI);
            repository.add("game", game);

           // playGame(game, true);

        } else if (input.equals("JOIN")) {
            System.out.println("Enter game URI... Format: tcp://<IP>:<PORT>");
            input = scanner.next();

            String hostUri = input + "/game?keep";

            RemoteSpace game = new RemoteSpace(hostUri);

            //playGame(game, false);

        }
    }

    private static void playGame(BoardField game, boolean sender) throws InterruptedException {
        Scanner scanner = new Scanner(System.in);
        if (sender) {
            while (true) {
                System.out.println("Input data to be transmitted: ");
                String data = scanner.next();
               // game.put(data);
            }
        } else {
            while (true) {
                // String receivedData = (String) game.getp(new FormalField(String.class))[0];
              //  System.out.println("Just received: " + receivedData);
            }
        }
    }
}