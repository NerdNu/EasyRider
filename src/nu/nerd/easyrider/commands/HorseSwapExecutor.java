package nu.nerd.easyrider.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import nu.nerd.easyrider.EasyRider;
import nu.nerd.easyrider.IPendingInteraction;
import nu.nerd.easyrider.db.SavedHorse;

// ----------------------------------------------------------------------------
/**
 * Executor for the /horse-swap command.
 */
public class HorseSwapExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * Default constructor.
     */
    public HorseSwapExecutor() {
        super("horse-swap", "help");
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
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("help")) {
                return false;
            }

            String prefix = args[0];
            List<SavedHorse> matches = EasyRider.DB.findHorsesByUUID(prefix);
            if (matches.size() == 1) {
                SavedHorse originalHorse = matches.get(0);
                player.sendMessage(ChatColor.GOLD + "Right click on the horse to swap stats with " +
                                   originalHorse.getUuid().toString() + ".");
                EasyRider.PLUGIN.getState(player).setPendingInteraction(new IPendingInteraction() {
                    @Override
                    public void onPlayerInteractEntity(PlayerInteractEntityEvent event, SavedHorse newHorse) {
                        // The original horse may not be loaded; may even have
                        // been vaporised mysteriously. If it is interacted with
                        // or ridden later, its attributes will be updated at
                        // that time.
                        originalHorse.setOutdatedAttributes(true);
                        newHorse.swapTrainingStats(originalHorse);
                        newHorse.updateAllAttributes((Horse) event.getRightClicked());
                        sender.sendMessage(ChatColor.GOLD + "Horse " +
                                           originalHorse.getUuid() + " has swapped stats with " +
                                           newHorse.getUuid() + ".");
                    }
                });
            } else {
                if (matches.size() > 10) {
                    sender.sendMessage(ChatColor.RED + "The prefix " + prefix + " matches more than 10 horses. ");
                } else {
                    sender.sendMessage(ChatColor.RED + "The prefix " + prefix + " matches the following horses: ");
                    for (SavedHorse savedHorse : matches) {
                        sender.sendMessage(ChatColor.RED + savedHorse.getUuid().toString());
                    }
                }
                sender.sendMessage(ChatColor.RED + "You need to specify more of the UUID.");
            }
            return true;
        }
        return false;
    }
} // class HorseSwapExecutor