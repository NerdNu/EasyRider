package nu.nerd.easyrider.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Horse;

import nu.nerd.easyrider.EasyRider;

// ----------------------------------------------------------------------------
/**
 * Encapsulates {@link SavedHorse} database access.
 * 
 * The current implementation makes no effort to save the database until the
 * save() method is called when the plugin is disabled.
 */
public class HorseDB {
    // ------------------------------------------------------------------------
    /**
     * Constructor.
     *
     * @param implType identifies the database implementation; one of "sqlite",
     *        "yaml" or "sqlite+yaml". If an invalid identifier is specified,
     *        the implementation defaults to "yaml".
     */
    public HorseDB(String implType) {
        _impl = makeHorseDBImpl(implType);
        if (_impl == null) {
            _impl = new HorseDBImplWithYAML();
            EasyRider.PLUGIN.getLogger().severe("Invalid database implementation: \"" +
                                                implType + "\" defaulting to \"yaml\".");
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Find the specified Horse in the database cache, adding it as necessary.
     * 
     * @param horse the Horse entity.
     */
    public synchronized SavedHorse findOrAddHorse(Horse horse) {
        SavedHorse savedHorse = findHorse(horse);
        if (savedHorse == null) {
            savedHorse = new SavedHorse(horse);
            _cache.put(savedHorse.getUuid(), savedHorse);

            savedHorse.setDistanceTravelled(0);
            savedHorse.setDistanceJumped(0);
            savedHorse.setNuggetsEaten(0);
            savedHorse.setSpeedLevel(1);
            savedHorse.setJumpLevel(1);
            savedHorse.setHealthLevel(1);
            savedHorse.updateAllAttributes(horse);
        }
        return savedHorse;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the SavedHorse corresponding to the in-game Horse entity, or null
     * if not stored in the database.
     * 
     * @param horse the Horse to find.
     * @return the corresponding database entry, or null if never saved.
     */
    public synchronized SavedHorse findHorse(Horse horse) {
        return _cache.get(horse.getUniqueId());
    }

    // --------------------------------------------------------------------------
    /**
     * Return a list of all horses whose UUID begins with the specified prefix.
     *
     * @param uuidPrefix the case insensitive UUID prefix to search for.
     * @return a list of all horses whose UUID begins with the specified prefix.
     */
    public synchronized List<SavedHorse> findHorsesByUUID(String uuidPrefix) {
        uuidPrefix = uuidPrefix.toLowerCase();
        ArrayList<SavedHorse> matches = new ArrayList<SavedHorse>();
        for (SavedHorse savedHorse : _cache.values()) {
            if (savedHorse.getUuid().toString().startsWith(uuidPrefix)) {
                matches.add(savedHorse);
            }
        }
        return matches;
    }

    // ------------------------------------------------------------------------
    /**
     * Return a deep copy of all SavedHorses in arbitrary order.
     *
     * The returned list can be acted upon in other threads.
     *
     * @return a deep copy of all SavedHorses in arbitrary order.
     */
    public synchronized ArrayList<SavedHorse> cloneAllHorses() {
        ArrayList<SavedHorse> horses = new ArrayList<SavedHorse>(_cache.size());
        for (SavedHorse savedHorse : _cache.values()) {
            try {
                horses.add((SavedHorse) savedHorse.clone());
            } catch (CloneNotSupportedException ex) {
                // Should never happen.
            }
        }
        return horses;
    }

    // --------------------------------------------------------------------------
    /**
     * Remove the specified horse from the cache, and queue up deletion from the
     * database.
     *
     * @param savedHorse the database state of the horse.
     */
    public synchronized void removeHorse(SavedHorse savedHorse) {
        _cache.remove(savedHorse.getUuid());
        _removedHorses.put(savedHorse.getUuid(), savedHorse);
        removeOwnedHorse(savedHorse.getOwnerUuid(), savedHorse);
    }

    // ------------------------------------------------------------------------
    /**
     * Return a non-null ArrayList<> of the horses owned by a player.
     *
     * @param player the player.
     * @return a non-null ArrayList<> of the horses owned by a player.
     */
    public ArrayList<SavedHorse> getOwnedHorses(OfflinePlayer player) {
        return getOwnedHorses(player.getUniqueId());
    }

    // ------------------------------------------------------------------------
    /**
     * Return a non-null ArrayList<> of the horses owned by the player with the
     * specified UUID.
     *
     * @param ownerUuid the owning player's UUID.
     * @return the horses owned by the player with the specified UUID.
     */
    public ArrayList<SavedHorse> getOwnedHorses(UUID ownerUuid) {
        // Remove horses that have changed to a different owner.
        TreeSet<SavedHorse> horses = getOwnedHorsesSet(ownerUuid);
        for (Iterator<SavedHorse> it = horses.iterator(); it.hasNext();) {
            SavedHorse savedHorse = it.next();
            if (!ownerUuid.equals(savedHorse.getOwnerUuid())) {
                it.remove();
            }
        }

        return new ArrayList<SavedHorse>(horses);
    }

    // ------------------------------------------------------------------------
    /**
     * Update the SavedHorse to reflect the current state of the Horse Entity as
     * it is observed in the world and update the mapping from owners to sets of
     * owned horses.
     *
     * @see SavedHorse#observe(Horse)
     * @param savedHorse the database state of the horse.
     * @param horse the Horse Entity.
     */
    public void observe(SavedHorse savedHorse, Horse horse) {
        UUID oldOwnerUuid = savedHorse.getOwnerUuid();
        AnimalTamer owner = horse.getOwner();
        UUID newOwnerUuid = (owner == null) ? null : owner.getUniqueId();
        if (oldOwnerUuid == null) {
            if (newOwnerUuid != null) {
                savedHorse.setOwnerUuid(newOwnerUuid);
                addOwnedHorse(newOwnerUuid, savedHorse);
            }
        } else {
            if (!oldOwnerUuid.equals(newOwnerUuid)) {
                removeOwnedHorse(oldOwnerUuid, savedHorse);
                addOwnedHorse(newOwnerUuid, savedHorse);
            }
        }
        savedHorse.observe(horse);
    }

    // ------------------------------------------------------------------------
    /**
     * Make a backup of the database, if that is possible (e.g. backed by a
     * file).
     */
    public synchronized void backup() {
        _impl.backup();
    }

    // ------------------------------------------------------------------------
    /**
     * Load all horses into the in-memory cache.
     * 
     * On the first run, initialise the schema.
     */
    public synchronized void load() {
        long now = System.currentTimeMillis();
        for (SavedHorse savedHorse : _impl.loadAll()) {
            _cache.put(savedHorse.getUuid(), savedHorse);
            addOwnedHorse(savedHorse.getOwnerUuid(), savedHorse);
        }

        long millis = System.currentTimeMillis() - now;
        EasyRider.PLUGIN.getLogger().info("Database load time: " + millis + " ms");
    }

    // --------------------------------------------------------------------------
    /**
     * Save all updated horses to the database.
     */
    public synchronized void save() {
        long start = System.currentTimeMillis();
        _impl.saveAll(_cache.values());

        long millis = System.currentTimeMillis() - start;
        EasyRider.PLUGIN.getLogger().info("Database save time: " + millis + " ms");
    }

    // --------------------------------------------------------------------------
    /**
     * Delete all removed horses from the database.
     */
    public synchronized void purgeAllRemovedHorses() {
        long start = System.nanoTime();
        _impl.delete(_removedHorses.values());
        _removedHorses.clear();

        double millis = 1e-6 * (System.nanoTime() - start);
        EasyRider.PLUGIN.getLogger().info("Database purge time: " + millis + " ms");
    }

    // --------------------------------------------------------------------------
    /**
     * Migrate the database to the specified implementation.
     *
     * @param sender the command sender.
     * @param string the database implementation type identifier.
     */
    public synchronized void migrate(CommandSender sender, String implType) {
        String oldImplType = _impl.getType();
        if (oldImplType.equals(implType)) {
            sender.sendMessage(ChatColor.RED + "The database implementation is already: " + implType);
            return;
        }

        IHorseDBImpl newImpl = makeHorseDBImpl(implType);
        if (newImpl == null) {
            sender.sendMessage(ChatColor.RED + "Invalid database implementation specified: " + implType);
            return;
        }

        // Write current implementation to disk.
        // Equivalent to save() and purgeAllRemovedHorses():
        _impl.saveAll(_cache.values());
        _impl.delete(_removedHorses.values());
        _removedHorses.clear();

        // Clear out any existing contents of the new database.
        newImpl.delete(newImpl.loadAll());

        // Mark all horses in the cache as new (to be inserted), then save.
        for (SavedHorse savedHorse : _cache.values()) {
            savedHorse.setNew();
        }
        newImpl.saveAll(_cache.values());

        // Update implementation reference and config setting.
        _impl = newImpl;
        EasyRider.CONFIG.DATABASE_IMPLEMENTATION = implType;
        EasyRider.CONFIG.save();

        sender.sendMessage(ChatColor.GOLD + "Database migrated from " + oldImplType + " to " + implType + ".");
    } // migrate

    // ------------------------------------------------------------------------
    /**
     * Create a database implementation of the specified type.
     * 
     * @param implType identifies the database implementation; one of "sqlite",
     *        "yaml" or "sqlite+yaml".
     * @return the implementation, or null if the type is invalid.
     */
    protected synchronized IHorseDBImpl makeHorseDBImpl(String implType) {
        switch (implType) {
        case "yaml":
            return new HorseDBImplWithYAML();
        default:
            return null;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return a non-null reference to the mutable set of SavedHorses belonging
     * to the player with the specified UUID.
     *
     * @param ownerUuid the UUID of the owning player.
     * @return the corresponding set of owned SavedHorses, which may be empty
     *         but will never be null.
     */

    protected TreeSet<SavedHorse> getOwnedHorsesSet(UUID ownerUuid) {
        TreeSet<SavedHorse> horses = _ownedHorses.get(ownerUuid);
        if (horses == null) {
            horses = new TreeSet<SavedHorse>((h1, h2) -> h1.getUuid().compareTo(h2.getUuid()));
            _ownedHorses.put(ownerUuid, horses);
        }
        return horses;
    }

    // ------------------------------------------------------------------------
    /**
     * Add the horse to the set of horses attributed to the owner.
     *
     * @param ownerUuid the owning player's UUID.
     * @param savedHorse the database horse.
     */
    protected void addOwnedHorse(UUID ownerUuid, SavedHorse savedHorse) {
        if (ownerUuid == null) {
            return;
        }

        getOwnedHorsesSet(ownerUuid).add(savedHorse);
    }

    // ------------------------------------------------------------------------
    /**
     * Remove the specified horse from the set of horses recorded as owned by
     * its current owner.
     *
     * @param ownerUuid the owning player's UUID.
     * @param savedHorse the database horse.
     */
    protected void removeOwnedHorse(UUID ownerUuid, SavedHorse savedHorse) {
        if (ownerUuid == null) {
            return;
        }

        getOwnedHorsesSet(ownerUuid).remove(savedHorse);
    }

    // ------------------------------------------------------------------------
    /**
     * Database implementation.
     */
    protected IHorseDBImpl _impl;

    /**
     * Known horses.
     */
    protected HashMap<UUID, SavedHorse> _cache = new HashMap<UUID, SavedHorse>();

    /**
     * Horses that must be removed from the database.
     */
    protected HashMap<UUID, SavedHorse> _removedHorses = new HashMap<UUID, SavedHorse>();

    /**
     * Map from owner UUID to the set of horses owned by that player.
     *
     * Each set of horses is a TreeSet<> that orders the entries in ascending
     * order by Horse Entity UUID. The set may be transiently incorrect when
     * ownership changes. Extra horses will be removed from the set when
     * returned by {#link getOwnedHorses()}.
     */
    protected HashMap<UUID, TreeSet<SavedHorse>> _ownedHorses = new HashMap<UUID, TreeSet<SavedHorse>>();
} // class HorseDB