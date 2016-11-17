package nu.nerd.easyrider.db;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;

import com.avaje.ebean.validation.NotNull;

import nu.nerd.easyrider.EasyRider;
import nu.nerd.easyrider.Util;

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
     * @param horse the Horse entity.
     */
    public SavedHorse(Horse horse) {
        setNew();
        setUuid(horse.getUniqueId());
        AnimalTamer owner = horse.getOwner();
        setOwnerUuid((owner != null) ? owner.getUniqueId() : null);
        setAppearance(Util.getAppearance(horse));
        speedLevel = jumpLevel = healthLevel = 1;
        setHydration(0.5);
    }

    // ------------------------------------------------------------------------
    /**
     * Mark this as a new instance to be inserted into the database.
     */
    public void setNew() {
        _new = true;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if this is a new instance to be inserted into the database.
     *
     * @return true if this is a new instance to be inserted into the database.
     */
    public boolean isNew() {
        return _new;
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
        _dirty = _new = false;
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
     * Set the UUID of the owner of this horse.
     *
     * @param ownerUuid the owning player's UUID, or null if not owned.
     */
    public void setOwnerUuid(UUID ownerUuid) {
        // Minimise setDirty() calls.
        if (this.ownerUuid == null) {
            if (ownerUuid == null) {
                return;
            }
        } else if (this.ownerUuid.equals(ownerUuid)) {
            return;
        }

        this.ownerUuid = ownerUuid;
        setDirty();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the UUID of the owner of this horse, or null if not owned.
     *
     * @return the UUID of the owner of this horse, or null if not owned.
     */
    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the owner of this horse, or null if not owned.
     *
     * @param owner the owner or null.
     */
    public void setOwner(AnimalTamer owner) {
        setOwnerUuid((owner == null) ? null : owner.getUniqueId());
    }

    // ------------------------------------------------------------------------
    /**
     * Return the owner of this horse, or null if not owned.
     *
     * @return the owner of this horse, or null if not owned.
     */
    public OfflinePlayer getOwner() {
        return (ownerUuid != null) ? Bukkit.getOfflinePlayer(ownerUuid) : null;
    }

    // ------------------------------------------------------------------------
    /**
     * Reserved for future use.
     */
    public void setName(String name) {
        this.name = name;
        setDirty();
    }

    // ------------------------------------------------------------------------
    /**
     * Reserved for future use.
     */
    public String getName() {
        return name;
    }

    // ------------------------------------------------------------------------
    /**
     * Reserved for future use.
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        setDirty();
    }

    // ------------------------------------------------------------------------
    /**
     * Reserved for future use.
     */
    public String getDisplayName() {
        return displayName;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the appearance of this horse.
     *
     * @param appearance the appearance of the horse, including colour, style
     *        and variant.
     */
    public void setAppearance(String appearance) {
        this.appearance = appearance;
        setDirty();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the appearance of the horse, including colour, style and variant.
     *
     * @return the appearance of the horse, including colour, style and variant.
     */
    public String getAppearance() {
        return appearance;
    }

    // ------------------------------------------------------------------------
    /**
     * Reserved for future use.
     */
    public void setEquipment(int equipment) {
        this.equipment = equipment;
        setDirty();
    }

    // ------------------------------------------------------------------------
    /**
     * Reserved for future use.
     */
    public int getEquipment() {
        return equipment;
    }

    // ------------------------------------------------------------------------
    /**
     * Reserved for future use.
     */
    public void setLocation(String location) {
        this.location = location;
        setDirty();
    }

    // ------------------------------------------------------------------------
    /**
     * Reserved for future use.
     */
    public String getLocation() {
        return location;
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
     * Set the hydration of this horse from 0.0 (dehydrated) to 1.0 (fully
     * hydrated).
     *
     * @param hydration in the range [0.0, 1.0].
     */
    public void setHydration(double hydration) {
        this.hydration = (hydration > 1.0 ? 1.0 : (hydration < 0.0 ? 0.0 : hydration));
        setDirty();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the hydration level of this horse in the range 0.0 (dehydrated) to
     * 1.0 (fully hydrated).
     *
     * @return the hydration level of this horse in the range 0.0 (dehydrated)
     *         to 1.0 (fully hydrated).
     */
    public double getHydration() {
        return hydration;
    }

    // ------------------------------------------------------------------------
    /**
     * This method is called every tick when the horse is being ridden to do
     * various accounting tasks.
     *
     * CAUTION: this method may throw the rider off the horse. Make it the last
     * call in the movement event handler. This method should only be called
     * when a Player is riding the horse, i.e. in onPlayerMove().
     *
     * @param relativeTick a counter that increases by one every tick; the
     *        starting value is arbitrary.
     * @param horse the Horse entity.
     */
    public void onRidden(int relativeTick, Horse horse) {
        if (_lastLocation != null) {
            double dist = Util.getHorizontalDistance(_lastLocation, horse.getLocation());
            _lastLocation = horse.getLocation();

            setHydration(getHydration() - (dist / EasyRider.CONFIG.DEHYDRATION_DISTANCE));
            if (getHydration() < 0.001) {
                Player rider = (Player) horse.getPassenger();
                rider.sendMessage(ChatColor.RED + "This horse is too dehydrated to ride. Give it a bucket of water.");
                horse.eject();
                return;
            }
        }
        _lastLocation = horse.getLocation();
    }

    // ------------------------------------------------------------------------
    /**
     * Load this horse from the specified section of a YAML file.
     *
     * @param section the ConfigurationSection.
     */
    public void load(ConfigurationSection section) {
        setUuid(UUID.fromString(section.getName()));
        setOwnerUuid((section.isSet("ownerUuid")) ? UUID.fromString(section.getString("ownerUuid")) : null);
        setName(section.getString("name"));
        setDisplayName(section.getString("displayName"));
        setAppearance(section.getString("appearance"));
        setLocation(section.getString("location"));
        setEquipment(section.getInt("equipment"));
        setDistanceTravelled(section.getDouble("distanceTravelled"));
        setDistanceJumped(section.getDouble("distanceJumped"));
        setNuggetsEaten(section.getInt("nuggetsEaten"));
        setSpeedLevel(section.getInt("speedLevel"));
        setJumpLevel(section.getInt("jumpLevel"));
        setHealthLevel(section.getInt("healthLevel"));
        setHydration(section.getDouble("hydration", 1.0));
        setClean();
    }

    // ------------------------------------------------------------------------
    /**
     * Save this horse to a YAML configuration.
     *
     * @param section the parent section that contains a section named after
     *        this horse's UUID. The latter contains the attributes of this
     *        horse.
     */
    public void save(ConfigurationSection parent) {
        ConfigurationSection section = parent.getConfigurationSection(getUuid().toString());
        if (section == null) {
            section = parent.createSection(getUuid().toString());
        }

        // Setting a value of null removes a key.
        section.set("ownerUuid", (getOwnerUuid() != null) ? getOwnerUuid().toString() : null);

        section.set("name", getName());
        section.set("displayName", getDisplayName());
        section.set("appearance", getAppearance());
        section.set("location", getLocation());
        section.set("equipment", getEquipment());
        section.set("distanceTravelled", getDistanceTravelled());
        section.set("distanceJumped", getDistanceJumped());
        section.set("nuggetsEaten", getNuggetsEaten());
        section.set("speedLevel", getSpeedLevel());
        section.set("jumpLevel", getJumpLevel());
        section.set("healthLevel", getHealthLevel());
        section.set("hydration", getHydration());
        setClean();
    }

    // --------------------------------------------------------------------------
    /**
     * @see java.lang.Object#hashCode()
     *
     *      Generated by Eclipse IDE.
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((appearance == null) ? 0 : appearance.hashCode());
        result = prime * result + ((displayName == null) ? 0 : displayName.hashCode());
        long temp;
        temp = Double.doubleToLongBits(distanceJumped);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(distanceTravelled);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + equipment;
        result = prime * result + healthLevel;
        result = prime * result + jumpLevel;
        result = prime * result + ((location == null) ? 0 : location.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + nuggetsEaten;
        result = prime * result + ((ownerUuid == null) ? 0 : ownerUuid.hashCode());
        result = prime * result + speedLevel;
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
        return result;
    } // hashCode

    // --------------------------------------------------------------------------
    /**
     * @see java.lang.Object#equals(java.lang.Object)
     *
     *      Generated by Eclipse IDE.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof SavedHorse)) {
            return false;
        }
        SavedHorse other = (SavedHorse) obj;
        if (appearance == null) {
            if (other.appearance != null) {
                return false;
            }
        } else if (!appearance.equals(other.appearance)) {
            return false;
        }
        if (displayName == null) {
            if (other.displayName != null) {
                return false;
            }
        } else if (!displayName.equals(other.displayName)) {
            return false;
        }
        if (Double.doubleToLongBits(distanceJumped) != Double.doubleToLongBits(other.distanceJumped)) {
            return false;
        }
        if (Double.doubleToLongBits(distanceTravelled) != Double.doubleToLongBits(other.distanceTravelled)) {
            return false;
        }
        if (equipment != other.equipment) {
            return false;
        }
        if (healthLevel != other.healthLevel) {
            return false;
        }
        if (jumpLevel != other.jumpLevel) {
            return false;
        }
        if (location == null) {
            if (other.location != null) {
                return false;
            }
        } else if (!location.equals(other.location)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (nuggetsEaten != other.nuggetsEaten) {
            return false;
        }
        if (ownerUuid == null) {
            if (other.ownerUuid != null) {
                return false;
            }
        } else if (!ownerUuid.equals(other.ownerUuid)) {
            return false;
        }
        if (speedLevel != other.speedLevel) {
            return false;
        }
        if (uuid == null) {
            if (other.uuid != null) {
                return false;
            }
        } else if (!uuid.equals(other.uuid)) {
            return false;
        }
        return true;
    } // equals

    // ------------------------------------------------------------------------
    /**
     * The unique ID of the horse, used as the primary key.
     */
    @Id
    private UUID uuid;

    /**
     * The owning player's UUID, or null if not owned.
     */
    private UUID ownerUuid;

    /**
     * The name of the horse.
     *
     * Not currently used; reserved for future use.
     */
    @Column(length = 32)
    private String name;

    /**
     * The display name (custom name) of the horse.
     *
     * Not currently used; reserved for future use.
     */
    @Column(length = 32)
    private String displayName;

    /**
     * The horse's appearance: colour, markings and variant.
     */
    @Column(length = 32)
    private String appearance;

    /**
     * The last known location of the horse: world,x,y,z as a string.
     *
     * Not currently used; reserved for future use.
     */
    @Column(length = 40)
    private String location;

    /**
     * The equipment of the horse (saddle, armour) expressed as bit flags.
     *
     * Not currently used; reserved for future use.
     */
    @NotNull
    private int equipment;

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
     * Hydration level of the horse.
     */
    @NotNull
    private double hydration;

    /**
     * True if this bean has never been in the database, i.e. it will result in
     * a database insert.
     */
    @Transient
    private boolean _new;

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

    /**
     * The location of the horse in the last call to onRidden(), or null if not
     * called before.
     */
    @Transient
    private Location _lastLocation;
} // class SavedHorse