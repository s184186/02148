package Model;

import org.jspace.ActualField;
import org.jspace.Space;

import java.util.*;

import static Model.Cards.*;
import static Model.Templates.*;

public class Game implements Runnable {
    private int noOfPlayers;
    private int lower = -1;
    private int upper = -2;
    private int offset;
    private int counter = 0;
    private int version;
    private String[] users;
    private boolean[] finished;
    private Space game;
    private BoardField[] board;
    private String playerTurn;
    private int startingTeamsNumber;
    private int playerTurnIndex;
    private int teamTurnIndex;
    private int decksize = 13;
    private int[] teams;
    private int winningTeam = -1;
    private boolean justStarted;
    private int numberOfTeams;
    private int needCardsCounter;
    private ArrayList<Player> teamOne = new ArrayList<>();
    private ArrayList<Player> teamTwo = new ArrayList<>();
    private ArrayList<Player> teamThree = new ArrayList<>();
    private ArrayList<CardObj> deck;


    public Game(String host, String[] players, int[] teams, int version, Space game, int numberOfTeams) { //assumes that user array's elements are alternating in terms of teams.
        this.game = game;
        this.users = players;
        this.numberOfTeams = numberOfTeams;
        this.teams = teams;
        this.board = new BoardField[noOfPlayers * 15 + noOfPlayers * 4 + noOfPlayers];
        this.noOfPlayers = players.length;
        this.version = version;
        this.finished = new boolean[noOfPlayers];
        this.justStarted=true;
    }

