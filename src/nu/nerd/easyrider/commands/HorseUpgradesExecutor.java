package nu.nerd.easyrider.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import nu.nerd.easyrider.Ability;
import nu.nerd.easyrider.EasyRider;

// --------------------------------------------------------------------------
/**
 * Executor for the /horse-upgrades command.
 */
public class HorseUpgradesExecutor extends ExecutorBase {
    // --------------------------------------------------------------------------
    /**
     */
    public HorseUpgradesExecutor() {
        super("horse-upgrades", "health", "jump", "speed", "help");
    }

    // --------------------------------------------------------------------------
    /**
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender,
     *      org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("help")) {
                return false;
            }

            Ability ability = EasyRider.CONFIG.getAbility(args[0]);
            if (ability == null) {
                sender.sendMessage(ChatColor.RED + "That is not a valid ability name.");
                return true;
            }

            sender.sendMessage(ChatColor.GOLD + "Table of Level, corresponding " + ability.getDisplayName() +
                               " in " + ability.getValueUnits() + " and minimum training effort in " + ability.getEffortUnits() + ":");
            for (int level = 1; level <= ability.getMaxLevel(); ++level) {
                sender.sendMessage(String.format("%s%2d %s%5.2f %s %s%5.2f %s",
                                                 ChatColor.GOLD.toString(), level,
                                                 ChatColor.YELLOW.toString(), ability.getDisplayValue(level), ability.getValueUnits(),
                                                 ChatColor.GRAY.toString(), ability.getEffortForLevel(level), ability.getEffortUnits()));
            }
            return true;
        }
        return false;
    }
} // class HorseUpgradesExecutor