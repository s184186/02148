package Model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.Space;

import java.lang.reflect.Type;
import java.util.*;

import static Model.Cards.*;

public class Game implements Runnable {
    private boolean readOnly;
    private int noOfPlayers;
    private int lower = -1;
    private int upper = -2;
    private int offset;
    private int version;
    private String result;
    private Integer position;
    private Integer endPosition;
    private String[] users;
    private boolean[] finished;
    private String host;
    private Space game;
    private BoardField[] board;
    private int playerTurnIndex;
    private int decksize = 13;
    private int[] startPos = new int[4];
    private int[] teams;
    private int[] positions;
    private int winningTeam = -1;
    private boolean justStarted;
    private int numberOfTeams;
    private ArrayList<Cards>[] playerHands;
    private Integer[] pieceIndexes;
    private Gson gson = new Gson();
    private ArrayList<Player> players = new ArrayList<>();
    private ArrayList<Player> teamOne = new ArrayList<>();
    private ArrayList<Player> teamTwo = new ArrayList<>();
    private ArrayList<Player> teamThree = new ArrayList<>();
    private ArrayList<CardObj> deck;
    private volatile boolean exit;


    public Game(String host, String[] players, int[] teams, int version, Space game, int numberOfTeams) {
        this.host = host;
        this.game = game;
        this.users = players;
        this.numberOfTeams = numberOfTeams;
        this.teams = teams;
        this.noOfPlayers = players.length;
        this.board = new BoardField[noOfPlayers * 15 + noOfPlayers * 4 + noOfPlayers];
        this.version = version;
        this.finished = new boolean[noOfPlayers];
        this.justStarted = true;
        this.playerHands = new ArrayList[noOfPlayers];
        for (int i = 0; i < noOfPlayers; i++) {
            playerHands[i] = new ArrayList<>();
        }
        this.positions = new int[noOfPlayers * 4];
        this.pieceIndexes = new Integer[noOfPlayers * 4];
    }

