package nu.nerd.easyrider;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import nu.nerd.easyrider.db.SavedHorse;

// ----------------------------------------------------------------------------
/**
 * Transient, per-player state, created on join and removed when the player
 * leaves.
 */
public class PlayerState {
    /**
     * Constructor.
     *
     * @param player the player.
     * @param config the configuration from which player preferences are loaded.
     */
    public PlayerState(Player player, YamlConfiguration config) {
        _player = player;
        load(config);
    }

    // ------------------------------------------------------------------------
    /**
     * Store an {@link IPendingInteraction} that should be handled when this
     * player next interacts with a horse.
     * 
     * Any previous pending interaction is overridden. The pending interaction
     * is not stored in the player configuration and so does not survive
     * restarts or relogs. That is the desired behaviour.
     * 
     * @param interaction the pending interaction.
     */
    public void setPendingInteraction(IPendingInteraction interaction) {
        _pendingInteraction = interaction;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if there is a pending interaction.
     * 
     * @return true if there is a pending interaction.
     */
    public boolean hasPendingInteraction() {
        return _pendingInteraction != null;
    }

    // ------------------------------------------------------------------------
    /**
     * Handle a pending interaction (if there is one) by passing it a
     * PlayerInteractEntityEvent, and then clear the pending interaction.
     * 
     * @param event the event.
     * @param savedHorse the database state of the horse.
     */
    public void handlePendingInteraction(PlayerInteractEntityEvent event, SavedHorse savedHorse) {
        if (hasPendingInteraction() && event.getPlayer() == _player) {
            _pendingInteraction.onPlayerInteractEntity(event, savedHorse);
            _pendingInteraction = null;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Save this player's preferences to the specified configuration.
     *
     * @param config the configuration to update.
     */
    public void save(YamlConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection(_player.getUniqueId().toString());
        section.set("name", _player.getName());
    }

    // ------------------------------------------------------------------------
    /**
     * Load the Player's preferences from the specified configuration
     *
     * @param config the configuration from which player preferences are loaded.
     */
    protected void load(YamlConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection(_player.getUniqueId().toString());
        if (section == null) {
            section = config.createSection(_player.getUniqueId().toString());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * The Player.
     */
    protected Player _player;

    /**
     * Pending interaction with horse.
     */
    protected IPendingInteraction _pendingInteraction;
} // class PlayerState