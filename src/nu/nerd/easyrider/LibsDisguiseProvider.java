package nu.nerd.easyrider;

import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.utilities.parser.DisguiseParser;

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
     *      java.lang.String, java.util.Set)
     */
    @Override
    public boolean applyDisguise(Entity target, String encodedDisguise, Set<Player> players) {
        Disguise disguise = null;
        try {
            disguise = DisguiseParser.parseDisguise(Bukkit.getConsoleSender(), target, encodedDisguise);
        } catch (Throwable ex) {
            Throwable cause = ex.getCause();
            EasyRider.PLUGIN.getLogger().severe("Error applying disguise \"" + encodedDisguise +
                                                "\" to " + target.getUniqueId().toString() + ": " +
                                                (cause != null ? cause.getMessage() : ex.getMessage()));
        }

        if (disguise == null) {
            return false;
        }

        DisguiseAPI.undisguiseToAll(target);
        DisguiseAPI.disguiseToPlayers(target, disguise, players);
        return true;
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