package Lobby;

import Model.Cards;
import com.google.gson.Gson;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;
import java.awt.*;

import static java.lang.Math.round;

import javafx.geometry.Insets;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.Space;

public class GameView{

    public StackPane stackPane;
    public Pane pane;
    public Label card1, card2, card3, card4, label;

    private static final int boardWidth = 900;
    private static final int buttonHeight = 100;
    private static final int boardHeight = boardWidth+buttonHeight;

    private static int version;
    private static int numberOfFields;
    private static final int startFieldOffset = 7;

    private static final int pieceRadius = 25;
    private static final int startFieldRadius = 4*pieceRadius;
    private static final int endFieldRadius = round(3f*pieceRadius);
    private static final int endFieldDistance = 30;
    private static final int endFieldSizeDec = 5;

    private static final int outerCircleBorderPadding = 150;
    private static final int innerCircleBorderPadding = outerCircleBorderPadding+250;

    private static int pieceIndex = 0;
    private Piece[] pieces = new Piece[24];

    private Field[][] endFields = new Field[6][4];
    private Field[] fields;

    private Piece selectedPiece = null;
    private Field selectedField = null;
    private int selectedCard = -1;
    String currentMove = "";

    private String[] colorNames;
    private Color[] colors;
    static Color blue = Color.rgb(61,88,222);
    static Color purple = Color.rgb(162,19,192);
    static Color red = Color.rgb(219,35,35);
    static Color orange = Color.rgb(255,149,23);
    static Color yellow = Color.rgb(233,227,23);
    static Color green  = Color.rgb(24,170,24);
    private Space gameSpace;
    private String username;
    private Space userSpace;
    private Cards[] hand = new Cards[5];
    private String host;
    private String[] users;
    private int[] teams;
    private int numberOfTeams;

