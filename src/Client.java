
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Scanner;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;

public class Client implements ZooKeeperMonitor.ZooKeeperMonitorListener
{
    ZooKeeperMonitor zkm; // object responsible for communicating with ZooKeeper
    Canvas canvas; // stores the canvas data
    Chunk currentChunk; // stores the current chunk
    String message; // message to show while drawing the display
    boolean hasDrawnDisplayOnce; // used to make sure formatting is correct while drawing the display
    boolean hasUserInputtedCommand;
    Object displayLock = new Object();
    Object chunkLock = new Object();
    String lastDisplayStr;
    String savedCommand;
    Scanner reader;
    ArrayList<Long> updateLog;
    int errorRetryCounter; // the current number of times a request has failed in a row
    int errorRetryLimit; // the number of times to retry sending a request before giving up

    public Client(String hostPort) throws KeeperException, IOException {
        zkm = new ZooKeeperMonitor(this, hostPort);
        canvas = new Canvas();
        currentChunk = null;
        hasDrawnDisplayOnce = false;
        hasUserInputtedCommand = false;
        message = "";
        lastDisplayStr = "";
        savedCommand = "";
        reader = new Scanner(System.in).useDelimiter("");
        updateLog = new ArrayList<Long>();
        errorRetryCounter = 0;
        errorRetryLimit = 5;
    }

    // if an error occured 
    public void waitForRetryDueToError() {

        if (errorRetryCounter >= errorRetryLimit) {
            System.out.printf("error: could not successfully make a request to ZooKeeper after retrying %d times, exiting...\n", errorRetryCounter);
            System.exit(1);
        }

        // sleep for 5 seconds before retrying the request
        try {
            Thread.sleep(5000);
        } catch (Exception e) {

        }

        errorRetryCounter++;
    }

    // load a chunk from ZooKeeper (create it if necessary) and set it as the current chunk
    public Chunk getChunkFromZK(int x, int y) {
        // System.out.printf("get chunk (%d, %d) from ZK\n", x, y);

        // try to get given chunk from the Canvas, if it does not exist create a new one
        Chunk chunk;

        synchronized (chunkLock) {
            chunk = canvas.getChunkAtCoords(x, y);
            if (chunk == null) {
                chunk = canvas.addNewChunk(x, y);
            }
            currentChunk = chunk;
        }

        try {
            
            // check if the root node exists in zookeeper
            boolean rootExists = zkm.syncExists(Helpers.rootNodePath, false);

            // if it doesn't, create it
            if (!rootExists) {
                zkm.syncCreate(Helpers.rootNodePath, false, null);
            }
            
            // check if the given chunk exists in zookeeper
            String chunkPath = Helpers.chunkCoordsToNodePath(x, y);
            boolean chunkExists = zkm.syncExists(chunkPath, false);

            // if it doesn't, create it
            if (!chunkExists) {
                zkm.syncCreate(chunkPath, false, null);
            }

            // check if the given chunk has a child called "tiles"
            String chunkTilesNodePath = Helpers.tilesPathForChunkCoords(x, y);
            boolean chunkTilesNodeExists = zkm.syncExists(chunkTilesNodePath, false);

            // if it doesn't, create it
            if (!chunkTilesNodeExists) {
                zkm.syncCreate(chunkTilesNodePath, false, null);
            }

            updateAllChunkTiles(chunk, false);
            errorRetryCounter = 0;

        } catch (KeeperException e) {
            waitForRetryDueToError();
            message = String.format("KeeperException: %s", e.getMessage());
            getChunkFromZK(x, y);
        } catch (InterruptedException e) {
            message = String.format("InterruptedException: %s", e.getMessage());
        }

        return chunk;
    }

