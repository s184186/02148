package Protocol;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.Template;

public final class Templates {
    public static final Template connectToGameReq = new Template(new ActualField("connectToGameReq"), new FormalField(String.class));
    public static final Template numberOfPlayers = new Template(new ActualField("numberOfPlayers"), new FormalField(Integer.class));
    public static final Template connect(String s){ return new Template(new ActualField("connect"), new ActualField(s), new FormalField(String.class));}
    public static final Template connectToGameAck(String s){ return new Template(new ActualField("connectToGameAck"), new ActualField(s), new FormalField(String.class));}
    public static final Template lobbyInfo(String s){ return new Template(new ActualField("lobbyInfo"), new ActualField(s), new FormalField(String.class));}

    public static final Template lobbyUpdate(String receiver){ return new Template(new ActualField("lobbyUpdate"), new FormalField(String.class), new FormalField(String.class), new ActualField(receiver), new FormalField(Integer.class));}

    public static final Template connectedUser = new Template(new ActualField("connectedUser"), new FormalField(String.class), new FormalField(Integer.class));
}
