package Lobby;

import javafx.animation.TranslateTransition;
import javafx.application.Application;
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
import javafx.stage.Stage;
import javafx.util.Duration;
import org.jspace.SequentialSpace;
import org.jspace.Space;
import java.awt.*;

import static java.lang.Math.abs;
import static java.lang.Math.round;
import javafx.geometry.Insets;

public class GameUpdater extends Application {

    static Space space = new SequentialSpace();
    private static String username = "fred", card = "0";
    public StackPane stackPane;
    private static Field[] fields;
    public Pane pane;
    public Label label;

    public final int boardWidth = 850;
    public final int buttonHeight = 150;
    public final int boardHeight = boardWidth+buttonHeight;


    public static int numberOfFields;
    public final int startFieldOffset = 7;
    public final int version = 0;

    public final int startFieldRadius = 90;
    public final int pieceRadius = 25;

    public final int outerCircleBorderPadding = 100;
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
        primaryStage.getIcons().add(new Image(getClass().getResource("/icon.png").toExternalForm()));
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
            int x1 = (int) ((v*(boardWidth-innerCircleBorderPadding)/2)+boardWidth/2);
            int y1 = (int) ((w*(boardWidth-innerCircleBorderPadding)/2)+boardWidth/2);
            int x2 = (int) ((v*(boardWidth-outerCircleBorderPadding)/2)+boardWidth/2);
            int y2 = (int) ((w*(boardWidth-outerCircleBorderPadding)/2)+boardWidth/2);
            fields[i].setStart(x1, y1, x2, y2, i, version);
            fields[(i+numberOfFields-1)%numberOfFields].setEnd(x1, y1, x2, y2);
        }

        for(int i = 0; i < numberOfFields; i++){
            fields[i].setField();
            int finalI = i;
            fields[i].getPath().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> selectedField =  fields[finalI]);
            int xO, yO;

            int xI = (int) ((fields[i].getX1S()+fields[i].getX1E())/2);
            int yI = (int) ((fields[i].getY1S()+fields[i].getY1E())/2);

            int x = (int) (fields[i].getX2S()+fields[i].getX2E());
            int y = (int) (fields[i].getY2S()+fields[i].getY2E());

            double v = Math.cos(Math.toRadians((i+0.5+startFieldOffset)*360/numberOfFields));
            double w = Math.sin(Math.toRadians((i+0.5+startFieldOffset)*360/numberOfFields));

            double v1 = Math.cos(Math.toRadians((i)*360/numberOfFields));
            double w1 = Math.sin(Math.toRadians((i)*360/numberOfFields));

            double ratio1 = v-v*4/5;
            double ratio2 = w-w*4/5;

            int width1 = 60;
             if(i==0){
                if(numberOfFields==90){
                    xO = (int) round(x/2.-(startFieldRadius*ratio2));
                    yO = (int) round(y/2.-(startFieldRadius*ratio2));

                    int o11 = 3*width1/4;
                    int o12 = o11 + 10;
                    int o13 = o12 + 10;
                    int o14 = o13 + 10;

                    int o21 = 11*width1/12;
                    int o22 = o21 + 15;
                    int o23 = o22 + 15;
                    int o24 = o23 + 15;

                    drawEndFields(xI, yI, width1, o11, o12, o13, o14, o21, o22, o23, o24,purple);
                } else {
                    System.out.println(v);
                    System.out.println(w);
                    xO = (int) (x/2-(startFieldRadius*ratio1));
                    yO = (int) (y/2-(startFieldRadius*ratio2));

                    int o11 = (int) round(width1*ratio1);
                    int o12 = o11 + 20;
                    int o13 = o12 + 20;
                    int o14 = o13 + 20;

                    endFields[0] = drawEndFields(xI, yI, width1, o11, o12, o13, o14, o11, o12, o13, o14,blue);
                }


                pieces[0] = drawStartPieces(xO, yO, blue);

            } else if(i==15){
                if(numberOfFields==90){
                    xO = x/2-(startFieldRadius/2);
                    yO = y/2;

                    int o11 = width1;
                    int o12 = o11 + 17;
                    int o13 = o12 + 17;
                    int o14 = o13 + 17;

                    int o21 = width1/2;
                    int o22 = o21 - 3;
                    int o23 = o22 - 3;
                    int o24 = o23 - 3;

                    drawEndFields(xI, yI, width1, o11, o12, o13, o14, o21, o22, o23, o24,red);
                } else {
                    System.out.println(v);
                    System.out.println(w);
                    xO = (int) (x/2-(startFieldRadius*ratio1));
                    yO = (int) (y/2-(startFieldRadius*ratio2));

                    int o11 = (int) round(width1*ratio1);
                    int o12 = o11 + 20;
                    int o13 = o12 + 20;
                    int o14 = o13 + 20;

                    int o21 = (int) round(width1*ratio2);
                    int o22 = o21 - 25;
                    int o23 = o22 - 25;
                    int o24 = o23 - 25;

                    endFields[1] = drawEndFields(xI, yI, width1, o11, o12, o13, o14, o21, o22, o23, o24,red);
                }

                 pieces[1] =drawStartPieces(xO, yO, red);
            }else if(i==30){
                if(numberOfFields==90){
                    xO =  x/2-(11*startFieldRadius/12);
                    yO = y/2-startFieldRadius/4;

                    int o11 = 3*width1/4;
                    int o12 = o11 + 15;
                    int o13 = o12 + 15;
                    int o14 = o13 + 15;

                    int o21 = width1/12;
                    int o22 = o21 - 20;
                    int o23 = o22 - 20;
                    int o24 = o23 - 20;

                    drawEndFields(xI, yI, width1, o11, o12, o13, o14, o21, o22, o23, o24,orange);

                } else {
                    System.out.println(v);
                    System.out.println(w);
                    xO = (int) (x/2-(startFieldRadius*ratio1));
                    yO = (int) (y/2-(startFieldRadius*ratio2));

                    int o21 = (int) round(width1*ratio2);
                    int o22 = o21 - 25;
                    int o23 = o22 - 25;
                    int o24 = o23 - 25;

                    endFields[2] = drawEndFields(xI, yI, width1, o21, o22, o23, o24, o21, o22, o23, o24,yellow);
                }

                 pieces[2] =drawStartPieces(xO, yO, yellow);
            }else if(i==45){
                if(numberOfFields==90){
                    xO = x/2-(11*startFieldRadius/12);
                    yO = y/2-(3*startFieldRadius/4);

                    int o11 = width1/12;
                    int o12 = o11 + 15;
                    int o13 = o12 + 15;
                    int o14 = o13 + 15;

                    int o21 = width1/4;
                    int o22 = o21 - 20;
                    int o23 = o22 - 20;
                    int o24 = o23 - 20;

                    drawEndFields(xI, yI, width1, o21, o22, o23, o24, o11, o12, o13, o14,yellow);

                } else {
                    System.out.println(v);
                    System.out.println(w);
                    xO = (int) (x/2-(startFieldRadius*ratio1));
                    yO = (int) (y/2-(startFieldRadius*ratio2));

                    int o11 = (int) round(width1*ratio1);
                    int o12 = o11 + 20;
                    int o13 = o12 + 20;
                    int o14 = o13 + 20;

                    int o21 = (int) round(width1*ratio2);
                    int o22 = o21 - 25;
                    int o23 = o22 - 25;
                    int o24 = o23 - 25;

                    endFields[3] = drawEndFields(xI, yI, width1, o21, o22, o23, o24, o11, o12, o13, o14, green);
                }

                 pieces[3] =drawStartPieces(xO, yO, green);
            } else if (i==60){
                 xO = x/2-(startFieldRadius/2);
                 yO = y/2-(startFieldRadius);

                 int o11 = width1/2;
                 int o12 = o11 + 15;
                 int o13 = o12 + 15;
                 int o14 = o13 + 15;

                 int o21 = 0;
                 int o22 = o21 - 20;
                 int o23 = o22 - 20;
                 int o24 = o23 - 20;

                 drawEndFields(xI, yI, width1, o21, o22, o23, o24, o11, o12, o13, o14,blue);
                 drawStartPieces(xO, yO, green);

            } else if (i==75){
                 xO = x/2-(startFieldRadius/12);
                 yO = y/2-(3*startFieldRadius/4);

                 int o11 = 11*width1/12;
                 int o12 = o11 + 15;
                 int o13 = o12 + 15;
                 int o14 = o13 + 15;

                 int o21 = width1/4;
                 int o22 = o21 - 20;
                 int o23 = o22 - 20;
                 int o24 = o23 - 20;

                 drawEndFields(xI, yI, width1, o21, o22, o23, o24, o11, o12, o13, o14,blue);
                 drawStartPieces(xO, yO, blue);
            }
        }
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
        endField1.setOnMouseEntered(e -> endField1.fillProperty().setValue(((Color)endField1.getFill()).darker()));
        endField1.setOnMousePressed(e -> endField1.fillProperty().setValue(((Color)endField1.getFill()).darker()));
        endField1.setOnMouseReleased(e -> endField1.fillProperty().setValue(((Color)endField1.getFill()).brighter()));
        endField1.setOnMouseExited(e -> endField1.fillProperty().setValue(((Color)endField1.getFill()).brighter()));
        endField1.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> selectedField = field1);
        pane.getChildren().add(endField1);
        return field1;
    }

    private Field[] drawEndFields(int xI, int yI, int width1, int o11, int o12, int o13, int o14, int o21, int o22, int o23, int o24, Color color) {
        int width2 = width1 - 5;
        int width3 = width2 - 5;
        int width4 = width3 - 5;

        Field endField1 = drawEndField(xI-o21+width1/2., yI-o11+width1/2., width1/2, color);
        Field endField2 =drawEndField(xI-o22+width2/2., yI-o12+width2/2., width2/2, color);
        Field endField3 =drawEndField(xI-o23+width3/2., yI-o13+width3/2., width3/2, color);
        Field endField4 = drawEndField(xI-o24+width4/2., yI-o14+width4/2., width4/2, color);

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
        rectangle1.setOnMouseEntered(e -> rectangle1.fillProperty().setValue(((Color)rectangle1.getFill()).darker()));
        rectangle1.setOnMousePressed(e -> rectangle1.fillProperty().setValue(((Color)rectangle1.getFill()).darker()));
        rectangle1.setOnMouseReleased(e -> rectangle1.fillProperty().setValue(((Color)rectangle1.getFill()).brighter()));
        rectangle1.setOnMouseExited(e -> rectangle1.fillProperty().setValue(((Color)rectangle1.getFill()).brighter()));
        pane.getChildren().add(rectangle1);
    }

    private Piece[] drawStartPieces(int x, int y, Color color) {
        Circle startField = new Circle(x+startFieldRadius/2, y+startFieldRadius/2, startFieldRadius/2);
        startField.setFill(color);
        startField.setStroke(Color.BLACK);
        pane.getChildren().add(startField);
        
        Piece piece1 = new Piece();
        Circle piece1C = new Circle(x+(startFieldRadius-pieceRadius)/2.+pieceRadius/2., y+startFieldRadius-(pieceRadius*1.5)+pieceRadius/2., pieceRadius/2);
        piece1.setCircle(piece1C);
        piece1.setName("1"+color);
        piece1C.setFill(color);
        piece1C.setStroke(Paint.valueOf("black"));
        piece1C.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> selectedPiece = piece1);
        piece1C.setOnMouseEntered(e -> piece1C.fillProperty().setValue(((Color)piece1C.getFill()).darker()));
        piece1C.setOnMouseExited(e -> piece1C.fillProperty().setValue(((Color)piece1C.getFill()).brighter()));
        piece1C.setOnMousePressed(e -> piece1C.fillProperty().setValue(((Color)piece1C.getFill()).darker()));
        piece1C.setOnMouseReleased(e -> piece1C.fillProperty().setValue(((Color)piece1C.getFill()).brighter()));
        pane.getChildren().add(piece1C);

        Piece piece2 = new Piece();
        Circle piece2C = new Circle(x+(startFieldRadius-pieceRadius)/2.+pieceRadius/2., y+pieceRadius/2.+pieceRadius/2., pieceRadius/2);
        piece2.setCircle(piece2C);
        piece2.setName("2"+color);
        piece2C.setFill(color);
        piece2C.setStroke(Paint.valueOf("black"));
        piece2C.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> selectedPiece = piece2);
        piece2C.setOnMouseEntered(e -> piece2C.fillProperty().setValue(((Color)piece2C.getFill()).darker()));
        piece2C.setOnMouseExited(e -> piece2C.fillProperty().setValue(((Color)piece2C.getFill()).brighter()));
        piece2C.setOnMousePressed(e -> piece2C.fillProperty().setValue(((Color)piece2C.getFill()).darker()));
        piece2C.setOnMouseReleased(e -> piece2C.fillProperty().setValue(((Color)piece2C.getFill()).brighter()));
        pane.getChildren().add(piece2C);

        Piece piece3 = new Piece();
        Circle piece3C = new Circle(x+startFieldRadius-(pieceRadius*1.5)+pieceRadius/2., y+(startFieldRadius-pieceRadius)/2.+pieceRadius/2., pieceRadius/2);
        piece3.setCircle(piece3C);
        piece3.setName("3"+color);
        piece3C.setFill(color);
        piece3C.setStroke(Paint.valueOf("black"));
        piece3C.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> selectedPiece = piece3);
        piece3C.setOnMouseEntered(e -> piece3C.fillProperty().setValue(((Color)piece3C.getFill()).darker()));
        piece3C.setOnMouseExited(e -> piece3C.fillProperty().setValue(((Color)piece3C.getFill()).brighter()));
        piece3C.setOnMousePressed(e -> piece3C.fillProperty().setValue(((Color)piece3C.getFill()).darker()));
        piece3C.setOnMouseReleased(e -> piece3C.fillProperty().setValue(((Color)piece3C.getFill()).brighter()));
        pane.getChildren().add(piece3C);

        Piece piece4 = new Piece();
        Circle piece4C = new Circle(x+pieceRadius/2.+pieceRadius/2., y+(startFieldRadius-pieceRadius)/2.+pieceRadius/2., pieceRadius/2);
        piece4.setCircle(piece4C);
        piece4.setName("4"+color);
        piece4C.setFill(color);
        piece4C.setStroke(Paint.valueOf("black"));
        piece4C.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> selectedPiece = piece4);
        piece4C.setOnMouseEntered(e -> piece4C.fillProperty().setValue(((Color)piece4C.getFill()).darker()));
        piece4C.setOnMouseExited(e -> piece4C.fillProperty().setValue(((Color)piece4C.getFill()).brighter()));
        piece4C.setOnMousePressed(e -> piece4C.fillProperty().setValue(((Color)piece4C.getFill()).darker()));
        piece4C.setOnMouseReleased(e -> piece4C.fillProperty().setValue(((Color)piece4C.getFill()).brighter()));
        pane.getChildren().add(piece4C);

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

    public void setEnd(int x1E, int y1E, int x2E, int y2E){
        this.x1E = x1E;
        this.y1E = y1E;
        this.x2E = x2E;
        this.y2E = y2E;

    }

    public void setStart(int x1S, int y1S, int x2S, int y2S, int index, int version){
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

        path.setOnMouseEntered(e -> path.fillProperty().setValue(((Color)path.getFill()).darker()));
        path.setOnMousePressed(e -> path.fillProperty().setValue(((Color)path.getFill()).darker()));
        path.setOnMouseReleased(e -> path.fillProperty().setValue(((Color)path.getFill()).brighter()));
        path.setOnMouseExited(e -> path.fillProperty().setValue(((Color)path.getFill()).brighter()));

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

