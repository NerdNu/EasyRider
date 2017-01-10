package nu.nerd.easyrider;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Horse.Variant;

// ----------------------------------------------------------------------------
/**
 * Utility functions that don't necessarily belong in a specific class.
 */
public class Util {
    // ------------------------------------------------------------------------
    /**
     * Find the Horse entity with the specified UUID near the specified
     * location.
     *
     * A square of chunks around the location are loaded if necessary. If that
     * area does not contain the Horse, all loaded chunks in all worlds are
     * searched, starting with the world containing the specified Location.
     *
     * @param uuid the Horse's UUID.
     * @param loc the Location where the Horse was last seen; if null, it is not
     *        used.
     * @param chunkRadius the radius of a square, expressed in chunks, around
     *        the Location that will be searched. This number should be small as
     *        loading chunks can be time-consuming and may lag out the server.
     * @return the matching Horse, if found, or null.
     */
    public static Horse findHorse(UUID uuid, Location loc, int chunkRadius) {
        if (loc == null) {
            return findHorse(uuid);
        }

        World centreWorld = loc.getWorld();
        Chunk centreChunk = loc.getChunk();
        Horse horse = findHorse(uuid, centreChunk);
        if (horse != null) {
            return horse;
        }

        for (int r = 1; r < chunkRadius; ++r) {
            // Top and bottom rows of chunks around centreChunk.
            for (int x = -chunkRadius; x <= chunkRadius; ++x) {
                int chunkX = centreChunk.getX() + x;
                Chunk chunk = centreWorld.getChunkAt(chunkX, centreChunk.getZ() - chunkRadius);
                horse = findHorse(uuid, chunk);
                if (horse != null) {
                    return horse;
                }
                chunk = centreWorld.getChunkAt(chunkX, centreChunk.getZ() + chunkRadius);
                horse = findHorse(uuid, chunk);
                if (horse != null) {
                    return horse;
                }
            }
            // Left and right columns of chunks, excluding top/bottom row.
            for (int z = -chunkRadius + 1; z <= chunkRadius - 1; ++z) {
                int chunkZ = centreChunk.getZ() + z;
                Chunk chunk = centreWorld.getChunkAt(centreChunk.getX() - chunkRadius, chunkZ);
                horse = findHorse(uuid, chunk);
                if (horse != null) {
                    return horse;
                }
                chunk = centreWorld.getChunkAt(centreChunk.getX() + chunkRadius, chunkZ);
                horse = findHorse(uuid, chunk);
                if (horse != null) {
                    return horse;
                }
            }
        }

        // Search loaded chunks, starting with original World.
        horse = findHorse(uuid, centreWorld);
        if (horse != null) {
            return horse;
        }
        for (String worldName : EasyRider.CONFIG.SCAN_WORLD_RADIUS.keySet()) {
            World world = Bukkit.getWorld(worldName);
            if (world != null && world != centreWorld) {
                horse = findHorse(uuid, world);
                if (horse != null) {
                    return horse;
                }
            }
        }

        return null;
    }

    // ------------------------------------------------------------------------
    /**
     * Search all loaded chunks of all configured worlds, in arbitrary order,
     * for a Horse with the specified UUID.
     *
     * @param uuid the Horse's UUID.
     * @return the Horse entity or null if not found.
     */
    public static Horse findHorse(UUID uuid) {
        for (String worldName : EasyRider.CONFIG.SCAN_WORLD_RADIUS.keySet()) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                Horse horse = findHorse(uuid, world);
                if (horse != null) {
                    return horse;
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------
    /**
     * Find the Horse entity with the specified UUID in the specified chunk.
     *
     * @param uuid the Horse's UUID.
     * @param chunk the chunk to search.
     * @return the matching Horse, if found, or null.
     */
    public static Horse findHorse(UUID uuid, Chunk chunk) {
        if (!chunk.isLoaded() && !chunk.load(false)) {
            return null;
        }
        for (Entity entity : chunk.getEntities()) {
            if (entity.getUniqueId().equals(uuid)) {
                return (Horse) entity;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------
    /**
     * Find the Horse entity with the specified UUID in the currently loaded
     * chunks of the specified World.
     *
     * @param uuid the Horse's UUID.
     * @param chunk the chunk to search.
     * @return the matching Horse, if found, or null.
     */
    public static Horse findHorse(UUID uuid, World world) {
        for (Entity entity : world.getEntities()) {
            if (entity.getUniqueId().equals(uuid)) {
                return (Horse) entity;
            }
        }
        return null;
    }

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