    @Override
    public void run() {
        try {
            //Users will be represented as string list and handed down from server.
            setupBoard();
            shuffleCards(users);
            for (int i = 0; i < noOfPlayers; i++) {
                game.put("gameUpdate", "switchCard", "", users[i], "", "", "");
            }
            //initiate users to switch card. We need to make sure that same user, doesn't try to switch cards more than once.
            Object[][] switchInfos = new Object[noOfPlayers][];
            for (int i = 0; i < noOfPlayers; i++) {
                switchInfos[i] = game.get(new ActualField("gameRequest"), new ActualField("switchCard"),
                        new ActualField(users[i]), new FormalField(String.class), new FormalField(String.class)); //switch req consits of from, to and card fields
            }
            for (Object[] switchInfo : switchInfos) {
                switchCards(switchInfo);
            }
            // Move will be represented by position, the card used and username.
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int cantMakeMoveCounter = 0;
        Thread thread = new Thread(new GameEndedUpdater(game, host, users));
        thread.setDaemon(true);
        thread.start();
        while (winningTeam == -1 && !exit) {
            try {
                if (cantMakeMoveCounter == 4) {
                    shuffleCards(users);
                    cantMakeMoveCounter = 0;
                }
                if (!canUserMakeMove(users[playerTurnIndex])) {
                    playerTurnIndex = (playerTurnIndex + 1) % users.length;
                    cantMakeMoveCounter++;
                    continue;
                }
                cantMakeMoveCounter = 0;
                game.put("gameUpdate", "yourTurn", "", users[playerTurnIndex], "", "", "");
                result = "";
                ArrayList<Integer> pieces = null;
                ArrayList<Integer> pieceMovesToField = null;
                Cards card = null;
                while (!result.matches("ok")) {
                    System.out.println("here1");
                    Object[] potentialMove = game.get(new ActualField("gameRequest"), new ActualField("turnRequest"),
                            new ActualField(users[playerTurnIndex]), new FormalField(String.class), new FormalField(String.class), new FormalField(String.class)
                            , new FormalField(Integer.class)); // A basic move will b e represented by position, the card used and username and extra field.

                    Type listType = new TypeToken<ArrayList<Integer>>() {
                    }.getType();

                    String username = (String) potentialMove[2];
                    card = gson.fromJson((String) potentialMove[3], Cards.class);
                    pieces = gson.fromJson((String) potentialMove[4], listType); // de brikker der bliver flyttet på (deres indekser
                    pieceMovesToField = gson.fromJson((String) potentialMove[5], listType); //hvor langt brikker er flyttet frem. bliver kun brugt på syv'er
                    int chosenCard = (int) potentialMove[6];
                    System.out.println("pieces.get(0): " + pieces.get(0));
                    System.out.println("position " + positions[pieces.get(0)]);
                    System.out.println("card.getmoves: " + card.getMoves());
                    position = positions[pieces.get(0)];
                    endPosition = position + card.getMoves();

                    result = calculateMove(username, card, pieces, pieceMovesToField, chosenCard);
                    game.put("gameUpdate", "turnRequestAck", result, users[playerTurnIndex],
                            "", "", "");

                }
                playerHands[getIndexByUsername(users[playerTurnIndex])].remove(card);
                movePiece(pieces, pieceMovesToField);
                update(users[playerTurnIndex]);
                playerTurnIndex = (playerTurnIndex + 1) % users.length;

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Team " + winningTeam + " won");
    }

    public void exit() {
        exit = true;
    }

    private void setupDeck() {
        deck = new ArrayList<CardObj>() {
            {
                add(new CardObj(Cards.THREE));
                add(new CardObj(Cards.FOUR));
                add(new CardObj(Cards.FIVE));
                add(new CardObj(Cards.SIX));
                add(new CardObj(SEVEN));
                add(new CardObj(Cards.NINE));
                add(new CardObj(Cards.TEN));
                add(new CardObj(Cards.TWELVE));
                add(new CardObj(Cards.HEART));
                add(new CardObj(Cards.SWITCH));
                add(new CardObj(Cards.EIGHT_H));
                add(new CardObj(Cards.THIRT_H));
                add(new CardObj(Cards.ONE_FOURT));
            }
        };
    }

    private void shuffleCards(Object[] users) throws InterruptedException { //Cards are shuffled and handed out to users and the player to start is chosen
        if (deck == null || getCardsLeftInDeck() < noOfPlayers * 4) { //If there aren't enough cards left to hand out make a deck consisting of all the used cards and all the unused ones
            setupDeck();
        }

        Random random = new Random();
        for (int i = 0; i < noOfPlayers; i++) {
            ArrayList<Cards> hand = new ArrayList<>();
            for (int j = 0; j < 4; j++) {
                int index = random.nextInt(decksize);
                int amount = deck.get(index).getAmount();
                deck.get(index).setAmount(amount - 1);
                hand.add(deck.get(index).getCard());
                if (amount == 1) { //if last card of a specific type in deck, this type will be removed
                    deck.remove(index);
                    decksize--;
                }
            }
            playerHands[i] = hand;
            String handJson = gson.toJson(hand);
            game.put("gameUpdate", "hand", "", users[i], handJson, "", ""); //Each user's hand is put in the tuple space. The users name is the id factor.
        }
        if (justStarted) {
            playerTurnIndex = random.nextInt(users.length);
            justStarted = false;
        }
    }

    private int getCardsLeftInDeck() {
        int cardsleft = 0;
        for (CardObj x : deck) {
            cardsleft += x.getAmount();
        }
        return cardsleft;
    }

    // update playerHands
    private void switchCards(Object[] switchInfo) throws InterruptedException {
        Cards card = gson.fromJson((String) switchInfo[3], Cards.class);
        String username = (String) switchInfo[2];
        playerHands[getIndexByUsername(username)].remove(card);
        int index = getPlayerIndexToTheLeftOfUsername(username);
        playerHands[index].add(card);
        String handJson = gson.toJson(new Cards[]{card});
        game.put("gameUpdate", "getSwitchedCard", username, users[index], handJson, "", "");
    }

    private String calculateMove(String username, Cards card, ArrayList<Integer> pieces, ArrayList<Integer> pieceMoves, int chosenCard) {

        int homefieldPos = -1;
        int position = positions[pieces.get(0)];
        //can either represent the piece a user wants to switch positions with, or in case of the card seven, how many moves forward this piece should move
        endPosition = position + card.getMoves();

        switch (card) {
            case FOUR: //move backwards
                if (position > 59)
                    return "illegal move!"; // you can't move something in the homecircles with a number card

                if (!finished[playerTurnIndex] && !board[position].getPieces()[0].matches(username)) {
                    return "illegal move!";
                }//If you haven't finished but you're trying to move another person's pieces, it's illegal.
                if (finished[playerTurnIndex] && (getTeamByUsername(board[position].getPieces()[0]) != getTeamByUsername(users[playerTurnIndex]))) {
                    return "illegal move!"; //If you've finished and you're trying to move an opponents piece.
                }
                if (board[position].isLocked() || position > 59 && position < noOfPlayers * 15 + noOfPlayers * 4) {
                    return "illegal move!"; //if piece is locked or in goal circle it can't be moved with a -4 card.
                }
                if (position % 15 < endPosition % 15) { //if this is the case, you've crossed a homefield
                    homefieldPos = 15 * (endPosition / 15); //figure out the position of homefield crossed
                    if (homefieldPos == noOfPlayers * 15) homefieldPos = 0;
                    if (!board[homefieldPos].getHomeField().matches(username) && board[homefieldPos].getPieces()[0] != null) { // if it's not your homefield and someone is on that homefield, you can't play -4.
                        return "illegal move!";
                    } else {
                        endPosition--; //otherwise decrement by 1 since you're jumping over homefield.
                    }
                }
                if (endPosition < 0) { //If you're endposition is negative we'll wrap around and count from 59.
                    endPosition = endPosition % (noOfPlayers * 15);
                }
                for (int i = 0; i < 4; i++) {
                    if (board[endPosition].getPieces()[i].matches(username))
                        continue; //If you are already on that field, find an available place for your piece on that field.
                    if (board[endPosition].getPieces()[i] == null) { //if there is room, insert piece there
                        if (readOnly) return "ok";
                        board[endPosition].getPieces()[i] = username; //update board
                        return "ok";
                    } else {
                        for (int j = 0; j < noOfPlayers; j++) {
                            if (board[15 * (j)].getHomeField().matches(username)) {
                                for (int k = 0; k < noOfPlayers; k++) {
                                    if (board[noOfPlayers * 15 + 4 * noOfPlayers].getPieces()[k] == null) {
                                        if (readOnly) return "ok";
                                        board[noOfPlayers * 15 + 4 * noOfPlayers].getPieces()[k] = username;
                                        return "ok";
                                    }
                                }
                            }
                        }
                    }
                    break;
                }

                break;

            case SEVEN: //split. When user uses split, the move is of the form:position, card, username, moves
                int sum = 0;

                if (position > 75) return "illegal move!";
                if (!finished[playerTurnIndex] && !board[position].getPieces()[0].matches(username)) {
                    return "illegal move!";
                }
                if (finished[playerTurnIndex] && (getTeamByUsername(board[position].getPieces()[0]) != getTeamByUsername(users[playerTurnIndex]))) {
                    return "illegal move!";
                }
                for (int i = 0; i < pieceMoves.size(); i++) {
                    sum += pieceMoves.get(i);
                }
                if (sum != 7) {
                    return "illegal move!";
                }

                for (int i = 0; i < pieces.size(); i++) {
                    ArrayList<Integer> a = new ArrayList<>();
                    a.add(positions[pieces.get(i)]);
                    ArrayList<Integer> b = new ArrayList<>();
                    b.add(pieceMoves.get(i));
                    calculateMove(username, card, a, b, chosenCard);
                }

                return "ok";  // User who plays a 7 needs to make sure that he only gives up his turn when he receives a split done

            case HEART: //release piece
                if (!finished[playerTurnIndex] && !board[position].getPieces()[0].matches(username)) {
                    return "illegal move!";

                }
                if (finished[playerTurnIndex] && (getTeamByUsername(board[position].getPieces()[0]) != getTeamByUsername(users[playerTurnIndex]))) {
                    return "illegal move!"; //If you've finished and you're trying to move an opponents piece.
                }
                homefieldPos = getHomeFieldByUsername(username);
                //check if you have pieces in homecircle
                if (!readOnly) liftPiece(getPlayerByUsername(username).getHomeCirclePos());
                for (int j = 0; j < 4; j++) {
                    if (board[homefieldPos].getPieces()[j] != null)
                        continue; //If you are already on that field, find an available place for your piece on that field.
                    if (board[homefieldPos].getPieces()[j] == null) { //if there is room, insert piece there
                        if (readOnly) return "ok";
                        board[homefieldPos].getPieces()[j] = username; //update board
                        endPosition = homefieldPos;
                        return "ok";
                    }
                }
                break;

            case SWITCH: //switch pieces
                if (board[positions[pieces.get(1)]].getPieces()[0] == null || board[positions[pieces.get(0)]].getPieces()[0] == null) { //can't switch if there isn't a piece in min. one of the fields
                    return "illegal move!";
                }
                if (board[positions[pieces.get(0)]].isLocked() || board[positions[pieces.get(1)]].isLocked() || board[positions[pieces.get(1)]].isProtect() || board[positions[pieces.get(0)]].isProtect()) {
                    return "illegal move!";
                } //can't switch if piece is on a locked or protected field.
                if (readOnly) return "ok";
                String tmp1 = board[positions[pieces.get(0)]].getPieces()[0];
                String tmp2 = board[positions[pieces.get(1)]].getPieces()[0];
                board[positions[pieces.get(1)]].getPieces()[0] = tmp1;
                board[positions[pieces.get(0)]].getPieces()[0] = tmp2;
                startPos[0] = positions[pieces.get(0)];
                startPos[1] = positions[pieces.get(1)];
                return "ok";

            case EIGHT_H:
                if (chosenCard == 1) {
                    card = EIGHT;
                } else {
                    card = HEART;
                }
                return calculateMove(username, card, pieces, pieceMoves, chosenCard);

            //switch case to default or heart depending on choice
            case THIRT_H:
                if (chosenCard == 1) {
                    card = THIRT;
                } else {
                    card = HEART;
                }
                return calculateMove(username, card, pieces, pieceMoves, chosenCard);

            case ONE_FOURT:
                //switch case to default or heart depending on choice
                if (chosenCard == 1) {
                    card = Cards.ONE;
                } else {
                    card = FOURT;
                }
                return calculateMove(username, card, pieces, pieceMoves, chosenCard);

            //switch case to default
            default: //Default corresponds to all enums with the function fw/forward
                if (position > 75)
                    return "illegal move!"; // you can't move something in the homecircles with a number card
                if (!finished[playerTurnIndex] && !board[position].getPieces()[0].matches(username)) {
                    return "illegal move!"; //If you haven't finished but you're trying to move another person's pieces, it's illegal.
                }
                if (finished[playerTurnIndex] && (getTeamByUsername(board[position].getPieces()[0]) != getTeamByUsername(users[playerTurnIndex]))) {
                    return "illegal move!"; //If you've finished and you're trying to move an opponents piece.
                }
                if (board[position].isLocked() || finished[playerTurnIndex]) {
                    return "illegal move!"; //if piece is locked, or you've finished, you can't move this piece.
                }
                if (position > 59 && position < (noOfPlayers * 15 + 4 * noOfPlayers) && isStuck(position, 15 * (position / 15))) { //if your piece is in it's goalcircles.
                    int felter = upper - lower;
                    if (!readOnly) liftPiece(position);
                    endPosition = (endPosition % felter) + position + offset;
                    if (!readOnly) tryLock(endPosition);
                    if (!readOnly) board[endPosition].getPieces()[0] = username;
                    if (!readOnly)
                        finished[playerTurnIndex] = endPosition % 4 == 0 && isPlayerDone(username); //check if player is finished
                    if (isTeamDone(username) && !readOnly) {
                        winningTeam = getTeamNumberByUsername(username);
                    }
                    if (readOnly) return "ok";
                    return "ok";
                } else if (position % 15 > endPosition % 15) { //if this is the case, you've crossed a homefield
                    homefieldPos = 15 * (endPosition / 15); //figure out the position of homefield crossed
                    if (homefieldPos == 15 * noOfPlayers) homefieldPos = 0;
                    if (board[homefieldPos].getHomeField().matches(username)) { // if it's your homefield.
                        if (readOnly) return "ok";
                        goalSquaresUpper(homefieldPos, position, endPosition, username);
                        finished[playerTurnIndex] = endPosition % 4 == 0 && isPlayerDone(username);
                        if (isTeamDone(username)) {
                            winningTeam = getTeamNumberByUsername(username);
                        }
                        return "ok"; //remove card from space/card was valid and has been used
                    }
                    //if it's not your homefield
                    else if (board[homefieldPos].getPieces()[0] != null) {
                        return "illegal move!"; //You can't play that card because something is blocking you.
                    } else {
                        endPosition++; //Nothing is blocking you, so you can cross this homefield.
                    }
                }
                //The move is legal, first we remove your piece from it's current position
                if (!readOnly) liftPiece(position);
                if (endPosition > noOfPlayers * 15 - 1) { //If this wasn't your homefield, we'll wrap around and begin at 0.
                    endPosition = endPosition % (noOfPlayers * 15);
                }
                //Now we place your piece on its end position. There is no one already on this field, so you move to the field.
                //If you aren't passing your own homefield.
                for (int i = 0; i < 4; i++) {
                    if (board[endPosition].getPieces()[i] == null) { //if there is room, insert piece there
                        if (readOnly) return "ok";
                        board[endPosition].getPieces()[i] = username; //update board
                        return "ok";
                    }
                    if (board[endPosition].getPieces()[i].matches(username)) {
                        continue; //If you are already on that field, find an available place for your piece on that field.
                    } else break;
                }
                        //otherwise, there was no room for you, back to home circle
                        for (int j = 0; j < noOfPlayers; j++) {
                            if (board[15 * (j)].getHomeField().matches(username)) {
                                endPosition = 15 * j;
                                for (int k = 0; k < 4; k++) {
                                    if (board[noOfPlayers * 15 + 4 * noOfPlayers].getPieces()[k] != null) {
                                        if (readOnly) return "ok";
                                        board[noOfPlayers * 15 + 4 * noOfPlayers].getPieces()[k] = username;
                                        return "ok";
                                    }
                                }
                            }
                        }
                }
        return "illegal move!";
        }

    private boolean canUserMakeMove(String username) throws InterruptedException {
        readOnly = true;
        ArrayList<Cards> hand = playerHands[getPlayerIndex(username)];
        int[] piecesPositions = new int[4];
        int[] pieces = new int[4];
        int switchIndex = -1;
        int splitIndex = -1;
        int counter = 0;
        int[] maxMovesArr = new int[4];
        for (int k = 0; k < noOfPlayers * 4; k++) {
            if (board[positions[k]].getPieces()[0].matches(username)) {
                piecesPositions[counter] = positions[k];
                pieces[counter] = k;
                counter++;
            }
        }
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).getName().matches("Switch")) switchIndex = i;
            if (hand.get(i).getName().matches("Seven")) splitIndex = i;
        }

        for (int i = 0; i < hand.size(); i++) { // go through each card
            for (int j = 0; j < 4; j++) { // through each piece
                ArrayList<Integer> test = new ArrayList<Integer>();
                test.add(pieces[j]);
                if (!hand.get(i).getName().matches("Switch") && !hand.get(i).getName().matches("Seven") && (calculateMove(username, hand.get(i), test, null, 1).matches("ok")
                        || calculateMove(username, hand.get(i), test, null, 2).matches("ok"))) {
                    System.out.println("normal move is possible: ");
                    System.out.println("using card: " + hand.get(i).getName() + " at position: " + positions[pieces[j]]);
                    readOnly = false;
                    return true;
                }
            }
        }
        if (switchIndex != -1) {
            for (int i = 0; i < noOfPlayers * 4; i++) {
                for (int j = 0; j < noOfPlayers * 4; j++) {
                    ArrayList<Integer> test = new ArrayList<Integer>();
                    test.add(j);
                    test.add(i);
                    if (calculateMove(username, hand.get(switchIndex), test, null, 1).matches("ok")) {
                        System.out.println("switch is possible with pieces [" + j + "," + i + "]");
                        readOnly = false;
                        return true;
                    }
                }
            }
            //try to switch with everyone and everything.
        }
        if (splitIndex != -1) {
            for (int i = 0; i < 4; i++) { //each piece
                for (int j = 1; j < 8; j++) { //each card under 8
                    ArrayList<Integer> test = new ArrayList<Integer>();
                    test.add(pieces[i]);
                    ArrayList<Integer> pieceMoves = new ArrayList<Integer>();
                    pieceMoves.add(7);
                    if (calculateMove(username, Cards.getEnumByNoOfMoves(j), test, pieceMoves, 1).matches("ok")) {
                        maxMovesArr[i] = j;
                    }
                }

            }
            int sum = 0;
            for (int l = 0; l < 4; l++) {
                sum += maxMovesArr[0];
            }
            if (sum > 6) {
                readOnly = false;
                System.out.println("split is possible! ");
                return true;
            }
        }
        readOnly = false;
        System.out.println("SKIP! User: " + username + " couldn't make a move");
        return false;
    }


