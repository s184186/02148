package Lobby;

import Model.Cards;
import com.google.gson.Gson;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.awt.*;
import java.io.IOException;

import static java.lang.Math.round;

import javafx.geometry.Insets;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.Space;

public class GameView{

    public StackPane stackPane;
    public Pane pane;
    public Label card1, card2, card3, card4, label;

    private final int boardWidth = 900;
    private final int buttonHeight = 100;
    private final int boardHeight = boardWidth+buttonHeight;

    private static final int version = 0;
    private static int numberOfFields = 60 + version * 30;
    private final int startFieldOffset = 7;

    private final int pieceRadius = 25;
    private final int startFieldRadius = 4*pieceRadius;
    private final int endFieldRadius = round(3f*pieceRadius);

    private final int outerCircleBorderPadding = 150;
    private final int innerCircleBorderPadding = outerCircleBorderPadding+250;

    private static int pieceIndex = 0;
    private Piece[] pieces = new Piece[24];

    private static Field[][] endFields = new Field[6][4];
    private static Field[] fields;

    private Piece selectedPiece = null;
    private Field selectedField = null;
    private int selectedCard = -1;

    static Color blue = Color.rgb(61,88,222);
    static Color purple = Color.rgb(162,19,192);
    static Color red = Color.rgb(219,35,35);
    static Color orange = Color.rgb(255,149,23);
    static Color yellow = Color.rgb(233,227,23);
    static Color green  = Color.rgb(24,170,24);
    private Space space;
    private String username;

//    public static void main(String[] args){
//        launch(args);
//    }
//
//    @Override
//    public void start(Stage primaryStage) throws InterruptedException {
//        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/game.fxml"));
//        Parent root = null;
//
//        try {
//            root = fxmlLoader.load();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        Scene scene = new Scene(root);
//        primaryStage.setResizable(false);
//        primaryStage.setTitle("Partners");
//        primaryStage.getIcons().add(new Image(getClass().getResource("/icon.png").toExternalForm()));
//        primaryStage.setScene(scene);
//        primaryStage.show();
//    }

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

