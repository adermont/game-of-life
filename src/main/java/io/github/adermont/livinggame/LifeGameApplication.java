package io.github.adermont.livinggame;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LifeGameApplication extends Application {

    public static void main(String[] args)
    {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage)
    {
        LifeGame root = new LifeGame();
        primaryStage.setTitle("Jeu de la vie");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }
}
