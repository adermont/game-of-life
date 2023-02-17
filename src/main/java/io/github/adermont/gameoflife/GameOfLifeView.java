package io.github.adermont.gameoflife;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Composant JavaFX qui hérite de BorderPane et contient une grille du jeu de la vie.
 * <p>
 * Exemple d'utilisation :
 * <pre>
 * public class GameOfLifeApp extends Application {
 *     public static void main(String[] args)
 *     {
 *         launch(args);
 *     }
 *     public void start(Stage primaryStage)
 *     {
 *         GameOfLifeView root = new GameOfLifeView();
 *         primaryStage.setTitle("Game of life");
 *         primaryStage.setScene(new Scene(root));
 *         primaryStage.show();
 *     }
 * }
 * </pre>
 * </p>
 * Evolutions futures :
 * <ul>
 *     <li>Ajouter un slider pour moduler la vitesse de l'animation</li>
 *     <li>Sauvegarder l'état initial avant de lancer la simulation</li>
 *     <li>Charger un état initial à partir d'une sauvegarde d'une simulation précédente</li>
 *     <li>Charger une liste d'états initiaux intéressants dans une ComboBox</li>
 * </ul>
 * Bugs connus :
 * <ul>
 *     <li>Parfois un ou plusieurs pixels restent en noir même après un clear...</li>
 * </ul>
 */
public class GameOfLifeView extends BorderPane
{
    /** Taille par défaut d'une cellule (en pixels)). */
    public static final int DEFAULT_CELL_SIZE = 16;

    /** Nombre de cases par défaut sur la grille. */
    public static final int NB_CASES_GRILLE = 40;

    /** Durée par défaut d'un tour du jeu (en millisecondes). */
    public static final int FRAME_DURATION = 100;

    // ---------------------------------------------------------------

    private static final String BUTTON_START_LABEL_KEY = "button.start.label";
    private static final String BUTTON_STOP_LABEL_KEY  = "button.stop.label";
    private static final String BUTTON_CLEAR_LABEL_KEY = "button.clear.label";

    // ---------------------------------------------------------------

    // Le logger pour tracer les informations de débogage.
    private static final System.Logger logger = System.getLogger(GameOfLifeView.class.getName());

    // Nombre de générations de la simulation courante.
    private final SimpleIntegerProperty mNbGenerations;
    /**
     * Les renderers servent à afficher les cellules du modèle de données. On ne définit pas quel
     * est leur type, on garde le type générique "Node" comme ça on choisira plus tard de modifier
     * les composants graphiques qui servent à afficher une cellule.
     */
    private final Node[][]              mRenderers;

    /** Le bouton start permet de lancer la simulation. */
    private final Button mButtonStart;

    /** Le bouton stop arrête la simulation. */
    private final Button mButtonStop;

    /** Le bouton clear permet d'effacer toutes les cellules du plateau de jeu. */
    private final Button mButtonClear;

    // Composants graphiques de la vue -----------------------------------
    /** La grille contenant les "renderers" qui affichent les cellules. */
    private final GridPane mGridPane;

    /**
     * Le texte en rouge en haut à gauche affiche le nombre de tours du jeu, c'est à dire le nombre
     * de générations de cellules après la première génération initiale.
     */
    private final Label mLabelGeneration;

    // Numéro de la génération où apparait une cellule.
    private int mNumGenerationEtatStable;

    // Tâche de calcul d'une nouvelle génération
    private Task<Void> mComputeNewGenerationTask;

    // Modèle d'une grille de cellules vivantes.
    private GameOfLifeModel mModel;

    // Le modèle de la génération précédente.
    private GameOfLifeModel mPreviousModel;

    // --------------------------------------------------------------------
    // Préférences d'affichage'
    private int  mGridCellSizeInPixels;
    private long mFrameDuration;
    // --------------------------------------------------------------------

    // Le "bundle" qui contient les labels des boutons et autres trucs qui doivent être traduits.
    private ResourceBundle labelsBundle;

    /**
     * Constructeur par défaut. La taille des cellules sur la grille est par défaut de
     * {@link #DEFAULT_CELL_SIZE} pixels.
     */
    public GameOfLifeView()
    {
        this(DEFAULT_CELL_SIZE);
        reloadBundle();
    }