    public Piece[] getPieces(){
        return this.pieces;
    }

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
        centerButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> doSomething());
        pane.getChildren().add(centerButton);

        Circle belowCenterButton = new Circle(boardWidth/2, 2*boardWidth/3., 50);
        belowCenterButton.setFill(Paint.valueOf("black"));
        belowCenterButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> resetBoard());
        pane.getChildren().add(belowCenterButton);

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

            int sizeDec = 5;

            double xO = (v*(boardWidth-outerCircleBorderPadding+100)/2)+boardWidth/2.;
            double yO = (w*(boardWidth-outerCircleBorderPadding+100)/2)+boardWidth/2.;

            double x1 = (v*(boardWidth-outerCircleBorderPadding+50)/2)+boardWidth/2.;
            double y1 = (w*(boardWidth-outerCircleBorderPadding+50)/2)+boardWidth/2.;

            double xI = (v*(boardWidth-innerCircleBorderPadding-75)/2)+boardWidth/2.;
            double yI = (w*(boardWidth-innerCircleBorderPadding-75)/2)+boardWidth/2.;

            fields[i].setField(x1,y1);
            fields[i].getPath().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> selectedField = fields[finalI]);

            if (numberOfFields == 90) {
                if (i == 0) {
                    drawEndFields(xI, yI, endFieldRadius, purple, sizeDec, 25, 14);
                    drawStartPieces(xO, yO, purple,"purple");
                } else if (i == 15) {
                    drawEndFields(xI, yI, endFieldRadius, red, sizeDec, 0, 25);
                    drawStartPieces(xO, yO, red,"red");
                } else if (i == 30) {
                    drawEndFields(xI, yI, endFieldRadius,orange,sizeDec, -25, 14);
                    drawStartPieces(xO, yO, orange,"orange");
                } else if (i == 45) {
                    drawEndFields(xI, yI, endFieldRadius,yellow,sizeDec, -25, -14);
                    drawStartPieces(xO, yO, yellow,"yellow");
                } else if (i == 60) {
                    drawEndFields(xI, yI, endFieldRadius,green,sizeDec, 0, -25);
                    drawStartPieces(xO, yO, green,"green");
                } else if (i == 75) {
                    drawEndFields(xI, yI, endFieldRadius,blue,sizeDec, 25, -14);
                    drawStartPieces(xO, yO, blue,"blue");
                }
            } else {
                if (i == 0) {
                    endFields[0] = drawEndFields(xI, yI, endFieldRadius, blue, sizeDec, 25, 25);
                    drawStartPieces(xO, yO, blue,"blue");
                } else if (i == 15) {
                    endFields[1] = drawEndFields(xI, yI, endFieldRadius, red, sizeDec, -25, 25);
                    drawStartPieces(xO, yO, red,"red");
                } else if (i == 30) {
                    endFields[2] = drawEndFields(xI, yI, endFieldRadius, yellow, sizeDec, -25, -25);
                    drawStartPieces(xO, yO, yellow, "yellow");
                } else if (i == 45) {
                    endFields[3] = drawEndFields(xI, yI, endFieldRadius, green, sizeDec, 25, -25);
                    drawStartPieces(xO, yO, green, "green");
                }
            }
        }
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

    private void doSomething() {
        if(selectedPiece == null || selectedField == null || selectedCard == -1){
            return;
        }
        double x, y;
        if(selectedField.isEndField()){
            x = selectedField.getCircle().getCenterX();
            y = selectedField.getCircle().getCenterY();
        } else {
            Point p = selectedField.getNextSpot(selectedPiece);
            if(p == null){
                return;
            }
            x = p.x;
            y = p.y;
        }

        if(selectedPiece.getCurrentField() != null){
            selectedPiece.getCurrentField().removeFromSpot(selectedPiece);
            selectedPiece.setCurrentField(null);
        }

        System.out.println(selectedField.getIndex());
        System.out.println(selectedPiece.getName());
        System.out.println(selectedCard);

        selectedPiece.setCurrentField(selectedField);

        TranslateTransition translateTransition = new TranslateTransition();
        translateTransition.setDuration(Duration.millis(1000));
        translateTransition.setNode(selectedPiece.getCircle());
        translateTransition.setByX(x-selectedPiece.getCircle().getCenterX()-selectedPiece.getCircle().getTranslateX());
        translateTransition.setByY(y-selectedPiece.getCircle().getCenterY()-selectedPiece.getCircle().getTranslateY());
        translateTransition.setCycleCount(1);
        translateTransition.setAutoReverse(false);
        translateTransition.play();
        selectedPiece.getCircle().toFront();
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
        Piece piece = new Piece();
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

    public void setSpace(Space space) {
        this.space = space;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Field[] getFields() {
        return this.fields;
    }
}

class GameUpdater implements Runnable{

    private Space space;
    private String username;
    private GameView gameView;

    public GameUpdater(Space space, String username, GameView gameView){

        this.space = space;
        this.username = username;
        this.gameView = gameView;
    }

    @Override
    public void run() {
        Gson gson = new Gson();

        while(true){
            Object[] gameUpdate = new Object[0];
            try {
                gameUpdate = space.get(new ActualField("gameUpdate"), new FormalField(String.class), new FormalField(String.class),
                        new ActualField(username), new FormalField(String.class), new FormalField(String.class), new FormalField(String.class));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String type = (String) gameUpdate[1];
            String actor = (String) gameUpdate[2];
            Cards[] card = gson.fromJson((String) gameUpdate[4], Cards[].class);
            int[] pieceIndexes = gson.fromJson((String) gameUpdate[5], int[].class);
            int[] positions = gson.fromJson((String) gameUpdate[6], int[].class);

            switch (type){
                case "playerMove":
                    for(int i = 0; i < pieceIndexes.length; i++){
                        int finalI = i;
                        Platform.runLater(
                                () -> gameView.moveTo(gameView.getPieces()[pieceIndexes[finalI]], gameView.getFields()[positions[finalI]]));
                    }
                    break;

                case "switch":

                    break;

                case "hand":

                    break;

                case "getSwitchedCard":

                    break;

                case "resetBoard":
                    Platform.runLater(
                            () -> gameView.resetBoard());
                    break;
            }
        }
    }
}

class Piece{
    private Circle circle;
    private Field currentField;
    private String name;

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
