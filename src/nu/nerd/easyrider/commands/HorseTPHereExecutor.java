package nu.nerd.easyrider.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;

import nu.nerd.easyrider.EasyRider;
import nu.nerd.easyrider.Util;
import nu.nerd.easyrider.db.SavedHorse;

//----------------------------------------------------------------------------
/**
 * Executor for the /horse-tphere command.
 */
public class HorseTPHereExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * Default constructor.
     */
    public HorseTPHereExecutor() {
        super("horse-tphere", "help");
    }

    // --------------------------------------------------------------------------
    /**
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender,
     *      org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
            return false;
        }
        if (args.length > 1) {
            sender.sendMessage(ChatColor.RED + "This command expects a horse's UUID as its only argument.");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be in game to use this command.");
            return true;
        }

        Player sendingPlayer = (Player) sender;
        String uuidPrefix = args[0];
        List<SavedHorse> horses = EasyRider.DB.findHorsesByUUID(uuidPrefix);
        if (horses.size() == 0) {
            sender.sendMessage(ChatColor.RED + "The UUID prefix " + uuidPrefix + " doesn't match any horses.");
        } else if (horses.size() > 1) {
            sender.sendMessage(ChatColor.RED + "The UUID prefix " + uuidPrefix + " matches more than one horse.");
        } else {
            SavedHorse savedHorse = horses.get(0);
            Location loc = savedHorse.getLocation();

            long start = System.nanoTime();
            Horse horse = Util.findHorse(savedHorse.getUuid(), loc, 2);
            if (EasyRider.CONFIG.DEBUG_FINDS) {
                EasyRider.PLUGIN.getLogger().info("findHorse() took " + (System.nanoTime() - start) * 0.001 + " microseconds.");
            }

            if (horse != null) {
                tpHorse(horse, sendingPlayer);
                savedHorse.observe(horse);
            } else {
                sender.sendMessage(ChatColor.GOLD + "The specified horse could not be found.");
            }
        }

        return true;
    }

    // ------------------------------------------------------------------------
    /**
     * Teleport a Horse to a Player.
     *
     * @param horse the Horse.
     * @param player the player.
     */
    protected void tpHorse(Horse horse, Player player) {
        Location loc = player.getLocation();
        player.sendMessage(ChatColor.GOLD +
                           "Teleporting " + horse.getUniqueId().toString() +
                           " to " + Util.formatLocation(loc) + ".");
        horse.teleport(loc);
    }
} // class HorseTPHereExecutor