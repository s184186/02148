package Model;

public enum Cards {

    Three("Tre", 3, "fw"),
    Four("Fire", 4, "bw"),
    Fem("Fem", 4, "fw"),
    Seks("Seks", 4, "fw"),
    Syv("Syv", 4, "sp"),
    Ni("Ni", 4, "fw"),
    Ti("Ti", 4, "fw"),
    Twelve("Twelve", 4, "fw"),
    Hjerte("Hjerte", 4, "unl"),
    Byt("Byt", 4, "sw"),
    OtteH("OtteH", 4, "ch"),
    TretH("TretH", 4, "ch"),
    EtFjor("EtFjor", 4, "fw");


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
}