    private void update(String username) throws InterruptedException {
        String pieceindex = gson.toJson(pieceIndexes);
        String position = gson.toJson(positions);
        for (int i = 0; i < noOfPlayers; i++) {
            game.put("gameUpdate", "playerMove", username, users[i], "", pieceindex, position); // empty field is cards
        }
    }


    private void goalSquaresUpper(int homefieldPos, int position, int endPosition, String username) { //WRONG. needs to find correct upper bound, not necessarily last goal circle.
        int upperbound = 0;
        switch (homefieldPos) {
            case 0:
                upperbound = 63;
                offset = 0;
                if (noOfPlayers > 4) {
                    upperbound = 93;
                }
                break;
            case 15:
                upperbound = 19;
                offset = 78;
                break;
            case 30:
                upperbound = 34;
                offset = 67;
                break;
            case 45:
                upperbound = 49;
                offset = 56;
                break;
            case 60:
                upperbound = 64;
                offset = 45;
                break;
            case 75:
                upperbound = 79;
                offset = 34;
                break;
        }

        if (endPosition > upperbound - 5 && position < homefieldPos) { //You enter goal circle
            endPosition++;
        }
        if (endPosition > upperbound) //Upper bound, in case you have to reverse
        {
            endPosition = upperbound - (endPosition - upperbound); //16 is upper bound. 15 is homefield
        }
        if (endPosition < homefieldPos + 1) { //You've exited goalcircles.
            endPosition--;
        }
        liftPiece(position);
        board[endPosition].getPieces()[endPosition] = username; //update board. WRONG
        tryLock(endPosition); //attempt to lock the piece.
    }

