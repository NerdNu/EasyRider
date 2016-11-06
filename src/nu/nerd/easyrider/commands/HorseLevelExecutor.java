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
                    Player p = event.getPlayer();
                    p.sendMessage(ChatColor.GOLD + "Horse: " + ChatColor.YELLOW + horse.getUniqueId());

                }
            });

            return true;
        } else {
            return false;
        }
    }

    protected void showLevel(Player player, Ability ability, SavedHorse savedHorse) {

        player.sendMessage(ChatColor.GOLD + "Speed: " +
                           ChatColor.GRAY + "Level " + savedHorse.getSpeedLevel() +
                           ChatColor.GOLD + " - " + ChatColor.YELLOW + horse.getUniqueId());

    }

} // class HorseLevelExecutor
