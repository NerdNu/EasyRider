package nu.nerd.easyrider;

import org.bukkit.event.player.PlayerInteractEntityEvent;

import nu.nerd.easyrider.db.SavedHorse;

// ----------------------------------------------------------------------------
/**
 * Records pending processing of a PlayerInteractEntityEvent for a specific
 * player.
 * 
 * This is the Command Object design pattern. The pending interaction is stored
 * with the PlayerState of the Player who initiated it (with a command), and
 * called from the plugin's PlayerInteractEntityEvent handler when a matching
 * event for the owning player is processed.
 */
public interface IPendingInteraction {
    /**
     * This method is called when a {@link PlayerState} is holding an instance
     * of this interface and event.getPlayer() == PlayerState.getPlayer().
     * 
     * @param event the entity interaction event.
     * @param savedHorse the persistent state of the horse in the event.
     */
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event, SavedHorse savedHorse);
} // class IPendingInteraction