package nu.nerd.easyrider.commands;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import nu.nerd.easyrider.Ability;
import nu.nerd.easyrider.EasyRider;
import nu.nerd.easyrider.db.SavedHorse;

// ----------------------------------------------------------------------------
/**
 * CommandExecutor implementation for the /horse-top command.
 *
 * When the maximum possible level is exceeded by training, colour the level
 * text red.
 */
public class HorseTopExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * Default constructor.
     */
    public HorseTopExecutor() {
        super("horse-top", "health", "jump", "speed", "help");
    }

    // ------------------------------------------------------------------------
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

            final int COUNT = 10;
            SavedHorse[] top = EasyRider.DB.rank(ability, COUNT);
            sender.sendMessage(ChatColor.GOLD + "Top " + COUNT + " horses by " + ability.getDisplayName() + ":");
            if (top.length == 0) {
                sender.sendMessage(ChatColor.GOLD + "Nobody has trained any horses.");
            } else {
                for (int i = 0; i < top.length; ++i) {
                    SavedHorse savedHorse = top[i];
                    OfflinePlayer owner = savedHorse.getOwner();
                    String ownerName = (owner != null) ? owner.getName() : "<no owner>";
                    double fractionalLevel = ability.getLevelForEffort(ability.getEffort(savedHorse));
                    ChatColor levelColour = (fractionalLevel >= ability.getMaxLevel()) ? ChatColor.RED : ChatColor.YELLOW;
                    sender.sendMessage(ChatColor.GOLD + "#" + (i + 1) + " " +
                                       levelColour + "Level " + String.format("%5.3f ", fractionalLevel) +
                                       ChatColor.WHITE + ownerName + " " +
                                       ChatColor.GRAY + savedHorse.getAppearance() + " " +
                                       ChatColor.WHITE + savedHorse.getUuid().toString().substring(0, 6) + "... ");
                }
            }
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------------
    /**
     * Horse variant names.
     */
    private static final String[] VARIANT_NAMES = {};
} // class HorseTopExecutor