    private boolean isStuck(int position, int homefieldPos) {

        for (int i = 0; i < 4; i++) {
            if (position - i > homefieldPos && board[position - i] != null) {
                lower = position - i;
                break;
            }
        }
        for (int j = 0; j < 4; j++) {
            if (position + j <= homefieldPos + 4 && board[position + j] != null) {
                upper = position + j;
            }
        }
        return !(lower == upper);
    }

    private void liftPiece(int position) {
        for (int j = 3; j > -1; j--) {
            if (board[position].getPieces()[j] != null) {
                board[position].getPieces()[j] = "";
                break;
            }
        }
    }

    private boolean isPlayerDone(String username) {
        int pieces = 0;
        int index = getPlayerIndexOnTeam(username);
        ArrayList<Player> team = getTeamByUsername(username);
        int homepos = team.get(index).getHomePos();
        int goalPos = getGoalPosByHomefield(homepos);
        for (int i = 0; i < 4; i++) {
            if (board[goalPos + i].getPieces()[0] != null) pieces++;
        }
        return pieces == 4;
    }

    private boolean isTeamDone(String username) {
        int count = 0;
        int size = 0;
        if (getTeamNumberByUsername(username) == 1) {
            size = teamOne.size();
            for (Player x : teamOne) {
                if (x.isDone()) count++;
            }
        }
        if (getTeamNumberByUsername(username) == 2) {
            size = teamTwo.size();
            for (Player x : teamTwo) {
                if (x.isDone()) count++;
            }
        }
        if (getTeamNumberByUsername(username) == 3) {
            size = teamThree.size();
            for (Player x : teamThree) {
                if (x.isDone()) count++;
            }
        }
        return count == size;
    }

