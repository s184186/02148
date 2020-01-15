package Model;

public class CardObj {
    private Cards card;
    private int amount;


    public CardObj(Cards card){
        this.card=card;
        this.amount=4;
    }

    public Cards getCard() {
        return card;
    }

    public void setCard(Cards card) {
        this.card = card;
    }
    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}
