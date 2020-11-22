package nu.nerd.easyrider.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import nu.nerd.easyrider.EasyRider;
import nu.nerd.easyrider.Util;
import nu.nerd.easyrider.db.SavedHorse;

// ----------------------------------------------------------------------------
/**
 * Executor for /horse-gps.
 */
public class HorseGPSExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * Default constructor.
     */
    public HorseGPSExecutor() {
        super("horse-gps", "help");
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender,
     *      org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be in game to use this command.");
            return true;
        }

        Player sendingPlayer = (Player) sender;
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
            return false;
        }

        List<SavedHorse> horses;
        String identifier;
        if (args.length == 1) {
            identifier = args[0];
            horses = findHorses(sendingPlayer, identifier);
        } else {
            identifier = String.join(" ", args);
            if (sender.hasPermission("easyrider.gps-player")) {
                horses = findHorses(sendingPlayer, identifier);
                if (horses.size() == 0) {
                    @SuppressWarnings("deprecation")
                    OfflinePlayer owner = Bukkit.getOfflinePlayer(args[0]);
                    if (owner != null) {
                        identifier = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                        horses = findHorses(owner, identifier);
                    }
                }
            } else {
                // If a player doesn't have permission to locate others' horses,
                // all args are the horse name.
                horses = findHorses(sendingPlayer, identifier);
            }
        }

        if (horses.size() == 0) {
            sender.sendMessage(ChatColor.GOLD + "No animal with the identifier \"" + identifier + "\" could be found.");
        } else if (horses.size() == 1) {
            pointTo(sendingPlayer, horses.get(0));
        } else {
            sender.sendMessage(ChatColor.GOLD + "The identifier \"" + identifier + "\" matches multiple animals.");
        }
        return true;
    } // onCommand

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
    protected List<SavedHorse> findHorses(OfflinePlayer owner, String identifier) {
        final String lowerIdent = identifier.toLowerCase();

        ArrayList<SavedHorse> horses = EasyRider.DB.getOwnedHorses(owner);
        try {
            int index = Integer.parseInt(lowerIdent);
            if (index > 0 && index <= horses.size()) {
                return Arrays.asList(horses.get(index - 1));
            }
        } catch (NumberFormatException ex) {
        }

        List<SavedHorse> found = horses.stream()
            .filter(h -> h.getDisplayName().toLowerCase().startsWith(lowerIdent))
            .collect(Collectors.toList());
        if (found.size() > 0) {
            return found;
        }

        found = horses.stream()
            .filter(h -> h.getUuid().toString().toLowerCase().startsWith(lowerIdent))
            .collect(Collectors.toList());
        if (found.size() > 0) {
            return found;
        }

        return new LinkedList<SavedHorse>();
    } // findHorses

    // ------------------------------------------------------------------------
    /**
     * Point the player to the horse if it's Location can be ascertained and
     * they are in the same world.
     *
     * @param player     the player whose look direction is set.
     * @param savedHorse the sought horse.
     */
    protected void pointTo(Player player, SavedHorse savedHorse) {
        // Search for the horse only if no location has been saved.
        long start = System.nanoTime();
        AbstractHorse horse = Util.findHorse(savedHorse.getUuid(), savedHorse.getLocation(), 1);
        if (EasyRider.CONFIG.DEBUG_FINDS) {
            EasyRider.PLUGIN.getLogger().info("findHorse() took " + (System.nanoTime() - start) * 0.001 + " microseconds.");
        }
        if (horse != null) {
            EasyRider.DB.observe(savedHorse, horse);
        }

        Location playerLoc = player.getLocation();
        Location horseLoc = savedHorse.getLocation();
        if (horseLoc == null) {
            playerLoc.getWorld().playSound(playerLoc, Sound.ENTITY_ITEM_BREAK, SoundCategory.NEUTRAL, 1.0f, 1.0f);
            player.sendMessage(ChatColor.GOLD + "The specified animal could not be found.");
        } else {
            playerLoc.getWorld().playSound(playerLoc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.NEUTRAL, 1.0f, 1.0f);

            // Send VoxelMap compatible coordinates.
            // The mod uses lowercase name, x, y, z, no trailing space after
            // colon. But it accepts capitals and spaces from /place. Use for
            // readability. VoxelMap strips colours and sets the [...] as aqua.
            // VoxelMap also accepts: "World: name" and "dim: " followed by one
            // of minecraft:overworld, minecraft:the_nether or
            // minecraft:the_end. VoxelMap is still happy if the "minecraft:"
            // namespace is dropped. VoxelMap's "Edit Waypoint" dialog populates
            // the world checkboxes based on the "dim: " value. If EasyRider
            // sends a "world: " value, that presumably relies on players
            // setting their "multiworld" world names the same as the server's
            // idea of world names. But the world name is useful information,
            // since "dim:" does not disambiguate e.g. overworld and mapworld.
            // VoxelMap does add a checkbox to the "Edit Waypoint" GUI when a
            // different dimension from the default 3 is used, but when
            // VoxelMap encounters such a waypoint in chat, it hides it from the
            // chat. VoxelMap's handling of dimensions and non-vanilla worlds
            // still seems to be a very unsatisfactory user experience.
            // Note that VoxelMap will always create a new waypoint in the
            // player's current world, regardless of what or how another
            // world/dimension is indicated.
            StringBuilder message = new StringBuilder();
            message.append(ChatColor.WHITE).append('[');
            message.append(ChatColor.GOLD).append("Name: ").append(ChatColor.YELLOW).append("HGPS");
            message.append(ChatColor.WHITE).append(", ");
            message.append(ChatColor.GOLD).append("X: ").append(ChatColor.YELLOW).append(horseLoc.getBlockX());
            message.append(ChatColor.WHITE).append(", ");
            message.append(ChatColor.GOLD).append("Y: ").append(ChatColor.YELLOW).append(horseLoc.getBlockY());
            message.append(ChatColor.WHITE).append(", ");
            message.append(ChatColor.GOLD).append("Z: ").append(ChatColor.YELLOW).append(horseLoc.getBlockZ());

            String dimension;
            switch (horseLoc.getWorld().getEnvironment()) {
            default:
            case NORMAL:
                dimension = "overworld";
                break;
            case NETHER:
                dimension = "the_nether";
                break;
            case THE_END:
                dimension = "the_end";
                break;
            }
            String worldName = horseLoc.getWorld().getName();
            if (DEFAULT_WORLD_NAMES.contains(worldName)) {
                message.append(ChatColor.WHITE).append(", ");
                message.append(ChatColor.GOLD).append("Dim: ").append(ChatColor.YELLOW).append(dimension);
            } else {
                message.append(ChatColor.WHITE).append(", ");
                message.append(ChatColor.GOLD).append("World: ").append(ChatColor.YELLOW).append(worldName);
            }
            message.append(ChatColor.WHITE).append(']');

            if (horseLoc.getWorld().equals(playerLoc.getWorld())) {
                int distance = (int) playerLoc.distance(horseLoc);
                message.append(" (").append(distance).append(" m)");

                // Point player at horse if in the same world.
                Entity vehicle = player.getVehicle();
                if (EasyRider.CONFIG.LOOK_ANGLE_WORKAROUND && player.isInsideVehicle()) {
                    player.sendMessage(ChatColor.YELLOW
                                       + "Due to a Mojang client bug, we can't point you in the right direction"
                                       + " when you're in a vehicle (SPIGOT-6187, SPIGOT-5891).");
                } else {
                    playerLoc.setDirection(horseLoc.clone().subtract(playerLoc).toVector());
                    player.teleport(playerLoc);
                    if (vehicle != null) {
                        vehicle.addPassenger(player);
                    }
                }
            }
            player.sendMessage(message.toString());
        }
    } // pointTo

    // ------------------------------------------------------------------------
    /**
     * Set of default (vanilla Minecraft) worlds.
     *
     * This is used to suppress the world name in /hgps waypoints in chat when
     * it is implied by the dimension.
     */
    protected Set<String> DEFAULT_WORLD_NAMES = new HashSet(Arrays.asList("world", "world_nether", "world_the_end"));

} // class HorseGPSExecutor
