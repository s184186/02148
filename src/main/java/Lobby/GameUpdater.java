package Lobby;

import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
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

import static java.lang.Math.abs;
import static java.lang.Math.round;
import javafx.geometry.Insets;

public class GameUpdater extends Application {

    public StackPane stackPane;
    private static Field[] fields;
    public Pane pane;
    public Label label;

    public final int boardWidth = 900;
    public final int buttonHeight = 100;
    public final int boardHeight = boardWidth+buttonHeight;

    public static int numberOfFields;
    public final int startFieldOffset = 7;
    public final int version = 1;

    public final int pieceRadius = 15;
    public final int startFieldRadius = 4*pieceRadius;
    public final int endFieldRadius = round(2.5f*pieceRadius);

    public final int outerCircleBorderPadding = 150;
    public final int innerCircleBorderPadding = outerCircleBorderPadding+250;
    public Label card4;
    public Label card2;
    public Label card1;
    public Label card3;
    public Piece[][] pieces = new Piece[4][4];
    public Field[][] endFields = new Field[4][4];

    public Piece selectedPiece = new Piece();
    public Field selectedField = new Field(pane);

    public static Color blue = Color.rgb(61,88,222);
    public static Color purple = Color.rgb(162,19,192);
    public static Color red = Color.rgb(219,35,35);
    public static Color orange = Color.rgb(255,149,23);
    public static Color yellow = Color.rgb(233,227,23);
    public static Color green  = Color.rgb(24,170,24);

    public static void main(String[] args){
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(MainMenuView.class.getResource("/game.fxml"));
        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root);
        primaryStage.setResizable(false);
        primaryStage.setTitle("Partners");
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    public void initialize(){
        Label[] cards = {card1, card2, card3, card4};
        if(version == 1){
            numberOfFields = 90;
        } else{
            numberOfFields = 60;
        }
        label.setText("Choose a card");

        label.setLayoutX(boardWidth/2.-label.getPrefWidth()/2);
        label.setLayoutY(1.2*boardWidth/2.-label.getPrefHeight()/2);

        for(int i = 0; i < 4; i++){
            cards[i].setLayoutX((1+i*2)*boardWidth/8.-label.getPrefWidth()/2);
            cards[i].setLayoutY(boardHeight-75-label.getPrefHeight()/2);
        }

        pane.setBackground(new Background(new BackgroundFill(Color.TURQUOISE, CornerRadii.EMPTY, Insets.EMPTY)));

        stackPane.setPrefSize(boardWidth,boardHeight);

        createButton(0);
        createButton(boardWidth/4);
        createButton(2*boardWidth/4);
        createButton(3*boardWidth/4);

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
            fields[i].setField();
            int finalI = i;
            fields[i].getPath().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> selectedField = fields[finalI]);
            double xO, yO;

            double xI = (fields[i].getX1S() + fields[i].getX1E()) / 2;
            double yI = (fields[i].getY1S() + fields[i].getY1E()) / 2;

            double x = (fields[i].getX2S() + fields[i].getX2E()) / 2;
            double y = (fields[i].getY2S() + fields[i].getY2E()) / 2;

            double v = Math.cos(Math.toRadians((i + 0.5 + startFieldOffset) * 360 / numberOfFields));
            double w = Math.sin(Math.toRadians((i + 0.5 + startFieldOffset) * 360 / numberOfFields));

            double v1 = (int) round(Math.cos(Math.toRadians((i) * 360 / numberOfFields)));
            double w1 = (int) round(Math.sin(Math.toRadians((i) * 360 / numberOfFields)));

            double ratio1 = abs(v * ((3.5 - v1 * 2.5) / 5) - w1 * w * 2.5 / 5);
            double ratio2 = abs(w * ((3.5 - w1 * 2.5) / 5) - v1 * v * 2.5 / 5);

            xO = x - (startFieldRadius * ratio1);
            yO = y - (startFieldRadius * ratio2);

            int sizeDec = 5;
            double endFieldDistance1 = 25;
            double endFieldDistance2 = -25;

            double offsetx1 = v1 * (endFieldRadius / 2.) * abs(v) - w1 * (endFieldRadius / 2.) * abs(w);
            double offsety1 = w1 * (endFieldRadius / 2.) * abs(w) - v1 * (endFieldRadius / 2.) * abs(v);