    public void initialize(){
        Label[] cards = {card1, card2, card3, card4};

        label.setLayoutX(boardWidth/2.-label.getPrefWidth()/2);
        label.setLayoutY(1.2*boardWidth/2.-label.getPrefHeight()/2);

        for(int i = 0; i < 4; i++){
            cards[i].setLayoutX((1+i*2)*boardWidth/8.-label.getPrefWidth()/2);
            cards[i].setLayoutY(boardHeight-75-label.getPrefHeight()/2);
        }

        pane.setBackground(new Background(new BackgroundFill(Color.TURQUOISE, CornerRadii.EMPTY, Insets.EMPTY)));

        stackPane.setPrefSize(boardWidth,boardHeight);

        createButton(0, 0);
        createButton(boardWidth/4, 1);
        createButton(2*boardWidth/4, 2);
        createButton(3*boardWidth/4, 3);

        Circle centerButton = new Circle(boardWidth/2, boardWidth/2., 50);
        centerButton.setFill(Paint.valueOf("black"));
        centerButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> confirmMove());
        pane.getChildren().add(centerButton);
    }

    public void setup(){
        if(version == 0){
            colorNames = new String[]{"blue", "red", "yellow", "green"};
            colors = new Color[]{blue, red, yellow, green};
        } else {
            colorNames = new String[]{"purple", "red", "orange", "yellow", "green", "blue"};
            colors = new Color[]{purple, red, orange, yellow, green, blue};
        }
        numberOfFields = 60 + version * 30;

        fields = new Field[numberOfFields];
        for(int i = 0; i < numberOfFields; i++){
            fields[i] = new Field(pane);
        }

        for(int i = 0; i < numberOfFields; i++){
            double v = Math.cos(Math.toRadians((i+startFieldOffset)*360/numberOfFields));
            double w = Math.sin(Math.toRadians((i+startFieldOffset)*360/numberOfFields));
            double x1 = (v*(boardWidth-innerCircleBorderPadding)/2)+boardWidth/2.;
            double y1 = (w*(boardWidth-innerCircleBorderPadding)/2)+boardWidth/2.;
            double x2 = (v*(boardWidth-outerCircleBorderPadding)/2)+boardWidth/2.;
            double y2 = (w*(boardWidth-outerCircleBorderPadding)/2)+boardWidth/2.;
            fields[i].setStart(x1, y1, x2, y2, i, version);
            fields[(i+numberOfFields-1)%numberOfFields].setEnd(x1, y1, x2, y2);
        }

        for(int i = 0; i < numberOfFields; i++) {
            double v = Math.cos(Math.toRadians((i + 0.5 + startFieldOffset) * 360 / numberOfFields));
            double w = Math.sin(Math.toRadians((i + 0.5 + startFieldOffset) * 360 / numberOfFields));
            int finalI = i;

            double xO = (v*(boardWidth-outerCircleBorderPadding+100)/2)+boardWidth/2.;
            double yO = (w*(boardWidth-outerCircleBorderPadding+100)/2)+boardWidth/2.;

            double x1 = (v*(boardWidth-outerCircleBorderPadding+50)/2)+boardWidth/2.;
            double y1 = (w*(boardWidth-outerCircleBorderPadding+50)/2)+boardWidth/2.;

            double xI = (v*(boardWidth-innerCircleBorderPadding-75)/2)+boardWidth/2.;
            double yI = (w*(boardWidth-innerCircleBorderPadding-75)/2)+boardWidth/2.;

            fields[i].setField(x1,y1);
            fields[i].getPath().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> selectedField = fields[finalI]);

            if(i%15 == 0){
                int n = i/15;
                drawEndFields(xI, yI, endFieldRadius, colors[n], endFieldSizeDec, endFieldDistance*v, endFieldDistance*w);
                drawStartPieces(xO, yO, colors[n],colorNames[n]);
            }
        }
        GameUpdater gameUpdater = new GameUpdater(gameSpace, userSpace, username, this);
        new Thread(gameUpdater).start();
    }

    public void moveTo(Piece piece, Field field) {
        double x, y;
        if(field.isEndField()){
            x = field.getCircle().getCenterX();
            y = field.getCircle().getCenterY();
        } else {
            Point p = field.getNextSpot(piece);
            if(p == null){
                return;
            }
            x = p.x;
            y = p.y;
        }

        if(piece.getCurrentField() != null){
            piece.getCurrentField().removeFromSpot(piece);
            piece.setCurrentField(null);
        }

        piece.setCurrentField(field);

        TranslateTransition translateTransition = new TranslateTransition();
        translateTransition.setDuration(Duration.millis(1000));
        translateTransition.setNode(piece.getCircle());
        translateTransition.setByX(x-piece.getCircle().getCenterX()-piece.getCircle().getTranslateX());
        translateTransition.setByY(y-piece.getCircle().getCenterY()-piece.getCircle().getTranslateY());
        translateTransition.setCycleCount(1);
        translateTransition.setAutoReverse(false);
        translateTransition.play();
        piece.getCircle().toFront();
    }

    public void resetBoard() {
        for(Piece piece: pieces){
            if(piece != null && piece.getCurrentField() != null){
                TranslateTransition translateTransition = new TranslateTransition();
                translateTransition.setDuration(Duration.millis(1000));
                translateTransition.setNode(piece.getCircle());
                translateTransition.setByX(-piece.getCircle().getTranslateX());
                translateTransition.setByY(-piece.getCircle().getTranslateY());
                translateTransition.setCycleCount(1);
                translateTransition.setAutoReverse(false);
                translateTransition.play();
                piece.getCircle().toFront();
                piece.getCurrentField().removeFromSpot(piece);
                piece.setCurrentField(null);
            }
        }
    }

    private void confirmMove() {
        if(currentMove.matches("switch") && selectedCard == -1){
            return;
        } else if (currentMove.matches("yourTurn") && selectedCard == -1 && selectedPiece == null){
            return;
        }

        try {
            userSpace.put("confirmMove");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Field drawEndField(double x, double y, double radius, Color color){
        Field field1 = new Field(pane);
        field1.setEndField(true);
        Circle endField1 = new Circle( x, y, radius);
        field1.setCircle(endField1);
        endField1.setFill(color);
        endField1.setStroke(Paint.valueOf("black"));
        makeDarker(endField1);
        endField1.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> selectedField = field1);
        endField1.toBack();
        pane.getChildren().add(endField1);
        return field1;
    }

    private Field[] drawEndFields(double xI, double yI, int width1, Color color, int sizeDec, double endFieldDistance1, double endFieldDistance2) {

        Field endField1 = drawEndField(xI, yI, width1/2, color);
        Field endField2 = drawEndField(xI - endFieldDistance1, yI - endFieldDistance2, (width1-sizeDec)/2, color);
        Field endField3 = drawEndField(xI - endFieldDistance1*2, yI - endFieldDistance2*2, (width1-sizeDec*2)/2, color);
        Field endField4 = drawEndField(xI - endFieldDistance1*3, yI - endFieldDistance2*3, (width1-sizeDec*3)/2, color);

        return new Field[]{endField1, endField2, endField3, endField4};
    }

    private void createButton(int x, int i){
        Rectangle rectangle1 = new Rectangle();
        rectangle1.setX(x);
        rectangle1.setY(boardWidth);
        rectangle1.setHeight(buttonHeight);
        rectangle1.setWidth(boardWidth/4);
        rectangle1.setFill(Color.DARKGRAY);
        rectangle1.setStroke(Color.BLACK);
        rectangle1.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> selectedCard = i);
        makeDarker(rectangle1);
        pane.getChildren().add(rectangle1);
    }

    static void makeDarker(Shape shape){
        EventHandler<MouseEvent> mouseEventEventHandler = e -> shape.fillProperty().setValue(((Color) shape.getFill()).darker());
        EventHandler<MouseEvent> mouseEventEventHandler1 = e -> shape.fillProperty().setValue(((Color) shape.getFill()).brighter());
        shape.setOnMouseEntered(mouseEventEventHandler);
        shape.setOnMousePressed(mouseEventEventHandler);
        shape.setOnMouseReleased(mouseEventEventHandler1);
        shape.setOnMouseExited(mouseEventEventHandler1);
    }

    private void drawPiece(double x, double y, Color color, String colorName){
        Piece piece = new Piece(pieceIndex);
        Circle pieceC = new Circle(x, y, pieceRadius/2);
        piece.setCircle(pieceC);
        piece.setName("1"+colorName);
        pieceC.setFill(color);
        pieceC.setStroke(Paint.valueOf("black"));
        pieceC.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> selectedPiece = piece);
        makeDarker(pieceC);
        pieceC.toBack();
        pane.getChildren().add(pieceC);
        pieces[pieceIndex] = piece;
        pieceIndex++;
    }

    private void drawStartPieces(double x, double y, Color color, String colorName) {
        Circle startField = new Circle(x, y, startFieldRadius/2);
        startField.setFill(color);
        startField.setStroke(Color.BLACK);
        pane.getChildren().add(startField);

        drawPiece(x-pieceRadius, y, color, colorName);
        drawPiece(x+pieceRadius, y,color, colorName);
        drawPiece(x, y-pieceRadius,color, colorName);
        drawPiece(x, y+pieceRadius,color, colorName);
    }

    public void setGameSpace(Space gameSpace) {
        this.gameSpace = gameSpace;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Field[] getFields() {
        return this.fields;
    }

    public void setCurrentMove(String type) {
        this.currentMove = type;
    }

    public Cards getSelectedCard() {
        return hand[selectedCard];
    }

    public void setUserSpace(Space userSpace) {
        this.userSpace = userSpace;
    }

    public void setHand(Cards[] hand) {
        this.hand = hand;
    }

    public void addCardToHand(Cards card) {
        this.hand[4] = card;
    }

    public Piece[] getPieces(){
        return this.pieces;
    }

    public Piece getSelectedPiece() {
        return selectedPiece;
    }

    public void setHostName(String host) {
        this.host = host;
    }

    public void setUsers(String[] users) {
        this.users = users;
    }

    public void setTeams(int[] teams) {
        this.teams = teams;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setNumberOfTeams(int numberOfTeams) {
        this.numberOfTeams = numberOfTeams;
    }

    public void removeSelectedCard() {
        hand[selectedCard] = null;
    }
}

class GameUpdater implements Runnable{

    private Space game;
    private Space userSpace;
    private String username;
    private GameView gameView;

    public GameUpdater(Space game, Space userSpace, String username, GameView gameView){
        this.game = game;
        this.userSpace = userSpace;
        this.username = username;
        this.gameView = gameView;
    }

    @Override
    public void run() {
        Gson gson = new Gson();

        try {

            while (true) {
                Object[] gameUpdate = game.get(new ActualField("gameUpdate"), new FormalField(String.class), new FormalField(String.class),
                            new ActualField(username), new FormalField(String.class), new FormalField(String.class), new FormalField(String.class));
                String type = (String) gameUpdate[1];
                String actor = (String) gameUpdate[2];
                Cards[] cards = gson.fromJson((String) gameUpdate[4], Cards[].class);
                int[] pieceIndexes = gson.fromJson((String) gameUpdate[5], int[].class);
                int[] positions = gson.fromJson((String) gameUpdate[6], int[].class);

                String cardJson;

                System.out.println(type);

                switch (type) {
                    case "playerMove":
                        for (int i = 0; i < pieceIndexes.length; i++) {
                            int finalI = i;
                            Platform.runLater(() -> gameView.moveTo(gameView.getPieces()[pieceIndexes[finalI]], gameView.getFields()[positions[finalI]]));
                        }
                        break;

                    case "switchCard":
                        gameView.setCurrentMove(type);
                        userSpace.get(new ActualField("confirmMove"));
                        cardJson = gson.toJson(gameView.getSelectedCard());
                        gameView.removeSelectedCard();
                        game.put("gameRequest", "cardSwitch", username, cardJson, "");
                        break;

                    case "hand":
                        for(Cards card: cards){
                            System.out.println(card.getName());
                        }
                        gameView.setHand(cards);
                        break;

                    case "getSwitchedCard":
                        System.out.println(cards[0].getName());
                        gameView.addCardToHand(cards[0]);
                        break;

                    case "resetBoard":
                        Platform.runLater(() -> gameView.resetBoard());
                        break;

                    case "yourTurn":
                        while(true) {
                            gameView.setCurrentMove(type);
                            userSpace.get(new ActualField("confirmMove"));
                            cardJson = gson.toJson(gameView.getSelectedCard());
                            game.put("gameRequest", "turnRequest", username, cardJson, gameView.getSelectedPiece().getIndex());

                            Object[] resp = game.get(new ActualField("gameUpdate"), new ActualField("turnRequestAck"), new FormalField(String.class), new ActualField(username),
                                    new FormalField(String.class), new FormalField(String.class), new FormalField(String.class));

                            if (((String) resp[2]).matches("ok")) {
                                break;
                            }
                        }
                        break;

                    case "gameEnd":
                        break;
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}

class Piece{
    private Circle circle;
    private Field currentField;
    private String name;
    private int index;

    Piece(int index) {
        this.index = index;
    }

    public Field getCurrentField() {
        return currentField;
    }

    public void setCurrentField(Field currentField) {
        this.currentField = currentField;
    }

    public Circle getCircle() {
        return circle;
    }

    public void setCircle(Circle circle) {
        this.circle = circle;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getIndex() {
        return index;
    }
}

class Field{
    private double x1E;
    private double y1E;
    private double x2E;
    private double y2E;
    private double x1S;
    private double y1S;
    private double x2S;
    private double y2S;

    private Path path;
    private Circle circle;

    private boolean isEndField;

    private int index;
    private int version;
    private Pane pane;

    private Piece[] openSpots = new Piece[4];
    private Point[] spots;

    public Field(Pane pane){
        this.pane = pane;
    }

    public void setEnd(double x1E, double y1E, double x2E, double y2E){
        this.x1E = x1E;
        this.y1E = y1E;
        this.x2E = x2E;
        this.y2E = y2E;

    }

    public void setStart(double x1S, double y1S, double x2S, double y2S, int index, int version){
        this.x1S = x1S;
        this.y1S = y1S;
        this.x2S = x2S;
        this.y2S = y2S;
        this.index = index;
        this.version = version;
    }

    public void setField(double v, double w){
        double cIx = (x1E + x1S) / 2;
        double cIy = (y1E + y1S) / 2;
        double cOx = (x2E + x2S) / 2;
        double cOy = (y2E + y2S) / 2;

        Point s1 = new Point((int) round((5.9 * cIx + 1.1 * cOx) / 7), (int) round((5.9 * cIy + 1.1 * cOy) / 7));
        Point s2 = new Point((int) round((4.3 * cIx + 2.7 * cOx) / 7), (int) round((4.3 * cIy + 2.7 * cOy) / 7));
        Point s3 = new Point((int) round((2.7 * cIx + 4.3 * cOx) / 7), (int) round((2.7 * cIy + 4.3 * cOy) / 7));
        Point s4 = new Point((int) round((1.1 * cIx + 5.9 * cOx) / 7), (int) round((1.1 * cIy + 5.9 * cOy) / 7));

        spots = new Point[]{s1, s2, s3, s4};

        path = new Path();

        Color color = Color.BROWN;

        if(version == 0) {
            if (index % 4 == 0) {
                color = GameView.blue;
            } else if (index % 4 == 1) {
                color = GameView.green;
            } else if (index % 4 == 2) {
                color = GameView.yellow;
            } else if (index % 4 == 3) {
                color = GameView.red;
            }
        } else {
            if (index <= 7 || index >=83) {
                color = GameView.purple;
            } else if (index <= 22) {
                color = GameView.red;
            } else if (index <= 37) {
                color = GameView.orange;
            } else if (index <= 52) {
                color = GameView.yellow;
            } else if (index <= 67) {
                color = GameView.green;
            } else {
                color = GameView.blue;

            }
        }

        MoveTo moveTo = new MoveTo(x1S, y1S);
        LineTo line1 = new LineTo(x2S, y2S);
//        LineTo line2 = new LineTo(x2E, y2E);
        QuadCurveTo quadTo = new QuadCurveTo();

        quadTo.setControlX(v);
        quadTo.setControlY(w);

        quadTo.setX(x2E);
        quadTo.setY(y2E);

        LineTo line3 = new LineTo(x1E, y1E);
        LineTo line4 = new LineTo(x1S, y1S);
        path.getElements().add(moveTo);
        path.getElements().addAll(line1, quadTo, line3, line4);

        path.fillProperty().setValue(color);

        GameView.makeDarker(path);

        pane.getChildren().add(path);

    }

    public Point getNextSpot(Piece selectedPiece){
        for(int i = 0; i < 4; i++){
            if(openSpots[i] == null){
                openSpots[i] = selectedPiece;
                return spots[i];
            }
        }
        return null;
    }

    public void removeFromSpot(Piece selectedPiece){
        for(int i = 0; i < 4; i++){
            if(selectedPiece.equals(openSpots[i])){
                openSpots[i] = null;
            }
        }
    }

    public int getIndex() {
        return index;
    }

    public Path getPath(){
        return path;
    }

    public void setCircle(Circle circle) {
        this.circle = circle;
    }

    public Circle getCircle() {
        return circle;
    }

    public boolean isEndField() {
        return isEndField;
    }

    public void setEndField(boolean endField) {
        isEndField = endField;
    }
}
