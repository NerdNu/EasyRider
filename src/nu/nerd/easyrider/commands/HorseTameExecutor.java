package nu.nerd.easyrider.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import nu.nerd.easyrider.EasyRider;
import nu.nerd.easyrider.IPendingInteraction;
import nu.nerd.easyrider.Util;
import nu.nerd.easyrider.db.SavedHorse;

// ----------------------------------------------------------------------------
/**
 * Handles the /horse-tame command.
 */
public class HorseTameExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * Default constructor.
     */
    public HorseTameExecutor() {
        super("horse-tame", "help");
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

        if (args.length != 1 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
            return false;
        }

        OfflinePlayer newOwner = Bukkit.getOfflinePlayer(args[0]);
        if (newOwner == null) {
            sender.sendMessage(ChatColor.RED + args[0] + " is not a valid player name.");
        } else if (!newOwner.hasPlayedBefore()) {
            sender.sendMessage(ChatColor.RED + args[0] + " has never played on this server.");
        } else {
            sender.sendMessage(ChatColor.GOLD + "Right click on the horse to tame to " + newOwner.getName());
            Player player = (Player) sender;
            EasyRider.PLUGIN.getState(player).setPendingInteraction(new IPendingInteraction() {
                @Override
                public void onPlayerInteractEntity(PlayerInteractEntityEvent event, SavedHorse savedHorse) {
                    AbstractHorse abstractHorse = (AbstractHorse) event.getRightClicked();
                    abstractHorse.setOwner(newOwner);
                    EasyRider.DB.observe(savedHorse, abstractHorse);
                    savedHorse.clearPermittedPlayers();
                    sender.sendMessage(ChatColor.GOLD +
                                       "The " + Util.entityTypeName(abstractHorse) + ", " +
                                       Util.limitString(savedHorse.getUuid().toString(), 20) +
                                       ", now belongs to " + newOwner.getName() + ".");
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                }
            });
        }

        return true;
    }
} // class HorseTameExecutor