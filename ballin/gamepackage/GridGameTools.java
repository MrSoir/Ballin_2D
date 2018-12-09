package gamepackage;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class GridGameTools {

    /**
     * The colors that are utilized by the game.
     */
    public static Color[] colors = new Color[] { new Color(0, 0, 100), // dunkelblau
        new Color(0, 128, 0), // dunkelgruen
        new Color(0, 150, 230), // hellblau
        new Color(128, 0, 0), // dunkelrot
        new Color(200, 128, 0), // orange
        new Color(100, 0, 200) // lila
    };

    /**
     * Determines in which area the specified location is located.
     * 
     * @param x
     *            x location
     * @param y
     *            y location
     * @return 0,1,... for the respectively ordered areas, -1 otherwise.
     */
    public static int containedInWhichArea(int x, int y, Rectangle... areas) {
        for (int i = 0; i < areas.length; i++) {
            if (x > areas[i].x && x < areas[i].x + areas[i].width && y > areas[i].y && y < areas[i].y + areas[i].height)
                return i;
        }
        return -1;
    }

    /**
     * Paints the grid game area.
     * 
     * @param g
     * @param gameArea
     * @param myTiles
     *            The color of the respective tiles (as specified in static
     *            colors object of this class).
     * @param tileSize
     *            The size of the tiles.
     */
    public static void paintGridGameArea(Graphics2D g, Rectangle gameArea, int[][] myTiles, int tileSize) {
        int xCur = gameArea.x;
        int yCur = gameArea.y;
        // painting starts from the left top!
        for (int row = 0; row < myTiles.length; row++) {
            for (int col = 0; col < myTiles[row].length; col++) {
                drawSquare(g, xCur, yCur, colors[myTiles[row][col]], tileSize);
                // g.setColor(colors[myTiles[row][col]]);
                // g.fillRect(xCur, yCur, tileSize, tileSize);
                xCur += tileSize; // Proceed along the columns, that is, to the
                // right.
            }
            yCur += tileSize; // next row - that is, one row down.
            xCur = gameArea.x; // starting again with the left-most tile.
        }

    }

    public static void paintColorPickerArea(Graphics2D g, Rectangle colorPickerArea, int selectedColor) {
        int tileWidth = colorPickerArea.width / GridGameTools.colors.length;
        int tileHeight = colorPickerArea.height;
        int xCur = colorPickerArea.x;
        int yCur = colorPickerArea.y;
        // painting starts from the left top!
        for (int row = 0; row < GridGameTools.colors.length; row++) {
            g.setColor(GridGameTools.colors[row]);
            g.fillRect(xCur, yCur, tileWidth, tileHeight);
            if (selectedColor == row) {
                g.setColor(Color.WHITE);
                g.drawRect(xCur + 2, yCur + 2, tileWidth - 4, tileHeight - 4);
            }
            xCur += tileWidth; // proceed along the columns, that is, to the
                               // right.
        }

    }

    public static void drawSquare(Graphics2D g, int x, int y, Color color, int tileSize) {
        drawFilledRect(g, x, y, tileSize, tileSize, color);
    }

    public static void drawFilledRect(Graphics2D g, int x, int y, int width, int height, Color color) {
        g.setColor(color);
        g.fillRect(x + 1, y + 1, width - 2, height - 2);

        g.setColor(color.brighter());
        g.drawLine(x, y + height - 1, x, y);
        g.drawLine(x, y, x + width - 1, y);

        g.setColor(color.darker());
        g.drawLine(x + 1, y + height - 1, x + width - 1, y + height - 1);
        g.drawLine(x + width - 1, y + height - 1, x + width - 1, y + 1);

    }

}
