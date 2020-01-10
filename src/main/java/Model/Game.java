package Model;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.Space;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static Model.Templates.*;

public class Game implements Runnable {
    private int N = 10;
    private List<Object[]> users = null;
    private Space space;
    private char[] board = new char[92];

    public Game(Space space, String users[]) {
        this.space = space;
    }

    @Override
    public void run() {


        try {
            users = space.queryAll(new FormalField(Object.class));
            shuffleCards(users);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        while (true) {
            try {
                Object[] potentialMove = space.query(move.getFields()); //Object arr should contain which piece wants to move, it's position, where it wants to move, and the type of card.

                space.get(move.getFields()); //Move is legal, and card is played/removed from the users hand
                updateBoard();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    public void shuffleCards(List<Object[]> users) throws InterruptedException {
        //Missing something which ensures that no more than 4 cards of the same kind are handed out.
        List<Cards> deck;
        for(int i=0; i<4; i++){
        Arrays.asList(Cards.values());}
        //Make list of crds
        Cards[] hand = new Cards[4];
        Random random = new Random();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < 4; j++) {
/*
                hand[j] = deck[random.nextInt(deck.length)];
*/
            }
            space.put(users.get(i)[0], hand);
        }
    }

    public void move(Object[] potentialMove) {
        Cards card = (Cards) potentialMove[3];
/*
        if(card.getFunction().matches("fw") && potentialMove[1])
*/
    }

    public void updateBoard() {

    }


    //moves piece to new position

}