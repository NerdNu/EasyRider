package nu.nerd.easyrider.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import nu.nerd.easyrider.EasyRider;
import nu.nerd.easyrider.IPendingInteraction;
import nu.nerd.easyrider.PlayerState;
import nu.nerd.easyrider.Util;
import nu.nerd.easyrider.db.SavedHorse;

// ----------------------------------------------------------------------------
/**
 * Handles the /horse-free command.
 */
public class HorseFreeExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * Default constructor.
     */
    public HorseFreeExecutor() {
        super("horse-free", "help");
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender,
     *      org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length > 1 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be in game to use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "Right click on a horse that you own.");
            EasyRider.PLUGIN.getState(player).setPendingInteraction(new IPendingInteraction() {
                @Override
                public void onPlayerInteractEntity(PlayerInteractEntityEvent event, SavedHorse savedHorse) {
                    Horse horse = (Horse) event.getRightClicked();
                    PlayerState playerState = EasyRider.PLUGIN.getState(player);
                    if (player.equals(horse.getOwner()) || playerState.isBypassEnabled()) {
                        EasyRider.DB.freeHorse(savedHorse, horse);
                        sender.sendMessage(ChatColor.GOLD +
                                           "Horse " + Util.limitString(savedHorse.getUuid().toString(), 20) +
                                           " has been freed.");
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    } else {
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                        sender.sendMessage(ChatColor.RED + "You don't own that horse!");
                        if (sender.hasPermission("easyrider.bypass")) {
                            sender.sendMessage(ChatColor.RED + "Use /horse-bypass to bypass access checks.");
                        }
                    }
                }
            });
        } else {
            // Free a horse remotely by UUID.
            String uuidArg = args[0];
            ArrayList<SavedHorse> horses = EasyRider.DB.getOwnedHorses(player);
            List<SavedHorse> found = horses.stream()
            .filter(h -> h.getUuid().toString().toLowerCase().startsWith(uuidArg))
            .collect(Collectors.toList());
            if (found.size() == 0) {
                sender.sendMessage(ChatColor.RED + "You don't own a horse with a UUID that begins with \"" + uuidArg + "\".");
            } else if (found.size() > 1) {
                sender.sendMessage(ChatColor.RED + "The identifier \"" + uuidArg + "\" matches multiple horses.");
            } else {
                SavedHorse savedHorse = found.get(0);
                Horse horse = Util.findHorse(savedHorse.getUuid(), savedHorse.getLocation(), 2);
                EasyRider.DB.freeHorse(savedHorse, horse);
                sender.sendMessage(ChatColor.GOLD +
                                   "Horse " + Util.limitString(savedHorse.getUuid().toString(), 20) +
                                   " has been freed.");
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }
        }

        return true;
    }
} // class HorseFreeExecutor