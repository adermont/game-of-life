package io.github.adermont.livinggame;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;

public class LifeGameModel {

    // Matrice de valeurs observables
    private BooleanProperty[][] mModel;

    // Valeur initiale des cellules après constructeur et après clear()
    private boolean mInitialCellValues;

    /**
     * Constructeur par défaut.
     *
     * @param nbCells Taille de la matrice en largeur et hauteur.
     */
    public LifeGameModel(int nbCells)
    {
        this(nbCells, nbCells, false);
    }

    /**
     * Constructeur par défaut.
     *
     * @param nbCells           Taille de la matrice en largeur et hauteur.
     * @param initialCellValues Valeur initiale des cases (elles auront aussi cette valeur après {@link #clear()}).
     */
    public LifeGameModel(int nbCells, boolean initialCellValues)
    {
        this(nbCells, nbCells, initialCellValues);
    }

    /**
     * Constructeur par défaut.
     *
     * @param colCount          Taille de la matrice en largeur.
     * @param rowCount          Taille de la matrice en hauteur.
     * @param initialCellValues Valeur initiale des cases (elles auront aussi cette valeur après
     *                          {@link #clear()}).
     */
    public LifeGameModel(int colCount, int rowCount, boolean initialCellValues)
    {
        mModel = new BooleanProperty[rowCount][colCount];
        clear();
    }

    /**
     * Crée un nouveau modèle en recopiant les valeurs d'un autre.
     *
     * @param other L'autre modèle à copier.
     */
    public LifeGameModel(LifeGameModel other)
    {
        mModel = new BooleanProperty[other.getRowCount()][other.getColumnCount()];
        for (int i = 0; i < mModel.length; i++) {
            for (int j = 0; j < mModel[i].length; j++) {
                mModel[i][j] = new SimpleBooleanProperty(other.get(i, j));
            }
        }
    }

    /**
     * Réinitialise toutes les valeurs du modèle.
     */
    public void clear()
    {
        for (int i = 0; i < mModel.length; i++) {
            for (int j = 0; j < mModel[i].length; j++) {
                if (mModel[i][j] == null) {
                    mModel[i][j] = new SimpleBooleanProperty();
                }
                mModel[i][j].set(mInitialCellValues);
            }
        }
    }

    public void addListener(int row, int col, ChangeListener<Boolean> listener)
    {
        mModel[row][col].addListener(listener);
    }

    public void set(int row, int col, boolean value)
    {
        mModel[row][col].set(value);
    }

    public boolean get(int row, int col)
    {
        return mModel[row][col].get();
    }

    public int getColumnCount()
    {
        return mModel[0].length;
    }

    public int getRowCount()
    {
        return mModel.length;
    }

    /**
     * Retourne le nombre de cellules vivantes autour d'une cellule.
     *
     * @param row Index de la ligne.
     * @param col Index de la colonne.
     */
    public int getNbLivingCellsAround(int row, int col)
    {
        int count = 0;
        if (row - 1 > 0) {
            if (col - 1 > 0) {
                count += mModel[row - 1][col - 1].get() ? 1 : 0;
            }
            count += mModel[row - 1][col].get() ? 1 : 0;
            if (col + 1 < mModel[row - 1].length) {
                count += mModel[row - 1][col + 1].get() ? 1 : 0;
            }
        }
        if (col + 1 < mModel[row].length) {
            count += mModel[row][col + 1].get() ? 1 : 0;
        }
        if (col - 1 > 0) {
            count += mModel[row][col - 1].get() ? 1 : 0;
        }
        if (row + 1 < mModel.length) {
            count += mModel[row + 1][col].get() ? 1 : 0;
            if (col + 1 < mModel[row].length) {
                count += mModel[row + 1][col + 1].get() ? 1 : 0;
            }
            if (col - 1 > 0) {
                count += mModel[row + 1][col - 1].get() ? 1 : 0;
            }
        }
        return count;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof LifeGameModel) {
            LifeGameModel other = (LifeGameModel) obj;
            for (int i = 0; i < mModel.length; i++) {
                for (int j = 0; j < mModel[i].length; j++) {
                    if (mModel[i][j].get() != other.get(i, j)) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
        //if (obj instanceof LifeGameModel)
        //return Arrays.deepEquals(this.mModel, ((LifeGameModel) obj).mModel);
        //return false;
    }
}
