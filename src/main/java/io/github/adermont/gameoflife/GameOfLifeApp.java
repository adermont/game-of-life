package io.github.adermont.gameoflife;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * JavaFX Application of the game of life.
 */
public class GameOfLifeApp extends Application
{
    // Key for the stage's title in the ResourceBundle
    private static final String STAGE_TITLE_KEY = "stage.title";

    /**
     * Launches a GameOfLife simulation.
     *
     * @param args No arguments expected for this launcher.
     */
    public static void main(String[] args)
    {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage)
    {
        // First load the bundle containing translations for labels and titles
        ResourceBundle bundle = ResourceBundle.getBundle(getClass().getName(), Locale.getDefault());

        // Instantiates a new GameOfLifeView and adds it as a root component of the Scene
        GameOfLifeView root = new GameOfLifeView();
        primaryStage.setScene(new Scene(root));

        // Update the stage's title with a localized title
        primaryStage.setTitle(bundle.getString(STAGE_TITLE_KEY));
        primaryStage.show();
    }
}
