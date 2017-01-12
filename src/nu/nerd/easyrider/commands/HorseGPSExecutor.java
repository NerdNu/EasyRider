package nu.nerd.easyrider.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Horse;
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
        if (args.length == 1) {
            horses = findHorses(sendingPlayer, args[0]);
        } else {
            if (sender.hasPermission("easyrider.gps-all")) {
                horses = findHorses(sendingPlayer, String.join(" ", args));
                if (horses.size() == 0) {
                    OfflinePlayer owner = Bukkit.getOfflinePlayer(args[0]);
                    if (owner != null) {
                        horses = findHorses(owner, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                    }
                }
            } else {
                // If a player doesn't have permission to locate others' horses,
                // all args are the horse name.
                horses = findHorses(sendingPlayer, String.join(" ", args));
            }
        }

        if (horses.size() == 0) {
            sender.sendMessage(ChatColor.GOLD + "No horse with that identifier could be found.");
        } else if (horses.size() == 1) {
            pointTo(sendingPlayer, horses.get(0));
        } else {
            sender.sendMessage(ChatColor.GOLD + "That identifier matches multiple horses.");
        }
        return true;
    } // onCommand

    // ------------------------------------------------------------------------
    /**
     * Return a list of SavedHorses owned by the specified player that match the
     * specified identifier.
     * 
     * @param owner the owning player.
     * @param identifier identifies the horse, either with the horse's
     *        /horse-owned index, a Horse Entity UUID or the name of the horse.
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
     * @param player the player whose look direction is set.
     * @param savedHorse the sought horse.
     */
    protected void pointTo(Player player, SavedHorse savedHorse) {
        // Search for the horse only if no location has been saved.
        long start = System.nanoTime();
        Horse horse = Util.findHorse(savedHorse.getUuid(), savedHorse.getLocation(), 1);
        if (EasyRider.CONFIG.DEBUG_FINDS) {
            EasyRider.PLUGIN.getLogger().info("findHorse() took " + (System.nanoTime() - start) * 0.001 + " microseconds.");
        }
        if (horse != null) {
            EasyRider.DB.observe(savedHorse, horse);
        }

        Location playerLoc = player.getLocation();
        Location horseLoc = savedHorse.getLocation();
        if (horseLoc == null) {
            playerLoc.getWorld().playSound(playerLoc, Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            player.sendMessage(ChatColor.GOLD + "The specified horse could not be found.");
        } else {
            playerLoc.getWorld().playSound(playerLoc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            String id = (savedHorse.getDisplayName().length() > 0) ? savedHorse.getDisplayName()
                                                                   : Util.limitString(savedHorse.getUuid().toString(), 20);
            int distance = (int) playerLoc.distance(horseLoc);
            player.sendMessage(ChatColor.GOLD + id +
                               ChatColor.GRAY + " at " +
                               ChatColor.YELLOW + Util.formatLocation(horseLoc) +
                               ChatColor.WHITE + " (" + distance + " m)");

            if (horseLoc.getWorld().equals(playerLoc.getWorld())) {
                playerLoc.setDirection(horseLoc.clone().subtract(playerLoc).toVector());
                player.teleport(playerLoc);
            }
        }
    } // pointTo
} // class HorseGPSExecutor