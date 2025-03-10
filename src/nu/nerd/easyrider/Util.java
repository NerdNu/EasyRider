package nu.nerd.easyrider;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import nu.nerd.easyrider.db.SavedHorse;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Llama;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.entity.ZombieHorse;

// ----------------------------------------------------------------------------
/**
 * Utility functions that don't necessarily belong in a specific class.
 */
public class Util {
    // ------------------------------------------------------------------------
    /**
     * Return the first passenger of an Entity, or null if there are none.
     * 
     * @param steed the ridden entity.
     * @return the first passenger of an Entity, or null if there are none.
     */
    public static Entity getPassenger(Entity steed) {
        List<Entity> passengers = steed.getPassengers();
        return passengers.isEmpty() ? null : passengers.get(0);
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the specified entity is trackable.
     *
     * Trackable entities have their location, appearance and some inventory
     * state tracked. They are guaranteed to be instances of AbstractHorse.
     *
     * @param entity an entity.
     * @return true if the specified entity is trackable.
     */
    public static boolean isTrackable(Entity entity) {
        return entity instanceof AbstractHorse;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the entity can have its attributes improved through
     * training.
     *
     * Horses, SkeletonHorses and ZombieHorses are trainable; Llamas are not.
     *
     * @param entity an entity.
     * @return true if the entity can have its attributes improved through
     *         training.
     */
    public static boolean isTrainable(Entity entity) {
        return isTrackable(entity) && !(entity instanceof Llama);
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the specified entity is a skeleton horse or zombie horse.
     *
     * @return true if the specified entity is a skeleton horse or zombie horse.
     */
    public static boolean isUndeadHorse(Entity entity) {
        return entity instanceof SkeletonHorse ||
               entity instanceof ZombieHorse;
    }

    // ------------------------------------------------------------------------
    /**
     * Return a human-readable entity type name.
     *
     * @param entity the Entity.
     * @return a human-readable entity type name.
     */
    public static String entityTypeName(Entity entity) {
        return entity.getType().name().toLowerCase().replace('_', ' ');
    }

    // ------------------------------------------------------------------------
    /**
     * Find the AbstractHorse entity with the specified UUID near the specified
     * location.
     *
     * A square of chunks around the location are loaded if necessary. If that
     * area does not contain the AbstractHorse, all loaded chunks in all worlds
     * are searched, starting with the world containing the specified Location.
     *
     * @param uuid the AbstractHorse's UUID.
     * @param loc the Location where the AbstractHorse was last seen; if null,
     *        it is not used.
     * @param chunkRadius the radius of a square, expressed in chunks, around
     *        the Location that will be searched. This number should be small as
     *        loading chunks can be time-consuming and may lag out the server.
     * @return the matching AbstractHorse, if found, or null.
     */
    public static AbstractHorse findHorse(UUID uuid, Location loc, int chunkRadius) {
        if (loc == null) {
            return findHorse(uuid);
        }

        World centreWorld = loc.getWorld();
        Chunk centreChunk = loc.getChunk();
        AbstractHorse horse = findHorse(uuid, centreChunk);
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
     * for an AbstractHorse with the specified UUID.
     *
     * @param uuid the AbstractHorse's UUID.
     * @return the AbstractHorse entity or null if not found.
     */
    public static AbstractHorse findHorse(UUID uuid) {
        for (String worldName : EasyRider.CONFIG.SCAN_WORLD_RADIUS.keySet()) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                AbstractHorse horse = findHorse(uuid, world);
                if (horse != null) {
                    return horse;
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------
    /**
     * Find the AbstractHorse entity with the specified UUID in the specified
     * chunk.
     *
     * @param uuid the AbstractHorse's UUID.
     * @param chunk the chunk to search.
     * @return the matching AbstractHorse, if found, or null.
     */
    public static AbstractHorse findHorse(UUID uuid, Chunk chunk) {
        if (!chunk.isLoaded() && !chunk.load(false)) {
            return null;
        }
        for (Entity entity : chunk.getEntities()) {
            if (entity.getUniqueId().equals(uuid)) {
                return (AbstractHorse) entity;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------
    /**
     * Find the AbstractHorse entity with the specified UUID in the currently
     * loaded chunks of the specified World.
     *
     * @param uuid the AbstractHorse's UUID.
     * @param chunk the chunk to search.
     * @return the matching AbstractHorse, if found, or null.
     */
    public static AbstractHorse findHorse(UUID uuid, World world) {
        for (Entity entity : world.getEntities()) {
            if (entity.getUniqueId().equals(uuid)) {
                return (AbstractHorse) entity;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------
    /**
     * Return a list of SavedHorses owned by the specified player that match the
     * specified identifier.
     *
     * @param owner      the owning player.
     * @param identifier identifies the horse, either with the horse's
     *                   /horse-owned index, an AbstractHorse Entity UUID or the
     *                   name of the horse.
     * @return a non-null list of SavedHorses; this will be empty if no match is
     *         found.
     */
    public static List<SavedHorse> findHorses(OfflinePlayer owner, String identifier) {
        final String lowerIdent = identifier.toLowerCase();

        List<SavedHorse> horses = EasyRider.DB.getOwnedHorses(owner);
        try {
            int index = Integer.parseInt(lowerIdent);
            if (index > 0 && index <= horses.size()) {
                return Collections.singletonList(horses.get(index - 1));
            }
        } catch (NumberFormatException ignored) {
        }

        List<SavedHorse> found = horses.stream()
                .filter(h -> h.getDisplayName().toLowerCase().startsWith(lowerIdent))
                .collect(Collectors.toList());
        if (!found.isEmpty()) {
            return found;
        }

        found = horses.stream()
                .filter(h -> h.getUuid().toString().toLowerCase().startsWith(lowerIdent))
                .collect(Collectors.toList());
        if (!found.isEmpty()) {
            return found;
        }

        return new LinkedList<SavedHorse>();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the appearance of the specified AbstractHorse as a displayable
     * String.
     * 
     * @param abstractHorse the horse-like creature.
     * @return the appearance of the specified AbstractHorse as a displayable
     *         String.
     */
    public static String getAppearance(AbstractHorse abstractHorse) {
        if (abstractHorse instanceof Horse) {
            Horse horse = (Horse) abstractHorse;
            return COLOR_TO_APPEARANCE[horse.getColor().ordinal()] +
                   STYLE_TO_APPEARANCE[horse.getStyle().ordinal()];
        } else if (abstractHorse instanceof Llama) {
            Llama llama = (Llama) abstractHorse;
            Llama.Color colour = llama.getColor();
            return colour.name().toLowerCase() + " llama";
        } else {
            return entityTypeName(abstractHorse);
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
     * Change the first letter of a string to upper case.
     *
     * @param s the string.
     * @return the string, with the first letter changed to upper case.
     */
    public static String capitalise(String s) {
        return s.isEmpty() ? s : s.charAt(0) + s.substring(1);
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
     * Return true if the specified block is water, or is a waterlogged block.
     * 
     * @param block the block; if null, the result is false.
     * @return true if the specified block is water, or is a waterlogged block.
     */
    public static boolean isWaterlogged(Block block) {
        if (block == null) {
            return false;
        }

        if (block.getType() == Material.WATER) {
            return true;
        }

        BlockData blockData = block.getBlockData();
        return blockData instanceof Waterlogged && ((Waterlogged) blockData).isWaterlogged();
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