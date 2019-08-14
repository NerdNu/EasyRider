package nu.nerd.easyrider.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;

import nu.nerd.easyrider.EasyRider;
import nu.nerd.easyrider.SpecialSaddles;

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

        if (EasyRider.PLUGIN.getDisguiseProvider() == null) {
            sender.sendMessage(ChatColor.RED + "Disguise saddles are not enabled!");
            return true;
        }

        Player player = (Player) sender;
        if (player.getVehicle() instanceof AbstractHorse) {
            AbstractHorse abstractHorse = (AbstractHorse) player.getVehicle();
            // Toggle disguise.
            String encodedDisguise = SpecialSaddles.getSaddleEncodedDisguise(abstractHorse);
            if (encodedDisguise == null) {
                sender.sendMessage(ChatColor.RED + "You must be riding a horse with a disguise saddle to see the disguise!");
                return true;
            }

            boolean showToRider = !SpecialSaddles.isSaddleDisguiseVisibleToRider(abstractHorse);
            if (showToRider) {
                sender.sendMessage(ChatColor.GOLD + "You will now be able to see your steed's disguise.");
                sender.sendMessage(ChatColor.GOLD + "Due to limitations in the Minecraft client, you can't move.");
                sender.sendMessage(ChatColor.GOLD + "Run this command again to allow movement.");
            } else {
                sender.sendMessage(ChatColor.GOLD + "You can no longer see your steed's disguise.");
            }
            EasyRider.PLUGIN.getDisguiseProvider().removeDisguise(abstractHorse);
            SpecialSaddles.applySaddleDisguise(abstractHorse, player, encodedDisguise, showToRider, true);

        } else {
            sender.sendMessage(ChatColor.RED + "You must be riding a horse with a disguise saddle to see the disguise!");
        }

        return true;
    }
} // class HorseDisguiseSelfExecutor