            if (numberOfFields == 90) {
                if (i == 0) {
                    xO = (x - (startFieldRadius / 12.));
                    yO = (y - (startFieldRadius / 4.));

                    endFieldDistance1 = 25;
                    endFieldDistance2 = 14;

                    offsetx1 = 5 * endFieldRadius / 12;
                    offsety1 = endFieldRadius / 4.;

                    drawEndFields(xI, yI, endFieldRadius, offsetx1, offsety1, purple, sizeDec, endFieldDistance1, endFieldDistance2);
                    pieces[0] = drawStartPieces(xO, yO, purple);
                } else if (i == 15) {
                    xO = x - (startFieldRadius / 2.);
                    yO = y;

                    endFieldDistance1 = 0;
                    endFieldDistance2 = 25;

                    offsetx1 = 0;
                    offsety1 = endFieldRadius / 2;

                    drawEndFields(xI, yI, endFieldRadius, offsetx1, offsety1, red, sizeDec, endFieldDistance1, endFieldDistance2);
                    pieces[1] = drawStartPieces(xO, yO, red);
                } else if (i == 30) {
                    xO = x - (11 * startFieldRadius / 12.);
                    yO = y - startFieldRadius / 4.;

                    endFieldDistance1 = -25;
                    endFieldDistance2 = 14;

                    offsetx1 = -5 * endFieldRadius / 12;
                    offsety1 = endFieldRadius / 4.;

                    drawEndFields(xI, yI, endFieldRadius, offsetx1, offsety1,orange,sizeDec, endFieldDistance1, endFieldDistance2);
                    pieces[2] = drawStartPieces(xO, yO, orange);

                } else if (i == 45) {
                    xO = x - (11 * startFieldRadius / 12.);
                    yO = y - (3 * startFieldRadius / 4.);

                    endFieldDistance1 = -25;
                    endFieldDistance2 = -14;

                    offsetx1 = -5* endFieldRadius / 12;
                    offsety1 = -endFieldRadius / 4;

                    drawEndFields(xI, yI, endFieldRadius, offsetx1, offsety1,yellow,sizeDec, endFieldDistance1, endFieldDistance2);
                    pieces[3] = drawStartPieces(xO, yO, yellow);
                } else if (i == 60) {
                    xO = x - (startFieldRadius / 2.);
                    yO = y - (startFieldRadius);

                    endFieldDistance1 = 0;
                    endFieldDistance2 = -25;

                    offsetx1 = 0;
                    offsety1 = -endFieldRadius / 2;

                    drawEndFields(xI, yI, endFieldRadius, offsetx1, offsety1,green,sizeDec, endFieldDistance1, endFieldDistance2);
                    drawStartPieces(xO, yO, green);

                } else if (i == 75) {
                    xO = x - (startFieldRadius / 12.);
                    yO = y - (3 * startFieldRadius / 4.);

                    endFieldDistance1 = 25;
                    endFieldDistance2 = -14;

                    offsetx1 = 5 * endFieldRadius / 12;
                    offsety1 = -endFieldRadius / 4.;

                    drawEndFields(xI, yI, endFieldRadius, offsetx1, offsety1,blue,sizeDec, endFieldDistance1, endFieldDistance2);
                    drawStartPieces(xO, yO, blue);
                }
            } else {
                if (i == 0) {
                    endFields[0] = drawEndFields(xI, yI, endFieldRadius, offsetx1, offsetx1, blue, sizeDec, endFieldDistance1, endFieldDistance1);
                    pieces[0] = drawStartPieces(xO, yO, blue);
                } else if (i == 15) {
                    endFields[1] = drawEndFields(xI, yI, endFieldRadius, offsetx1, offsety1, red, sizeDec, endFieldDistance2, endFieldDistance1);
                    pieces[1] = drawStartPieces(xO, yO, red);
                } else if (i == 30) {
                    endFields[2] = drawEndFields(xI, yI, endFieldRadius, offsetx1, offsetx1, yellow, sizeDec, endFieldDistance2, endFieldDistance2);
                    pieces[2] = drawStartPieces(xO, yO, yellow);
                } else if (i == 45) {
                    endFields[3] = drawEndFields(xI, yI, endFieldRadius, offsetx1, offsety1, green, sizeDec, endFieldDistance1, endFieldDistance2);
                    pieces[3] = drawStartPieces(xO, yO, green);
                }
            }
        }


        Line line = new Line();
        line.setStartX(0);
        line.setStartY(0);
        line.setEndX(boardWidth);
        line.setEndY(boardWidth);
        line.toFront();
        pane.getChildren().add(line);

        line = new Line();
        line.setStartX(0);
        line.setStartY(boardWidth);
        line.setEndX(boardWidth);
        line.setEndY(0);
        line.toFront();
        pane.getChildren().add(line);
    }

    private void resetBoard() {
        for(Piece[] teamPieces: pieces){
            for(Piece piece: teamPieces){
                if(piece.getCurrentField() != null){
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
    }

    private void doSomething() {
        double x, y;
        if(selectedField.isEndField()){
            x = selectedField.getCircle().getCenterX();
            y = selectedField.getCircle().getCenterY();
        } else {
            Point p = selectedField.getNextSpot(selectedPiece);
            x = p.x;
            y = p.y;
        }

        if(selectedPiece.getCurrentField() != null){
            selectedPiece.getCurrentField().removeFromSpot(selectedPiece);
            selectedPiece.setCurrentField(null);
        }

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

    private Field[] drawEndFields(double xI, double yI, int width1, double offsetx1, double offsety1, Color color, int sizeDec, double endFieldDistance2, double endFieldDistance1) {
        int width2 = width1 - sizeDec;
        int width3 = width2 - sizeDec;
        int width4 = width3 - sizeDec;

        double offsetx2 = offsetx1 + endFieldDistance2;
        double offsetx3 = offsetx2 + endFieldDistance2;
        double offsetx4 = offsetx3 + endFieldDistance2;

        double offsety2 = offsety1 + endFieldDistance1;
        double offsety3 = offsety2 + endFieldDistance1;
        double offsety4 = offsety3 + endFieldDistance1;

        Field endField1 = drawEndField(xI-offsetx1, yI-offsety1, width1/2, color);
        Field endField2 = drawEndField(xI-offsetx2, yI-offsety2, width2/2, color);
        Field endField3 = drawEndField(xI-offsetx3, yI-offsety3, width3/2, color);
        Field endField4 = drawEndField(xI-offsetx4, yI-offsety4, width4/2, color);

        return new Field[]{endField1, endField2, endField3, endField4};
    }

    public void createButton(int x){
        Rectangle rectangle1 = new Rectangle();
        rectangle1.setX(x);
        rectangle1.setY(boardWidth);
        rectangle1.setHeight(buttonHeight);
        rectangle1.setWidth(boardWidth/4);
        rectangle1.setFill(Color.DARKGRAY);
        rectangle1.setStroke(Color.BLACK);
        makeDarker(rectangle1);
        pane.getChildren().add(rectangle1);
    }

    public static void makeDarker(Shape shape){
        EventHandler<MouseEvent> mouseEventEventHandler = e -> shape.fillProperty().setValue(((Color) shape.getFill()).darker());
        EventHandler<MouseEvent> mouseEventEventHandler1 = e -> shape.fillProperty().setValue(((Color) shape.getFill()).brighter());
        shape.setOnMouseEntered(mouseEventEventHandler);
        shape.setOnMousePressed(mouseEventEventHandler);
        shape.setOnMouseReleased(mouseEventEventHandler1);
        shape.setOnMouseExited(mouseEventEventHandler1);
    }

    private Piece drawPiece(double x, double y, Color color){
        Piece piece = new Piece();
        Circle pieceC = new Circle(x, y, pieceRadius/2);
        piece.setCircle(pieceC);
        piece.setName("1"+piece);
        pieceC.setFill(color);
        pieceC.setStroke(Paint.valueOf("black"));
        pieceC.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> selectedPiece = piece);
        makeDarker(pieceC);
        pieceC.toBack();
        pane.getChildren().add(pieceC);
        return piece;
    }

    private Piece[] drawStartPieces(double x, double y, Color color) {
        Circle startField = new Circle(x+startFieldRadius/2., y+startFieldRadius/2., startFieldRadius/2);
        startField.setFill(color);
        startField.setStroke(Color.BLACK);
        pane.getChildren().add(startField);

        Piece piece1 = drawPiece(x+(startFieldRadius-pieceRadius)/2.+pieceRadius/2., y+startFieldRadius-(pieceRadius*1.5)+pieceRadius/2., color);
        Piece piece2 = drawPiece(x+(startFieldRadius-pieceRadius)/2.+pieceRadius/2., y+pieceRadius/2.+pieceRadius/2.,color);
        Piece piece3 = drawPiece(x+startFieldRadius-(pieceRadius*1.5)+pieceRadius/2., y+(startFieldRadius-pieceRadius)/2.+pieceRadius/2.,color);
        Piece piece4 = drawPiece(x+pieceRadius/2.+pieceRadius/2., y+(startFieldRadius-pieceRadius)/2.+pieceRadius/2.,color);

        return new Piece[]{piece1, piece2, piece3, piece4};
    }

}

class Piece{
    private double x, y;
    private Circle circle;
    private Field currentField;
    private String name;

    public Field getCurrentField() {
        return currentField;
    }

    public void setCurrentField(Field currentField) {
        this.currentField = currentField;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
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

    private double cIx, cIy, cOx, cOy;

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

    public void setField(){
        cIx = (x1E+x1S)/2;
        cIy = (y1E+y1S)/2;
        cOx = (x2E+x2S)/2;
        cOy = (y2E+y2S)/2;

        Point s1 = new Point((int) round((5.9 * cIx + 1.1 * cOx) / 7), (int) round((5.9 * cIy + 1.1 * cOy) / 7));
        Point s2 = new Point((int) round((4.3 * cIx + 2.7 * cOx) / 7), (int) round((4.3 * cIy + 2.7 * cOy) / 7));
        Point s3 = new Point((int) round((2.7 * cIx + 4.3 * cOx) / 7), (int) round((2.7 * cIy + 4.3 * cOy) / 7));
        Point s4 = new Point((int) round((1.1 * cIx + 5.9 * cOx) / 7), (int) round((1.1 * cIy + 5.9 * cOy) / 7));

        spots = new Point[]{s1, s2, s3, s4};

        path = new Path();

        Color color = Color.BROWN;

        if(version == 0) {
            if (index % 4 == 0) {
                color = GameUpdater.blue;
            } else if (index % 4 == 1) {
                color = GameUpdater.green;
            } else if (index % 4 == 2) {
                color = GameUpdater.yellow;
            } else if (index % 4 == 3) {
                color = GameUpdater.red;
            }
        } else {
            if (index <= 7 || index >=83) {
                color = GameUpdater.purple;
            } else if (index <= 22) {
                color = GameUpdater.red;
            } else if (index <= 37) {
                color = GameUpdater.orange;
            } else if (index <= 52) {
                color = GameUpdater.yellow;
            } else if (index <= 67) {
                color = GameUpdater.green;
            } else {
                color = GameUpdater.blue;

            }
        }

        MoveTo moveTo = new MoveTo(x1S, y1S);
        LineTo line1 = new LineTo(x2S, y2S);
//        LineTo line2 = new LineTo(x2E, y2E);
        QuadCurveTo quadTo = new QuadCurveTo();
        quadTo.setControlX((cOx));
        quadTo.setControlY((cOy));
        quadTo.setX(x2E);
        quadTo.setY(y2E);

        LineTo line3 = new LineTo(x1E, y1E);
        LineTo line4 = new LineTo(x1S, y1S);
        path.getElements().add(moveTo);
        path.getElements().addAll(line1, quadTo, line3, line4);

        path.fillProperty().setValue(color);

        GameUpdater.makeDarker(path);

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

    public double getX1E() {
        return x1E;
    }

    public double getY1E() {
        return y1E;
    }

    public double getX2E() {
        return x2E;
    }

    public double getY2E() {
        return y2E;
    }

    public double getX1S() {
        return x1S;
    }

    public double getY1S() {
        return y1S;
    }

    public double getX2S() {
        return x2S;
    }

    public double getY2S() {
        return y2S;
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

