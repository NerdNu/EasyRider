package nu.nerd.easyrider.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import nu.nerd.easyrider.Ability;
import nu.nerd.easyrider.EasyRider;
import nu.nerd.easyrider.IPendingInteraction;
import nu.nerd.easyrider.db.SavedHorse;

//-----------------------------------------------------------------------------
/**
 * CommandExecutor implementation for the /horse-level command.
 */
public class HorseLevelsExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * Default constructor.
     */
    public HorseLevelsExecutor() {
        super("horse-levels", "help");
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

        if (args.length == 0) {
            Player player = (Player) sender;
            if (player.getVehicle() instanceof Horse) {
                showLevels(player, (Horse) player.getVehicle());
            } else {
                sender.sendMessage(ChatColor.GOLD + "Right click on a horse to show level information.");
                EasyRider.PLUGIN.getState(player).setPendingInteraction(new IPendingInteraction() {
                    @Override
                    public void onPlayerInteractEntity(PlayerInteractEntityEvent event, SavedHorse savedHorse) {
                        showLevels(event.getPlayer(), (Horse) event.getRightClicked());
                    }
                });
            }
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Send the specified player information about the current levels of the
     * specified horse.
     */
    protected void showLevels(Player player, Horse horse) {
        SavedHorse savedHorse = EasyRider.DB.findOrAddHorse(horse);
        player.sendMessage(ChatColor.GOLD + "Horse: " + ChatColor.YELLOW + horse.getUniqueId());
        AnimalTamer owner = horse.getOwner();
        String ownerName = (owner != null) ? owner.getName() : "<no owner>";
        player.sendMessage(ChatColor.GOLD + "Owner: " + ChatColor.YELLOW + ownerName);
        showLevel(player, EasyRider.CONFIG.SPEED, savedHorse);
        showLevel(player, EasyRider.CONFIG.HEALTH, savedHorse);
        showLevel(player, EasyRider.CONFIG.JUMP, savedHorse);
    }

    // ------------------------------------------------------------------------
    /**
     * Show information about the current level of an ability, the corresponding
     * attribute value, and the total effort expended for that ability on a
     * specific horse.
     *
     * When the maximum possible level is exceeded by training, colour the level
     * text red.
     *
     * @param player the player to be send messages.
     * @param abililty the {@link Ability}.
     * @param savedHorse the database state of the horse.
     */
    protected void showLevel(Player player, Ability ability, SavedHorse savedHorse) {
        double fractionalLevel = ability.getLevelForEffort(ability.getEffort(savedHorse));
        ChatColor levelColour = (fractionalLevel >= ability.getMaxLevel()) ? ChatColor.RED : ChatColor.WHITE;
        player.sendMessage(ChatColor.GOLD + ability.getDisplayName() + ": " +
                           levelColour + "Level " + String.format("%5.3f", fractionalLevel) +
                           ChatColor.GOLD + " - " +
                           ChatColor.YELLOW + ability.getFormattedValue(savedHorse) +
                           ChatColor.GRAY + " (" + ability.getFormattedEffort(savedHorse) + ")");
    }
} // class HorseLevelsExecutor
