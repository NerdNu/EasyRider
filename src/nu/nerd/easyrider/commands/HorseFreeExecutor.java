package nu.nerd.easyrider.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

import nu.nerd.easyrider.EasyRider;
import nu.nerd.easyrider.IPendingInteraction;
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
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be in game to use this command.");
            return true;
        }

        if (args.length != 0) {
            return false;
        }

        sender.sendMessage(ChatColor.GOLD + "Right click on a horse that you own.");
        Player player = (Player) sender;
        EasyRider.PLUGIN.getState(player).setPendingInteraction(new IPendingInteraction() {
            @Override
            public void onPlayerInteractEntity(PlayerInteractEntityEvent event, SavedHorse savedHorse) {
                Horse horse = (Horse) event.getRightClicked();
                if (player.equals(horse.getOwner())) {
                    horse.setOwner(null);
                    horse.setTamed(false);
                    horse.setDomestication(1);
                    for (ItemStack item : horse.getInventory().getContents()) {
                        if (item != null) {
                            horse.getWorld().dropItemNaturally(horse.getLocation(), item);
                            horse.getInventory().remove(item);
                        }
                    }
                    if (horse.isCarryingChest()) {
                        horse.setCarryingChest(false);
                        horse.getWorld().dropItemNaturally(horse.getLocation(), new ItemStack(Material.CHEST));
                    }
                    EasyRider.DB.observe(savedHorse, horse);
                    savedHorse.clearPermittedPlayers();
                    sender.sendMessage(ChatColor.GOLD +
                                       "Horse " + Util.limitString(savedHorse.getUuid().toString(), 20) +
                                       " has been freed.");
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                } else {
                    sender.sendMessage(ChatColor.RED + "You don't own that horse!");
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                }
            }
        });

        return true;
    }
} // class HorseFreeExecutor