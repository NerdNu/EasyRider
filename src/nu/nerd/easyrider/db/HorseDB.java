package nu.nerd.easyrider.db;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.UUID;

import javax.persistence.PersistenceException;

import org.bukkit.entity.Horse;

import com.avaje.ebean.EbeanServer;

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
     * Add the specified Horse to the database cache.
     * 
     * @param horse
     */
    public SavedHorse addHorse(Horse horse) {
        SavedHorse savedHorse = findHorse(horse);
        if (savedHorse == null) {
            savedHorse = new SavedHorse(horse);
            _cache.put(savedHorse.getUuid(), savedHorse);
        }
        return savedHorse;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the SavedHorse coresponding to the in-game Horse entity, or null
     * if not stored in the database.
     * 
     * @param horse the Horse to find.
     * @return the corresponding database entry, or null if never saved.
     */
    public SavedHorse findHorse(Horse horse) {
        return _cache.get(horse.getUniqueId());
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
                double h1Level = ability.getLevelForEffort(h1);
                double h2Level = ability.getLevelForEffort(h2);
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
     * Load all horses into the in-memory cache.
     * 
     * On the first run, initialise the schema.
     */
    public void load() {
        long start = System.nanoTime();
        try {
            getDatabase().find(SavedHorse.class).findRowCount();
            for (SavedHorse h : getDatabase().find(SavedHorse.class).findList()) {
                _cache.put(h.getUuid(), h);
            }
        } catch (PersistenceException ex) {
            EasyRider.PLUGIN.getLogger().info("First run, initialising database.");
            EasyRider.PLUGIN.installDDL();
        }
        double millis = 1e-6 * (System.nanoTime() - start);
        EasyRider.PLUGIN.getLogger().info("Load time: " + millis + " ms");
    }

    // --------------------------------------------------------------------------
    /**
     * Save all updated horses to the database.
     */
    public void save() {
        long start = System.nanoTime();
        getDatabase().beginTransaction();
        try {
            for (SavedHorse horse : _cache.values()) {
                if (horse.isDirty()) {
                    getDatabase().save(horse);
                    horse.setClean();
                }
            }
            getDatabase().commitTransaction();
        } catch (Exception ex) {
            EasyRider.PLUGIN.getLogger().severe("Error saving horses: " + ex.getMessage());

        } finally {
            getDatabase().endTransaction();
        }
        double millis = 1e-6 * (System.nanoTime() - start);
        EasyRider.PLUGIN.getLogger().info("Save time: " + millis + " ms");
    }

    // ------------------------------------------------------------------------
    /**
     * Return the Ebeans database.
     * 
     * @return the Ebeans database.
     */
    protected EbeanServer getDatabase() {
        return EasyRider.PLUGIN.getDatabase();
    }

    // ------------------------------------------------------------------------
    /**
     * Known horses.
     */
    protected HashMap<UUID, SavedHorse> _cache = new HashMap<UUID, SavedHorse>();

} // class HorseDB