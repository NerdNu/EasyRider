package nu.nerd.easyrider;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.entity.ZombieHorse;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;

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
     * Re-apply disguises to all disguised steeds when a player joins.
     */
    public static void refreshSaddleDisguises() {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getVehicle() instanceof AbstractHorse) {
                AbstractHorse abstractHorse = (AbstractHorse) onlinePlayer.getVehicle();
                EntityType disguiseEntityType = Util.getSaddleDisguiseType(abstractHorse);
                if (disguiseEntityType != null) {
                    boolean showToRider = Util.isSaddleDisguiseVisibleToRider(abstractHorse);
                    Util.applySaddleDisguise(abstractHorse, onlinePlayer, disguiseEntityType, showToRider, false);
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Disguise a horse as a specified EntityType and notify a player when it is
     * still disguised.
     * 
     * @param abstractHorse the horse-like entity.
     * @param rider the rider, to be notified if a disguise is applied.
     * @param disguiseEntityType the EntityType of the disguise.
     * @param showToRider if true, the disguise is visible to the rider.
     * @param tellRider if true, tell the rider what disguise is in use.
     */
    public static void applySaddleDisguise(AbstractHorse abstractHorse, Player rider, EntityType disguiseEntityType,
                                           boolean showToRider, boolean tellRider) {
        if (disguiseEntityType == null) {
            return;
        }

        DisguiseType disguiseType = DisguiseType.getType(disguiseEntityType);
        if (disguiseType != null) {
            MobDisguise disguise = new MobDisguise(disguiseType);
            Set<Player> players = new HashSet<>(Bukkit.getOnlinePlayers());
            if (!showToRider) {
                players.remove(rider);
            }
            DisguiseAPI.undisguiseToAll(abstractHorse);
            DisguiseAPI.disguiseToPlayers(abstractHorse, disguise, players);
            if (tellRider) {
                rider.sendMessage(ChatColor.GOLD + "Your steed is disguised as " + disguiseEntityType + "!");
            }

            abstractHorse.removeMetadata(SELF_DISGUISE_KEY, EasyRider.PLUGIN);
            if (showToRider) {
                abstractHorse.setMetadata(SELF_DISGUISE_KEY, new FixedMetadataValue(EasyRider.PLUGIN, null));
            }
        } else {
            Logger logger = EasyRider.PLUGIN.getLogger();
            logger.warning("Horse " + abstractHorse.getUniqueId() + " accessed by " + rider.getName() +
                           " has a saddle with unsupported disguise " + disguiseEntityType + ".");
        }
    }

    // --------------------------------------------------------------------------
    /**
     * Return true if the saddle disguise of the specified horse is visible to
     * the rider.
     * 
     * @param abstractHorse the horse-like entity.
     * @return true if the saddle disguise of the specified horse is visible to
     *         the rider.
     */
    public static boolean isSaddleDisguiseVisibleToRider(AbstractHorse abstractHorse) {
        return !abstractHorse.getMetadata(SELF_DISGUISE_KEY).isEmpty();
    }

    // --------------------------------------------------------------------------
    /**
     * Return the EntityType of the disguise associated with a horse's saddle,
     * or null if the saddle doesn't confer a disguise (or it's not a saddle).
     * 
     * @param abstractHorse the horse-like entity.
     * @return the EntityType of the disguise, or null if no disguise should be
     *         applied.
     */
    public static EntityType getSaddleDisguiseType(AbstractHorse abstractHorse) {
        ItemStack saddle = getSaddleItemStack(abstractHorse);
        if (saddle == null || saddle.getType() != Material.SADDLE) {
            return null;
        }

        ItemMeta meta = saddle.getItemMeta();
        if (meta != null && meta.hasLore()) {
            for (String lore : meta.getLore()) {
                if (lore.startsWith(EasyRider.DISGUISE_PREFIX)) {
                    String entityTypeName = lore.substring(EasyRider.DISGUISE_PREFIX.length()).trim().toUpperCase();
                    try {
                        return EntityType.valueOf(entityTypeName);
                    } catch (IllegalArgumentException ex) {
                    }
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------
    /**
     * Get the ItemStack in the saddle slot of a horse, donkey or mule.
     * 
     * @param abstractHorse the horse-like entity (includes llamas).
     * @return the ItemStack in the saddle slot, or null if there is no saddle
     *         slot (as in the case of llamas).
     */
    public static ItemStack getSaddleItemStack(AbstractHorse abstractHorse) {
        if (abstractHorse instanceof Llama) {
            return null;
        }

        // Mules, donkeys, zombie- and skeletal- horses do not have a
        // HorseInventory. So get the saddle by ID rather than calling
        // HorseInventory.getSaddle().
        return abstractHorse.getInventory().getItem(0);
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

    /**
     * Metadata key for metadata signifying that a saddle disguise is visible to
     * the rider.
     * 
     * If metadata with this key is absent, the rider cannot see the disguise.
     */
    private static final String SELF_DISGUISE_KEY = "EasyRider_self_disguise";

} // class Util