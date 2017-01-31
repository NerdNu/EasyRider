package nu.nerd.easyrider.commands;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import nu.nerd.easyrider.Ability;
import nu.nerd.easyrider.EasyRider;
import nu.nerd.easyrider.IPendingInteraction;
import nu.nerd.easyrider.Util;
import nu.nerd.easyrider.db.SavedHorse;

// ----------------------------------------------------------------------------
/**
 * Executor for the /horse-set-level admin command.
 */
public class HorseSetLevelExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * Default constructor.
     */
    public HorseSetLevelExecutor() {
        super("horse-set-level", "health", "jump", "speed", "help");
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

        Player player = (Player) sender;
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("help")) {
                return false;
            }

            Ability ability = EasyRider.CONFIG.getAbility(args[0].toLowerCase());
            if (ability == null) {
                sender.sendMessage(ChatColor.RED + "That is not a valid ability name.");
                return true;
            }

            if (args.length != 2) {
                sender.sendMessage(ChatColor.RED + "You need to specify the new level.");
                return true;
            }

            try {
                double newLevel = Double.parseDouble(args[1]);
                if (newLevel < 1.0) {
                    sender.sendMessage(ChatColor.RED + "The new level must be at least 1.0.");
                    return true;
                }
                if (newLevel > ability.getMaxLevel()) {
                    sender.sendMessage(ChatColor.RED + "WARNING: The new level exceeds the maximum trainable level, " +
                                       ability.getMaxLevel() + ".");
                }

                sender.sendMessage(ChatColor.GOLD + "Right click on a horse to set " +
                                   ability.getName() + " level " + newLevel + ". (To cancel, relog.)");
                EasyRider.PLUGIN.getState(player).setPendingInteraction(new IPendingInteraction() {
                    @Override
                    public void onPlayerInteractEntity(PlayerInteractEntityEvent event, SavedHorse savedHorse) {
                        AbstractHorse abstractHorse = (AbstractHorse) event.getRightClicked();
                        if (Util.isTrainable(abstractHorse)) {
                            Player player = event.getPlayer();
                            player.sendMessage(ChatColor.GOLD + "Horse: " + ChatColor.YELLOW + abstractHorse.getUniqueId());
                            showLevel(player, "Old ", ability, savedHorse);
                            ability.setLevel(savedHorse, (int) newLevel);
                            ability.setEffort(savedHorse, ability.getEffortForLevel(newLevel));
                            ability.updateAttribute(savedHorse, abstractHorse);
                            showLevel(player, "New ", ability, savedHorse);
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                        } else {
                            player.sendMessage(ChatColor.RED + "That " + Util.entityTypeName(abstractHorse) + " cannot be trained.");
                            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                        }
                    }
                });
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "The new level must be a number.");
            }
            return true;
        }
        return false;
    } // onCommand

    // ------------------------------------------------------------------------
    /**
     * Show the new or old level, effort and display value of a horse's ability.
     * 
     * @param player the player to be send messages.
     * @param prefix the prefix string before the Ability display name.
     * @param abililty the {@link Ability}.
     * @param savedHorse the database state of the horse.
     */
    protected void showLevel(Player player, String prefix, Ability ability, SavedHorse savedHorse) {
        player.sendMessage(ChatColor.GOLD + prefix + ability.getDisplayName() + " Level: " +
                           ChatColor.WHITE + String.format("%5.3f", ability.getFractionalLevel(savedHorse)) +
                           ChatColor.GOLD + " - " +
                           ChatColor.YELLOW + ability.getFormattedValue(savedHorse) +
                           ChatColor.GRAY + " (" + ability.getFormattedEffort(savedHorse) + ")");
    }
} // class HorseSetLevelExecutor