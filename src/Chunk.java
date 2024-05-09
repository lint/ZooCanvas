import java.util.List;

public class Chunk {

    public static final int size = 50; // height and width of the chunk
    public int xCoord;
    public int yCoord;
    public String path;
    public Tile tiles[][];
    public List<String> tileNodeNames;
    public List<String> lastReceivedTileNodeNames;
    Object tileLock = new Object();
    
    public Chunk(int x, int y) {
        this.xCoord = x;
        this.yCoord = y;
        this.path = Helpers.chunkCoordsToNodePath(x, y);
        
        // initialize the 2D tile array
        this.tiles = new Tile[size][];
        for (int i = 0; i < size; i++) {
            Tile tileRow[] = new Tile[size];

            for (int k = 0; k < size; k++) {
                tileRow[k] = new Tile();
            }

            this.tiles[i] = tileRow;
        }
    }

    // returns the tile at the given coordinates
    public Tile getTile(int x, int y) {

        Tile tile;

        synchronized (tileLock) {
            tile = tiles[y][x];
        }

        return tile;
    }

    // set the color of a given tile
    public void setTileColor(int x, int y, Colorer.Color color) {
        synchronized (tileLock) {
            Tile tile = getTile(x, y);
            tile.setColor(color);
        }
    }

    // returns true if a given tile is in the old list or not
    public boolean tileWasInPreviousList(String tileName) {
        synchronized (tileLock) {
            return lastReceivedTileNodeNames != null && lastReceivedTileNodeNames.contains(tileName);
        }
    }

    // sets the last received tile node names variable
    public void setLastReceivedTileNodeNames(List<String> newTileNames) {
        synchronized (tileLock) {
            lastReceivedTileNodeNames = newTileNames;
        }
    }

    // build a string of the canvas
    public String toString() {

        StringBuilder str = new StringBuilder();
        // str.append(String.format("grid (%d, %d):\n", xCoord, yCoord));
        
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {

                // tile toString contains color codes which color the text (two squares) when printed to the console
                String tileStr = getTile(x, y).toString();
                str.append(tileStr);
            }

            str.append("\n");
        }

        return str.toString();
    }
}
