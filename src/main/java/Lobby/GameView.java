package Lobby;

import Model.Cards;
import com.google.gson.Gson;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
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
    private Label[] cardNameLabels;

    private static final int boardWidth = 900;
    private static final int buttonHeight = 100;
    private static final int boardHeight = boardWidth+buttonHeight;

    private static int version;
    private static int numberOfFields;
    private static final int startFieldOffset = -7;

    private static final int pieceRadius = 25;
    private static final int startFieldRadius = 4*pieceRadius;
    private static final int endFieldRadius = round(3f*pieceRadius);
    private static final int endFieldDistance = 30;
    private static final int endFieldSizeDec = 5;

    private static final int outerCircleBorderPadding = 150;
    private static final int innerCircleBorderPadding = outerCircleBorderPadding+250;

    private static int pieceIndex = 0;
    private Piece[] pieces = new Piece[24];

    private static int endFieldIndex = 0;
    private Field[][] endFields = new Field[6][4];
    private Field[] fields;

    private Piece[] selectedPiece = new Piece[4];
    private Field[] selectedField = new Field[4];
    private int[] selectedCard = new int[4];
    String currentMove = "";

    private Label selectedCardLabel, selectedPiecesLabel, selectedFieldsLabel;

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
    private Label[] usernameLabels;

    public void initialize(){

        label.setLayoutX(boardWidth/2.-label.getPrefWidth()/2);
        label.setLayoutY(boardWidth-label.getPrefHeight()/2-25);

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

        Circle resetSelectionsButton = new Circle(boardWidth/2, 5*boardWidth/8., 25);
        resetSelectionsButton.setFill(Paint.valueOf("black"));
        resetSelectionsButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> resetSelections());
        pane.getChildren().add(resetSelectionsButton);

        VBox selectedBox = new VBox();
        selectedBox.setPrefHeight(15);
        selectedBox.setPrefWidth(150);
        selectedBox.setAlignment(Pos.CENTER);
        selectedBox.setLayoutX(boardWidth/2.-selectedBox.getPrefWidth()/2);
        selectedBox.setLayoutY(11*boardWidth/16.-selectedBox.getPrefHeight()/2);

        HBox selectedCardBox = new HBox();
        Label selectedCardLabelt = new Label("Selected card: ");
        selectedCardLabel = new Label();
        selectedCardBox.getChildren().addAll(selectedCardLabelt, selectedCardLabel);

        HBox selectedPiecesBox = new HBox();
        Label selectedPiecesLabelt = new Label("Selected pieces: ");
        selectedPiecesLabel = new Label();
        selectedPiecesBox.getChildren().addAll(selectedPiecesLabelt, selectedPiecesLabel);

        HBox selectedFieldsBox = new HBox();
        Label selectedFieldsLabelt = new Label("Selected fields: ");
        selectedFieldsLabel = new Label();
        selectedFieldsBox.getChildren().addAll(selectedFieldsLabelt, selectedFieldsLabel);

        selectedBox.getChildren().addAll(selectedCardBox, selectedPiecesBox, selectedFieldsBox);

        pane.getChildren().add(selectedBox);

        cardNameLabels = new Label[]{card1, card2, card3, card4};

        for(int i = 0; i < 4; i++) {
            cardNameLabels[i].setLayoutX((1 + i * 2) * boardWidth / 8. - cardNameLabels[i].getPrefWidth() / 2);
            cardNameLabels[i].setLayoutY(boardHeight - 75 - cardNameLabels[i].getPrefHeight() / 2);
            cardNameLabels[i].toFront();
        }
    }

    private void resetSelections() {
        selectedCardLabel.setText("");
        selectedCard = new int[4];
        selectedPiecesLabel.setText("");
        selectedPiece = new Piece[4];
        selectedFieldsLabel.setText("");
        selectedField = new Field[4];
    }

    public void setup(){
        if(version == 0){
            colorNames = new String[]{"green", "blue", "red", "yellow"};
            colors = new Color[]{green, blue, red, yellow};
        } else {
            colorNames = new String[]{"purple", "red", "orange", "yellow", "green", "blue"};
            colors = new Color[]{purple, red, orange, yellow, green, blue};
        }

        numberOfFields = 60 + version * 30;

        usernameLabels = new Label[numberOfFields/15];

        fields = new Field[numberOfFields + 20];
        for(int i = 0; i < numberOfFields + 20; i++){
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
            double v2 = Math.cos(Math.toRadians((i + 2.5 + version + startFieldOffset) * 360 / numberOfFields));
            double w2 = Math.sin(Math.toRadians((i + 2.5 + version + startFieldOffset) * 360 / numberOfFields));
            int finalI = i;

            double xStartFields = (v*(boardWidth-outerCircleBorderPadding+100)/2)+boardWidth/2.;
            double yStartFields = (w*(boardWidth-outerCircleBorderPadding+100)/2)+boardWidth/2.;

            double xControl = (v*(boardWidth-outerCircleBorderPadding+50)/2)+boardWidth/2.;
            double yControl = (w*(boardWidth-outerCircleBorderPadding+50)/2)+boardWidth/2.;

            double xEndFields = (v*(boardWidth-innerCircleBorderPadding-75)/2)+boardWidth/2.;
            double yEndFields = (w*(boardWidth-innerCircleBorderPadding-75)/2)+boardWidth/2.;

            double xUsernameLabels = (v2*(boardWidth-outerCircleBorderPadding+100)/2)+boardWidth/2.;
            double yUsernameLabels = (w2*(boardWidth-outerCircleBorderPadding+100)/2)+boardWidth/2.;

            fields[i].setField(xControl,yControl);
            fields[i].getPath().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {selectField(finalI);});

            if(i%15 == 0){
                int n = i/15;
                usernameLabels[n]=new Label();
                try{
                    usernameLabels[n].setText(users[n]);
                    if(users[n].matches(username)){
                        usernameLabels[n].setStyle("-fx-font-weight: bold");
                    }
                } catch (Exception e) {

                }
                usernameLabels[n].setPrefHeight(15);
                usernameLabels[n].setPrefWidth(100);
                usernameLabels[n].setLayoutX(xUsernameLabels-usernameLabels[n].getPrefWidth()/2);
                usernameLabels[n].setLayoutY(yUsernameLabels-usernameLabels[n].getPrefHeight()/2);
                usernameLabels[n].setAlignment(Pos.CENTER);
                pane.getChildren().add(usernameLabels[n]);
                drawEndFields(xEndFields, yEndFields, endFieldRadius, n, endFieldSizeDec, endFieldDistance*v, endFieldDistance*w);
                drawStartPieces(xStartFields, yStartFields, n,colorNames[n]);
            }
        }
        GameUpdater gameUpdater = new GameUpdater(gameSpace, userSpace, username, this);
        new Thread(gameUpdater).start();
    }

    private void selectField(int selectField) {
        for(int i = 0; i < 4; i++){
            if(selectedField[i] == null){
                selectedField[i] = fields[selectField];
                selectedFieldsLabel.setText(selectedFieldsLabel.getText()+fields[selectField].getIndex()+ ", ");
                return;
            }
        }
    }

    public void moveTo(Piece piece, Field field) {
        double x, y;
        if (field.isEndField()) {
            x = field.getCircle().getCenterX();
            y = field.getCircle().getCenterY();
        } else if (field.isStartField()){
            x = piece.getCircle().getCenterX();
            y = piece.getCircle().getCenterY();
        }else {
                Point p = field.getNextSpot(piece);
                if(p == null){
                    return;
                }
                x = p.x;
                y = p.y;
            }

        if(piece.getCurrentField() != null){
            piece.getCurrentField().removeFromSpot(piece);
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
        boolean empty = true;
        if(currentMove.matches("switchCard")){
            for(int card :selectedCard){
                if(card != 0){
                    empty = false;
                    break;
                }
            }
        } else if (currentMove.matches("yourTurn")){
            for(int card :selectedCard){
                if(card != 0){
                    empty = false;
                    break;
                }
            }
            for(Piece piece: selectedPiece){
                if(piece != null){
                    empty = false;
                    break;
                }
            }
        }

        if(empty){
            return;
        }

        try {
            userSpace.put("confirmMove");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Field drawEndField(double x, double y, double radius, Color color, int index){
        Field field1 = new Field(pane);
        field1.setEndField(true);
        Circle endField1 = new Circle( x, y, radius);
        field1.setCircle(endField1);
        endField1.setFill(color);
        endField1.setStroke(Paint.valueOf("black"));
        makeDarker(endField1);
        endField1.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> selectField(index));
        endField1.toBack();
        fields[index] = field1;
        fields[index].setIndex(index);
        pane.getChildren().add(endField1);
        return field1;
    }

    private Field[] drawEndFields(double xI, double yI, int width1, int n, int sizeDec, double endFieldDistance1, double endFieldDistance2) {

        Field endField1 = drawEndField(xI, yI, width1/2, colors[n], numberOfFields + n * 4);
        Field endField2 = drawEndField(xI - endFieldDistance1, yI - endFieldDistance2, (width1-sizeDec)/2, colors[n], numberOfFields + n * 4 + 1);
        Field endField3 = drawEndField(xI - endFieldDistance1*2, yI - endFieldDistance2*2, (width1-sizeDec*2)/2, colors[n], numberOfFields + n * 4 + 2);
        Field endField4 = drawEndField(xI - endFieldDistance1*3, yI - endFieldDistance2*3, (width1-sizeDec*3)/2, colors[n], numberOfFields + n * 4 + 3);

        return new Field[]{endField1, endField2, endField3, endField4};
    }

    private void createButton(int x, int i){
        HBox buttonBox = new HBox();

        buttonBox.setLayoutX(x);
        buttonBox.setLayoutY(boardWidth);

        Rectangle rectangle1 = new Rectangle();
        rectangle1.setHeight(buttonHeight);
        rectangle1.setWidth(boardWidth/8);
        rectangle1.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            selectedCard[i] = 1;
            selectedCardLabel.setText(hand[i].getName()+"L");
        });
        rectangle1.setFill(Color.DARKGRAY);

        Rectangle rectangle2 = new Rectangle();
        rectangle2.setHeight(buttonHeight);
        rectangle2.setWidth(boardWidth/8);
        rectangle2.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            selectedCard[i] = 2;
            selectedCardLabel.setText(hand[i].getName()+"R");
        });
        rectangle2.setFill(Color.DARKGRAY);

        buttonBox.getChildren().addAll(rectangle1, rectangle2);

        makeDarker(rectangle1);
        makeDarker(rectangle2);
        buttonBox.setBorder(new Border(new BorderStroke(Color.BLACK,
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
        pane.getChildren().add(buttonBox);
    }

    static void makeDarker(Shape shape){
        EventHandler<MouseEvent> mouseEventEventHandler = e -> shape.fillProperty().setValue(((Color) shape.getFill()).darker());
        EventHandler<MouseEvent> mouseEventEventHandler1 = e -> shape.fillProperty().setValue(((Color) shape.getFill()).brighter());
        shape.setOnMouseEntered(mouseEventEventHandler);
        shape.setOnMousePressed(mouseEventEventHandler);
        shape.setOnMouseReleased(mouseEventEventHandler1);
        shape.setOnMouseExited(mouseEventEventHandler1);
    }

    private Piece drawPiece(double x, double y, Color color, String colorName){
        Piece piece = new Piece(pieceIndex);
        Circle pieceC = new Circle(x, y, pieceRadius/2);
        Text text = new Text(String.valueOf(pieceIndex));
        StackPane stack = new StackPane();

        piece.setCircle(pieceC);
        piece.setName(pieceIndex+colorName);
        pieceC.setFill(color);
        pieceC.setStroke(Paint.valueOf("black"));
        makeDarker(pieceC);
        pieceC.toBack();

        stack.getChildren().addAll(pieceC, text);
        int pieceIndexFinal = pieceIndex;
        stack.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {selectPiece(pieceIndexFinal);});
        stack.setLayoutX(x-pieceRadius/2.);
        stack.setLayoutY(y-pieceRadius/2.);

        pane.getChildren().add(stack);
        pieces[pieceIndex] = piece;
        pieceIndex++;
        return piece;
    }

    private void selectPiece(int index) {
        for(int i = 0; i < 4; i++){
            if(selectedPiece[i] == null){
                selectedPiece[i] = pieces[index];
                selectedPiecesLabel.setText(selectedPiecesLabel.getText()+pieces[index].getIndex()+ ", ");
                return;
            }
        }
    }

    private void drawStartPieces(double x, double y, int n, String colorName) {
        Field startField = new Field(pane);
        Circle startFieldC = new Circle(x, y, startFieldRadius/2);
        startField.setCircle(startFieldC);
        startField.setIndex(76 + n);
        startField.setIsStartField(true);
        startFieldC.setFill(colors[n]);
        startFieldC.setStroke(Color.BLACK);
        fields[76 + n] = startField;
        pane.getChildren().add(startFieldC);

        drawPiece(x-pieceRadius, y, colors[n], colorName).setCurrentField(startField);
        drawPiece(x, y-pieceRadius,colors[n], colorName).setCurrentField(startField);
        drawPiece(x+pieceRadius, y,colors[n], colorName).setCurrentField(startField);
        drawPiece(x, y+pieceRadius,colors[n], colorName).setCurrentField(startField);
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
        for(int i = 0; i < 4; i++){
            if(selectedCard[i]>0){
                return hand[i];
            }
        }
        return null;
    }

    public int getSelectedCardOption(){
        for(int i = 0; i < 4; i++){
            if(selectedCard[i]>0){
                return selectedCard[i];
            }
        }
        return -1;
    }

    public void setUserSpace(Space userSpace) {
        this.userSpace = userSpace;
    }

    public void setHand(Cards[] hand) {
        Platform.runLater(
                () -> {
                    for(int i = 0; i < 4; i++){
                        cardNameLabels[i].setText(hand[i].getName());
                    }
                }
        );
        this.hand = hand;
    }

    public void addCardToHand(Cards newCard) {
        Platform.runLater(
                () -> {
                    for(int i = 0; i < 4; i++){
                        if(hand[i] == null){
                            hand[i] = newCard;
                        }
                        if(cardNameLabels[i].getText().matches("")){
                            cardNameLabels[i].setText(newCard.getName());
                            break;
                        }
                    }
                }
        );

    }

    public Piece[] getPieces(){
        return this.pieces;
    }

    public int[] getSelectedPieces() {
        int[] selectedPieceIndexes = new int[4];
        for(int i = 0; i < 4; i++){
            if(selectedPiece[i] != null) {
                selectedPieceIndexes[i] = selectedPiece[i].getCurrentField().getIndex();
            }
        }
        return selectedPieceIndexes;
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
        for(Label card: cardNameLabels){
            if(card.getText().matches(getSelectedCard().getName())){
                Platform.runLater(() -> card.setText(""));
                break;
            }
        }
        for(int i = 0; i < 4; i++) {
            if(hand[i].equals(getSelectedCard())){
                hand[i] = null;
            }
        }
    }

    public int[] getSelectedFields() {
        int[] selectedFieldIndexes = new int[4];
        for(int i = 0; i < 4; i++){
            if(selectedField[i] != null) {
                selectedFieldIndexes[i] = selectedField[i].getIndex();
            }
        }
        return selectedFieldIndexes;
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
        System.out.println(username);
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
                        game.put("gameRequest", "switchCard", username, cardJson, "");
                        break;

                    case "hand":
                        gameView.setHand(cards);
                        break;

                    case "getSwitchedCard":
                        System.out.println(actor + cards[0].getName());
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
                            int selectedCardOption = gameView.getSelectedCardOption() - 1;
                            String piecesJson = gson.toJson(gameView.getSelectedPieces());

                            String fieldsJson = gson.toJson(gameView.getSelectedFields());
                            game.put("gameRequest", "turnRequest", username, cardJson, piecesJson, fieldsJson, selectedCardOption);
                            Object[] resp = game.get(new ActualField("gameUpdate"), new ActualField("turnRequestAck"), new FormalField(String.class), new ActualField(username),
                                    new FormalField(String.class), new FormalField(String.class), new FormalField(String.class));

                            if (((String) resp[2]).matches("ok")) {
                                gameView.removeSelectedCard();
                                break;
                            }
                            System.out.println("Move illegal");
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

    private boolean isEndField, isStartField;

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
                color = GameView.green;
            } else if (index % 4 == 1) {
                color = GameView.yellow;
            } else if (index % 4 == 2) {
                color = GameView.red;
            } else if (index % 4 == 3) {
                color = GameView.blue;
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

    public void setIndex(int i){
        index = i;
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

    public boolean isStartField() {
        return isStartField;
    }

    public void setIsStartField(boolean b) {
        isStartField = b;
    }
}
