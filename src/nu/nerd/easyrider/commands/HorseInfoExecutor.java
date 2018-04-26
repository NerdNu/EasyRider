package nu.nerd.easyrider.commands;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import nu.nerd.easyrider.Ability;
import nu.nerd.easyrider.EasyRider;
import nu.nerd.easyrider.HorseEquipment;
import nu.nerd.easyrider.IPendingInteraction;
import nu.nerd.easyrider.Util;
import nu.nerd.easyrider.db.SavedHorse;

//-----------------------------------------------------------------------------
/**
 * CommandExecutor implementation for the /horse-info command.
 */
public class HorseInfoExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * Default constructor.
     */
    public HorseInfoExecutor() {
        super("horse-info", "help");
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
            if (player.getVehicle() instanceof AbstractHorse) {
                showLevels(player, (AbstractHorse) player.getVehicle());
            } else {
                sender.sendMessage(ChatColor.GOLD + "Right click on an animal to show level information.");
                EasyRider.PLUGIN.getState(player).setPendingInteraction(new IPendingInteraction() {
                    @Override
                    public void onPlayerInteractEntity(PlayerInteractEntityEvent event, SavedHorse savedHorse) {
                        showLevels(event.getPlayer(), (AbstractHorse) event.getRightClicked());
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
     * Show the horse's hydration level to the player.
     * 
     * @param player the player.
     * @param savedHorse the horse.
     */
    public static void showHydration(Player player, SavedHorse savedHorse) {
        int hydration = (int) Math.round(100 * savedHorse.getHydration());
        player.sendMessage(ChatColor.GOLD + "Hydration: " + (hydration < 5 ? ChatColor.RED : ChatColor.YELLOW) + hydration);
    }

    // ------------------------------------------------------------------------
    /**
     * Send the player information about the current levels of the specified
     * horse or the attributes of a llama.
     *
     * @param player the player.
     * @param asbtractHorse the AbstractHorse.
     */
    protected void showLevels(Player player, AbstractHorse abstractHorse) {
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
            showHydration(player, savedHorse);
        } else if (abstractHorse instanceof Llama) {
            Llama llama = (Llama) abstractHorse;
            showAttribute(player, EasyRider.CONFIG.SPEED, abstractHorse);
            showAttribute(player, EasyRider.CONFIG.HEALTH, abstractHorse);
            showAttribute(player, EasyRider.CONFIG.JUMP, abstractHorse);
            player.sendMessage(ChatColor.GOLD + "Strength: " + ChatColor.GRAY + llama.getStrength());
        }
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }

    // ------------------------------------------------------------------------
    /**
     * Show information about the current level of an ability, the corresponding
     * attribute value, and the total effort expended for that ability on a
     * specific horse.
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
        ChatColor levelColour = (fractionalLevel >= ability.getMaxLevel()) ? ChatColor.RED : ChatColor.WHITE;
        player.sendMessage(ChatColor.GOLD + ability.getDisplayName() + ": " +
                           levelColour + "Level " + String.format("%5.3f", fractionalLevel) +
                           ChatColor.GOLD + " - " +
                           ChatColor.YELLOW + ability.getFormattedValue(savedHorse) +
                           ChatColor.GRAY + " (" + ability.getFormattedEffort(savedHorse) + ")");
    }

    // ------------------------------------------------------------------------
    /**
     * Show the attribute value in a specified Ability of an AbstractHorse.
     *
     * @param player the player to message.
     * @param ability the Ability.
     * @param abstractHorse the horse-like entity.
     */
    protected void showAttribute(Player player, Ability ability, AbstractHorse abstractHorse) {
        double attributeValue = ability.getAttribute(abstractHorse);
        double displayValue = ability.toDisplayValue(attributeValue);
        String formattedValue = ability.formatValue(displayValue);
        player.sendMessage(ChatColor.GOLD + ability.getDisplayName() + ": " + ChatColor.GRAY + formattedValue);
    }
} // class HorseInfoExecutor
