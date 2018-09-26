package nu.nerd.easyrider;

import java.util.Set;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;

// ----------------------------------------------------------------------------
/**
 * A {@link DisguiseProvider} implementation using LibsDisguises.
 * 
 * In the absence of the LibsDisguises plugin, this class is never resolved by
 * the JVM, and the lack of the LibsDisguises class files causes no problems.
 */
public class LibsDisguiseProvider implements DisguiseProvider {
    // --------------------------------------------------------------------------
    /**
     * @see nu.nerd.easyrider.DisguiseProvider#applyDisguise(org.bukkit.entity.Entity,
     *      org.bukkit.entity.EntityType, java.util.Set)
     */
    @Override
    public boolean applyDisguise(Entity target, EntityType disguiseEntityType, Set<Player> players) {
        DisguiseType disguiseType = DisguiseType.getType(disguiseEntityType);
        boolean validType = (disguiseType != null);
        if (validType) {
            MobDisguise disguise = new MobDisguise(disguiseType);
            DisguiseAPI.undisguiseToAll(target);
            DisguiseAPI.disguiseToPlayers(target, disguise, players);
        }
        return validType;
    }

    // ------------------------------------------------------------------------
    /**
     * @see nu.nerd.easyrider.DisguiseProvider#removeDisguise(org.bukkit.entity.Entity)
     */
    @Override
    public void removeDisguise(Entity target) {
        DisguiseAPI.undisguiseToAll(target);
    }
} // class LibsDisguiseProvider