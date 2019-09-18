package nu.nerd.easyrider.commands;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.ChatColor;
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
 * Executor for the /horse-set-name admin command.
 */
public class HorseSetNameExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * Default constructor.
     */
    public HorseSetNameExecutor() {
        super("horse-set-name");
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender,
     *      org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be in game to use this command.");
            return true;
        }

        String newName = ChatColor.translateAlternateColorCodes('&', Stream.of(args).collect(Collectors.joining(" ")));
        sender.sendMessage(ChatColor.GOLD + "Right click on a horse to set its name to: " + ChatColor.WHITE + newName);
        sender.sendMessage(ChatColor.GOLD + "To cancel, relog.");

        Player player = (Player) sender;
        EasyRider.PLUGIN.getState(player).setPendingInteraction(new IPendingInteraction() {
            @Override
            public void onPlayerInteractEntity(PlayerInteractEntityEvent event, SavedHorse savedHorse) {
                AbstractHorse abstractHorse = (AbstractHorse) event.getRightClicked();
                String oldName = abstractHorse.getCustomName() != null ? abstractHorse.getCustomName() : "";
                abstractHorse.setCustomName(newName);

                Player player = event.getPlayer();
                player.sendMessage(ChatColor.GOLD + "Horse: " + ChatColor.YELLOW + abstractHorse.getUniqueId());
                player.sendMessage(ChatColor.GOLD + "Old name: " + ChatColor.WHITE + oldName);
                player.sendMessage(ChatColor.GOLD + "New name: " + ChatColor.WHITE + newName);
            }
        });
        return true;
    } // onCommand
} // class HorseSetNameExecutor