    @Override
    public void run() {
        try {
            //Users will be represented as string list and handed down from server.
            setupBoard();
            shuffleCards(users);
            //TODO: put
            game.put("switch!"); //initiate users to switch card. We need to make sure that same user, doesn't try to switch cards more than once.
            for (int i = 0; i < noOfPlayers; i++) {
                Object[] switchInfo = game.get(switchReq.getFields()); //switch req consits of from, to and card fields
                switchCards(switchInfo);
            }

            // Move will b e represented by position, the card used and username.
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        while (true) {
            try {
                if (winningTeam != -1) break;
                if(game.getp(new ActualField("need cards"))!=null) needCardsCounter++;
                if(needCardsCounter==noOfPlayers) shuffleCards(users);
                //TODO: query
                Object[] potentialMove = game.query(move(playerTurn).getFields()); // A basic move will b e represented by position, the card used and username and extra field.
                moves(potentialMove);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Team " + winningTeam + " won");
    }

    private void setupDeck(){
        deck= new ArrayList<>() {
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
        if (getCardsLeftInDeck()<noOfPlayers*4){ //If there aren't enough cards left to hand out make a deck consisting of all the used cards and all the unused ones
            setupDeck();
        }
        Cards[] hand = new Cards[4];
        Random random = new Random();
        for (int i = 0; i < noOfPlayers; i++) {
            for (int j = 0; j < 4; j++) {
                int index = random.nextInt(decksize);
                int amount = deck.get(index).getAmount();
                deck.get(index).setAmount(amount - 1);
                hand[j] = deck.get(index).getCard();
                if (amount == 1) { //if last card of a specific type in deck, this type will be removed
                    deck.remove(index);
                    decksize--;
                }

            }
            game.put(users[i], hand); //Each user's hand is put in the tuple space. The users name is the id factor.
        }
        if(justStarted) {
        playerTurnIndex = random.nextInt(noOfPlayers); //figuring out who has the first turn
        teamTurnIndex = teams[playerTurnIndex];
        startingTeamsNumber=teamTurnIndex;
        playerTurn = (String) users[playerTurnIndex];
        justStarted=false;
        }
    }

    private int getCardsLeftInDeck(){
        int cardsleft=0;
        for (CardObj x : deck) {
            cardsleft+=x.getAmount();
        }
        return cardsleft;
    }

    private void switchCards(Object[] switchInfo) throws InterruptedException {
        String from = (String) switchInfo[0];
        String to = (String) switchInfo[1];
        Cards card = (Cards) switchInfo[2];
        //TODO: put
        game.put(to, card);
        //server tells users to mix cards
        //server tells user with username 'to' to add 'card' to his deck.
    }

    private void moves(Object[] potentialMove) throws InterruptedException { //make compatible with partners plus.
        int homefieldPos = -1;
        int position = (int) potentialMove[0];
        Cards card = (Cards) potentialMove[1];
        String username = (String) potentialMove[2];
        int extra = (int) potentialMove[3];   //can either represent the piece a user wants to switch positions with, or in case of the card seven, how many moves forward this piece should move
        int endPosition = (int) potentialMove[0] + card.getMoves();
        switch (card) {

            case FOUR: //move backwards
                if (!finished[playerTurnIndex] && board[position].getPieces()[0].matches(username))
                    return; //If you haven't finished but you're trying to move another person's pieces, it's illegal.
                if (finished[playerTurnIndex] && (getTeamByUsername(board[position].getPieces()[0]) != getTeamByUsername(playerTurn)))
                    return; //If you've finished and you're trying to move an opponents piece.

                if (board[position].isLocked() || position > 59 && position < noOfPlayers * 15 + noOfPlayers * 4)
                    return; //if piece is locked or in goal circle it can't be moved with a -4 card.
                if (position % 15 < endPosition % 15) { //if this is the case, you've crossed a homefield
                    homefieldPos = 15 * (endPosition / 15); //figure out the position of homefield crossed
                    if (homefieldPos == noOfPlayers * 15) homefieldPos = 0;
                    if (!board[homefieldPos].getHomeField().matches(username) && board[homefieldPos] != null) { // if it's not your homefield and someone is on that homefield, you can't play -4.
                        return;
                    } else {
                        endPosition--; //otherwise decrement by 1 since you're jumping over homefield.
                    }
                }
                if (endPosition < 0) { //If you're endposition is negative we'll wrap around and count from 59.
                    endPosition = endPosition % (noOfPlayers * 15);
                }
                for (int i = 0; i < 4; i++) {
                    if (board[endPosition].getPieces()[i].equals(username))
                        continue; //If you are already on that field, find an available place for your piece on that field.
                    if (board[endPosition].getPieces()[i] == null) { //if there is room, insert piece there
                        board[endPosition].getPieces()[i] = username; //update board
                        //TODO: get
                        game.get(move(username).getFields());
                        nextTurn(); //Figuring out which user's turn it is
                    } else {
                        for (int j = 0; j < noOfPlayers; j++) {
                            if (board[15 * (j)].getHomeField().matches(username)) {
                                for (int k = 0; k < noOfPlayers; k++) {
                                    if (board[noOfPlayers * 15 + 4 * noOfPlayers].getPieces()[k] != null) {
                                        board[noOfPlayers * 15 + 4 * noOfPlayers].getPieces()[k] = username;
                                        //TODO: get
                                        game.get(move(username).getFields());
                                        nextTurn();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    break;
                }

                break;
            case SEVEN: //split
                if (!finished[playerTurnIndex] && board[position].getPieces()[0].matches(username))
                    return; //If you haven't finished but you're trying to move another person's pieces, it's illegal.
                if (finished[playerTurnIndex] && (getTeamByUsername(board[position].getPieces()[0]) != getTeamByUsername(playerTurn)))
                    return; //If you've finished and you're trying to move an opponents piece.
                if (extra == 0 || extra > 7 - counter) return;
                potentialMove[1] = card.getEnum(extra);
                counter += extra;
                moves(potentialMove);
                if (counter == 7) {
                    counter = 0;
                    //TODO: get
                    game.get(move(username).getFields());
                    nextTurn();
                    break;
                }
                return;
            case HEART: //release piece
                if (!finished[playerTurnIndex] && board[position].getPieces()[0].matches(username))
                    return; //If you haven't finished but you're trying to move another person's pieces, it's illegal.
                if (finished[playerTurnIndex] && (getTeamByUsername(board[position].getPieces()[0]) != getTeamByUsername(playerTurn)))
                    return; //If you've finished and you're trying to move an opponents piece.

                for (int i = 0; i < noOfPlayers; i++) {
                    if (board[noOfPlayers * 15 + noOfPlayers * 4 + i].getPieces()[3 - i] != null && board[15 * (i)].getHomeField().matches(username)) { //If you use a card and you have pieces in homecircles left to use it on.
                        liftPiece(noOfPlayers * 15 + noOfPlayers * 4 + i);
                        for (int j = 0; j < 4; j++) {
                            if (board[15 * i].getPieces()[j].equals(username))
                                continue; //If you are already on that field, find an available place for your piece on that field.
                            if (board[15 * i].getPieces()[j] == null) { //if there is room, insert piece there
                                board[15 * i].getPieces()[j] = username; //update board
                                //TODO: get
                                game.get(move(username).getFields());
                                nextTurn(); //Figuring out which user's turn it is
                                break;
                            }
                        }
                        break;
                    }
                }
                break;
            case SWITCH: //switch pieces
                String tmp1 = "default1";
                String tmp2 = "default2";

                if (board[position] == null || board[extra] == null || board[position].isLocked() || board[position].isProtect() || board[extra].isLocked() || board[extra].isProtect())
                    return;

                for (int i = 3; i > -1; i--) {
                    if (board[extra].getPieces()[i] != null) {
                        tmp1 = board[extra].getPieces()[i];
                        board[extra].getPieces()[i] = null;
                    }
                    if (board[position].getPieces()[i] != null) {
                        tmp2 = board[position].getPieces()[i];
                        board[position].getPieces()[i] = null;
                    }
                }
                for (int j = 0; j < 4; j++) {
                    if (board[extra].getPieces()[j] == null) {
                        board[extra].getPieces()[j] = tmp2;
                    }
                    if (board[position].getPieces()[j] == null) {
                        board[position].getPieces()[j] = tmp1;
                    }
                }
                //TODO: get
                game.get(move(username).getFields());
                nextTurn();

                break;
            case EIGHT_H:
                potentialMove[1] = EIGHT;
                if (extra == 0) potentialMove[1] = HEART;
                moves(potentialMove);
                break;
            //switch case to default or heart depending on choice
            case THIRT_H:
                //switch case to default or heart depending on choice
                potentialMove[1] = THIRT;
                if (extra == 0) potentialMove[1] = HEART;
                moves(potentialMove);
                break;

            case ONE_FOURT:
                potentialMove[1] = ONE;
                if (extra == 0) potentialMove[1] = FOURT;
                moves(potentialMove);
                break;

            //switch case to default
            default: //Default corresponds to all enums with the function fw/forward
                if (!finished[playerTurnIndex] && board[position].getPieces()[0] != username)
                    return; //If you haven't finished but you're trying to move another person's pieces, it's illegal.
                if (finished[playerTurnIndex] && (getTeamByUsername(board[position].getPieces()[0]) != getTeamByUsername(playerTurn)))
                    return; //If you've finished and you're trying to move an opponents piece.
                if (board[position].isLocked() || finished[playerTurnIndex])
                    return; //if piece is locked, or you've finished, you can't move this piece.
                if (position > 59 && position < (noOfPlayers * 15 + 4 * noOfPlayers) && isStuck(position, 15 * (position / 15))) { //if your piece is in it's goalcircles.
                    int felter = upper - lower;
                    liftPiece(position);
                    endPosition = (endPosition % felter) + position + offset;
                    tryLock(endPosition);
                    board[endPosition].getPieces()[0] = username;
                    finished[playerTurnIndex] = endPosition % 4 == 0 && isPlayerDone(username); //check if player is finished
                    if (isTeamDone(username)) {
                        winningTeam = getTeamNumber(username);
                        System.out.println("Team " + winningTeam + " won");
                    }
                    //TODO: get
                    game.get(move(username).getFields());
                    nextTurn();
                } else if (position % 15 > endPosition % 15) { //if this is the case, you've crossed a homefield
                    homefieldPos = 15 * (endPosition / 15); //figure out the position of homefield crossed
                    if (homefieldPos == 15 * noOfPlayers) homefieldPos = 0;
                    if (board[homefieldPos].getHomeField().matches(username)) { // if it's your homefield.
                        goalSquaresUpper(homefieldPos, position, endPosition, username);
                        finished[playerTurnIndex] = endPosition % 4 == 0 && isPlayerDone(username);
                        if (isTeamDone(username)) {
                            winningTeam = getTeamNumber(username);
                            System.out.println("Team " + winningTeam + " won");
                        }
                        //TODO: get
                        game.get(move(username).getFields()); //remove card from space/card was valid and has been used
                        nextTurn();
                        return;
                    }
                    //if it's not your homefield
                    else if (board[homefieldPos] != null) {
                        return; //You can't play that card because something is blocking you.
                    } else {
                        endPosition++; //Nothing is blocking you, so you can cross this homefield.
                    }
                }
                //The move is legal, first we remove your piece from it's current position
                liftPiece(position);
                if (endPosition > noOfPlayers * 15 - 1) { //If this wasn't your homefield, we'll wrap around and begin at 0.
                    endPosition = endPosition % (noOfPlayers * 15);
                }
                //Now we place your piece on its end position. There is no one already on this field, so you move to the field.
                if (homefieldPos == -1) { //If you aren't passing your own homefield.
                    for (int i = 0; i < 4; i++) {
                        if (board[endPosition].getPieces()[i].equals(username))
                            continue; //If you are already on that field, find an available place for your piece on that field.
                        if (board[endPosition].getPieces()[i] == null) { //if there is room, insert piece there
                            board[endPosition].getPieces()[i] = username; //update board
                            //TODO: get
                            game.get(move(username).getFields());
                            nextTurn(); //Figuring out which user's turn it is
                            break;
                        }
                    }
                } else {
                    //otherwise back to home circle
                    for (int j = 0; j < noOfPlayers; j++) {
                        if (board[15 * (j)].getHomeField().matches(username)) {
                            for (int k = 0; k < 4; k++) {
                                if (board[noOfPlayers * 15 + 4 * noOfPlayers].getPieces()[k] != null) {
                                    board[noOfPlayers * 15 + 4 * noOfPlayers].getPieces()[k] = username;
                                    //TODO: get
                                    game.get(move(username).getFields());
                                    nextTurn();
                                    break;
                                }
                            }
                        }
                    }

                }
        }
    }


    private void goalSquaresUpper(int homefieldPos, int position, int endPosition, String username) {
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
        board[endPosition].getPieces()[endPosition] = username; //update board
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
                board[position].getPieces()[j] = null;
                break;
            }
        }
    }

    private void nextTurn() {
        teamTurnIndex = ++teamTurnIndex % (numberOfTeams+1); //there is no 0'th team.
        if(teamTurnIndex==0) teamTurnIndex=1; //there is no 0'th team.
        if(teamTurnIndex==startingTeamsNumber) playerTurnIndex= ++playerTurnIndex%getTeamByNumber(teamTurnIndex).size();
        playerTurn = getTeamByNumber(teamTurnIndex).get(playerTurnIndex).getUsername();
    }

    private boolean isPlayerDone(String username) {
        int pieces = 0;
        int index = getPlayerIndex(username);
        ArrayList<Player> team = getTeamByUsername(username);
        int homepos = team.get(index).getHomePos();
        int goalPos = getGoalPos(homepos);
        for (int i = 0; i < 4; i++) {
            if (board[goalPos + i] != null) pieces++;
        }
        return pieces == 4;
    }

    private boolean isTeamDone(String username) {
        int count = 0;
        int size = 0;
        if (getTeamNumber(username) == 1) {
            size = teamOne.size();
            for (Player x : teamOne) {
                if (x.isDone()) count++;
            }
        }
        if (getTeamNumber(username) == 2) {
            size = teamTwo.size();
            for (Player x : teamTwo) {
                if (x.isDone()) count++;
            }
        }
        if (getTeamNumber(username) == 3) {
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
        //setup each team's players
        for (int j = 0; j < noOfPlayers; j++) {
            if (teams[j] == 1) {
                teamOne.add(new Player(users[j], 1, 0));
            }
            if (teams[j] == 2) {
                teamTwo.add(new Player(users[j], 2, 0));
            }
            if (teams[j] == 3) {
                teamThree.add(new Player(users[j], 3, 0));
            }
        }
        int count = 0;
        //Sets each player's starting points and ending points.
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
            setProtectedFields(i);
            String username=board[i * 15].getHomeField();
            board[noOfPlayers * 15 + noOfPlayers * 4 + i].setPieces(new String[]{username, username, username, username}); //Place alle pieces in their homefields.
        }
    }

    private void setStartAndEndFields(int pos, Player user) { //Sets each players goalcircles/end fields and homefields/home positions.
        user.setHomePos(pos * 15);
        board[pos * 15].setHomeField(user.getUsername());
        board[noOfPlayers * 15 + pos*4].setEndField(user.getUsername());
        board[noOfPlayers * 15 + pos*4+1].setEndField(user.getUsername());
        board[noOfPlayers * 15 + pos*4+2].setEndField(user.getUsername());
        board[noOfPlayers * 15 + pos*4+3].setEndField(user.getUsername());
    }

    private void setProtectedFields(int count) {
        board[noOfPlayers * 15 + count * 4].setProtect(true); //set all goal circles to be protected
        board[noOfPlayers + count * 4 + 1].setProtect(true);
        board[noOfPlayers * 15 + count * 4 + 2].setProtect(true);
        board[noOfPlayers * 15 + count * 4 + 3].setProtect(true);
        board[15 * count].setProtect(true); //all home fields are protected
        board[noOfPlayers * 15 + noOfPlayers * 4 + count].setProtect(true); //all home circles are protected
    }

    private int getTeamNumber(String username) {
        int index = 0;
        for (int i = 0; i < noOfPlayers; i++) {
            if (users[i].matches(username)) {
                index = i;
            }
        }
        return teams[index];
    }

    private ArrayList<Player> getTeamByNumber(int teamNumber){
        if(teamNumber==1) return teamOne;
        if(teamNumber==2) return teamTwo;
        if(teamNumber==3) return teamThree;
        return null;
    }

    private ArrayList<Player> getTeamByUsername(String username) {
        int teamNo = getTeamNumber(username);
        if (teamNo == 1) return teamOne;
        if (teamNo == 2) return teamTwo;
        if (teamNo == 3) return teamThree;
        return null;
    }

    private int getPlayerIndex(String username) {
        ArrayList<Player> tmp = getTeamByUsername(username);
        for (int i = 0; i < tmp.size(); i++) {
            if (tmp.get(i).getUsername().matches(username)) return i;
        }
        return -1;
    }

    private int getGoalPos(int homePos) {
        if (version == 0) return 60 + homePos / 15 * 4;
        if (version == 1) return 90 + homePos / 15 * 4;
        return -1;
    }
}
