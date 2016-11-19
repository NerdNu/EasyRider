package nu.nerd.easyrider.db;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Horse;

import nu.nerd.easyrider.Ability;
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
    public SavedHorse findOrAddHorse(Horse horse) {
        SavedHorse savedHorse = findHorse(horse);
        if (savedHorse == null) {
            savedHorse = new SavedHorse(horse);
            _cache.put(savedHorse.getUuid(), savedHorse);

            EasyRider.CONFIG.SPEED.setLevel(savedHorse, 1);
            EasyRider.CONFIG.SPEED.setEffort(savedHorse, 0);
            EasyRider.CONFIG.SPEED.updateAttributes(savedHorse, horse);
            EasyRider.CONFIG.JUMP.setLevel(savedHorse, 1);
            EasyRider.CONFIG.JUMP.setEffort(savedHorse, 0);
            EasyRider.CONFIG.JUMP.updateAttributes(savedHorse, horse);
            EasyRider.CONFIG.HEALTH.setLevel(savedHorse, 1);
            EasyRider.CONFIG.HEALTH.setEffort(savedHorse, 0);
            EasyRider.CONFIG.HEALTH.updateAttributes(savedHorse, horse);
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
    public SavedHorse findHorse(Horse horse) {
        return _cache.get(horse.getUniqueId());
    }

    // --------------------------------------------------------------------------
    /**
     * Return a list of all horses whose UUID begins with the specified prefix.
     *
     * @param prefix the case insensitive UUID prefix to search for.
     * @return a list of all horses whose UUID begins with the specified prefix.
     */
    public List<SavedHorse> findAllHorses(String prefix) {
        prefix = prefix.toLowerCase();
        ArrayList<SavedHorse> matches = new ArrayList<SavedHorse>();
        for (SavedHorse savedHorse : _cache.values()) {
            if (savedHorse.getUuid().toString().startsWith(prefix)) {
                matches.add(savedHorse);
            }
        }
        return matches;
    }

    // --------------------------------------------------------------------------
    /**
     * Remove the specified horse from the cache when it dies, and queue up
     * deletion from the database.
     *
     * @param savedHorse the database state of the horse.
     */
    public void removeDeadHorse(SavedHorse savedHorse) {
        _cache.remove(savedHorse.getUuid());
        _removedHorses.put(savedHorse.getUuid(), savedHorse);
    }

    // ------------------------------------------------------------------------
    /**
     * Return a specified number of the top horses ranked in descending order of
     * the specified Ability.
     *
     * @param ability the Ability.
     * @param count the maximum number of ranked horses to return.
     * @return the top count horses in descending order of that ability.
     */
    public SavedHorse[] rank(Ability ability, int count) {
        // Comparator ensures best horse is first, worst is last.
        TreeSet<SavedHorse> rankings = new TreeSet<SavedHorse>(new Comparator<SavedHorse>() {
            @Override
            public int compare(SavedHorse h1, SavedHorse h2) {
                double h1Level = ability.getLevelForEffort(ability.getEffort(h1));
                double h2Level = ability.getLevelForEffort(ability.getEffort(h2));
                if (h1Level < h2Level) {
                    return 1;
                } else if (h2Level < h1Level) {
                    return -1;
                } else {
                    return h1.getUuid().compareTo(h2.getUuid());
                }
            }
        });

        for (SavedHorse horse : _cache.values()) {
            rankings.add(horse);
            if (rankings.size() > count) {
                Iterator<SavedHorse> it = rankings.descendingIterator();
                if (it.hasNext()) {
                    it.next();
                    it.remove();
                }
            }
        }

        SavedHorse[] result = new SavedHorse[Math.min(count, rankings.size())];
        return rankings.toArray(result);
    }

    // ------------------------------------------------------------------------
    /**
     * Make a backup of the database, if that is possible (e.g. backed by a
     * file).
     */
    public void backup() {
        _impl.backup();
    }

    // ------------------------------------------------------------------------
    /**
     * Load all horses into the in-memory cache.
     * 
     * On the first run, initialise the schema.
     */
    public void load() {
        long start = System.nanoTime();
        for (SavedHorse savedHorse : _impl.loadAll()) {
            _cache.put(savedHorse.getUuid(), savedHorse);
        }

        double millis = 1e-6 * (System.nanoTime() - start);
        EasyRider.PLUGIN.getLogger().info("Database load time: " + millis + " ms");
    }

    // --------------------------------------------------------------------------
    /**
     * Save all updated horses to the database.
     */
    public void save() {
        long start = System.nanoTime();
        _impl.saveAll(_cache.values());

        double millis = 1e-6 * (System.nanoTime() - start);
        EasyRider.PLUGIN.getLogger().info("Database save time: " + millis + " ms");
    }

    // --------------------------------------------------------------------------
    /**
     * Delete all removed horses from the database.
     */
    public void purgeAllRemovedHorses() {
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
    public void migrate(CommandSender sender, String implType) {
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
    protected IHorseDBImpl makeHorseDBImpl(String implType) {
        switch (implType) {
        case "sqlite":
            return new HorseDBImplWithSqlite();
        case "yaml":
            return new HorseDBImplWithYAML();
        case "sqlite+yaml":
            return new HorseDBImplWithSqliteAndYAML();
        default:
            return null;
        }
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

} // class HorseDB