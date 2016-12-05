package nu.nerd.easyrider.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;
import nu.nerd.easyrider.EasyRider;
import nu.nerd.easyrider.PlayerState;

// ----------------------------------------------------------------------------
/**
 * Executor for the /horse-speed-limit command.
 */
public class HorseSpeedLimitExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * Default constructor.
     */
    public HorseSpeedLimitExecutor() {
        super("horse-speed-limit", "help");
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender,
     *      org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be in-game to use this command.");
            return true;
        }

        Player player = (Player) sender;
        PlayerState playerState = EasyRider.PLUGIN.getState(player);

        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + String.format("The speed of any horse you ride is capped at %.2f %s.",
                                                              EasyRider.CONFIG.SPEED.toDisplayValue(playerState.getMaxSpeed()),
                                                              EasyRider.CONFIG.SPEED.getValueUnits()));
            return true;
        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("help")) {
                return false;
            }

            try {
                playerState.setMaxSpeed(EasyRider.CONFIG.SPEED.toAttributeValue(Double.parseDouble(args[0])));
                sender.sendMessage(ChatColor.GOLD + String.format("The speed of any horse you ride is now capped at %.2f %s.",
                                                                  EasyRider.CONFIG.SPEED.toDisplayValue(playerState.getMaxSpeed()),
                                                                  EasyRider.CONFIG.SPEED.getValueUnits()));
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + args[0] + " is not a valid number.");
            }
            return true;
        }
        return false;
    } // onCommand
} // class HorseSpeedLimitExecutor