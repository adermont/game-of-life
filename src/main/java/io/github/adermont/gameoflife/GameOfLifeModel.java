package io.github.adermont.gameoflife;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;

/**
 * Modèle de données du jeu de la vie. Il s'agit d'une matrice carrée dans laquelle sont stockées
 * des objets BooleanProperty. C'est ainsi plus simple de réaliser un binding avec des propriétés
 * d'objets graphiques JavaFX.
 */
public class GameOfLifeModel
{

    // Matrice de valeurs observables
    private final BooleanProperty[][] mModel;

    // Valeur initiale des cellules après constructeur et après clear()
    private boolean mInitialCellValues;

    /**
     * Constructeur par défaut.
     *
     * @param nbCells Taille de la matrice en largeur et hauteur.
     */
    public GameOfLifeModel(int nbCells)
    {
        this(nbCells, nbCells, false);
    }

    /**
     * Constructeur par défaut.
     *
     * @param nbCells           Taille de la matrice en largeur et hauteur.
     * @param initialCellValues Valeur initiale des cases (elles auront aussi cette valeur après
     *                          {@link #clear()}).
     */
    public GameOfLifeModel(int nbCells, boolean initialCellValues)
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
    public GameOfLifeModel(int colCount, int rowCount, boolean initialCellValues)
    {
        mModel = new BooleanProperty[rowCount][colCount];
        clear();
    }

    /**
     * Crée un nouveau modèle en recopiant les valeurs d'un autre.
     *
     * @param other L'autre modèle à copier.
     */
    public GameOfLifeModel(GameOfLifeModel other)
    {
        mModel = new BooleanProperty[other.getRowCount()][other.getColumnCount()];
        for (int i = 0; i < mModel.length; i++)
        {
            for (int j = 0; j < mModel[i].length; j++)
            {
                mModel[i][j] = new SimpleBooleanProperty(other.get(i, j));
            }
        }
    }

    /**
     * Réinitialise toutes les valeurs du modèle.
     */
    public void clear()
    {
        for (int row = 0; row < mModel.length; row++)
        {
            for (int col = 0; col < mModel[row].length; col++)
            {
                if (mModel[row][col] == null)
                {
                    mModel[row][col] = new SimpleBooleanProperty();
                }
                mModel[row][col].set(mInitialCellValues);
            }
        }
    }

    /**
     * Ajoute un listener pour écouter les évènements de naissance ou mot d'une cellule située dans
     * la case (row,col) de la grille.
     *
     * @param row      Le numéro de ligne de la cellule dont on souhaite suivre l'état.
     * @param col      Le numéro de colonne de la celulle dont on souhaite suivre l'état.
     * @param listener L'objet qui écoutera les évènements de la cellule.
     */
    public void addListener(int row, int col, ChangeListener<Boolean> listener)
    {
        mModel[row][col].addListener(listener);
    }

    /**
     * Modifie la valeur d'une case de la grille.
     *
     * @param row   Numéro de ligne de la cellule.
     * @param col   Numéro de colonne de la cellule.
     * @param value La nouvelle valeur de la case située aux coordonnées (row,col).
     */
    public void set(int row, int col, boolean value)
    {
        mModel[row][col].set(value);
    }

    /**
     * @param row Numéro de ligne de la cellule.
     * @param col Numéro de colonne de la cellule.
     * @return La valeur de la case située aux coordonnées (row,col).
     */
    public boolean get(int row, int col)
    {
        return mModel[row][col].get();
    }

    /**
     * @return Le nombre de colonnes de la grille.
     */
    public int getColumnCount()
    {
        return mModel[0].length;
    }

    /**
     * @return Le nombre de lignes de la grille.
     */
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
        if (row - 1 > 0)
        {
            if (col - 1 > 0)
            {
                count += mModel[row - 1][col - 1].get() ? 1 : 0;
            }
            count += mModel[row - 1][col].get() ? 1 : 0;
            if (col + 1 < mModel[row - 1].length)
            {
                count += mModel[row - 1][col + 1].get() ? 1 : 0;
            }
        }
        if (col + 1 < mModel[row].length)
        {
            count += mModel[row][col + 1].get() ? 1 : 0;
        }
        if (col - 1 > 0)
        {
            count += mModel[row][col - 1].get() ? 1 : 0;
        }
        if (row + 1 < mModel.length)
        {
            count += mModel[row + 1][col].get() ? 1 : 0;
            if (col + 1 < mModel[row].length)
            {
                count += mModel[row + 1][col + 1].get() ? 1 : 0;
            }
            if (col - 1 > 0)
            {
                count += mModel[row + 1][col - 1].get() ? 1 : 0;
            }
        }
        return count;
    }

    /**
     * Compare deux états du jeu.
     *
     * @param obj L'objet auquel on doit omparer l'objet courant.
     * @return true si les deux modèle GameOfLifeModel sont dans le même état (donc ils ont les
     *         mêmes cellules vivantes).
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof GameOfLifeModel other)
        {
            for (int i = 0; i < mModel.length; i++)
            {
                for (int j = 0; j < mModel[i].length; j++)
                {
                    if (mModel[i][j].get() != other.get(i, j))
                    {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }
}
