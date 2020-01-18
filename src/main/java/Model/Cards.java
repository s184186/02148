package Model;

public enum Cards {
    ONE("One", 1, "fw"),
    TWO("Two", 2, "fw"),
    THREE("Three", 3, "fw"),
    FOUR("Four", -4, "bw"),
    FOURFW("Four", 4, "fw"),
    FIVE("Five", 5, "fw"),
    SIX("Six", 6, "fw"),
    SEVEN("Seven", 7, "sp"),
    EIGHT("Eight", 8, "fw"),
    NINE("Nine", 9, "fw"),
    TEN("Ten", 10, "fw"),
    TWELVE("Twelve", 12, "fw"),
    THIRT("Thirteen", 13, "fw"),
    FOURT("Fourteen", 14, "fw"),
    HEART("Heart", 0, "unl"),
    SWITCH("Switch", 0, "sw"),
    EIGHT_H("EightH", 0, "ch"),
    THIRT_H("ThirteenH", 0, "ch"),
    ONE_FOURT("OneFourteen", 0, "fw");


    private String name;
    private int moves;
    private String function;

    Cards(String name, int moves, String function) {
        this.name = name;
        this.moves = moves;
        this.function = function;
    }

    public String getName() {
        return name;
    }

    public int getMoves() {
        return moves;
    }

    public String getFunction() {
        return function;
    }

    public Cards getEnumByNoOfMoves(int moves){
        for(Cards x : Cards.values()){
            if (x.getMoves()==moves) return x;
        }
        return null;
    }
}