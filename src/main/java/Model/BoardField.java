package Model;

public class BoardField {
    private String[] pieces;
    private char homeField;
    private char endField;
    public BoardField(String[] pieces, char homeField, char endField){
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

    public char getHomeField() {
        return homeField;
    }

    public void setHomeField(char homeField) {
        this.homeField = homeField;
    }

    public char getEndField() {
        return endField;
    }

    public void setEndField(char endField) {
        this.endField = endField;
    }
}
