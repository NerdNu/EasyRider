package nu.nerd.easyrider;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

// ----------------------------------------------------------------------------
/**
 * Namespace class for holding utility methods pertaining to special saddles.
 */
public class SpecialSaddles {
    // ------------------------------------------------------------------------
    /**
     * Get the ItemStack in the saddle slot of a horse, donkey or mule.
     * 
     * @param abstractHorse the horse-like entity (includes llamas).
     * @return the ItemStack in the saddle slot, or null if there is no saddle
     *         slot (as in the case of llamas).
     */
    public static ItemStack getSaddleItemStack(AbstractHorse abstractHorse) {
        if (abstractHorse instanceof Llama) {
            return null;
        }

        // Mules, donkeys, zombie- and skeletal- horses do not have a
        // HorseInventory. So get the saddle by ID rather than calling
        // HorseInventory.getSaddle().
        return abstractHorse.getInventory().getItem(0);
    }

    // ------------------------------------------------------------------------
    /**
     * Re-apply disguises to all disguised steeds when a player joins.
     */
    public static void refreshSaddleDisguises() {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getVehicle() instanceof AbstractHorse) {
                AbstractHorse abstractHorse = (AbstractHorse) onlinePlayer.getVehicle();
                EntityType disguiseEntityType = SpecialSaddles.getSaddleDisguiseType(abstractHorse);
                if (disguiseEntityType != null) {
                    boolean showToRider = SpecialSaddles.isSaddleDisguiseVisibleToRider(abstractHorse);
                    SpecialSaddles.applySaddleDisguise(abstractHorse, onlinePlayer, disguiseEntityType, showToRider, false);
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Disguise a horse as a specified EntityType and notify a player when it is
     * still disguised.
     * 
     * @param abstractHorse the horse-like entity.
     * @param rider the rider, to be notified if a disguise is applied.
     * @param disguiseEntityType the EntityType of the disguise.
     * @param showToRider if true, the disguise is visible to the rider.
     * @param tellRider if true, tell the rider what disguise is in use.
     */
    public static void applySaddleDisguise(AbstractHorse abstractHorse, Player rider, EntityType disguiseEntityType,
                                           boolean showToRider, boolean tellRider) {
        if (disguiseEntityType == null || EasyRider.PLUGIN.getDisguiseProvider() == null) {
            return;
        }

        Set<Player> players = new HashSet<>(Bukkit.getOnlinePlayers());
        if (!showToRider) {
            players.remove(rider);
        }
        boolean validDisguise = EasyRider.PLUGIN.getDisguiseProvider().applyDisguise(abstractHorse, disguiseEntityType, players);
        if (validDisguise) {
            if (tellRider) {
                rider.sendMessage(ChatColor.GOLD + "Your steed is disguised as " + disguiseEntityType + "!");
            }

            abstractHorse.removeMetadata(SpecialSaddles.SELF_DISGUISE_KEY, EasyRider.PLUGIN);
            if (showToRider) {
                abstractHorse.setMetadata(SpecialSaddles.SELF_DISGUISE_KEY, new FixedMetadataValue(EasyRider.PLUGIN, null));
            }
        } else {
            Logger logger = EasyRider.PLUGIN.getLogger();
            logger.warning("Horse " + abstractHorse.getUniqueId() + " accessed by " + rider.getName() +
                           " has a saddle with unsupported disguise " + disguiseEntityType + ".");
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the saddle disguise of the specified horse is visible to
     * the rider.
     * 
     * @param abstractHorse the horse-like entity.
     * @return true if the saddle disguise of the specified horse is visible to
     *         the rider.
     */
    public static boolean isSaddleDisguiseVisibleToRider(AbstractHorse abstractHorse) {
        return !abstractHorse.getMetadata(SpecialSaddles.SELF_DISGUISE_KEY).isEmpty();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the EntityType of the disguise associated with a horse's saddle,
     * or null if the saddle doesn't confer a disguise (or it's not a saddle).
     * 
     * @param abstractHorse the horse-like entity.
     * @return the EntityType of the disguise, or null if no disguise should be
     *         applied.
     */
    public static EntityType getSaddleDisguiseType(AbstractHorse abstractHorse) {
        ItemStack saddle = SpecialSaddles.getSaddleItemStack(abstractHorse);
        if (saddle == null || saddle.getType() != Material.SADDLE) {
            return null;
        }

        ItemMeta meta = saddle.getItemMeta();
        if (meta != null && meta.hasLore()) {
            for (String lore : meta.getLore()) {
                if (lore.startsWith(EasyRider.DISGUISE_PREFIX)) {
                    String entityTypeName = lore.substring(EasyRider.DISGUISE_PREFIX.length()).trim().toUpperCase();
                    try {
                        return EntityType.valueOf(entityTypeName);
                    } catch (IllegalArgumentException ex) {
                    }
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------
    /**
     * Metadata key for metadata signifying that a saddle disguise is visible to
     * the rider.
     * 
     * If metadata with this key is absent, the rider cannot see the disguise.
     */
    static final String SELF_DISGUISE_KEY = "EasyRider_self_disguise";

} // class SpecialSaddles
