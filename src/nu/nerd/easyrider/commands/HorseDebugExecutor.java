package nu.nerd.easyrider.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import nu.nerd.easyrider.EasyRider;
import nu.nerd.easyrider.IPendingInteraction;
import nu.nerd.easyrider.db.SavedHorse;

// --------------------------------------------------------------------------
/**
 * Handle the /horse-debug command.
 */
public class HorseDebugExecutor extends ExecutorBase {
    // --------------------------------------------------------------------------
    /**
     * Constructor.
     */
    public HorseDebugExecutor() {
        super("horse-debug", "on", "off", "help");
    }

    // --------------------------------------------------------------------------
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

        if (args.length != 1 || (!args[0].equalsIgnoreCase("on") && !args[0].equalsIgnoreCase("off"))) {
            return false;
        } else {
            Player player = (Player) sender;
            String debug = args[0].toLowerCase();
            sender.sendMessage(ChatColor.GOLD + "Right click on a horse to turn debugging " + debug + ".");
            EasyRider.PLUGIN.getState(player).setPendingInteraction(new IPendingInteraction() {
                @Override
                public void onPlayerInteractEntity(PlayerInteractEntityEvent event, SavedHorse savedHorse) {
                    savedHorse.setDebug(debug.equals("on"));
                    event.getPlayer().sendMessage(ChatColor.GOLD + "Debugging for horse " +
                                                  event.getRightClicked().getUniqueId() +
                                                  " is now " + debug + ".");
                }
            });
            return true;
        }
    }
} // class HorseDebugExecutor