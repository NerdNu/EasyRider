package nu.nerd.easyrider;

import org.bukkit.entity.Horse;
import org.bukkit.entity.Horse.Variant;

// ----------------------------------------------------------------------------
/**
 * Utility functions that don't necessarily belong in a specific class.
 */
public class Util {
    // ------------------------------------------------------------------------
    /**
     * Return the appearance of the specified Horse as a displayable String.
     * 
     * @param horse the horse.
     * @return the appearance of the specified Horse as a displayable String.
     */
    public static String getAppearance(Horse horse) {
        Horse.Variant variant = horse.getVariant();
        if (variant == Variant.HORSE) {
            return COLOR_TO_APPEARANCE[horse.getColor().ordinal()] +
                   STYLE_TO_APPEARANCE[horse.getStyle().ordinal()];
        } else {
            return variant.name().toLowerCase().replace('_', ' ');
        }
    }

    // ------------------------------------------------------------------------
    /**
     * The string form of Horse.Color constants as returned by getAppearance(),
     * listed in the same order as the enum.
     */
    private static final String[] COLOR_TO_APPEARANCE = {
        "white", "creamy", "chestnut", "brown", "black", "gray", "dark brown"
    };

    /**
     * The string form of Horse.Style constants as returned by getAppearance(),
     * listed in the same order as the enum.
     */
    private static final String[] STYLE_TO_APPEARANCE = {
        "", ", socks", ", whitefield", ", white dots", ", black dots" };
} // class Util