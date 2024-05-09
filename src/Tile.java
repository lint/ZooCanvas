
// class used to contain tile data in a chunk
public class Tile {

    public final String content = "██"; // the string that is used to represent the tile on the grid when printed
    Colorer.Color color; // the color to print the tile
    int timesUpdated; // the number of times the tile was updated to a new color (locally)
    String path; // the path of the tile

    // default constructor
    public Tile() {
        color = Colorer.Color.WHITE;
        timesUpdated = 0;
    }

    // constructor overriding the default color
    public Tile(Colorer.Color color) {
        this.color = color;
        this.timesUpdated = 0;
    }

    // set the tile to a new color
    public void setColor(Colorer.Color color) {
        this.color = color;
        timesUpdated++;
    }

    // color the content string and return it
    public String toString() {
        return Colorer.colorString(content, color);
    }
}