    private void tryLock(int endPosition) {
        board[endPosition].setLocked(endPosition > noOfPlayers * 15 - 1 && (endPosition == noOfPlayers * 15 + 3 || endPosition == noOfPlayers * 15 + 7 || endPosition == noOfPlayers * 15 + 11 || endPosition == noOfPlayers * 15 + 15 || endPosition == noOfPlayers * 15 + 19 || endPosition == noOfPlayers * 15 + 23 || board[endPosition + 1].isLocked()));
    }

    private void setupBoard() { //update this one
        setAllFields();
        //setup each team's players
        for (int j = 0; j < noOfPlayers; j++) {
            if (teams[j] == 1) {
                teamOne.add(new Player(users[j], 1, 0, 0));
            }
            if (teams[j] == 2) {
                teamTwo.add(new Player(users[j], 2, 0, 0));
            }
            if (teams[j] == 3) {
                teamThree.add(new Player(users[j], 3, 0, 0));
            }
        }
        int count = 0;
        //Sets each player's starting circles and end circles.
        //depends on how many teams.
        if (numberOfTeams == 2) {
            for (Player x : teamOne) {
                setStartAndEndFields(count, x);
                count += 2;
            }
            count = 1;
            for (Player x : teamTwo) {
                setStartAndEndFields(count, x);
                count += 2;
            }
        } else {
            for (Player x : teamOne) {
                setStartAndEndFields(count, x);
                count += 3;
            }
            count = 1;
            for (Player x : teamTwo) {
                setStartAndEndFields(count, x);
                count += 3;
            }
            count = 2;
            for (Player x : teamThree) {
                setStartAndEndFields(count, x);
                count += 3;
            }
        }
        for (int i = 0; i < noOfPlayers; i++) {
            setProtectedFields(i); //i.e. end circle, start circle and home field.
            String username = board[i * 15].getHomeField();
            board[noOfPlayers * 15 + noOfPlayers * 4 + i].setPieces(new String[]{username, username, username, username}); //Place alle pieces in their home circles.
            getPlayerByUsername(username).setHomeCirclePos(noOfPlayers * 15 + noOfPlayers * 4 + i);
            positions[4 * i] = noOfPlayers * 15 + noOfPlayers * 4 + i;
            positions[4 * i + 1] = noOfPlayers * 15 + noOfPlayers * 4 + i;
            positions[4 * i + 2] = noOfPlayers * 15 + noOfPlayers * 4 + i;
            positions[4 * i + 3] = noOfPlayers * 15 + noOfPlayers * 4 + i;
        }
    }

