package Model;

public class Player {
    private int team;
    private int homeFieldPos;
    private String username;
    private int homeCirclePos;
    private boolean done;

    public int getHomeFieldPos() {
        return homeFieldPos;
    }

    public void setHomeFieldPos(int homeFieldPos) {
        this.homeFieldPos = homeFieldPos;
    }

    public int getHomeCirclePos() {
        return homeCirclePos;
    }

    public void setHomeCirclePos(int homeCirclePos) {
        this.homeCirclePos = homeCirclePos;
    }

    public Player(String username, int team, int homeFieldPos, int homeCirclePos){
        this.username=username;
        this.team=team;
        this.homeFieldPos=homeFieldPos;
        this.homeCirclePos=homeCirclePos;
    }

    public int getTeam() {
        return team;
    }

    public void setTeam(int team) {
        this.team = team;
    }

    public int getHomePos() {
        return homeFieldPos;
    }

    public void setHomePos(int homePos) {
        this.homeFieldPos = homePos;
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
