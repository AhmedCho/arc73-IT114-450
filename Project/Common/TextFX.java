package Project;

/**
 * Utility to attempt to provide colored text in the terminal.
 * <p>Important: This does not satisfy the text formatting feature/requirement for chatroom projects.</p>
 */
public abstract class TextFX {

    /**
     * TextFX.Color list of available colors
     * <p>Important: This does not satisfy the text formatting feature/requirement for chatroom projects.</p>
     */
    public enum Color {
        BLACK("\033[0;30m"),
        RED("\033[0;31m"),
        GREEN("\033[0;32m"),
        YELLOW("\033[0;33m"),
        BLUE("\033[0;34m"),
        PURPLE("\033[0;35m"),
        CYAN("\033[0;36m"),
        WHITE("\033[0;37m");

        private final String code;

        Color(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    public static final String RESET = "\033[0m";
    public static final String BOLD = "\033[1m";
    public static final String ITALIC = "\033[3m";
    public static final String UNDERLINE = "\033[4m";

    /**
     * Generates a String with the original message wrapped in the ASCII of the
     * color and RESET
     * 
     * <p>Note: May not work for all terminals</p>
     * <p>Important: This does not satisfy the text formatting feature/requirement for chatroom projects.</p>
     * 
     * @param text  Input text to colorize
     * @param color Enum of Color choice from TextFX.Color
     * @return wrapped String
     */
    public static String colorize(String text, Color color) {
        return color.getCode() + text + RESET;
    }

    public static String applyBold(String text) {
        return BOLD + text + RESET;
    }

    public static String applyItalic(String text) {
        return ITALIC + text + RESET;
    }

    public static String applyUnderline(String text) {
        return UNDERLINE + text + RESET;
    }


    public static void main(String[] args) {
        // Example usage:
        System.out.println(TextFX.colorize("Hello, world!", Color.RED));
        System.out.println(TextFX.colorize("This is some blue text.", Color.BLUE));
        System.out.println(TextFX.colorize("And this is green!", Color.GREEN));
        System.out.println(TextFX.applyBold("And this is bold text."));
        System.out.println(TextFX.applyItalic("And this is italic text."));
        System.out.println(TextFX.applyUnderline("And this is underlined text."));
    }
}
