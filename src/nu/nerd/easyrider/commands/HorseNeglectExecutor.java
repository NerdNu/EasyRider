package nu.nerd.easyrider.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import nu.nerd.easyrider.EasyRider;
import nu.nerd.easyrider.PlayerState;

// ----------------------------------------------------------------------------
/**
 * CommandExecutor implementation for the /horse-neglect command.
 */
public class HorseNeglectExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * Default constructor.
     */
    public HorseNeglectExecutor() {
        super("horse-neglect", "help");
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender,
     *      org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be in game to use this command.");
            return true;
        }

        Player player = (Player) sender;
        PlayerState playerState = EasyRider.PLUGIN.getState(player);
        playerState.setNeglectful(!playerState.isNeglectful());

        String verb = playerState.isNeglectful() ? "ignore" : "receive";
        player.sendMessage(ChatColor.GOLD + "You now " + verb + " horse dehydration messages and sounds.");
        return true;
    }
} // class HorseNeglectExecutor