    // gets all tile data for a chunk from ZooKeeper
    public void updateAllChunkTiles(Chunk chunk, boolean synchronous) {
        // System.out.println("updateAllChunkTiles");

        try {

            // get the list of tileNames
            String chunkTilesNodePath = Helpers.tilesPathForChunkCoords(chunk.xCoord, chunk.yCoord);
            List<String> tileNames = zkm.syncGetChildren(chunkTilesNodePath, true);
            
            tileNames.sort((String tileName1, String tileName2) -> {
                
                int tile1XCoord = Helpers.tileXCoordForTileName(tileName1);
                int tile1YCoord = Helpers.tileYCoordForTileName(tileName1);

                int tile2XCoord = Helpers.tileXCoordForTileName(tileName2);
                int tile2YCoord = Helpers.tileYCoordForTileName(tileName2);

                if (tile1YCoord < tile2YCoord) {
                    return -1;
                } else if (tile1YCoord > tile2YCoord) {
                    return 1;
                } else {
                    if (tile1XCoord < tile2XCoord) {
                        return -1;
                    } else if (tile1XCoord > tile2XCoord) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            });

            // update each tile in the chunk that was not previously updated
            for (int i = 0; i < tileNames.size(); i++) {
                String tileName = tileNames.get(i);
                if (!chunk.tileWasInPreviousList(tileName)) {
                    updateChunkTile(chunk, tileName, synchronous);
                }
            }

            chunk.setLastReceivedTileNodeNames(tileNames);

            errorRetryCounter = 0;

        } catch (KeeperException e) {
            waitForRetryDueToError();
            message = String.format("KeeperException: %s", e.getMessage());
            updateAllChunkTiles(chunk, synchronous);
        } catch (InterruptedException e) {
            message = String.format("InterruptedException: %s", e.getMessage());
        }
    }

    // updates a given tile in a chunk
    public void updateChunkTile(Chunk chunk, String tileName, boolean synchronous) {
        // System.out.printf("updateChunkTile: tileName: %s\n", tileName);

        int tileXCoord = Helpers.tileXCoordForTileName(tileName);
        int tileYCoord = Helpers.tileYCoordForTileName(tileName);

        if (tileXCoord < 0 || tileXCoord > Chunk.size || tileYCoord < 0 || tileYCoord > Chunk.size) {
            message = String.format("error: tried to update tile (%d, %d) which is outside the chunk range", tileXCoord, tileYCoord);
        } else {
            updateChunkTile(chunk, tileXCoord, tileYCoord, synchronous);
        }
    }

    // updates a given tile in a chunk
    public void updateChunkTile(Chunk chunk, int tileXCoord, int tileYCoord, boolean synchronous) {
        // System.out.printf("updateCurrentChunkTile: tile coords (%d, %d)\n", tileXCoord, tileYCoord);

        if (synchronous) {
            Colorer.Color color = getTileColorFromZKSync(chunk.xCoord, chunk.yCoord, tileXCoord, tileYCoord);

            if (color != null) {
                chunk.setTileColor(tileXCoord, tileYCoord, color);
            }

            drawTileUpdate(tileXCoord, tileYCoord);

            errorRetryCounter = 0;
        } else {
            getTileColorFromZKAsync(chunk.xCoord, chunk.yCoord, tileXCoord, tileYCoord);
        }   
    }

    // given chunk and tile coordinates, get the tile's data from ZooKeeper
    public Colorer.Color getTileColorFromZKSync(int chunkXCoord, int chunkYCoord, int tileXCoord, int tileYCoord) {
        // System.out.printf("getTileUpdateFromZK: chunk: (%d, %d), tile: (%d, %d)\n", chunkXCoord, chunkYCoord, tileXCoord, tileYCoord);

        try {

            // get the path for the given tile
            String tilePath = Helpers.tileCoordsToNodePath(chunkXCoord, chunkYCoord, tileXCoord, tileYCoord);

            // get the tile data from ZooKeeper
            byte[] data = zkm.syncGetData(tilePath, true);

            // if data was received, convert it to color enum
            if (data != null) {
                return Colorer.stringToColor(Helpers.bytesToASCII(data));
            }
            
            errorRetryCounter = 0;
        } catch (KeeperException e) {
            // message = String.format("KeeperException: %s", e.getMessage());
        } catch (InterruptedException e) {
            message = String.format("InterruptedException: %s", e.getMessage());
        }
        
        return null;
    }

    // given chunk and tile coordinates, get the tile's data from ZooKeeper
    public void getTileColorFromZKAsync(int chunkXCoord, int chunkYCoord, int tileXCoord, int tileYCoord) {
        // System.out.printf("getTileUpdateFromZK: chunk: (%d, %d), tile: (%d, %d)\n", chunkXCoord, chunkYCoord, tileXCoord, tileYCoord);

        try {

            // get the path for the given tile
            String tilePath = Helpers.tileCoordsToNodePath(chunkXCoord, chunkYCoord, tileXCoord, tileYCoord);

            // get the tile data from ZooKeeper
            zkm.asyncGetData(tilePath, true);
            
        } catch (KeeperException e) {
            message = String.format("KeeperException: %s", e.getMessage());
        } catch (InterruptedException e) {
            message = String.format("InterruptedException: %s", e.getMessage());
        }
    }

    // handle when a watched GetChildren call receives an update
    public void handleWatchedGetChildren(String path) {
        // System.out.printf("handle watched get children for path: %s\n", path);

        Chunk chunk;
        boolean isCurrentChunk;
        
        synchronized (chunkLock) {
            chunk = canvas.getChunkWithPath(path);
            isCurrentChunk = chunk == currentChunk;
        }

        // check if the update to the list of children was for the current chunk being viewed
        if (isCurrentChunk) {
            updateAllChunkTiles(chunk, false);
            drawDisplay();
        }
    }

    // handle when a watched GetData call receives an update (meaning the tile color was updated)
    public void handleWatchedGetData(String path) {

        Chunk chunk;
        boolean isCurrentChunk;
        
        synchronized (chunkLock) {
            chunk = canvas.getChunkWithPath(path);
            isCurrentChunk = chunk == currentChunk;
        }

        // only update the chunk if it is the current chunk
        if (isCurrentChunk) {
            String tileName = Helpers.tileNameFromPath(path);
            updateChunkTile(chunk, tileName, false);
        }
    }

    // handle when an asynchronous call to GetData returns its data
    public void handleGetDataCallback(String path, byte[] data) {

        updateLog.add(System.currentTimeMillis());

        Chunk chunk;
        boolean isCurrentChunk;
        
        synchronized (chunkLock) {
            chunk = canvas.getChunkWithPath(path);
            isCurrentChunk = chunk == currentChunk;
        }

        int tileXCoord = Helpers.tileXCoordFromPath(path);
        int tileYCoord = Helpers.tileYCoordFromPath(path);

        Colorer.Color color = Colorer.Color.WHITE;

        if (data != null) {
            color = Colorer.stringToColor(Helpers.bytesToASCII(data));
        }

        chunk.setTileColor(tileXCoord, tileYCoord, color);

        if (isCurrentChunk) {
            drawTileUpdate(tileXCoord, tileYCoord);
        }
    }

    // handle if the ZooKeeper session is no longer valid
    public void handleSessionClose(Code reasonCode) {
        System.out.printf("error received from ZooKeeper: %s\n", reasonCode);
        System.exit(1);
    }

    // handles when a state change of ZooKeeper was detected and sets the current message
    public void handleSessionStateUpdate(String stateString) {
        message = String.format("zookeeper system state was updated: %s", stateString);
    }

    // uses a thread to set a new tile color
    public void setNewTileColorWithThread(int tileXCoord, int tileYCoord, String colorStr, boolean synchronous) {

        new Thread(() -> {
           setNewTileColor(tileXCoord, tileYCoord, colorStr, synchronous);
        }).start();
    } 

    // sends a request to zookeeper to set the color of a given tile
    public void setNewTileColor(int tileXCoord, int tileYCoord, String colorStr, boolean synchronous) {
        // message = String.format("setting tile (%d, %d) to %s", tileXCoord, tileYCoord, colorStr);

        // makes sure that the tile is in the chunk
        if (tileXCoord < 0 || tileXCoord >= Chunk.size || tileYCoord < 0 || tileYCoord >= Chunk.size) {
            message = "error: invalid tile coordinates";
            return;
        }
        
        Colorer.Color color = Colorer.stringToColor(colorStr);

        if (color == null) {
            message = "error: invalid color, not setting tile data";
            return;
        }

        try {

            Chunk chunk;

            synchronized (chunkLock) {
                chunk = currentChunk;
            }

            // convert the color to bytes to write as the node data
            byte[] colorData = colorStr.getBytes(StandardCharsets.UTF_8);

            String tilePath = Helpers.tileCoordsToNodePath(chunk.xCoord, chunk.yCoord, tileXCoord, tileYCoord);

            if (synchronous) {
                
                // make a call to ZooKeeper checking the current tile exists
                boolean tileExists = zkm.syncExists(tilePath, false);

                // check if the tile exists or not and set the data or create a new node accordingly
                if (tileExists) {
                    zkm.syncSetData(tilePath, colorData);
                } else {
                    zkm.syncCreate(tilePath, true, colorData);
                }

                drawTileUpdate(tileXCoord, tileYCoord);
            } else {

                zkm.asyncCreate(tilePath, true, colorData);
                zkm.asyncSetData(tilePath, colorData);
            }
            
        } catch (KeeperException e) {
            if (e.code() != Code.NODEEXISTS) {
                waitForRetryDueToError();
                message = String.format("KeeperException: %s", e.getMessage());
                setNewTileColor(tileXCoord, tileYCoord, colorStr, synchronous);
            }
        } catch (InterruptedException e) {
            message = String.format("InterruptedException: %s", e.getMessage());
        }
    }

    // fills the octant of drawing a circle part with bresenham's algorithm
    public void fillCirclePart(int xCenter, int yCenter, int xEdge, int yEdge, String colorStr) {

        int minX = Math.min(xCenter, xEdge);
        int maxX = Math.max(xCenter, xEdge);
        int minY = Math.min(yCenter, yEdge);
        int maxY = Math.max(yCenter, yEdge);

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                // setNewTileColor(x, y, colorStr, false);
                setNewTileColorWithThread(x, y, colorStr, true);
            }
        }
    }

    // bresenham's algorithm calculates 1 point and then reflects it over 8 octants of the circle
    public void drawCirclePart(int xCenter, int yCenter, int xOffset, int yOffset, boolean fill, String colorStr) {

        // set the circle outline
        setNewTileColorWithThread(xCenter+xOffset, yCenter+yOffset, colorStr, true);
        setNewTileColorWithThread(xCenter-xOffset, yCenter+yOffset, colorStr, true);
        setNewTileColorWithThread(xCenter+xOffset, yCenter-yOffset, colorStr, true);
        setNewTileColorWithThread(xCenter-xOffset, yCenter-yOffset, colorStr, true);
        setNewTileColorWithThread(xCenter+yOffset, yCenter+xOffset, colorStr, true);
        setNewTileColorWithThread(xCenter-yOffset, yCenter+xOffset, colorStr, true);
        setNewTileColorWithThread(xCenter+yOffset, yCenter-xOffset, colorStr, true);
        setNewTileColorWithThread(xCenter-yOffset, yCenter-xOffset, colorStr, true);
        
        if (fill) {
            // some parts do overlap, so the same tile will be drawn multiple times to the same color
            fillCirclePart(xCenter, yCenter, xCenter+xOffset, yCenter+yOffset, colorStr);
            fillCirclePart(xCenter, yCenter, xCenter-xOffset, yCenter+yOffset, colorStr);
            fillCirclePart(xCenter, yCenter, xCenter+xOffset, yCenter-yOffset, colorStr);
            fillCirclePart(xCenter, yCenter, xCenter-xOffset, yCenter-yOffset, colorStr);
            fillCirclePart(xCenter, yCenter, xCenter+yOffset, yCenter+xOffset, colorStr);
            fillCirclePart(xCenter, yCenter, xCenter-yOffset, yCenter+xOffset, colorStr);
            fillCirclePart(xCenter, yCenter, xCenter+yOffset, yCenter-xOffset, colorStr);
            fillCirclePart(xCenter, yCenter, xCenter-yOffset, yCenter-xOffset, colorStr);
        }
    }

    // draw's a circle using bresenham's algorithm
    public void drawBresenhamCircle(int xCenter, int yCenter, int radius, boolean fill, String colorStr) {
        
        int xOffset = 0;
        int yOffset = radius;
        int d = 3 - 2 * radius;

        drawCirclePart(xCenter, yCenter, xOffset, yOffset, fill, colorStr);
        while (yOffset >= xOffset) { 
            xOffset++;
     
            if (d > 0) {
                yOffset--;
                d = d + 4 * (xOffset - yOffset) + 10;
            } else {
                d = d + 4 * xOffset + 6;
            }

            drawCirclePart(xCenter, yCenter, xOffset, yOffset, fill, colorStr);
        }
    }

    // stores the current update log to file for data analysis
    public void saveUpdateLog(int serverId, int experimentNum, int expectedResults) {

        try {
            File outputDir = new File("experiment_output");
            if (!outputDir.exists()) {
                outputDir.mkdir();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        try {
            String fileName = String.format("experiment_output/updates_%d_%d_%d.txt", serverId, experimentNum, expectedResults);
            File file = new File(fileName);
            FileWriter fileWriter = new FileWriter(fileName);

            StringBuilder content = new StringBuilder();

            for (int i = 0; i < updateLog.size(); i++) {
                content.append(String.format("%d,%s\n", i+1, updateLog.get(i).toString()));
            }

            file.createNewFile();
            fileWriter.write(content.toString());
            fileWriter.close();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // performs a given number of writes to ZooKeeper and stores the current time to file
    public void performWriteExperimentAndSaveLog(int experimentNum, int numWrites) {

        int writesDone = 0;

        for (int y = 0; y <= 49; y++) {
            for (int x = 0; x <= 49; x++) {
                setNewTileColorWithThread(x, y, "RED", true);
                writesDone++;

                if (writesDone == numWrites) {
                    break;
                }
            }

            if (writesDone == numWrites) {
                break;
            }
        }

        try {
            File outputDir = new File("experiment_output");
            if (!outputDir.exists()) {
                outputDir.mkdir();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            String fileName = String.format("experiment_output/write_%d_%d.txt", numWrites, experimentNum);
            File file = new File(fileName);
            FileWriter fileWriter = new FileWriter(fileName);

            file.createNewFile();
            fileWriter.write(Long.toString(System.currentTimeMillis()));
            fileWriter.close();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // handle a user inputted command
    public void processUserCommand(String commandInput) {

        new Thread(() -> {

        String command = commandInput.trim().toLowerCase();
        String []parts = command.split(" ");
        String commandType = parts[0];

        Chunk chunk;

        synchronized (chunkLock) {
            chunk = currentChunk;
        }

        // check the type of command
        if (commandType.equals("set")) {

            if (parts.length != 4) {
                message = "usage: 'set x y color'";
                return;
            }

            String colorStr = parts[3];

            int tileXCoord = -1;
            int tileYCoord = -1;
            
            // try to parse the inputted tile coordinates
            try{
                tileXCoord = Integer.parseInt(parts[1]);
                tileYCoord = Integer.parseInt(parts[2]);
            }
            catch (NumberFormatException ex){
                message = "error: invalid tile coordinates";
                return;
            }

            setNewTileColor(tileXCoord, tileYCoord, colorStr, true);

        } else if (commandType.equals("move")) {

            if (parts.length != 2) {
                message = "usage: 'move direction' (direction: up, down, left, right)";
                return;
            }

            int newChunkXCoord = 0;
            int newChunkYCoord = 0;

            if (parts[1].equals("up")) {
                newChunkXCoord = chunk.xCoord;
                newChunkYCoord = chunk.yCoord - 1;
            } else if (parts[1].equals("down")) {
                newChunkXCoord = chunk.xCoord;
                newChunkYCoord = chunk.yCoord + 1;
            } else if (parts[1].equals("left")) {
                newChunkXCoord = chunk.xCoord - 1;
                newChunkYCoord = chunk.yCoord;
            } else if (parts[1].equals("right")) {
                newChunkXCoord = chunk.xCoord + 1;
                newChunkYCoord = chunk.yCoord;
            } else {
                message = "error: invalid direction, please use 'up', 'down', 'left', or 'right'";
                return;
            }

            // set the current chunk to the given coordinates and get the data from ZooKeeper
            getChunkFromZK(newChunkXCoord, newChunkYCoord);
            
        } else if (commandType.equals("view")) {
            
            if (parts.length != 3) {
                message = "usage: 'view x y' (x and y are chunk coordinates)";
                return;
            }

            int chunkXCoord = 0;
            int chunkYCoord = 0;
            
            // try to parse the inputted tile coordinates
            try{
                chunkXCoord = Integer.parseInt(parts[1]);
                chunkYCoord = Integer.parseInt(parts[2]);
            }
            catch (NumberFormatException ex){
                message = "error: invalid chunk coordinates";
                return;
            }

            // set the current chunk to the given coordinates and get the data from ZooKeeper
            getChunkFromZK(chunkXCoord, chunkYCoord);

        } else if (commandType.equals("rect")) {

            if (parts.length != 6) {
                message = "usage: 'rect x1 y1 x2 y2 color (x1, y1 = tile1 coords and x2, y2 = tile2 coords)";
                return;
            }

            String colorStr = parts[5];

            int tile1XCoord = -1;
            int tile1YCoord = -1;

            int tile2XCoord = -1;
            int tile2YCoord = -1;
            
            // try to parse the inputted tile coordinates
            try{
                tile1XCoord = Integer.parseInt(parts[1]);
                tile1YCoord = Integer.parseInt(parts[2]);
                tile2XCoord = Integer.parseInt(parts[3]);
                tile2YCoord = Integer.parseInt(parts[4]);
            }
            catch (NumberFormatException ex){
                message = "error: invalid tile coordinates";
                return;
            }

            int topLeftX = Math.min(tile1XCoord, tile2XCoord);
            int topLeftY = Math.min(tile1YCoord, tile2YCoord);
            int bottomRightX = Math.max(tile1XCoord, tile2XCoord);
            int bottomRightY = Math.max(tile1YCoord, tile2YCoord);

            for (int y = topLeftY; y <= bottomRightY; y++) {
                for (int x = topLeftX; x <= bottomRightX; x++) {
                    setNewTileColorWithThread(x, y, colorStr, true);
                }
            }
            
        } else if (commandType.equals("checker")) {

            if (parts.length != 7) {
                message = "usage: 'checker x1 y1 x2 y2 color1 color2 (x1, y1 = tile1 coords and x2, y2 = tile2 coords)";
                return;
            }

            String colorStr1 = parts[5];
            String colorStr2 = parts[6];

            int tile1XCoord = -1;
            int tile1YCoord = -1;

            int tile2XCoord = -1;
            int tile2YCoord = -1;
            
            // try to parse the inputted tile coordinates
            try{
                tile1XCoord = Integer.parseInt(parts[1]);
                tile1YCoord = Integer.parseInt(parts[2]);
                tile2XCoord = Integer.parseInt(parts[3]);
                tile2YCoord = Integer.parseInt(parts[4]);
            }
            catch (NumberFormatException ex){
                message = "error: invalid tile coordinates";
                return;
            }

            int topLeftX = Math.min(tile1XCoord, tile2XCoord);
            int topLeftY = Math.min(tile1YCoord, tile2YCoord);
            int bottomRightX = Math.max(tile1XCoord, tile2XCoord);
            int bottomRightY = Math.max(tile1YCoord, tile2YCoord);

            for (int y = topLeftY; y <= bottomRightY; y++) {
                for (int x = topLeftX; x <= bottomRightX; x++) {

                    // alternate colors in a checkboard pattern
                    String colorStr;
                    if (y % 2 == 0) {
                        if (x % 2 == 0) {
                            colorStr = colorStr1;
                        } else {
                            colorStr = colorStr2;
                        }
                    } else {
                        if (x % 2 == 0) {
                            colorStr = colorStr2;
                        } else {
                            colorStr = colorStr1;
                        }
                    }

                    setNewTileColorWithThread(x, y, colorStr, true);
                }
            }
            
        } else if (commandType.equals("circle")) {

            if (parts.length != 6) {
                message = "usage: 'circle x y r fill_type color (x, y = tile coords, r = radius, fill_type = fill or line)";
                return;
            }

            int tileXCoord = -1;
            int tileYCoord = -1;
            int radius = -1;
            
            // try to parse the inputted tile coordinates
            try{
                tileXCoord = Integer.parseInt(parts[1]);
                tileYCoord = Integer.parseInt(parts[2]);
                radius = Integer.parseInt(parts[3]);
            }
            catch (NumberFormatException ex){
                message = "error: invalid tile coordinates or radius";
                return;
            }

            if (radius < 1) {
                message = "error: invalid radius, please enter a postive integer";
                return;
            }

            String colorStr = parts[5];
            String fillType = parts[4];
            boolean shouldFill = false;

            if (fillType.equals("solid") || fillType.equals("fill")) {
                shouldFill = true;
            } else if (fillType.equals("line")) {
                shouldFill = false;
            } else {
                message = "invalid fill_type, defaulting to 'line'";
                shouldFill = false;
            }

            drawBresenhamCircle(tileXCoord, tileYCoord, radius, shouldFill, colorStr);

        } else if (commandType.equals("write_test")) {

            if (parts.length != 3) {
                message = "usage: 'write_test experiment_num num_writes";
                return;
            }

            int experiment_num = -1;
            int num_writes = -1;
            
            // try to parse the inputted tile coordinates
            try{
                experiment_num = Integer.parseInt(parts[1]);
                num_writes = Integer.parseInt(parts[2]);
            }
            catch (NumberFormatException ex){
                return;
            }

            performWriteExperimentAndSaveLog(experiment_num, num_writes);

        } else if (commandType.equals("store")) {

            if (parts.length != 4) {
                message = "usage: 'store server_id experiment_num updates_expected";
                return;
            }

            int experiment_num = -1;
            int updates_expected = -1;
            int server = -1;
            
            // try to parse the inputted tile coordinates
            try{
                server = Integer.parseInt(parts[1]);
                experiment_num = Integer.parseInt(parts[2]);
                updates_expected = Integer.parseInt(parts[3]);
            }
            catch (NumberFormatException ex){
                return;
            }

            saveUpdateLog(server, experiment_num, updates_expected);

        } else {
            message = "error: invalid command, please use 'set', 'move', 'view', 'rect', 'checker', or 'circle'";
        } 

        drawDisplay();

        }).start();     
    }

    // draws an individual tile to the display
    public void drawTileUpdate(int tileXCoord, int tileYCoord) {

        synchronized (displayLock) {

            Chunk chunk;

            synchronized (chunkLock) {
                chunk = currentChunk;
            }

            if (chunk == null) {
                return;
            }

            if (tileXCoord < 0 || tileXCoord >= Chunk.size || tileYCoord < 0 || tileYCoord >= Chunk.size) {
                return;
            }

            if (!hasDrawnDisplayOnce) {
                return;
            }
            
            StringBuilder displayStr = new StringBuilder();
            String tileStr = chunk.getTile(tileXCoord, tileYCoord).toString();
            int numDisplayedLines = lastDisplayStr.length() - lastDisplayStr.toString().replace("\n", "").length();

            // heavily dependent on the number of lines used to display, kinda wishy washy
            int numUps = numDisplayedLines - tileYCoord - 1;

            // moving the cursor up
            for (int i = 0; i < numUps; i++) {
                displayStr.append("\u001B[A");    
            }

            displayStr.append("\r");

            // moving the cursor right
            for (int i = 0; i < tileXCoord * 2; i++) {
                displayStr.append("\u001B[C");
            }

            // adding the tile data
            displayStr.append(tileStr);

            // moving the cursor back down
            for (int i = 0; i < numUps; i++) {
                displayStr.append("\u001B[B");    
            } 

            displayStr.append("\r>: ");

            System.out.print(displayStr.toString());
        }
    }

    // prints the current display to console
    public void drawDisplay() {

        synchronized (displayLock) {

            if (!hasDrawnDisplayOnce) {
                for (int i = 0; i < 10; i++) {
                    System.out.println();
                }
            }
            
            Chunk chunk;
            synchronized (chunkLock) {
                chunk = currentChunk;
            }
            StringBuilder displayStr = new StringBuilder();

            displayStr.append("\n");
            displayStr.append(chunk.toString());
            displayStr.append("                                                                                                        \r");
            displayStr.append(String.format("displaying: chunk (%d, %d)                                       \n", chunk.xCoord, chunk.yCoord));
            displayStr.append("                                                                                                        \r");
            displayStr.append(String.format("message: %s\n", message));
        
            // add lots of spaces to clear any previous messages
            for (int i = 0; i < 2; i++) {
                displayStr.append("                                                                                                        \n");
            }

            int numNewLines = displayStr.length() - displayStr.toString().replace("\n", "").length();

            // check if the display has been printed at least one time before
            if (hasDrawnDisplayOnce) {

                for (int i = 0; i < numNewLines; i++) {

                    // moves the cursor "up" which overwrites previous display
                    displayStr.insert(0, "\u001B[A");
                }
            }

            displayStr.append("                                                                                               \r>: ");
            // displayStr.append(">: ");
            
            lastDisplayStr = displayStr.toString();
            System.out.print(displayStr);

            hasDrawnDisplayOnce = true;
        }
    }

    // execute the main loop of the client
    public void run() {
        
        // start off by viewing chunk (0,0)
        getChunkFromZK(0, 0);
        drawDisplay();

        // continuously get user input
        while (reader.hasNextLine()) {

            String userInput = reader.nextLine();
                
            synchronized (displayLock) {
                System.out.print("\u001B[A");
            }
            
            hasUserInputtedCommand = true;
            processUserCommand(userInput);            
            
            drawDisplay();
        }

        reader.close();
    }

    // main method called on application launch
    public static void main(String[] args) throws KeeperException, IOException, InterruptedException {

        // handle argument checking
        if (args.length < 1) {
            System.out.printf("error: please enter a ZooKeeper server to connect to\n");
            return;
        }        
        
        Client client = new Client(args[0]);
        client.run();
    }
}
