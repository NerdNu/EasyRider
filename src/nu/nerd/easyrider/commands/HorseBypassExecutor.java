package nu.nerd.easyrider.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import nu.nerd.easyrider.EasyRider;
import nu.nerd.easyrider.PlayerState;

// ----------------------------------------------------------------------------
/**
 * Handles the /horse-bypass command.
 */
public class HorseBypassExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * Default constructor.
     */
    public HorseBypassExecutor() {
        super("horse-bypass", "help");
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

        if (args.length != 0) {
            return false;
        }

        PlayerState state = EasyRider.PLUGIN.getState((Player) sender);
        state.toggleBypassEnabled();
        if (state.isBypassEnabled()) {
            sender.sendMessage(ChatColor.GOLD + "You can now bypass horse access permission checks.");
        } else {
            sender.sendMessage(ChatColor.GOLD + "You can no longer bypass horse access permission checks.");
        }
        return true;
    }
} // class HorseBypassExecutor