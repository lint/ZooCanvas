
import java.util.Arrays;
import java.util.HashMap;

public class Canvas {
    
    HashMap<Integer, HashMap<Integer, Chunk>> chunks;
    int numTotalChunks;
    String path;

    public Canvas() {
        this.chunks = new HashMap<Integer, HashMap<Integer, Chunk>>();
        this.numTotalChunks = 0;
        this.path = Helpers.rootNodePath;
    }

    // adds a new chunk to the canvas at the given x and y coordinates
    public Chunk addNewChunk(int x, int y) {

        Chunk newChunk = new Chunk(x, y);
        Integer xCoord = Integer.valueOf(x);
        Integer yCoord = Integer.valueOf(y);

        // get the map of chunks on the x coordinate
        HashMap<Integer, Chunk> chunksAtX = chunks.get(xCoord);
        
        // create new map of chunks if the given x coordinate has not been used yet
        if (chunksAtX == null) {
            chunksAtX = new HashMap<Integer, Chunk>();
            chunks.put(Integer.valueOf(x), chunksAtX);
        }
        
        // check if there is already a chunk at the given y coordinate
        Chunk chunkAtXY = chunksAtX.get(yCoord);
        
        if (chunkAtXY == null) {
            chunkAtXY = newChunk;
            chunksAtX.put(yCoord, newChunk);
            numTotalChunks++;    
        } else {
            System.out.printf("tried to add a new chunk but one already exists at coordinates: (%d, %d), this shouldn't happen\n", x, y);
        }

        return chunkAtXY;
    }

    // returns the chunk object at the given x and y coordinates
    public Chunk getChunkAtCoords(int x, int y) {

        Integer xCoord = Integer.valueOf(x);
        Integer yCoord = Integer.valueOf(y);

        // check if there is a map of chunks at the x coordinate
        HashMap<Integer, Chunk> chunksAtX = chunks.get(xCoord);
        if (chunksAtX == null) {
            return null;
        }

        // return the chunk at (x, y) or null if it doesn't exist
        return chunksAtX.get(yCoord);
    }

    // returns the chunk object associated with a given path
    public Chunk getChunkWithPath(String path) {
        
        String parts[] = path.split("[,_/]");

        int chunkXCoord = -1;
        int chunkYCoord = -1;

        // try to parse the coordinates of the chunk from the path
        try {
            chunkXCoord = Integer.parseInt(parts[3]);
            chunkYCoord = Integer.parseInt(parts[4]);
        } catch (NumberFormatException ex){
            return null;
        }

        // return the chunk using the coordinates
        return getChunkAtCoords(chunkXCoord, chunkYCoord);
    }
}