    private void setStartAndEndFields(int pos, Player user) { //Sets each players goalcircles/end fields and homefields.
        user.setHomePos(pos * 15);
        players.add(user);
        board[pos * 15].setHomeField(user.getUsername());
        board[noOfPlayers * 15 + pos * 4].setEndField(user.getUsername());
        board[noOfPlayers * 15 + pos * 4 + 1].setEndField(user.getUsername());
        board[noOfPlayers * 15 + pos * 4 + 2].setEndField(user.getUsername());
        board[noOfPlayers * 15 + pos * 4 + 3].setEndField(user.getUsername());
    }

    private void setAllFields() {
        for (int i = 0; i < board.length; i++) {
            board[i] = new BoardField(new String[4], "", "");
        }
    }

    private void setProtectedFields(int count) {
        board[noOfPlayers * 15 + count * 4].setProtect(true); //set all goal circles to be protected
        board[noOfPlayers + count * 4 + 1].setProtect(true);
        board[noOfPlayers * 15 + count * 4 + 2].setProtect(true);
        board[noOfPlayers * 15 + count * 4 + 3].setProtect(true);
        board[15 * count].setProtect(true); //all home fields are protected
        board[noOfPlayers * 15 + noOfPlayers * 4 + count].setProtect(true); //all home circles are protected
    }

