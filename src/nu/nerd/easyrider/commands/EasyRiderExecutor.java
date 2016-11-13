package nu.nerd.easyrider.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import nu.nerd.easyrider.EasyRider;

// ----------------------------------------------------------------------------
/**
 * CommandExecutor implementation for the /easyrider command.
 */
public class EasyRiderExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * Default constructor.
     */
    public EasyRiderExecutor() {
        super("easyrider", "reload", "migrate", "help");
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender,
     *      org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
            return false;
        } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            EasyRider.CONFIG.reload();
            sender.sendMessage(ChatColor.GOLD + EasyRider.PLUGIN.getName() + " configuration reloaded.");
            return true;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("migrate")) {
            EasyRider.DB.migrate(sender, args[1]);
            return true;
        } else {
            sender.sendMessage(ChatColor.RED + "Invalid command. Type \"/" + getName().toLowerCase() + " help\" for help.");
            return true;
        }
    }
} // class EasyRiderExecutor