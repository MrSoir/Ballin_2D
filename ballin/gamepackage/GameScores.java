package gamepackage;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Class to pass on current game scores. Designed not to handle the scores of
 * the game continuously but just to pass on scores - thus preventing a
 * game-extern manipulation of the game. Also enables the drawing of the
 * specified game scores.
 * 
 * @author mvbutz
 */
public class GameScores {
    private String[] scoreNames;
    private int[] scores;

    private static Font myBaseFont = new Font("Times", Font.BOLD, 18);

    private GameScores() {
        /* empty constructor � use static constructors! */}

    public static GameScores createGameScores(String score){
    	return createGameScores(new String[] { score }, new int[]{});
    }
    public static GameScores createGameScores(String scoreName, int score) {
        return createGameScores(new String[] { scoreName }, new int[] { score });
    }

    public static GameScores createGameScores(String[] scoreNames, int[] scores) {
        GameScores mgs = new GameScores();
        mgs.scoreNames = scoreNames;
        mgs.scores = scores;
        return mgs;
    }
    public static GameScores createGameScores(String[] scoreNames) {
    	return createGameScores(scoreNames, null);
    }

    public String getScoreName(int index) {
        if (index >= 0 && index < scoreNames.length)
            return scoreNames[index];
        return null;
    }

    public int getScore(int index) {
        if (index >= 0 && index < scores.length)
            return scores[index];
        return -1;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < scores.length; i++) {
            sb.append(scoreNames[i]);
            sb.append(": ");
            sb.append(scores[i]);
            sb.append(" \n");
        }
        return sb.toString();
    }

    public void paintScore(Graphics2D g, Rectangle area) {
    	g.setColor(new Color(30,30,30));
    	g.fill(area);
		g.setColor(new Color(180,180,180));
        Font temp = g.getFont();
        g.setFont(myBaseFont);
        int firstHeight = area.y + area.height / 2 + myBaseFont.getSize() / 2;
        int heightSteps = (int) (1.2 * myBaseFont.getSize());
        if (scoreNames.length > 1) { // more than one line of scores to paint
            firstHeight -= (int) (((scoreNames.length - 1) / 2.) * heightSteps);
        }
        for (int i = 0; i < scoreNames.length; i++) {
        	String str;
        	if (scores != null && scores.length > i){
        		str = scoreNames[i] + ": " + scores[i];
        	}else{
        		str = scoreNames[i];
        	}
            g.drawString(str, area.x + area.width / 20, firstHeight + i * heightSteps);
        }
        g.setFont(temp);
    }
}
