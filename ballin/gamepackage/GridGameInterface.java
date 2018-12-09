package gamepackage;

import java.awt.Graphics2D;

public interface GridGameInterface {
    /**
     * Should return the current scores of the game in the form of a
     * MyGameScores Object.
     * 
     * @return the current game scores.
     */
    public GameScores getScores();

    /**
     * Method will be invoked at the end of this game.
     */
    public void closeGame();

    /**
     * Called when the game area should be repainted.
     * 
     * @param g
     */
    public void paintGameArea(Graphics2D g);

    /**
     * Called when the score area should be repainted.
     * 
     * @param g
     */
    public void paintControlArea(Graphics2D g);

    /**
     * Should return the name of the game.
     */
    public String getName();
    
    public default void startGame(){}
}
