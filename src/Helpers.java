import java.io.UnsupportedEncodingException;

public class Helpers {

    // the root path where the canvas is stored
    public static final String rootNodePath = "/canvas";

    // converts x and y coordinates to a chunk path
    public static String chunkCoordsToNodePath(int x, int y) {
        return String.format("%s/chunk_%d,%d", rootNodePath, x, y);
    }

    // converts x and y coordinates to the path of a chunk's tiles node
    public static String tilesPathForChunkCoords(int x, int y) {
        return String.format("%s/tiles", chunkCoordsToNodePath(x, y));
    }

    // converts x and y coordinates of a chunk and tile to the tile's path
    public static String tileCoordsToNodePath(int chunkX, int chunkY, int tileX, int tileY) {
        return String.format("%s/tile_%d,%d", tilesPathForChunkCoords(chunkX, chunkY), tileX, tileY);
    }

    // converts a byte array into a string
    public static String bytesToASCII(byte[] data) {
        try {
            return new String(data, 0, data.length, "ASCII");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    // extracts the tile name from a path
    public static String tileNameFromPath(String path) {

        String parts[] = path.split("/");

        if (parts.length < 5) {
            return "";
        } else {
            return parts[4];
        }
    }

    // extract the tile x coord from a full path
    public static int tileXCoordFromPath(String path) {

        String tileName = tileNameFromPath(path);
        String parts[] = tileName.split("[,_]");

        if (parts.length < 3) {
            return -1;
        } else {
            try {
                return Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
    }

    // extracts the tile y coord from a full path
    public static int tileYCoordFromPath(String path) {
        
        String tileName = tileNameFromPath(path);
        String parts[] = tileName.split("[,_]");

        if (parts.length < 3) {
            return -1;
        } else {
            try {
                return Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
    }
    
    // extracts the tile x coord from a tile name
    public static int tileXCoordForTileName(String tileName) {

        String parts[] = tileName.split("[,_]");

        try{
            return Integer.parseInt(parts[1]);
        }
        catch (NumberFormatException e){
            return -1;
        }
    }

    // extracts the tile y coord from a tile name
    public static int tileYCoordForTileName(String tileName) {

        String parts[] = tileName.split("[,_]");

        try{
            return Integer.parseInt(parts[2]);
        }
        catch (NumberFormatException e){
            return -1;
        }
    }

    // extracts the chunk name from a path
    public static String chunkNameFromPath(String path) {
        
        String parts[] = path.split("/");

        if (parts.length < 3) {
            return "";
        } else {
            return parts[2];
        }
    }

    // extracts the chunk x coord from a path
    public static int chunkXCoordFromPath(String path) {
        
        String chunkName = chunkNameFromPath(path);
        String parts[] = chunkName.split("[,_]");

        if (parts.length < 3) {
            return 0;
        } else {
            try {
                return Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }

    // extracts the chunk y coord from a path
    public static int chunkYCoordFromPath(String path) {
        
        String chunkName = chunkNameFromPath(path);
        String parts[] = chunkName.split("[,_]");

        if (parts.length < 3) {
            return 0;
        } else {
            try {
                return Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }
}
