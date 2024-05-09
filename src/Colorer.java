
// helper class used to color strings
public class Colorer {

    // enum representing different colors you can select
    public enum Color {
        NONE,
        RESET,
        BLACK,
        RED,
        GREEN,
        YELLOW,
        BLUE,
        PURPLE,
        CYAN,
        WHITE
    }

    // color codes to change the console text
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    // surround a string in color codes in order to color it
    public static String colorString(String input, Color color) {
        return colorToANSICode(color) + input + ANSI_RESET;
    }

    // convert a color enum to it's associated ANSI code
    public static String colorToANSICode(Color color) {
        if (color == Color.BLACK) {
            return ANSI_BLACK;
        } else if (color == Color.RED) {
            return ANSI_RED;
        } else if (color == Color.GREEN) {
            return ANSI_GREEN;
        } else if (color == Color.YELLOW) {
            return ANSI_YELLOW;
        } else if (color == Color.BLUE) {
            return ANSI_BLUE;
        } else if (color == Color.PURPLE) {
            return ANSI_PURPLE;
        } else if (color == Color.CYAN) {
            return ANSI_CYAN;
        } else if (color == Color.WHITE) {
            return ANSI_WHITE;
        } else {
            return ANSI_RESET;
        }
    }

    // convert a string to a color
    public static Colorer.Color stringToColor(String input) {

        input = input.trim().toUpperCase();

        if (input.equals("BLACK")) {
            return Color.BLACK;
        } else if (input.equals("RED")) {
            return Color.RED;
        } else if (input.equals("GREEN")) {
            return Color.GREEN;
        } else if (input.equals("YELLOW")) {
            return Color.YELLOW;
        } else if (input.equals("BLUE")) {
            return Color.BLUE;
        } else if (input.equals("PURPLE")) {
            return Color.PURPLE;
        } else if (input.equals("CYAN")) {
            return Color.CYAN;
        } else if (input.equals("WHITE")) {
            return Color.WHITE;
        } else if (input.equals("RESET")) {
            return Color.RESET;
        } else {
            return null;
        }
    }

    public static String colorToString(Colorer.Color color) {
        if (color == Color.BLACK) {
            return "BLACK";
        } else if (color == Color.RED) {
            return "RED";
        } else if (color == Color.GREEN) {
            return "GREEN";
        } else if (color == Color.YELLOW) {
            return "YELLOW";
        } else if (color == Color.BLUE) {
            return "BLUE";
        } else if (color == Color.PURPLE) {
            return "PURPLE";
        } else if (color == Color.CYAN) {
            return "CYAN";
        } else if (color == Color.WHITE) {
            return "WHITE";
        } else {
            return "NONE";
        }
    }
}
