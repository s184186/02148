package Protocol;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.Template;
import org.jspace.TemplateField;

public final class Templates {
    public static final TemplateField[] connectToGameReq = new Template(new ActualField("connectToGameReq"), new FormalField(String.class)).getFields();
    public static final TemplateField[] numberOfPlayers = new Template(new ActualField("numberOfPlayers"), new FormalField(Integer.class)).getFields();
    public static final TemplateField[] maxNumberOfPlayers = new Template(new ActualField("maxNumberOfPlayers"), new FormalField(Integer.class)).getFields();
    public static final TemplateField[] connectedUser = new Template(new ActualField("connectedUserSpecific"), new FormalField(String.class), new FormalField(Integer.class)).getFields();
    public static final TemplateField[] teamReq = new Template(new ActualField("teamReq"), new FormalField(String.class), new FormalField(String.class), new FormalField(Integer.class)).getFields();
    public static final TemplateField[] IPPort = new Template(new ActualField("IPPort"), new FormalField(String.class)).getFields();

    public static final TemplateField[] teamPlayers(int team){
        return new Template(new ActualField("teamPlayers"), new ActualField(team), new FormalField(Integer.class)).getFields();
    }

    public static final TemplateField[] pingACK(String sender){
        return new Template(new ActualField("pingack"), new ActualField(sender)).getFields();
    }

    public static final TemplateField[] lobbyUpdateDisconnected(String sender, String receiver){
        return new Template(new ActualField("lobbyUpdate"), new ActualField("disconnected"), new ActualField(sender), new ActualField(receiver), new FormalField(Integer.class)).getFields();
    }

    public static final TemplateField[] connectedUserSpecific(String s){
        return new Template(new ActualField("connectedUserSpecific"), new ActualField(s), new FormalField(Integer.class)).getFields();
    }

    public static final TemplateField[] connectToGameAck(String s) {
        return new Template(new ActualField("connectToGameAck"), new ActualField(s), new FormalField(String.class)).getFields();
    }

    public static final TemplateField[] lobbyInfoHost(String s) {
        return new Template(new ActualField("lobbyInfoHost"), new ActualField(s), new FormalField(String.class)).getFields();
    }

    public static final TemplateField[] lobbyInfoVersion(String s) {
        return new Template(new ActualField("lobbyInfoVersion"), new ActualField(s), new FormalField(String.class)).getFields();
    }

    public static final TemplateField[] lobbyInfoUsers(String s) {
        return new Template(new ActualField("lobbyInfoUsers"), new ActualField(s), new FormalField(String.class)).getFields();
    }

    public static final TemplateField[] lobbyInfoNTeams(String s) {
        return new Template(new ActualField("lobbyInfoNTeams"), new ActualField(s), new FormalField(String.class)).getFields();
    }

    public static final TemplateField[] lobbyInfoTeams(String s) {
        return new Template(new ActualField("lobbyInfoTeams"), new ActualField(s), new FormalField(String.class)).getFields();
    }

    public static final TemplateField[] lobbyUpdate(String receiver) {
        return new Template(new ActualField("lobbyUpdate"), new FormalField(String.class), new FormalField(String.class), new ActualField(receiver), new FormalField(Integer.class)).getFields();
    }


}
