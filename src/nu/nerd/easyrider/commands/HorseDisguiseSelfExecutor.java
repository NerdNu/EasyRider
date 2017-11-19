package nu.nerd.easyrider.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import me.libraryaddict.disguise.DisguiseAPI;
import nu.nerd.easyrider.Util;

// ----------------------------------------------------------------------------
/**
 * Handle the /horse-disguise command.
 */
public class HorseDisguiseSelfExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * Constructor.
     */
    public HorseDisguiseSelfExecutor() {
        super("horse-disguise-self", "help");
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender,
     *      org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be in game to use this command.");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
            return false;
        }

        if (args.length != 0) {
            sender.sendMessage(ChatColor.RED + "Invalid arguments. Try /" + label + " help.");
            return true;
        }

        Player player = (Player) sender;
        if (player.getVehicle() instanceof AbstractHorse) {
            AbstractHorse abstractHorse = (AbstractHorse) player.getVehicle();
            // Toggle disguise.
            EntityType disguiseEntityType = Util.getSaddleDisguiseType(abstractHorse);
            if (disguiseEntityType == null) {
                sender.sendMessage(ChatColor.RED + "You must be riding a horse with a disguise saddle to see the disguise!");
                return true;
            }

            boolean showToRider = !Util.isSaddleDisguiseVisibleToRider(abstractHorse);
            if (showToRider) {
                sender.sendMessage(ChatColor.GOLD + "You will now be able to see your steed's disguise.");
                sender.sendMessage(ChatColor.GOLD + "Due to limitations in the Minecraft client, you can't move.");
                sender.sendMessage(ChatColor.GOLD + "Run this command again to allow movement.");
            } else {
                sender.sendMessage(ChatColor.GOLD + "You can no longer see your steed's disguise.");
            }
            DisguiseAPI.undisguiseToAll(abstractHorse);
            Util.applySaddleDisguise(abstractHorse, player, disguiseEntityType, showToRider);

        } else {
            sender.sendMessage(ChatColor.RED + "You must be riding a horse with a disguise saddle to see the disguise!");
        }

        return true;
    }
} // class HorseDisguiseSelfExecutor