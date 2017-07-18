package nu.nerd.easyrider.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractHorse;
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
        final int maxArgs = (sender.hasPermission("easyrider.free-player") ? 2 : 1);

        if (maxArgs == 1 && args.length > 1) {
            sender.sendMessage(ChatColor.RED + "You only have permission to free your own horses. Use the 0 or 1 argument version of this command.");
            return true;
        }

        if (args.length > maxArgs || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be in game to use this command.");
            return true;
        }

        Player sendingPlayer = (Player) sender;
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "Right click on an animal that you own.");
            EasyRider.PLUGIN.getState(sendingPlayer).setPendingInteraction(new IPendingInteraction() {
                @Override
                public void onPlayerInteractEntity(PlayerInteractEntityEvent event, SavedHorse savedHorse) {
                    AbstractHorse abstractHorse = (AbstractHorse) event.getRightClicked();
                    PlayerState playerState = EasyRider.PLUGIN.getState(sendingPlayer);
                    String entityTypeName = Util.entityTypeName(abstractHorse);
                    if (abstractHorse.getOwner() == null) {
                        sender.sendMessage(ChatColor.RED + "Nobody owns that " + entityTypeName + "!");
                    } else if (sendingPlayer.equals(abstractHorse.getOwner()) || playerState.isBypassEnabled()) {
                        EasyRider.DB.freeHorse(savedHorse, abstractHorse);
                        sender.sendMessage(ChatColor.GOLD + "This " + entityTypeName + ", " +
                                           Util.limitString(savedHorse.getUuid().toString(), 20) +
                                           ", has been freed.");
                        sendingPlayer.playSound(sendingPlayer.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    } else {
                        sendingPlayer.playSound(sendingPlayer.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                        sender.sendMessage(ChatColor.RED + "You don't own that " + entityTypeName + "!");
                        if (sender.hasPermission("easyrider.bypass")) {
                            sender.sendMessage(ChatColor.RED + "Use /horse-bypass to bypass access checks.");
                        }
                    }
                }
            });
        } else {
            // Free a horse remotely by UUID.
            String uuidArg;
            OfflinePlayer owningPlayer;
            if (args.length == 2) {
                owningPlayer = Bukkit.getOfflinePlayer(args[0]);
                if (owningPlayer == null) {
                    sender.sendMessage(ChatColor.RED + "There is no known player named " + args[0] + ".");
                    sendingPlayer.playSound(sendingPlayer.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                    return true;
                }

                uuidArg = args[1];
            } else {
                owningPlayer = sendingPlayer;
                uuidArg = args[0];
            }

            ArrayList<SavedHorse> horses = EasyRider.DB.getOwnedHorses(owningPlayer);
            List<SavedHorse> found = horses.stream()
            .filter(h -> h.getUuid().toString().toLowerCase().startsWith(uuidArg))
            .collect(Collectors.toList());
            if (found.size() == 0) {
                sender.sendMessage(ChatColor.RED + owningPlayer.getName() +
                                   " doesn't own an animal with a UUID that begins with \"" + uuidArg + "\".");
            } else if (found.size() > 1) {
                sender.sendMessage(ChatColor.RED + "The identifier \"" + uuidArg + "\" matches multiple animals.");
            } else {
                SavedHorse savedHorse = found.get(0);
                AbstractHorse abstractHorse = Util.findHorse(savedHorse.getUuid(), savedHorse.getLocation(), 2);
                EasyRider.DB.freeHorse(savedHorse, abstractHorse);
                String entityTypeName = (abstractHorse != null) ? Util.entityTypeName(abstractHorse) : "animal";
                sender.sendMessage(ChatColor.GOLD + "The " + entityTypeName + ", " +
                                   Util.limitString(savedHorse.getUuid().toString(), 20) +
                                   ", has been freed.");
                sendingPlayer.playSound(sendingPlayer.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }
        }

        return true;
    }
} // class HorseFreeExecutor