    private int getTeamNumberByUsername(String username) {
        int index = 0;
        for (int i = 0; i < noOfPlayers; i++) {
            if (users[i].matches(username)) {
                index = i;
            }
        }
        return teams[index];
    }

    private ArrayList<Player> getTeamByNumber(int teamNumber) {
        if (teamNumber == 1) return teamOne;
        if (teamNumber == 2) return teamTwo;
        if (teamNumber == 3) return teamThree;
        return null;
    }

    private ArrayList<Player> getTeamByUsername(String username) {
        int teamNo = getTeamNumberByUsername(username);
        if (teamNo == 1) return teamOne;
        if (teamNo == 2) return teamTwo;
        if (teamNo == 3) return teamThree;
        return null;
    }

    private int getPlayerIndex(String username) {
        for (int i = 0; i < noOfPlayers; i++) {
            if (users[i].matches(username)) return i;
        }
        return -1;
    }

    private int getPlayerIndexOnTeam(String username) {
        ArrayList<Player> team = getTeamByUsername(username);
        for (int i = 0; i < team.size(); i++) {
            if (team.get(i).getUsername().matches(username)) return i;
        }
        return -1;
    }


    private int getGoalPosByHomefield(int homePos) {
        if (version == 0) return 60 + homePos / 15 * 4;
        if (version == 1) return 90 + homePos / 15 * 4;
        return -1;
    }

