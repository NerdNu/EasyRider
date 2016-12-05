package nu.nerd.easyrider;

import org.bukkit.Location;
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
     * Return the horizontal distance from a to b, ignoring Y coordinate
     * changes.
     *
     * @return the horizontal distance from a to b.
     */
    public static double getHorizontalDistance(Location a, Location b) {
        if (a.getWorld().equals(b.getWorld())) {
            double dx = b.getX() - a.getX();
            double dz = b.getZ() - a.getZ();
            return Math.sqrt(dx * dx + dz * dz);
        } else {
            return 0.0;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Limit a string to the specified maximum length, showing trailing elipses
     * if truncated.
     *
     * @param s the string.
     * @param maxLength the maximum length of the result, including the elipses.
     * @return the string limited to the specified maximum length.
     */
    public static String limitString(String s, int maxLength) {
        return s.length() <= maxLength ? s
                                       : s.substring(0, maxLength - 1) + "\u2026";
    }

    // ------------------------------------------------------------------------
    /**
     * Format a location as "(x, y, z) world", with integer coordinates.
     *
     * @param loc the location.
     * @return the location as a formatted String.
     */
    public static String formatLocation(Location loc) {
        return new StringBuilder().append('(')
        .append(loc.getBlockX()).append(", ")
        .append(loc.getBlockY()).append(", ")
        .append(loc.getBlockZ()).append(") ")
        .append(loc.getWorld().getName()).toString();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the linear interpolation between min and max.
     *
     * @param min the value to return for the minimum value of frac (0.0).
     * @param max the value to return for the maximum value of frac (1.0).
     * @param frac the fraction, in the range [0,1] of the max argument, with
     *        the remainder coming from the min argument. NOTE: if frac exceeds
     *        1.0, extrapolate linearly.
     * @return the interpolation between min and max.
     */
    public static double linterp(double min, double max, double frac) {
        return min + frac * (max - min);
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