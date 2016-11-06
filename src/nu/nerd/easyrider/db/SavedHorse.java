package nu.nerd.easyrider.db;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.avaje.ebean.validation.NotNull;

// ----------------------------------------------------------------------------
/**
 * Ebean for storing information about a horse.
 */
@Entity()
@Table(name = "horses")
public class SavedHorse {
    // ------------------------------------------------------------------------
    /**
     * Default constructor, required for deserialisation.
     */
    public SavedHorse() {
    }

    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param uuid the UUID of the horse.
     */
    public SavedHorse(UUID uuid) {
        setUuid(uuid);
        speedLevel = jumpLevel = healthLevel = 1;
    }

    // ------------------------------------------------------------------------
    /**
     * Signify that this bean has unsaved changes.
     */
    public void setDirty() {
        _dirty = true;
    }

    // ------------------------------------------------------------------------
    /**
     * Signify that this bean does not have any unsaved changes.
     */
    public void setClean() {
        _dirty = true;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if this bean has unsaved changes.
     * 
     * @return true if this bean has unsaved changes.
     */
    public boolean isDirty() {
        return _dirty;
    }

    // ------------------------------------------------------------------------
    /**
     * Specify whether this horse has been marked for debug logging.
     * 
     * @param debug if true, various events concerning this horse will be
     *        logged.
     */
    public void setDebug(boolean debug) {
        _debug = debug;

    }

    // ------------------------------------------------------------------------
    /**
     * Return true if this horse has been marked for debug logging.
     * 
     * @return true if this horse has been marked for debug logging.
     */
    public boolean isDebug() {
        return _debug;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the stored UUID of the horse.
     * 
     * @param uuid the UUID.
     */
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
        setDirty();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the stored UUID of the horse.
     * 
     * @return the stored UUID of the horse.
     */
    public UUID getUuid() {
        return uuid;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the total distance travelled in metres.
     * 
     * @param distanceTravelled the distance in metres.
     */
    public void setDistanceTravelled(double distanceTravelled) {
        this.distanceTravelled = distanceTravelled;
        setDirty();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the total distance travelled in metres.
     * 
     * @return the total distance travelled in metres.
     */
    public double getDistanceTravelled() {
        return distanceTravelled;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the total horizontal distance jumped, in metres.
     * 
     * @param distanceJumped the horizontal distance in metres.
     */
    public void setDistanceJumped(double distanceJumped) {
        this.distanceJumped = distanceJumped;
        setDirty();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the total horizontal distance jumped, in metres.
     * 
     * @return the total horizontal distance jumped, in metres.
     */
    public double getDistanceJumped() {
        return distanceJumped;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the total amount of gold consumed, in gold nuggets.
     * 
     * @param nuggetsEaten the amount of gold, measured in nuggets.
     */
    public void setNuggetsEaten(int nuggetsEaten) {
        this.nuggetsEaten = nuggetsEaten;
        setDirty();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the total amount of gold consumed, in gold nuggets.
     * 
     * @return the total amount of gold consumed, in gold nuggets.
     */
    public int getNuggetsEaten() {
        return nuggetsEaten;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the 1-based level that determines the horse's speed.
     * 
     * @param level the new level.
     */
    public void setSpeedLevel(int level) {
        this.speedLevel = level;
        setDirty();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the 1-based level that determines the horse's speed.
     * 
     * @return the 1-based level that determines the horse's speed.
     */
    public int getSpeedLevel() {
        return speedLevel;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the 1-based level that determines the horse's jump strength.
     * 
     * @param level the new level.
     */
    public void setJumpLevel(int level) {
        this.jumpLevel = level;
        setDirty();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the 1-based level that determines the horse's jump strength.
     * 
     * @return the 1-based level that determines the horse's jump strength.
     */
    public int getJumpLevel() {
        return jumpLevel;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the 1-based level that determines the horse's health.
     * 
     * @param level the new level.
     */
    public void setHealthLevel(int level) {
        this.healthLevel = level;
        setDirty();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the 1-based level that determines the horse's health.
     * 
     * @return the 1-based level that determines the horse's health.
     */
    public int getHealthLevel() {
        return healthLevel;
    }

    // ------------------------------------------------------------------------
    /**
     * The unique ID of the horse, used as the primary key.
     */
    @Id
    private UUID uuid;

    /**
     * The total distance travelled in metres.
     */
    @NotNull
    private double distanceTravelled;

    /**
     * The total horizontal distance jumped in metres.
     */
    @NotNull
    private double distanceJumped;

    /**
     * The total amount of gold consumed, converted to gold nuggets.
     */
    @NotNull
    private int nuggetsEaten;

    /**
     * The 1-based level that determines the horse's speed.
     */
    @NotNull
    private int speedLevel;

    /**
     * The 1-based level that determines the horse's jump strength.
     */
    @NotNull
    private int jumpLevel;

    /**
     * The 1-based level that determines the horse's health.
     */
    @NotNull
    private int healthLevel;

    /**
     * True if this bean has unsaved changes.
     */
    @Transient
    private boolean _dirty;

    /**
     * If true, this horse has been marked for debug logging.
     */
    @Transient
    private boolean _debug;

} // class SavedHorse