package nu.nerd.easyrider.commands;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import nu.nerd.easyrider.Ability;
import nu.nerd.easyrider.EasyRider;
import nu.nerd.easyrider.HorseEquipment;
import nu.nerd.easyrider.IPendingInteraction;
import nu.nerd.easyrider.Util;
import nu.nerd.easyrider.db.SavedHorse;

// ----------------------------------------------------------------------------
/**
 * Executor for the /horse-next command.
 */
public class HorseNextExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * Constructor.
     */
    public HorseNextExecutor() {
        super("horse-next", "help");
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

        if (args.length == 0) {
            Player player = (Player) sender;
            if (player.getVehicle() instanceof AbstractHorse) {
                showNext(player, (AbstractHorse) player.getVehicle());
            } else {
                sender.sendMessage(ChatColor.GOLD + "Right click on an animal to show upgrade information.");
                EasyRider.PLUGIN.getState(player).setPendingInteraction(new IPendingInteraction() {
                    @Override
                    public void onPlayerInteractEntity(PlayerInteractEntityEvent event, SavedHorse savedHorse) {
                        showNext(event.getPlayer(), (AbstractHorse) event.getRightClicked());
                    }
                });
            }
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Send the player information about the current levels and next upgrade of
     * the specified horse.
     *
     * @param player the player.
     * @param abtractHorse the AbstractHorse.
     */
    protected void showNext(Player player, AbstractHorse abstractHorse) {
        SavedHorse savedHorse = EasyRider.DB.findOrAddHorse(abstractHorse);
        String type = Util.capitalise(Util.entityTypeName(abstractHorse));
        player.sendMessage(ChatColor.GOLD + type + ": " + ChatColor.YELLOW + abstractHorse.getUniqueId());
        AnimalTamer owner = abstractHorse.getOwner();
        String ownerName = (owner != null) ? owner.getName() : "<no owner>";
        player.sendMessage(ChatColor.GOLD + "Owner: " + ChatColor.YELLOW + ownerName);
        player.sendMessage(ChatColor.GOLD + "Appearance: " +
                           ChatColor.WHITE + Util.getAppearance(abstractHorse) + " " +
                           ChatColor.YELLOW + HorseEquipment.description(HorseEquipment.bits(abstractHorse)));
        if (Util.isTrainable(abstractHorse)) {
            showLevel(player, EasyRider.CONFIG.SPEED, savedHorse);
            showLevel(player, EasyRider.CONFIG.HEALTH, savedHorse);
            showLevel(player, EasyRider.CONFIG.JUMP, savedHorse);
            HorseInfoExecutor.showHydration(player, savedHorse);
        } else {
            player.sendMessage(ChatColor.RED + "That " + Util.entityTypeName(abstractHorse) + " cannot be trained.");
        }
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }

    // ------------------------------------------------------------------------
    /**
     * Show information about the current level of an ability and the effort
     * required to reach the next level.
     *
     * When the maximum possible level is exceeded by training, colour the level
     * text red.
     *
     * @param player the player to be send messages.
     * @param abililty the {@link Ability}.
     * @param savedHorse the database state of the horse.
     */
    protected void showLevel(Player player, Ability ability, SavedHorse savedHorse) {
        double fractionalLevel = ability.getFractionalLevel(savedHorse);
        int intLevel = (int) fractionalLevel;
        boolean atMaximum = (intLevel >= ability.getMaxLevel());
        ChatColor levelColour = (atMaximum ? ChatColor.RED : ChatColor.WHITE);
        String nextEffort;
        if (atMaximum) {
            nextEffort = ChatColor.RED + "Maximum";
        } else {
            int nextLevel = intLevel + 1;
            double requiredEffort = ability.getEffortForLevel(nextLevel) - ability.getEffort(savedHorse);
            nextEffort = ChatColor.YELLOW + ability.formatEffort(requiredEffort) +
                         ChatColor.WHITE + " to Level " + nextLevel;
        }

        player.sendMessage(ChatColor.GOLD + ability.getDisplayName() + ": " +
                           levelColour + "Level " + String.format("%5.3f", fractionalLevel) +
                           ChatColor.GOLD + " - " + nextEffort);
    }
} // class HorseNextExecutor