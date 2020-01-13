package Controller;

public class LobbyModel {

    private String username;
    private int version;
    private int numberOfTeams;
    private String host;
    private String ip;

    public void setUsername(String username) {
        this.username = username;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setNumberOfTeams(int numberOfTeams) {
        this.numberOfTeams = numberOfTeams;
    }

    public String getUsername() {
        return username;
    }

    public int getVersion(){
        return version;
    }

    public int getNumberOfTeams(){
        return numberOfTeams;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