    private int getPlayerIndexToTheLeftOfUsername(String username) {
        int index = -1;
        ArrayList<Player> teamtmp = getTeamByUsername(username);
        for (int i = 0; i < teamtmp.size(); i++) {
            if (teamtmp.get(i).getUsername().matches(username)) index = i;
        }
        int targetIndex = (index + 1) % teamtmp.size();
        String usernameToTheLeft = teamtmp.get(targetIndex).getUsername();
        return getIndexByUsername(usernameToTheLeft);
    }

    private int getIndexByUsername(String username) {
        int index = -1;
        for (int i = 0; i < noOfPlayers; i++) {
            if (users[i].matches(username)) index = i;
        }
        return index;
    }

    private void movePiece(ArrayList<Integer> pieces, ArrayList<Integer> pieceMovesToField) { //This updates the positions and pieceindexes array which is used for gameupdates
        if (pieces.size() == 2 && pieceMovesToField.size() == 0) { //in case of switch cards
            pieceIndexes = new Integer[2];
            for (int i = 0; i < positions.length; i++) {
                if (startPos[0] == positions[i]) {
                    positions[i] = startPos[1];
                    pieceIndexes[0] = i;
                }
                if (startPos[1] == positions[i]) {
                    positions[i] = startPos[0];
                    pieceIndexes[1] = i;
                }
            }
        } else if (pieceMovesToField.size() > 0) { //in case of a seven.
            pieceIndexes = new Integer[4];
            for (int j = 0; j < pieceMovesToField.size(); j++) {
                if (pieceMovesToField.get(j) > 0) {
                    positions[pieces.get(j)] = pieceMovesToField.get(j);
                    pieceIndexes[j] = pieces.get(j);
                }
            }
        } else {
            pieceIndexes = pieces.toArray(new Integer[0]);
            positions[pieces.get(0)] = endPosition;
        }
    }

    private int getHomeFieldByUsername(String username) {
        for (Player x : players) {
            if (x.getUsername().matches(username)) return x.getHomePos();
        }
        return -1;
    }

    private Player getPlayerByUsername(String username) {
        for (Player x : players) {
            if (x.getUsername().matches(username)) return x;
        }
        return null;
    }
}


class GameEndedUpdater implements Runnable {

    private Space gameSpace;
    private String hostName;
    private String[] users;

    public GameEndedUpdater(Space gameSpace, String hostName, String[] users) {
        this.gameSpace = gameSpace;
        this.hostName = hostName;
        this.users = users;
    }

    @Override
    public void run() {
        try {
            gameSpace.get(new ActualField("gameEnd"), new ActualField(hostName));
            for (String user : users) {
                gameSpace.put("gameHasEnded", user);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}