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
                    SavedHorse horse = top[i];

                    OfflinePlayer owner = horse.getOwner();
                    String ownerName = (owner != null) ? owner.getName() : "<no owner>";
                    sender.sendMessage(ChatColor.GOLD + "#" + (i + 1) + " " +
                                       ChatColor.YELLOW + "Level " + String.format("%5.2f ", ability.getLevelForEffort(horse)) +
                                       ChatColor.WHITE + ownerName + " " +
                                       ChatColor.GRAY + horse.getAppearance() + " " +
                                       ChatColor.WHITE + horse.getUuid().toString().substring(0, 6) + "... ");
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