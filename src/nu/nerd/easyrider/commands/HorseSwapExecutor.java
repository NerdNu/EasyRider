package nu.nerd.easyrider.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractHorse;
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
            if (matches.size() == 0) {
                sender.sendMessage(ChatColor.RED + "That UUID does not match any animals.");
            } else if (matches.size() == 1) {
                SavedHorse originalHorse = matches.get(0);
                if (originalHorse.isTrainable()) {
                    player.sendMessage(ChatColor.GOLD + "Right click on the horse to swap stats with " +
                                       originalHorse.getUuid().toString() + ".");
                    EasyRider.PLUGIN.getState(player).setPendingInteraction(new IPendingInteraction() {
                        @Override
                        public void onPlayerInteractEntity(PlayerInteractEntityEvent event, SavedHorse newHorse) {
                            if (newHorse.isTrainable()) {
                                // The original horse may not be loaded; may
                                // even have been vaporised mysteriously. If it
                                // is interacted with or ridden later, its
                                // attributes will be updated at that time.
                                originalHorse.setOutdatedAttributes(true);
                                newHorse.swapTrainingStats(originalHorse);
                                newHorse.updateAllAttributes((AbstractHorse) event.getRightClicked());
                                sender.sendMessage(ChatColor.GOLD + "Horse " +
                                                   originalHorse.getUuid() + " has swapped stats with " +
                                                   newHorse.getUuid() + ".");
                                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.NEUTRAL, 1.0f, 1.0f);
                            } else {
                                player.sendMessage(ChatColor.RED + "That animal is not trainable.");
                                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.NEUTRAL, 1.0f, 1.0f);
                            }
                        }
                    });
                } else {
                    player.sendMessage(ChatColor.RED + "That animal is not trainable.");
                }
            } else {
                if (matches.size() > 10) {
                    sender.sendMessage(ChatColor.RED + "The prefix " + prefix + " matches more than 10 animals. ");
                } else {
                    sender.sendMessage(ChatColor.RED + "The prefix " + prefix + " matches the following animals: ");
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
