package nu.nerd.easyrider;

import java.util.Set;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

// ----------------------------------------------------------------------------
/**
 * Interface implemented by classes that interface to plugins that disguise
 * mobs.
 * 
 * Implementations of this interface allow EasyRider to function even when
 * LibsDisguises is unavailable.
 */
public interface DisguiseProvider {
    // ------------------------------------------------------------------------
    /**
     * Apply the specified disguise to the target entity.
     * 
     * @param target the target that becomes disguised.
     * @param encodedDisguise the string-encoded disguise.
     * @param players the set of Players that will see the disguise.
     * @return true if the disguise type is supported.
     */
    public boolean applyDisguise(Entity target, String encodedDisguise, Set<Player> players);

    // ------------------------------------------------------------------------
    /**
     * Remove any disguise on the specified target entity.
     * 
     * @param target the target that becomes disguised.
     */
    public void removeDisguise(Entity target);

} // class DisguiseProvider