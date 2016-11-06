package nu.nerd.easyrider.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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
public class HorseLevelExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * Default constructor.
     */
    public HorseLevelExecutor() {
        super("horse-level", "help");
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
            sender.sendMessage(ChatColor.GOLD + "Right click on a horse to show level information.");
            EasyRider.PLUGIN.getState(player).setPendingInteraction(new IPendingInteraction() {
                @Override
                public void onPlayerInteractEntity(PlayerInteractEntityEvent event, SavedHorse savedHorse) {
                    Horse horse = (Horse) event.getRightClicked();
                    Player player = event.getPlayer();
                    player.sendMessage(ChatColor.GOLD + "Horse: " + ChatColor.YELLOW + horse.getUniqueId());
                    showLevel(player, EasyRider.CONFIG.SPEED, savedHorse);
                    showLevel(player, EasyRider.CONFIG.HEALTH, savedHorse);
                    showLevel(player, EasyRider.CONFIG.JUMP, savedHorse);
                }
            });
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Show information about the current level of an ability, the corresponding
     * attribute value, and the total effort expended for that ability on a
     * specific horse.
     *
     * @param player the player to be send messages.
     * @param abililty the {@link Ability}.
     * @param savedHorse the database state of the horse.
     */
    protected void showLevel(Player player, Ability ability, SavedHorse savedHorse) {
        player.sendMessage(ChatColor.GOLD + ability.getDisplayName() + ": " +
                           ChatColor.WHITE + "Level " + ability.getLevel(savedHorse) +
                           ChatColor.GOLD + " - " +
                           ChatColor.YELLOW + ability.getFormattedValue(savedHorse) +
                           ChatColor.GRAY + " (" + ability.getFormattedEffort(savedHorse) + ")");
    }
} // class HorseLevelExecutor
