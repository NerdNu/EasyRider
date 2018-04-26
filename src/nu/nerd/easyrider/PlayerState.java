package nu.nerd.easyrider;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
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

    // --------------------------------------------------------------------------
    /**
     * Set the maximum speed that any horse ridden by this player will move, in
     * the natural units of a horse's speed attribute.
     *
     * The actual speed that horses move is the lesser of this value and the
     * horse's innate maximum speed.
     *
     * @param maxSpeed the maximum speed that the player can move on any horse.
     */
    public void setMaxSpeed(double maxSpeed) {
        _maxSpeed = Math.min(4 * EasyRider.CONFIG.SPEED.getMaxValue(), Math.max(0, maxSpeed));
    }

    // --------------------------------------------------------------------------
    /**
     * Return the maximum speed that the player can move on any horse, in the
     * natural units of a horse's speed attribute.
     *
     * @return the maximum speed that the player can move on any horse, in the
     *         natural units of a horse's speed attribute.
     */
    public double getMaxSpeed() {
        return _maxSpeed;
    }

    // --------------------------------------------------------------------------
    /**
     * Specify whether the player ignores horse dehydration.
     * 
     * @param neglectful if true, the player ignores horse dehydration.
     */
    public void setNeglectful(boolean neglectful) {
        _neglectful = neglectful;
    }

    // --------------------------------------------------------------------------
    /**
     * Return true if the player ignores horse dehydration.
     * 
     * @return true if the player ignores horse dehydration.
     */
    public boolean isNeglectful() {
        return _neglectful;
    }

    // --------------------------------------------------------------------------
    /**
     * Invalidate the stored last location of the ridden horse, clearing the
     * distance ridden in the last tick to zero.
     */
    public void clearHorseDistance() {
        Entity vehicle = _player.getVehicle();
        _riddenHorse = (vehicle instanceof AbstractHorse) ? (AbstractHorse) vehicle : null;
        _riddenHorseLocation = null;
    }

    // --------------------------------------------------------------------------
    /**
     * Update the last location of the currently ridden horse.
     *
     * If the player is not riding, or the player is riding a horse that is in
     * another vehicle, or if the horse has changed since last tick, clear the
     * location. Otherwise, the player is riding the same horse and its current
     * location is stored.
     */
    public void updateRiddenHorse() {
        Entity vehicle = _player.getVehicle();
        if (vehicle instanceof AbstractHorse) {
            AbstractHorse horse = (AbstractHorse) vehicle;
            if (horse == null || horse.getVehicle() != null || _riddenHorse != horse) {
                _riddenHorse = horse;
                _riddenHorseLocation = null;
            } else {
                _riddenHorseLocation = horse.getLocation();
            }
        }
    }

    // --------------------------------------------------------------------------
    /**
     * Return the distance travelled horizontally since the last call to
     * {@link #updateRiddenHorse()}.
     *
     * The distance will be 0 if the player was not riding a horse in the
     * previous tick, if they just mounted the horse, if they changed horse, or
     * if the horse changed world (e.g. with a portal).
     *
     * @return the horizontal distance travelled.
     */
    public double getTickHorizontalDistance() {
        AbstractHorse currentHorse = (_player.getVehicle() instanceof AbstractHorse) ? (AbstractHorse) _player.getVehicle()
                                                                                     : null;
        if (currentHorse == null || currentHorse != _riddenHorse || _riddenHorseLocation == null) {
            return 0;
        } else {
            return Util.getHorizontalDistance(_riddenHorseLocation, currentHorse.getLocation());
        }
    }

    // --------------------------------------------------------------------------
    /**
     * Toggle horse access permission checks.
     */
    public void toggleBypassEnabled() {
        _bypass = !_bypass;
    }

    // --------------------------------------------------------------------------
    /**
     * Return true if this player can bypass horse access permission checks.
     *
     * @return true if this player can bypass horse access permission checks.
     */
    public boolean isBypassEnabled() {
        return _bypass;
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
        section.set("max-speed", getMaxSpeed());
        section.set("neglectful", isNeglectful());
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

        setMaxSpeed(section.getDouble("max-speed", 4 * EasyRider.CONFIG.SPEED.getMaxValue()));
        setNeglectful(section.getBoolean("neglectful"));
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

    /**
     * The AbstractHorse being ridden in the most recent call to
     * {@link #updateRiddenHorse()}.
     */
    protected AbstractHorse _riddenHorse;

    /**
     * When riding a horse, this is the location of the horse in the previous
     * tick, if the player was riding then too. Otherwise it is null.
     */
    protected Location _riddenHorseLocation;

    /**
     * The player-set maximum speed that any horse ridden by this player will
     * move, in the natural units of the speed attribute.
     *
     * The actual speed that horses move is the lesser of this value and the
     * horse's innate maximum speed.
     */
    protected double _maxSpeed;

    /**
     * If true, the player can bypass horse access permission checks.
     */
    protected boolean _bypass;

    /**
     * If true, the player ignores horse dehydration.
     */
    protected boolean _neglectful;
} // class PlayerState