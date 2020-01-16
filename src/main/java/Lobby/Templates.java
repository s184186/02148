package Lobby;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.Template;
import org.jspace.TemplateField;

public final class Templates {
    public static final TemplateField[] lobbyRequest = new Template(new ActualField("lobbyRequest"), new FormalField(String.class), new FormalField(String.class), new FormalField(Integer.class), new FormalField(String.class)).getFields();
    public static final TemplateField[] numberOfPlayers = new Template(new ActualField("numberOfPlayers"), new FormalField(Integer.class)).getFields();
    public static final TemplateField[] connectedUser = new Template(new ActualField("connectedUserSpecific"), new FormalField(String.class), new FormalField(Integer.class)).getFields();
    public static final TemplateField[] IPPort = new Template(new ActualField("IPPort"), new FormalField(String.class)).getFields();

    public static final TemplateField[] teamPlayers(int team){
        return new Template(new ActualField("teamPlayers"), new ActualField(team), new FormalField(Integer.class)).getFields();
    }

    public static final TemplateField[] lobbyUpdateDisconnected(String sender, String receiver){
        return new Template(new ActualField("lobbyUpdate"), new ActualField("disconnected"), new ActualField(sender), new ActualField(receiver), new FormalField(Integer.class), new FormalField(String.class)).getFields();
    }

    public static final TemplateField[] connectedUserSpecific(String s){
        return new Template(new ActualField("connectedUserSpecific"), new ActualField(s), new FormalField(Integer.class)).getFields();
    }

    public static final TemplateField[] connectToGameAck(String s) {
        return new Template(new ActualField("connectToGameAck"), new ActualField(s), new FormalField(String.class)).getFields();
    }

    public static final TemplateField[] lobbyInfo(String s) {
        return new Template(new ActualField("lobbyInfo"), new ActualField(s), new FormalField(String.class)).getFields();
    }

    public static final TemplateField[] lobbyUpdate(String receiver) {
        return new Template(new ActualField("lobbyUpdate"), new FormalField(String.class), new FormalField(String.class), new ActualField(receiver), new FormalField(Integer.class), new FormalField(String.class)).getFields();
    }

    public static final TemplateField[] lobbyUpdatePing(String receiver) {
        return new Template(new ActualField("lobbyUpdate"), new ActualField("ping"), new FormalField(String.class), new ActualField(receiver), new FormalField(Integer.class), new FormalField(String.class)).getFields();
    }

}
