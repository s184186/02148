package Model;

public class BoardField {
    private String[] pieces; //List of pieces on field
    private String homeField; //If field is a homeField, the string homeField will contain the person whose homefield it is
    private String endField; ///If field is a endField, the string homeField will contain the person whose homefield it is
    private boolean locked; //Can the piece on this field move. Is false by default, but is set to true, when a piece is as close to the center as it can possibly be.
    private boolean protect;
    public BoardField(String[] pieces, String homeField, String endField){
        this.pieces=pieces;
        this.homeField=homeField;
        this.endField=endField;
    }

    public String[] getPieces() {
        return pieces;
    }

    public void setPieces(String[] pieces) {
        this.pieces = pieces;
    }

    public String getHomeField() {
        return homeField;
    }

    public void setHomeField(String homeField) {
        this.homeField = homeField;
    }

    public String getEndField() {
        return endField;
    }

    public void setEndField(String endField) {
        this.endField = endField;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isProtect() {
        return protect;
    }

    public void setProtect(boolean protect) {
        this.protect = protect;
    }
}
