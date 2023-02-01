package io.github.adermont.livinggame;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.Arrays;

public class LivingGame extends Application
{
    System.Logger logger = System.getLogger(LivingGame.class.getName());

    public static final int NB_CASES_GRILLE = 20;
    public static final int CELL_SIZE       = 24;
    public static final int FRAME_DURATION  = 300;
    boolean[][] mModel         = new boolean[NB_CASES_GRILLE][NB_CASES_GRILLE];
    boolean[][] mPreviousModel = null;

    Button mButtonStart = new Button("Start");
    Button   mButtonStop  = new Button("Stop");
    Button   mButtonClear = new Button("Clear");
    GridPane mGridPane    = new GridPane();
    Label    mLabelGeneration;

    private int mNbGenerations;
    private int mNumGenerationEtatStable; // Number of the generation where a stable state appeared.

    private Task<Void> mComputeNewGenerationTask;

    private int  mGridCellSizeInPixels = CELL_SIZE;
    private long mFrameDuration        = FRAME_DURATION;

    public static void main(String[] args)
    {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage)
    {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        mLabelGeneration = new Label(String.valueOf(mNbGenerations));

        mGridPane.setStyle("-fx-background-color: #455");
        mGridPane.setPadding(new Insets(10, 10, 10, 10));
        mGridPane.setGridLinesVisible(true);
        mGridPane.setOnMousePressed(e -> handleGridClick(e));
        mGridPane.setOnMouseDragged(e -> {
            if (e.isPrimaryButtonDown())
            {
                Platform.runLater(() -> handleGridClick(e));
            }
        });
        modelToView(mModel);

        StackPane.setAlignment(mLabelGeneration, Pos.TOP_LEFT);
        mLabelGeneration.setAlignment(Pos.TOP_LEFT);
        mLabelGeneration.setPadding(new Insets(20));
        mLabelGeneration.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 50));
        mLabelGeneration.setTextFill(Color.RED.deriveColor(1, 1, 1, 0.3));
        StackPane stackPane = new StackPane(mGridPane, mLabelGeneration);
        root.setCenter(stackPane);

        mButtonStart.setMaxWidth(Double.MAX_VALUE);
        mButtonStop.setMaxWidth(Double.MAX_VALUE);
        mButtonClear.setMaxWidth(Double.MAX_VALUE);

        mButtonStart.setOnAction(e -> handleStart());
        mButtonStop.setOnAction(e -> handleStop());
        mButtonClear.setOnAction(e -> resetModel());

        HBox.setHgrow(mButtonStart, Priority.ALWAYS);
        HBox.setHgrow(mButtonStop, Priority.ALWAYS);
        HBox.setHgrow(mButtonClear, Priority.ALWAYS);

        HBox buttonBarBottom = new HBox(10);
        buttonBarBottom.getChildren().addAll(mButtonStart, mButtonStop, mButtonClear);

        root.setBottom(buttonBarBottom);
        primaryStage.setTitle("Jeu de la vie");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public void resetModel()
    {
        mModel = new boolean[NB_CASES_GRILLE][NB_CASES_GRILLE];
        mPreviousModel = null;
        mNbGenerations = 0;
        mNumGenerationEtatStable = 0;
        modelToView(mModel);
    }

    public void rearmComputeGenerationTask()
    {
        mComputeNewGenerationTask = new Task<Void>()
        {
            @Override
            protected Void call() throws Exception
            {
                while (!isCancelled())
                {
                    computeNewGeneration();
                    Thread.sleep(getFrameDuration());
                    final boolean[][] m = mModel;
                    Platform.runLater(() -> {
                        modelToView(m);
                    });
                }
                return null;
            }
        };
        mComputeNewGenerationTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                mButtonStart.setDisable(false);
            });
        });
    }

    private void handleGridClick(MouseEvent e)
    {
        mNumGenerationEtatStable = 0;
        mNbGenerations = 0;

        double w = mGridPane.getWidth();
        double h = mGridPane.getHeight();
        double nbCases = mModel.length;
        double wCell = w / nbCases;
        double hCell = h / nbCases;
        int col = (int) (e.getX() / wCell);
        int row = (int) (e.getY() / hCell);

        mModel[row][col] = true;
        logger.log(System.Logger.Level.DEBUG, "Nouvelle cellule manuelle [{0}][{1}]%n", row, col);
        Platform.runLater(() -> modelToView(mModel));
    }

    public void modelToView(boolean[][] pModel)
    {
        long start = System.currentTimeMillis();

        mGridPane.getChildren().removeAll();

        for (int iRow = 0; iRow < pModel.length; iRow++)
        {
            for (int iCol = 0; iCol < pModel[iRow].length; iCol++)
            {
                int size = getGridCellSizeInPixels();
                HBox label = new HBox();
                label.setMinHeight(size);
                label.setMaxHeight(size);
                label.setMinWidth(size);
                label.setMaxWidth(size);

                if (pModel[iRow][iCol])
                {
                    label.setStyle("-fx-border-color: gray; -fx-background-color: black");
                }
                else
                {
                    label.setStyle("-fx-border-color: gray; -fx-background-color: white");
                }
                final int r = iRow, c = iCol;
                mGridPane.add(label, c, r);
            }
        }
        mLabelGeneration.setText(String.valueOf(mNbGenerations));

        long stop = System.currentTimeMillis();
        if (stop - start > getFrameDuration())
        {
            setFrameDuration(stop - start);
        }
        System.out.println("Duration : "+(stop-start)+"ms");
    }

    public void handleStart()
    {
        mButtonStart.setDisable(true);
        rearmComputeGenerationTask();
        new Thread(mComputeNewGenerationTask, "ComputeNextGeneration").start();
    }

    public void handleStop()
    {
        if (mComputeNewGenerationTask != null)
        {
            mComputeNewGenerationTask.cancel();
        }
        mButtonStart.setDisable(false);
    }

    public void computeNewGeneration()
    {
        mNbGenerations++;

        logger.log(System.Logger.Level.DEBUG, "Computing generation {0}...", mNbGenerations);
        boolean[][] ancienModel = mModel;
        mModel = new boolean[ancienModel.length][ancienModel[0].length];

        for (int iRow = 0; iRow < mModel.length; iRow++)
        {
            for (int iCol = 0; iCol < mModel[iRow].length; iCol++)
            {
                mModel[iRow][iCol] = ancienModel[iRow][iCol];
                int nbCellulesVivantes = getNbLivingCellsAround(ancienModel, iRow, iCol);
                if (ancienModel[iRow][iCol] && (nbCellulesVivantes > 3 || nbCellulesVivantes < 2))
                {
                    destroyCell(iRow, iCol);
                }
                if (!ancienModel[iRow][iCol] && nbCellulesVivantes == 3)
                {
                    createNewCell(iRow, iCol);
                }
            }
        }
        if (mNumGenerationEtatStable == 0 && Arrays.deepEquals(mModel, ancienModel))
        {
            mNumGenerationEtatStable = mNbGenerations;
            mComputeNewGenerationTask.cancel();
            logger.log(System.Logger.Level.INFO, "STABILISATION à la génération {0}", mNumGenerationEtatStable);
        }
        else if (mNumGenerationEtatStable == 0 && Arrays.deepEquals(mModel, mPreviousModel))
        {
            mNumGenerationEtatStable = mNbGenerations;
            //computeNewGenerationTask.cancel();
            logger.log(System.Logger.Level.INFO, "OSCILLATION à la génération {0}", mNumGenerationEtatStable);
        }
        mPreviousModel = ancienModel;
    }

    private void createNewCell(int pIRow, int pICol)
    {
        logger.log(System.Logger.Level.DEBUG, "\tBirth at [{0}][{1}]", pIRow, pICol);
        mModel[pIRow][pICol] = true;
    }

    private void destroyCell(int pIRow, int pICol)
    {
        logger.log(System.Logger.Level.DEBUG, "\tDeath at [{0}][{1}]", pIRow, pICol);
        mModel[pIRow][pICol] = false;
    }

    private int getNbLivingCellsAround(boolean[][] model, int row, int col)
    {
        int count = 0;
        if (row - 1 > 0)
        {
            if (col - 1 > 0)
            {
                count += model[row - 1][col - 1] ? 1 : 0;
            }
            count += model[row - 1][col] ? 1 : 0;
            if (col + 1 < model[row - 1].length)
            {
                count += model[row - 1][col + 1] ? 1 : 0;
            }
        }
        if (col + 1 < model[row].length)
        {
            count += model[row][col + 1] ? 1 : 0;
        }
        if (col - 1 > 0)
        {
            count += model[row][col - 1] ? 1 : 0;
        }
        if (row + 1 < model.length)
        {
            count += model[row + 1][col] ? 1 : 0;
            if (col + 1 < model[row].length)
            {
                count += model[row + 1][col + 1] ? 1 : 0;
            }
            if (col - 1 > 0)
            {
                count += model[row + 1][col - 1] ? 1 : 0;
            }
        }
        return count;
    }

    private int getGridCellSizeInPixels()
    {
        return mGridCellSizeInPixels;
    }

    private void setGridCellSizeInPixels(int pGridCellSizeInPixels)
    {
        logger.log(System.Logger.Level.INFO, "Set GridCellSizeInPixels to "+pGridCellSizeInPixels);
        mGridCellSizeInPixels = pGridCellSizeInPixels;
    }

    private long getFrameDuration()
    {
        return mFrameDuration;
    }

    private void setFrameDuration(long pFrameDuration)
    {
        logger.log(System.Logger.Level.INFO, "Set FrameDuration to "+pFrameDuration);
        mFrameDuration = pFrameDuration;
    }
}
