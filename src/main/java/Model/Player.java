package Model;

public class Player {
    private int team;
    private int homePos;
    private String username;
    private boolean done;
    public Player(String username, int team, int homePos){
        this.username=username;
        this.team=team;
        this.homePos=homePos;
    }

    public int getTeam() {
        return team;
    }

    public void setTeam(int team) {
        this.team = team;
    }

    public int getHomePos() {
        return homePos;
    }

    public void setHomePos(int homePos) {
        this.homePos = homePos;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }


}