    /**
     * Crée un nouveau jeu de la vie.
     *
     * @param gridCellSize Taille d'une cellule de la grille (en pixels).
     */
    public GameOfLifeView(int gridCellSize)
    {
        reloadBundle();
        mModel = new GameOfLifeModel(NB_CASES_GRILLE);
        mPreviousModel = null;
        mNbGenerations = new SimpleIntegerProperty();
        mRenderers = new Node[NB_CASES_GRILLE][NB_CASES_GRILLE];
        mGridCellSizeInPixels = gridCellSize;
        mFrameDuration = FRAME_DURATION;

        mButtonStart = new Button(labelsBundle.getString(BUTTON_START_LABEL_KEY));
        mButtonStop = new Button(labelsBundle.getString(BUTTON_STOP_LABEL_KEY));
        mButtonClear = new Button(labelsBundle.getString(BUTTON_CLEAR_LABEL_KEY));
        mGridPane = new GridPane();
        mLabelGeneration = new Label();

        createRenderers();

        mLabelGeneration.setText(String.valueOf(mNbGenerations));
        mLabelGeneration.textProperty().bind(mNbGenerations.asString());

        mGridPane.setStyle("-fx-background-color: rgba(0,0,0,0)");
        mGridPane.setPadding(new Insets(10));
        mGridPane.setGridLinesVisible(true);
        mGridPane.setOnMousePressed(e -> handleGridClick(e));
        mGridPane.setOnMouseDragged(e -> {
            if (e.isPrimaryButtonDown())
            {
                Platform.runLater(() -> handleGridClick(e));
            }
        });

        mLabelGeneration.setAlignment(Pos.TOP_LEFT);
        mLabelGeneration.setPadding(new Insets(20));
        mLabelGeneration.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 50));
        mLabelGeneration.setTextFill(Color.INDIANRED.deriveColor(1, 1, 1, 0.7));

        StackPane stackPane = new StackPane(mLabelGeneration, mGridPane);
        StackPane.setAlignment(mLabelGeneration, Pos.TOP_LEFT);
        setCenter(stackPane);

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

        setBottom(buttonBarBottom);
        setPadding(new Insets(10));
    }

    /**
     * Charge (ou recharge) le fichier de ressources qui contient les labels.
     */
    private void reloadBundle()
    {
        ResourceBundle.clearCache();
        labelsBundle = ResourceBundle.getBundle(getClass().getName(), Locale.getDefault());
    }

    /**
     * Création des HBox qui servent à faire le rendu des cases du modèle.
     */
    private void createRenderers()
    {
        for (int iRow = 0; iRow < mModel.getRowCount(); iRow++)
        {
            for (int iCol = 0; iCol < mModel.getColumnCount(); iCol++)
            {
                int size = getGridCellSizeInPixels();
                HBox box = new HBox();
                box.setMinSize(size, size);
                box.setMaxSize(size, size);
                mGridPane.add(box, iCol, iRow);
                mRenderers[iRow][iCol] = box;
                bindCellRendererToModel(iRow, iCol, box);
            }
        }
    }

    /**
     * Attache le renderer de la case (iRow,iCol) comme listener de la case (iRow,iCol) du modèle.
     *
     * @param iRow Numéro de la ligne.
     * @param iCol Numéro de la colonne.
     * @param node Renderer à attacher à la case (iRow,iCol).
     */
    private void bindCellRendererToModel(int iRow, int iCol, Node node)
    {
        mModel.addListener(iRow, iCol, (source, oldValue, newValue) -> {
            if (newValue)
            {
                node.setStyle("-fx-background-color: rgba(0,0,50,0.5)");
            }
            else
            {
                node.setStyle("-fx-background-color: rgba(255,255,255,0.1)");
            }
        });
    }

    /**
     * Réinitialise le modèle ainsi que le numéro de la génération courante.
     */
    public void resetModel()
    {
        mModel.clear();
        mPreviousModel = null;
        mNbGenerations.set(0);
        mNumGenerationEtatStable = 0;
    }

    /**
     * Réinitialise la tâche de calcul d'une nouvelle génération.
     */
    public void rearmComputeGenerationTask()
    {
        mComputeNewGenerationTask = new Task<Void>()
        {
            @Override
            protected Void call() throws Exception
            {
                while (!isCancelled())
                {
                    // Calcule une nouvelle génération de cellules
                    computeNewGeneration();

                    // Petite attente entre chaque simulation
                    Thread.sleep(getFrameDuration());
                }
                return null;
            }
        };
        EventHandler handler = e -> Platform.runLater(() -> {
            mButtonStart.setDisable(false);
        });
        mComputeNewGenerationTask.setOnCancelled(handler);
        mComputeNewGenerationTask.setOnSucceeded(handler);
    }

    /**
     * Gestionnaire d'évènement pour le clic souris sur la grille. On crée une nouvelle cellule
     * vivante sur la case cliquée.
     *
     * @param e
     */
    private void handleGridClick(MouseEvent e)
    {
        mNumGenerationEtatStable = 0;
        mNbGenerations.set(0);

        double w = mGridPane.getWidth();
        double h = mGridPane.getHeight();
        double nbCases = mModel.getColumnCount();
        double wCell = w / nbCases;
        double hCell = h / nbCases;
        int col = (int) (e.getX() / wCell);
        int row = (int) (e.getY() / hCell);

        mModel.set(row, col, true);
        logger.log(System.Logger.Level.DEBUG, "Manual birth at [{0}][{1}]%n", row, col);
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

    /**
     * Calcule une nouvelle génération de cellules.
     */
    public void computeNewGeneration()
    {
        Platform.runLater(() -> mNbGenerations.set(mNbGenerations.get() + 1));

        logger.log(System.Logger.Level.DEBUG, "Computing generation {0}...", mNbGenerations);
        GameOfLifeModel ancienModel = mModel;
        mModel = new GameOfLifeModel(mModel);

        for (int iRow = 0; iRow < mModel.getRowCount(); iRow++)
        {
            for (int iCol = 0; iCol < mModel.getColumnCount(); iCol++)
            {

                // Binding model->renderer
                bindCellRendererToModel(iRow, iCol, mRenderers[iRow][iCol]);

                // calcul du nombre de cellules voisines vivantes
                int nbCellulesVivantes = ancienModel.getNbLivingCellsAround(iRow, iCol);

                // Application des règles de survie de la cellule courante :
                //
                //  - SI (cellule vivante entourée de moins de 2 cellules ou plus de 3 cellules)
                //        ALORS:  mort
                //  - SI (pas de cellule vivante mais 3 cellules vivantes autour)
                //        ALORS: naissance
                if (ancienModel.get(iRow, iCol) && (nbCellulesVivantes > 3 || nbCellulesVivantes < 2))
                {
                    destroyCell(iRow, iCol);
                }
                if (!ancienModel.get(iRow, iCol) && nbCellulesVivantes == 3)
                {
                    createNewCell(iRow, iCol);
                }
            }
        }

        // Ici on fait une petite vérification par rapport à l'état précédent pour savoir
        // s'il y a eu des modifications. Sinon on s'arrête car l'état est stable.
        if (mNumGenerationEtatStable == 0 && mModel.equals(ancienModel))
        {
            mNumGenerationEtatStable = mNbGenerations.get();
            mComputeNewGenerationTask.cancel();
            logger.log(System.Logger.Level.INFO, "STABILISATION at generation {0}", mNumGenerationEtatStable);
        }

        // Ici on vérifie s'il y a des oscillateurs de période 2 (ie. quand l'état revient
        // systématiquement à l'état N-2).
        if (mNumGenerationEtatStable == 0 && mModel.equals(mPreviousModel))
        {
            mNumGenerationEtatStable = mNbGenerations.get();
            logger.log(System.Logger.Level.INFO, "OSCILLATION detected at generation {0}", mNumGenerationEtatStable);
        }
        mPreviousModel = ancienModel;
    }

    /**
     * Naissance d'une cellule à la case (pRow,pCol).
     *
     * @param iRow Numéro de la ligne.
     * @param iCol Numéro de la colonne.
     */
    private void createNewCell(int iRow, int iCol)
    {
        logger.log(System.Logger.Level.DEBUG, "\tBirth at [{0}][{1}]", iRow, iCol);
        mModel.set(iRow, iCol, true);
    }

    /**
     * Naissance d'une cellule à la case (pRow,pCol).
     *
     * @param iRow Numéro de la ligne.
     * @param iCol Numéro de la colonne.
     */
    private void destroyCell(int iRow, int iCol)
    {
        logger.log(System.Logger.Level.DEBUG, "\tDeath at [{0}][{1}]", iRow, iCol);
        mModel.set(iRow, iCol, false);
    }

    /**
     * @return La largeur/hauteur d'une seule case de la grille.
     */
    public int getGridCellSizeInPixels()
    {
        return mGridCellSizeInPixels;
    }

    /**
     * Modifie la taille des cellules (en pixels).
     *
     * @param pGridCellSizeInPixels Nouvelle taille des cellules (en pixels).
     */
    public void setGridCellSizeInPixels(int pGridCellSizeInPixels)
    {
        logger.log(System.Logger.Level.INFO, "Set GridCellSizeInPixels to " + pGridCellSizeInPixels);
        mGridCellSizeInPixels = pGridCellSizeInPixels;
    }

    /**
     * Retourne la durée d'une frame de l'animation.
     *
     * @return La durée d'une frame.
     */
    public long getFrameDuration()
    {
        return mFrameDuration;
    }

    /**
     * Retourne la durée d'une frame de l'animation.
     *
     * @return La durée d'une frame.
     */
    public void setFrameDuration(long pFrameDuration)
    {
        logger.log(System.Logger.Level.INFO, "Set FrameDuration to " + pFrameDuration);
        mFrameDuration = pFrameDuration;
    }
}
